package lt.partijapro.batch;

import lt.partijapro.pricing.PriceFlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record BatchResponse(Long id, String name, BigDecimal purchasePrice, BigDecimal shippingCost,
                            BigDecimal otherCosts, BigDecimal totalCost, List<ItemResponse> items) {

    public record ItemResponse(Long id, String category, String condition, BigDecimal manualCost,
                               BigDecimal marketPrice, LocalDate listedOn, BigDecimal cost,
                               BigDecimal recommendedPrice, BigDecimal minPrice, List<String> flags) {

        static ItemResponse from(Item item) {
            List<String> flags = new ArrayList<>();
            if (item.isLossRisk()) {
                flags.add(PriceFlag.LOSS_RISK.label());
            }
            return new ItemResponse(item.getId(), item.getCategory(), item.getCondition().label(),
                    item.getManualCost(), item.getMarketPrice(), item.getListedOn(), item.getCost(),
                    item.getRecommendedPrice(), item.getMinPrice(), flags);
        }
    }

    static BatchResponse from(Batch batch) {
        return new BatchResponse(batch.getId(), batch.getName(), batch.getPurchasePrice(), batch.getShippingCost(),
                batch.getOtherCosts(), batch.getTotalCost(),
                batch.getItems().stream().map(ItemResponse::from).toList());
    }
}
