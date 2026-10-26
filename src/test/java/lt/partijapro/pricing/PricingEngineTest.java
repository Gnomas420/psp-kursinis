package lt.partijapro.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static lt.partijapro.pricing.Money.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// tests for the main module, scenarios from design doc section 3.
// no Spring here, engine is created with plain "new"
class PricingEngineTest {

    private final PricingEngine engine =
            new PricingEngine(new WeightedCostAllocation(), FixedIntervalMarkdownPolicy.defaults());
    private final PricingSettings settings = PricingSettings.defaults();

    // scenario 1: typical batch
    @Test
    void typicalBatch() {
        List<ItemInput> items = new ArrayList<>();
        items.add(new ItemInput(new BigDecimal("3"), of("50"), null));
        repeat(items, 3, ItemInput.weighted("2"));
        repeat(items, 9, ItemInput.weighted("1"));

        PricingResult result = engine.calculate(new BatchInput(of("180"), of("20"), of("0"), items), settings);

        assertThat(result.totalCost()).isEqualTo(of("200.00"));
        ItemPricing jacket = result.items().get(0);
        ItemPricing jeans = result.items().get(1);
        ItemPricing tShirt = result.items().get(4);

        assertThat(jacket.cost()).isEqualTo(of("50.00"));
        assertThat(jeans.cost()).isEqualTo(of("20.00"));
        assertThat(tShirt.cost()).isEqualTo(of("10.00"));

        assertThat(jeans.recommendedPrice()).isEqualTo(of("40.00"));
        assertThat(jeans.minPrice()).isEqualTo(of("24.00"));
        assertThat(jacket.recommendedPrice()).isEqualTo(of("100.00"));
        assertThat(jacket.minPrice()).isEqualTo(of("60.00"));
        assertThat(tShirt.recommendedPrice()).isEqualTo(of("20.00"));
        assertThat(tShirt.minPrice()).isEqualTo(of("12.00"));

        assertThat(sumOfCosts(result)).isEqualTo(of("200.00"));
    }

    // scenario 2: rounding, sum must stay 100.00
    @Test
    void roundingKeepsEveryCent() {
        List<ItemInput> items = new ArrayList<>();
        repeat(items, 3, ItemInput.weighted("1"));

        PricingResult result = engine.calculate(new BatchInput(of("100"), of("0"), of("0"), items), settings);

        assertThat(result.items()).extracting(ItemPricing::cost)
                .containsExactly(of("33.34"), of("33.33"), of("33.33"));
        assertThat(sumOfCosts(result)).isEqualTo(of("100.00"));
    }

    // scenario 3: 70 days -> 36, 32, 28, 25, 24 (not 22)
    @Test
    void markdownStopsAtMinimumPrice() {
        LocalDate listedOn = LocalDate.of(2026, 9, 1);

        MarkdownSchedule schedule = engine.markdown(of("40"), of("24"), listedOn, listedOn.plusDays(70));

        assertThat(schedule.steps()).extracting(MarkdownSchedule.Step::price)
                .containsExactly(of("36"), of("32"), of("28"), of("25"), of("24"));
        assertThat(schedule.steps().get(0).date()).isEqualTo(listedOn.plusDays(14));
        assertThat(schedule.currentPrice()).isEqualTo(of("24"));
        assertThat(schedule.needsReview()).isTrue();
    }

    @Test
    void markdownAfterTwentyEightDays() {
        LocalDate listedOn = LocalDate.of(2026, 9, 1);

        MarkdownSchedule schedule = engine.markdown(of("40"), of("24"), listedOn, listedOn.plusDays(28));

        assertThat(schedule.currentPrice()).isEqualTo(of("32"));
        assertThat(schedule.needsReview()).isFalse();
    }

    @Test
    void noMarkdownBeforeFirstInterval() {
        LocalDate listedOn = LocalDate.of(2026, 9, 1);

        MarkdownSchedule schedule = engine.markdown(of("40"), of("24"), listedOn, listedOn.plusDays(13));

        assertThat(schedule.steps()).isEmpty();
        assertThat(schedule.currentPrice()).isEqualTo(of("40"));
    }

    // scenario 4: market price 9 < min 12
    @Test
    void marketPriceBelowMinimum() {
        List<ItemInput> items = List.of(new ItemInput(BigDecimal.ONE, null, of("9")));

        ItemPricing tShirt = engine.calculate(new BatchInput(of("10"), of("0"), of("0"), items), settings)
                .items().get(0);

        assertThat(tShirt.cost()).isEqualTo(of("10.00"));
        assertThat(tShirt.minPrice()).isEqualTo(of("12.00"));
        assertThat(tShirt.recommendedPrice()).isEqualTo(of("12.00"));
        assertThat(tShirt.flags()).isEqualTo(Set.of(PriceFlag.LOSS_RISK));
    }

    @Test
    void marketPriceBetweenMinimumAndRecommended() {
        List<ItemInput> items = List.of(new ItemInput(BigDecimal.ONE, null, of("15")));

        ItemPricing tShirt = engine.calculate(new BatchInput(of("10"), of("0"), of("0"), items), settings)
                .items().get(0);

        assertThat(tShirt.recommendedPrice()).isEqualTo(of("15.00"));
        assertThat(tShirt.flags()).isEmpty();
    }

    @Test
    void channelFeeIsIncludedInPrice() {
        PricingSettings withFee = new PricingSettings(new BigDecimal("0.80"), new BigDecimal("0.15"),
                new BigDecimal("0.05"));
        List<ItemInput> items = List.of(ItemInput.weighted("1"));

        ItemPricing item = engine.calculate(new BatchInput(of("20"), of("0"), of("0"), items), withFee)
                .items().get(0);

        assertThat(item.recommendedPrice()).isEqualTo(of("38.00"));
        assertThat(item.minPrice()).isEqualTo(of("25.00"));
    }

    // scenario 5: manual costs 210 > S 200
    @Test
    void manualCostsExceedingBatchCostAreRejected() {
        List<ItemInput> items = List.of(
                new ItemInput(BigDecimal.ONE, of("120"), null),
                new ItemInput(BigDecimal.ONE, of("90"), null),
                ItemInput.weighted("1"));

        assertThatThrownBy(() -> engine.calculate(new BatchInput(of("200"), of("0"), of("0"), items), settings))
                .isInstanceOf(PricingException.class)
                .hasMessage("Rankinės savikainos (210,00 €) viršija partijos savikainą (200,00 €)");
    }

    @Test
    void allManualCostsMustCoverBatchCost() {
        List<ItemInput> items = List.of(new ItemInput(BigDecimal.ONE, of("150"), null));

        assertThatThrownBy(() -> engine.calculate(new BatchInput(of("200"), of("0"), of("0"), items), settings))
                .isInstanceOf(PricingException.class)
                .hasMessageContaining("mažesnė už partijos savikainą");
    }

    @Test
    void emptyBatchIsRejected() {
        assertThatThrownBy(() -> new BatchInput(of("200"), of("0"), of("0"), List.of()))
                .isInstanceOf(PricingException.class)
                .hasMessage("Partijoje turi būti bent viena prekė");
    }

    private static void repeat(List<ItemInput> items, int count, ItemInput item) {
        for (int i = 0; i < count; i++) {
            items.add(item);
        }
    }

    private static BigDecimal sumOfCosts(PricingResult result) {
        return result.items().stream().map(ItemPricing::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
