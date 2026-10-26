package lt.partijapro.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// R1 + R2
// manual costs stay as they are, the rest (S - M) is split by weight.
// each share is rounded down to a cent, leftover cents go 1 by 1 to items
// with the biggest cut off part (if equal - to the first one)
public class WeightedCostAllocation implements CostAllocationStrategy {

    private static final int PRECISION = 10;

    @Override
    public List<BigDecimal> allocate(BigDecimal totalCost, List<ItemInput> items) {
        BigDecimal manualSum = sumManualCosts(items);
        if (manualSum.compareTo(totalCost) > 0) {
            throw new PricingException("Rankinės savikainos (" + Money.format(manualSum)
                    + ") viršija partijos savikainą (" + Money.format(totalCost) + ")");
        }

        BigDecimal remainder = totalCost.subtract(manualSum);
        List<Integer> weightedIndexes = indexesWithoutManualCost(items);
        BigDecimal totalWeight = weightedIndexes.stream()
                .map(i -> items.get(i).weight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (weightedIndexes.isEmpty() && remainder.signum() > 0) {
            throw new PricingException("Visos savikainos įvestos ranka, bet jų suma (" + Money.format(manualSum)
                    + ") mažesnė už partijos savikainą (" + Money.format(totalCost) + ")");
        }
        if (!weightedIndexes.isEmpty() && totalWeight.signum() == 0) {
            throw new PricingException("Prekių svorių suma turi būti didesnė už 0");
        }

        BigDecimal[] costs = new BigDecimal[items.size()];
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).hasManualCost()) {
                costs[i] = items.get(i).manualCost().setScale(2, RoundingMode.HALF_UP);
            }
        }
        distributeByWeight(remainder, totalWeight, weightedIndexes, items, costs);
        return List.of(costs);
    }

    private void distributeByWeight(BigDecimal remainder, BigDecimal totalWeight, List<Integer> indexes,
                                    List<ItemInput> items, BigDecimal[] costs) {
        List<Share> shares = new ArrayList<>();
        BigDecimal distributed = BigDecimal.ZERO;

        for (int index : indexes) {
            BigDecimal exact = remainder.multiply(items.get(index).weight())
                    .divide(totalWeight, PRECISION, RoundingMode.DOWN);
            BigDecimal floored = Money.floorToCent(exact);
            costs[index] = floored;
            distributed = distributed.add(floored);
            shares.add(new Share(index, exact.subtract(floored)));
        }

        // sort is stable, so with equal parts the earlier item gets the cent
        shares.sort(Comparator.comparing(Share::truncated).reversed());
        int leftoverCents = remainder.subtract(distributed).movePointRight(2).intValueExact();
        for (int k = 0; k < leftoverCents; k++) {
            int index = shares.get(k).index();
            costs[index] = costs[index].add(Money.CENT);
        }
    }

    private static BigDecimal sumManualCosts(List<ItemInput> items) {
        return items.stream()
                .filter(ItemInput::hasManualCost)
                .map(ItemInput::manualCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static List<Integer> indexesWithoutManualCost(List<ItemInput> items) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).hasManualCost()) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    // truncated = what was lost when rounding down
    private record Share(int index, BigDecimal truncated) {
    }
}
