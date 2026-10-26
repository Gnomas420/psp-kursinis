package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// R4: every N days price drops by X% from current price (rounded down to euro),
// but not below min price. When min is reached -> needs review
public class FixedIntervalMarkdownPolicy implements MarkdownPolicy {

    private final int intervalDays;
    private final BigDecimal reduction;

    public FixedIntervalMarkdownPolicy(int intervalDays, BigDecimal reduction) {
        if (intervalDays <= 0 || reduction.signum() <= 0 || reduction.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Bad markdown params");
        }
        this.intervalDays = intervalDays;
        this.reduction = reduction;
    }

    // from the design doc: -10% every 14 days
    public static FixedIntervalMarkdownPolicy defaults() {
        return new FixedIntervalMarkdownPolicy(14, new BigDecimal("0.10"));
    }

    @Override
    public MarkdownSchedule schedule(BigDecimal startPrice, BigDecimal minPrice, LocalDate listedOn, LocalDate date) {
        long periods = date.isBefore(listedOn) ? 0 : ChronoUnit.DAYS.between(listedOn, date) / intervalDays;
        List<MarkdownSchedule.Step> steps = new ArrayList<>();
        BigDecimal price = startPrice;

        for (int k = 1; k <= periods && price.compareTo(minPrice) > 0; k++) {
            BigDecimal reduced = Money.floorToEuro(price.multiply(BigDecimal.ONE.subtract(reduction)));
            price = reduced.max(minPrice);
            steps.add(new MarkdownSchedule.Step(listedOn.plusDays((long) k * intervalDays), price));
        }

        boolean needsReview = periods > 0 && price.compareTo(minPrice) <= 0;
        return new MarkdownSchedule(steps, price, needsReview);
    }
}
