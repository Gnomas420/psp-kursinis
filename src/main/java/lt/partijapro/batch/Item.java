package lt.partijapro.batch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lt.partijapro.pricing.ItemPricing;
import lt.partijapro.pricing.PriceFlag;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private Condition condition;

    @Column(precision = 12, scale = 2)
    private BigDecimal manualCost;

    @Column(precision = 12, scale = 2)
    private BigDecimal marketPrice;

    private LocalDate listedOn;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(precision = 12, scale = 2)
    private BigDecimal recommendedPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal minPrice;

    private boolean lossRisk;

    protected Item() {
    }

    public Item(String tenantId, String category, Condition condition, BigDecimal manualCost,
                BigDecimal marketPrice, LocalDate listedOn) {
        this.tenantId = tenantId;
        this.category = category;
        this.condition = condition;
        this.manualCost = manualCost;
        this.marketPrice = marketPrice;
        this.listedOn = listedOn;
    }

    public void applyPricing(ItemPricing pricing) {
        this.cost = pricing.cost();
        this.recommendedPrice = pricing.recommendedPrice();
        this.minPrice = pricing.minPrice();
        this.lossRisk = pricing.flags().contains(PriceFlag.LOSS_RISK);
    }

    void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public Condition getCondition() {
        return condition;
    }

    public BigDecimal getManualCost() {
        return manualCost;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public LocalDate getListedOn() {
        return listedOn;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public BigDecimal getRecommendedPrice() {
        return recommendedPrice;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public boolean isLossRisk() {
        return lossRisk;
    }
}
