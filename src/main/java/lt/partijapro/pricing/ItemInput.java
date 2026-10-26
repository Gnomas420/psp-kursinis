package lt.partijapro.pricing;

import java.math.BigDecimal;

// weight = category coef * condition coef, manualCost and marketPrice can be null
public record ItemInput(BigDecimal weight, BigDecimal manualCost, BigDecimal marketPrice) {

    public ItemInput {
        if (weight == null || weight.signum() < 0) {
            throw new PricingException("Prekės svoris negali būti neigiamas");
        }
        if (manualCost != null && manualCost.signum() < 0) {
            throw new PricingException("Rankinė savikaina negali būti neigiama");
        }
        if (marketPrice != null && marketPrice.signum() < 0) {
            throw new PricingException("Rinkos kaina negali būti neigiama");
        }
    }

    public static ItemInput weighted(String weight) {
        return new ItemInput(new BigDecimal(weight), null, null);
    }

    public boolean hasManualCost() {
        return manualCost != null;
    }
}
