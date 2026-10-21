package cl.duoc.pedidos360.catalog.exception;

import lombok.Getter;

/** El producto existe pero fue eliminado: no se puede descontar stock (409). */
@Getter
public class ProductoNoDisponibleException extends RuntimeException {

    private final Long productoId;

    public ProductoNoDisponibleException(Long productoId, String nombre) {
        super("Producto '" + nombre + "' ya no está disponible");
        this.productoId = productoId;
    }
}
