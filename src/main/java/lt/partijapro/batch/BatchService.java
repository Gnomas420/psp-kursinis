package lt.partijapro.batch;

import lt.partijapro.pricing.BatchInput;
import lt.partijapro.pricing.ItemInput;
import lt.partijapro.pricing.MarkdownSchedule;
import lt.partijapro.pricing.PricingEngine;
import lt.partijapro.pricing.PricingResult;
import lt.partijapro.tenant.Category;
import lt.partijapro.tenant.Tenant;
import lt.partijapro.tenant.TenantService;
import lt.partijapro.web.InvalidInputException;
import lt.partijapro.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Use cases for batches. Connects DB with PricingEngine.
// Every method takes tenantId and only works with that tenant's data.
@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final TenantService tenantService;
    private final PricingEngine pricingEngine;

    public BatchService(BatchRepository batchRepository, TenantService tenantService, PricingEngine pricingEngine) {
        this.batchRepository = batchRepository;
        this.tenantService = tenantService;
        this.pricingEngine = pricingEngine;
    }

    // creates and calculates the batch. If calculation fails the exception is thrown
    // before save(), so nothing gets saved
    @Transactional
    public BatchResponse create(String tenantId, CreateBatchRequest request) {
        Tenant tenant = tenantService.require(tenantId);
        Map<String, BigDecimal> coefficients = categoryCoefficients(tenantId);

        Batch batch = new Batch(tenantId, request.name(), request.purchasePrice(), request.shippingCost(),
                request.otherCosts() == null ? BigDecimal.ZERO : request.otherCosts());
        List<ItemInput> inputs = new ArrayList<>();
        for (CreateBatchRequest.ItemRequest row : request.items()) {
            BigDecimal weight = weight(row, coefficients);
            for (int i = 0; i < row.quantityOrOne(); i++) {
                batch.addItem(new Item(tenantId, row.category(), row.condition(), row.manualCost(),
                        row.marketPrice(), row.listedOn()));
                inputs.add(new ItemInput(weight, row.manualCost(), row.marketPrice()));
            }
        }

        PricingResult result = pricingEngine.calculate(
                new BatchInput(batch.getPurchasePrice(), batch.getShippingCost(), batch.getOtherCosts(), inputs),
                tenant.pricingSettings());

        batch.setTotalCost(result.totalCost());
        for (int i = 0; i < batch.getItems().size(); i++) {
            batch.getItems().get(i).applyPricing(result.items().get(i));
        }
        return BatchResponse.from(batchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public List<BatchSummary> list(String tenantId) {
        tenantService.require(tenantId);
        return batchRepository.findAllByTenantIdOrderByIdDesc(tenantId).stream()
                .map(BatchSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse get(String tenantId, Long batchId) {
        return BatchResponse.from(findOwnBatch(tenantId, batchId));
    }

    @Transactional
    public void delete(String tenantId, Long batchId) {
        batchRepository.delete(findOwnBatch(tenantId, batchId));
    }

    // "reprice today" list: items whose price by R4 should be lower now
    @Transactional(readOnly = true)
    public List<RepricingItem> repricing(String tenantId, Long batchId, LocalDate date) {
        List<RepricingItem> result = new ArrayList<>();
        for (Item item : findOwnBatch(tenantId, batchId).getItems()) {
            if (item.getListedOn() == null) {
                continue;
            }
            MarkdownSchedule schedule = pricingEngine.markdown(
                    item.getRecommendedPrice(), item.getMinPrice(), item.getListedOn(), date);
            boolean priceDropped = schedule.currentPrice().compareTo(item.getRecommendedPrice()) < 0;
            if (priceDropped || schedule.needsReview()) {
                result.add(new RepricingItem(item.getId(), item.getCategory(), item.getListedOn(),
                        item.getRecommendedPrice(), schedule.currentPrice(), item.getMinPrice(),
                        schedule.needsReview(), schedule.steps()));
            }
        }
        return result;
    }

    // the only way to load a batch: by id AND tenantId. Other tenant's batch = not found
    private Batch findOwnBatch(String tenantId, Long batchId) {
        tenantService.require(tenantId);
        return batchRepository.findByIdAndTenantId(batchId, tenantId)
                .orElseThrow(() -> new NotFoundException("Partija " + batchId + " nerasta"));
    }

    private Map<String, BigDecimal> categoryCoefficients(String tenantId) {
        return tenantService.categories(tenantId).stream()
                .collect(Collectors.toMap(Category::getName, Category::getCoefficient));
    }

    private static BigDecimal weight(CreateBatchRequest.ItemRequest row, Map<String, BigDecimal> coefficients) {
        BigDecimal categoryCoefficient = coefficients.get(row.category());
        if (categoryCoefficient == null) {
            throw new InvalidInputException("Nežinoma kategorija: " + row.category());
        }
        return categoryCoefficient.multiply(row.condition().coefficient());
    }
}
