package lt.partijapro.tenant;

import lt.partijapro.web.UnknownTenantException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final CategoryRepository categoryRepository;

    public TenantService(TenantRepository tenantRepository, CategoryRepository categoryRepository) {
        this.tenantRepository = tenantRepository;
        this.categoryRepository = categoryRepository;
    }

    public Tenant require(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new UnknownTenantException(tenantId));
    }

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public List<Category> categories(String tenantId) {
        require(tenantId);
        return categoryRepository.findAllByTenantIdOrderByName(tenantId);
    }
}
