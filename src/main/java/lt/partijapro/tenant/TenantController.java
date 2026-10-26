package lt.partijapro.tenant;

import lt.partijapro.batch.BatchController;
import lt.partijapro.batch.Condition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

// lookups for the UI. Tenant list is instead of login for now (login is stage III)
@RestController
@RequestMapping("/api")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/tenants")
    public List<TenantView> tenants() {
        return tenantService.findAll().stream()
                .map(t -> new TenantView(t.getId(), t.getName(), t.pricingSettings().targetMarkup(),
                        t.pricingSettings().minMarkup(), t.pricingSettings().channelFee()))
                .toList();
    }

    @GetMapping("/categories")
    public List<CategoryView> categories(@RequestHeader(BatchController.TENANT_HEADER) String tenantId) {
        return tenantService.categories(tenantId).stream()
                .map(c -> new CategoryView(c.getName(), c.getCoefficient()))
                .toList();
    }

    @GetMapping("/conditions")
    public List<ConditionView> conditions() {
        return Arrays.stream(Condition.values())
                .map(c -> new ConditionView(c.name(), c.label(), c.coefficient()))
                .toList();
    }

    public record TenantView(String id, String name, BigDecimal targetMarkup, BigDecimal minMarkup,
                             BigDecimal channelFee) {
    }

    public record CategoryView(String name, BigDecimal coefficient) {
    }

    public record ConditionView(String code, String label, BigDecimal coefficient) {
    }
}
