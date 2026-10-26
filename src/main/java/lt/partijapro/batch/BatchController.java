package lt.partijapro.batch;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// tenant comes from X-Tenant-Id header. No logic here, just passes to the service
@RestController
@RequestMapping("/api/batches")
public class BatchController {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final BatchService batchService;
    private final Clock clock;

    public BatchController(BatchService batchService, Clock clock) {
        this.batchService = batchService;
        this.clock = clock;
    }

    @GetMapping
    public List<BatchSummary> list(@RequestHeader(TENANT_HEADER) String tenantId) {
        return batchService.list(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse create(@RequestHeader(TENANT_HEADER) String tenantId,
                                @Valid @RequestBody CreateBatchRequest request) {
        return batchService.create(tenantId, request);
    }

    @GetMapping("/{id}")
    public BatchResponse get(@RequestHeader(TENANT_HEADER) String tenantId, @PathVariable Long id) {
        return batchService.get(tenantId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(TENANT_HEADER) String tenantId, @PathVariable Long id) {
        batchService.delete(tenantId, id);
    }

    // date is optional, default = today
    @GetMapping("/{id}/repricing")
    public List<RepricingItem> repricing(@RequestHeader(TENANT_HEADER) String tenantId, @PathVariable Long id,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return batchService.repricing(tenantId, id, date != null ? date : LocalDate.now(clock));
    }
}
