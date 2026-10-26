package lt.partijapro.batch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// every query filters by tenantId, so other tenant's batch is just "not found".
// BatchService never uses plain findById / findAll.
public interface BatchRepository extends JpaRepository<Batch, Long> {

    Optional<Batch> findByIdAndTenantId(Long id, String tenantId);

    List<Batch> findAllByTenantIdOrderByIdDesc(String tenantId);
}
