package cl.duoc.pedidos360.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true agrega la restricción a nivel de BD: última barrera contra duplicados
    // aunque dos requests concurrentes pasen el chequeo de ProductoService al mismo tiempo.
    @Column(nullable = false, length = 25, unique = true)
    private String nombre;

    @Column(length = 50)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    // Borrado lógico: los pedidos ya creados guardan productoId y nombre, así que la fila se conserva.
    // columnDefinition con default para que ddl-auto=update pueda agregar la columna a una tabla con filas.
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean eliminado = false;

    private Instant eliminadoEn;
}
