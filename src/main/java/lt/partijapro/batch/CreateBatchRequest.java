package lt.partijapro.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateBatchRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal purchasePrice,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal shippingCost,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal otherCosts,
        @NotEmpty List<@Valid ItemRequest> items) {

    // quantity lets you enter "3 džinsai" in one row, service turns it into 3 items
    public record ItemRequest(
            @NotBlank String category,
            @NotNull Condition condition,
            @Min(1) Integer quantity,
            @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal manualCost,
            @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal marketPrice,
            LocalDate listedOn) {

        public int quantityOrOne() {
            return quantity == null ? 1 : quantity;
        }
    }
}
