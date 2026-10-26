package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

// money helpers, everything is BigDecimal with 2 decimals
public final class Money {

    public static final BigDecimal CENT = new BigDecimal("0.01");

    private Money() {
    }

    public static BigDecimal of(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal floorToCent(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.FLOOR);
    }

    public static BigDecimal ceilToEuro(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.CEILING).setScale(2);
    }

    public static BigDecimal floorToEuro(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.FLOOR).setScale(2);
    }

    // 210 -> "210,00 €"
    public static String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " €";
    }
}
