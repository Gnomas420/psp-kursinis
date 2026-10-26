package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record BatchInput(BigDecimal purchasePrice, BigDecimal shippingCost, BigDecimal otherCosts,
                         List<ItemInput> items) {

    public BatchInput {
        requireNonNegative(purchasePrice, "Pirkimo kaina");
        requireNonNegative(shippingCost, "Pristatymo kaina");
        requireNonNegative(otherCosts, "Kitos išlaidos");
        if (items == null || items.isEmpty()) {
            throw new PricingException("Partijoje turi būti bent viena prekė");
        }
        items = List.copyOf(items);
    }

    // S = purchase + shipping + other costs
    public BigDecimal totalCost() {
        return purchasePrice.add(shippingCost).add(otherCosts).setScale(2, RoundingMode.HALF_UP);
    }

    private static void requireNonNegative(BigDecimal amount, String name) {
        if (amount == null || amount.signum() < 0) {
            throw new PricingException(name + " turi būti nurodyta ir negali būti neigiama");
        }
    }
}
