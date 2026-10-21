package cl.duoc.pedidos360.catalog.exception;

import lombok.Getter;

@Getter
public class StockInsuficienteException extends RuntimeException {

    private final Long productoId;
    private final int solicitado;
    private final int disponible;

    public StockInsuficienteException(Long productoId, String nombre, int solicitado, int disponible) {
        super("Stock insuficiente para \"" + nombre + "\" (id " + productoId + "): solicitado "
                + solicitado + ", disponible " + disponible);
        this.productoId = productoId;
        this.solicitado = solicitado;
        this.disponible = disponible;
    }
}
