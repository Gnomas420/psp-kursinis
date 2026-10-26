package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// steps = all price drops until the date, currentPrice = price on that date
public record MarkdownSchedule(List<Step> steps, BigDecimal currentPrice, boolean needsReview) {

    public MarkdownSchedule {
        steps = List.copyOf(steps);
    }

    public record Step(LocalDate date, BigDecimal price) {
    }
}
