package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.util.List;

// Strategy - how batch cost S is split between items.
// New rule (e.g. by market price) = new class, PricingEngine stays the same.
public interface CostAllocationStrategy {

    // returns cost for each item (same order), sum must be equal to totalCost
    List<BigDecimal> allocate(BigDecimal totalCost, List<ItemInput> items);
}
