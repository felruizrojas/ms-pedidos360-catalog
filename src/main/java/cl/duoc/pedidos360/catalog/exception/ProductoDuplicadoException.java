package cl.duoc.pedidos360.catalog.exception;

import lombok.Getter;

@Getter
public class ProductoDuplicadoException extends RuntimeException {

    /** true si el nombre lo ocupa un producto eliminado (el unique de la BD sigue aplicando). */
    private final boolean eliminado;

    public ProductoDuplicadoException(String nombre) {
        this(nombre, false);
    }

    public ProductoDuplicadoException(String nombre, boolean eliminado) {
        super("Ya existe un producto con este nombre en el catálogo: " + nombre);
        this.eliminado = eliminado;
    }
}
