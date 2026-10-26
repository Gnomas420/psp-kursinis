package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.util.List;

// items are in the same order as in BatchInput
public record PricingResult(BigDecimal totalCost, List<ItemPricing> items) {

    public PricingResult {
        items = List.copyOf(items);
    }
}
