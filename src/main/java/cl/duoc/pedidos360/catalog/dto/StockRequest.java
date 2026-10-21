package cl.duoc.pedidos360.catalog.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuerpo de /stock/decrease y /stock/increase. Lo usa ms-pedidos360-orders al aceptar (descuenta)
 * y al cancelar (repone) un pedido: todos los ítems van en una sola operación.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequest {

    @NotEmpty(message = "Debe indicar al menos un ítem.")
    private List<@NotNull(message = "Los ítems no pueden ser nulos.") @Valid StockItemRequest> items;
}
