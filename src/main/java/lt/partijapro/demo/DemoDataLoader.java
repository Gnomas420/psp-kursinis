package lt.partijapro.demo;

import lt.partijapro.batch.BatchService;
import lt.partijapro.batch.Condition;
import lt.partijapro.batch.CreateBatchRequest;
import lt.partijapro.batch.CreateBatchRequest.ItemRequest;
import lt.partijapro.pricing.PricingSettings;
import lt.partijapro.tenant.Category;
import lt.partijapro.tenant.CategoryRepository;
import lt.partijapro.tenant.Tenant;
import lt.partijapro.tenant.TenantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// demo data on startup: 2 tenants with different settings.
// batches go through the same BatchService, so prices are really calculated
@Component
@ConditionalOnProperty(name = "partijapro.demo-data", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {

    public static final String LOBIS = "lobis";
    public static final String SPINTA = "spinta";

    private final TenantRepository tenantRepository;
    private final CategoryRepository categoryRepository;
    private final BatchService batchService;
    private final Clock clock;

    public DemoDataLoader(TenantRepository tenantRepository, CategoryRepository categoryRepository,
                          BatchService batchService, Clock clock) {
        this.tenantRepository = tenantRepository;
        this.categoryRepository = categoryRepository;
        this.batchService = batchService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now(clock);

        // tenant 1: default settings 100% / 20% / 0%
        tenantRepository.save(new Tenant(LOBIS, "Vintažo lobis", PricingSettings.defaults()));
        addCategories(LOBIS, "striukė", "3", "džinsai", "2", "marškinėliai", "1", "suknelė", "2");

        batchService.create(LOBIS, new CreateBatchRequest("Outlet dėžė #1",
                money("180"), money("20"), BigDecimal.ZERO, List.of(
                new ItemRequest("striukė", Condition.LABAI_GERA, 1, money("50"), null, today.minusDays(3)),
                new ItemRequest("džinsai", Condition.LABAI_GERA, 3, null, null, today.minusDays(28)),
                new ItemRequest("marškinėliai", Condition.LABAI_GERA, 8, null, null, today.minusDays(3)),
                new ItemRequest("marškinėliai", Condition.LABAI_GERA, 1, null, money("9"), today.minusDays(3)))));

        batchService.create(LOBIS, new CreateBatchRequest("Urmo maišas (apvalinimas)",
                money("100"), BigDecimal.ZERO, BigDecimal.ZERO, List.of(
                new ItemRequest("marškinėliai", Condition.LABAI_GERA, 3, null, null, null))));

        // tenant 2: other markups, 5% channel fee, other coefficients
        tenantRepository.save(new Tenant(SPINTA, "Antra spinta", new PricingSettings(
                new BigDecimal("0.80"), new BigDecimal("0.15"), new BigDecimal("0.05"))));
        addCategories(SPINTA, "striukė", "4", "džinsai", "2", "marškinėliai", "1", "batai", "3");

        batchService.create(SPINTA, new CreateBatchRequest("Siunta iš Vokietijos",
                money("95"), money("12.50"), money("5"), List.of(
                new ItemRequest("batai", Condition.GERA, 2, null, null, today.minusDays(10)),
                new ItemRequest("džinsai", Condition.NAUJA, 2, null, null, today.minusDays(10)),
                new ItemRequest("marškinėliai", Condition.GERA, 5, null, null, today.minusDays(45)))));
    }

    // pairs: name, coef, name, coef...
    private void addCategories(String tenantId, String... nameCoefficientPairs) {
        for (int i = 0; i < nameCoefficientPairs.length; i += 2) {
            categoryRepository.save(new Category(tenantId, nameCoefficientPairs[i],
                    new BigDecimal(nameCoefficientPairs[i + 1])));
        }
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2);
    }
}
