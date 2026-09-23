package cl.duoc.pedidos360.catalog.exception;

public class ProductoDuplicadoException extends RuntimeException {

    public ProductoDuplicadoException(String nombre) {
        super("Ya existe un producto con este nombre en el catálogo: " + nombre);
    }
}
