package lt.partijapro.pricing;

public enum PriceFlag {
    LOSS_RISK("nuostolinga rizika"),     // R5
    NEEDS_REVIEW("reikia peržiūrėti");   // R4

    private final String label;

    PriceFlag(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
