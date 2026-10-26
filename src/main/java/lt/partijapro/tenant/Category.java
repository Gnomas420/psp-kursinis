package lt.partijapro.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

// tenant's category with coefficient, e.g. striukė 3, džinsai 2
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal coefficient;

    protected Category() {
    }

    public Category(String tenantId, String name, BigDecimal coefficient) {
        this.tenantId = tenantId;
        this.name = name;
        this.coefficient = coefficient;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCoefficient() {
        return coefficient;
    }
}
