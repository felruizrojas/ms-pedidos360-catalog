package cl.duoc.pedidos360.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItemRequest {

    @NotNull(message = "El productoId es obligatorio.")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria.")
    @Positive(message = "La cantidad debe ser mayor que 0.")
    @Max(value = 100_000, message = "La cantidad no puede superar 100.000 unidades.")
    private Integer cantidad;
}
