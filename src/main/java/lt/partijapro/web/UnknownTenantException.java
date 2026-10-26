package lt.partijapro.web;

public class UnknownTenantException extends RuntimeException {

    public UnknownTenantException(String tenantId) {
        super("Nežinomas klientas: " + tenantId);
    }
}
