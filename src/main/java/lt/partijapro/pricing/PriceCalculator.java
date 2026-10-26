package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Set;

// R3 + R5
public class PriceCalculator {

    public ItemPricing price(BigDecimal cost, BigDecimal marketPrice, PricingSettings settings) {
        BigDecimal recommended = priceWithMarkup(cost, settings.targetMarkup(), settings.channelFee());
        BigDecimal min = priceWithMarkup(cost, settings.minMarkup(), settings.channelFee());
        Set<PriceFlag> flags = EnumSet.noneOf(PriceFlag.class);

        // R5: lower market price -> use it, but never below min price
        if (marketPrice != null && marketPrice.compareTo(recommended) < 0) {
            recommended = marketPrice.max(min).setScale(2, RoundingMode.HALF_UP);
            if (marketPrice.compareTo(min) < 0) {
                flags.add(PriceFlag.LOSS_RISK);
            }
        }
        return new ItemPricing(cost, recommended, min, flags);
    }

    // R3: price = cost * (1 + markup) / (1 - channel fee), rounded up to euro
    BigDecimal priceWithMarkup(BigDecimal cost, BigDecimal markup, BigDecimal channelFee) {
        BigDecimal price = cost.multiply(BigDecimal.ONE.add(markup))
                .divide(BigDecimal.ONE.subtract(channelFee), 10, RoundingMode.HALF_UP);
        return Money.ceilToEuro(price);
    }
}
