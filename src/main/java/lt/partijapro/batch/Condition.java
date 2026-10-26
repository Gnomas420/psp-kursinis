package lt.partijapro.batch;

import java.math.BigDecimal;

// item condition + coefficient, weight = category coef * condition coef
public enum Condition {
    NAUJA("nauja", "1.2"),
    LABAI_GERA("labai gera", "1.0"),
    GERA("gera", "0.8"),
    PATENKINAMA("patenkinama", "0.6");

    private final String label;
    private final BigDecimal coefficient;

    Condition(String label, String coefficient) {
        this.label = label;
        this.coefficient = new BigDecimal(coefficient);
    }

    public String label() {
        return label;
    }

    public BigDecimal coefficient() {
        return coefficient;
    }
}
