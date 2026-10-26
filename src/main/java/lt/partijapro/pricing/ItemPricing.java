package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.util.Set;

public record ItemPricing(BigDecimal cost, BigDecimal recommendedPrice, BigDecimal minPrice, Set<PriceFlag> flags) {

    public ItemPricing {
        flags = Set.copyOf(flags);
    }
}
