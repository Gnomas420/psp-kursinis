package lt.partijapro.pricing;

import java.math.BigDecimal;

// values are fractions: 1.00 = 100%, 0.20 = 20%
public record PricingSettings(BigDecimal targetMarkup, BigDecimal minMarkup, BigDecimal channelFee) {

    public PricingSettings {
        if (minMarkup.signum() < 0 || minMarkup.compareTo(targetMarkup) > 0) {
            throw new PricingException("Minimalus antkainis turi būti tarp 0 ir tikslinio antkainio");
        }
        if (channelFee.signum() < 0 || channelFee.compareTo(BigDecimal.ONE) >= 0) {
            throw new PricingException("Kanalo mokestis turi būti nuo 0 iki 100 %");
        }
    }

    // defaults from the design doc: 100% / 20% / 0% (Vinted)
    public static PricingSettings defaults() {
        return new PricingSettings(new BigDecimal("1.00"), new BigDecimal("0.20"), BigDecimal.ZERO);
    }
}
