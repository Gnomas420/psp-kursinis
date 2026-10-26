package lt.partijapro.batch;

import lt.partijapro.pricing.MarkdownSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// one row of the "reprice today" list
public record RepricingItem(Long itemId, String category, LocalDate listedOn, BigDecimal listedPrice,
                            BigDecimal newPrice, BigDecimal minPrice, boolean needsReview,
                            List<MarkdownSchedule.Step> steps) {
}
