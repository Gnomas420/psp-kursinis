package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

// Strategy - how price drops over time for unsold items (stage III: seasonal policy)
public interface MarkdownPolicy {

    MarkdownSchedule schedule(BigDecimal startPrice, BigDecimal minPrice, LocalDate listedOn, LocalDate date);
}
