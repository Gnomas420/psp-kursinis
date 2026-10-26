package lt.partijapro.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lt.partijapro.pricing.PricingSettings;

import java.math.BigDecimal;

// tenant = organization, also keeps its pricing settings
@Entity
@Table(name = "tenants")
public class Tenant {

    // short id like "lobis", sent in X-Tenant-Id header
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal targetMarkup;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal minMarkup;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal channelFee;

    protected Tenant() {
    }

    public Tenant(String id, String name, PricingSettings settings) {
        this.id = id;
        this.name = name;
        this.targetMarkup = settings.targetMarkup();
        this.minMarkup = settings.minMarkup();
        this.channelFee = settings.channelFee();
    }

    public PricingSettings pricingSettings() {
        return new PricingSettings(targetMarkup, minMarkup, channelFee);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
