package cl.duoc.pedidos360.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mismas reglas que el formulario del dashboard (catalogo-admin.ts / producto-validators.ts
 * en pedidos-360-frontend): la validación del cliente es solo UX, esta es la que de verdad
 * protege al servicio contra clientes que no pasan por el formulario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoRequest {

    private static final String NOMBRE_PATTERN = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]+$";
    private static final String DESCRIPCION_PATTERN = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü0-9\\s]*$";

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 25, message = "El nombre no puede superar los 25 caracteres.")
    @Pattern(regexp = NOMBRE_PATTERN, message = "El nombre solo admite letras y espacios (sin números ni símbolos).")
    private String nombre;

    @Size(max = 50, message = "La descripción no puede superar los 50 caracteres.")
    @Pattern(regexp = DESCRIPCION_PATTERN, message = "La descripción solo admite letras y números (sin símbolos especiales).")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio.")
    @Positive(message = "El precio debe ser un monto entero mayor que 0.")
    @DecimalMax(value = "999999999", message = "El precio no puede superar $999.999.999.")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio.")
    @PositiveOrZero(message = "El stock no puede ser negativo.")
    @Max(value = 100_000, message = "El stock no puede superar 100.000 unidades.")
    private Integer stock;
}
