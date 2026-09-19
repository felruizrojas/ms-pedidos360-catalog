package cl.duoc.pedidos360.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoRequest {

    @NotBlank
    private String nombre;

    private String descripcion;

    @Positive
    private BigDecimal precio;

    @PositiveOrZero
    private Integer stock;

    private String categoria;
}
