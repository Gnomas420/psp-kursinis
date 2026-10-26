package lt.partijapro.batch;

import java.math.BigDecimal;

public record BatchSummary(Long id, String name, BigDecimal totalCost, int itemCount) {

    static BatchSummary from(Batch batch) {
        return new BatchSummary(batch.getId(), batch.getName(), batch.getTotalCost(), batch.getItems().size());
    }
}
