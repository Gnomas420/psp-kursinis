package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Main module. Depends only on the strategy interfaces, no Spring / DB here,
// so it can be tested with plain JUnit.
public class PricingEngine {

    private final CostAllocationStrategy costAllocation;
    private final MarkdownPolicy markdownPolicy;
    private final PriceCalculator priceCalculator = new PriceCalculator();

    public PricingEngine(CostAllocationStrategy costAllocation, MarkdownPolicy markdownPolicy) {
        this.costAllocation = costAllocation;
        this.markdownPolicy = markdownPolicy;
    }

    // R1, R2 (cost) + R3, R5 (prices)
    public PricingResult calculate(BatchInput batch, PricingSettings settings) {
        BigDecimal totalCost = batch.totalCost();
        List<BigDecimal> costs = costAllocation.allocate(totalCost, batch.items());

        List<ItemPricing> items = new ArrayList<>();
        for (int i = 0; i < costs.size(); i++) {
            items.add(priceCalculator.price(costs.get(i), batch.items().get(i).marketPrice(), settings));
        }
        return new PricingResult(totalCost, items);
    }

    // R4
    public MarkdownSchedule markdown(BigDecimal price, BigDecimal minPrice, LocalDate listedOn, LocalDate date) {
        return markdownPolicy.schedule(price, minPrice, listedOn, date);
    }
}
