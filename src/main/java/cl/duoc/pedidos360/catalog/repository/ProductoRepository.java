package cl.duoc.pedidos360.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.duoc.pedidos360.catalog.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Usado para bloquear nombres duplicados sin distinguir mayúsculas/minúsculas. */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Para explicar el 400 de duplicado cuando el nombre lo ocupa un producto eliminado. */
    boolean existsByNombreIgnoreCaseAndEliminadoTrue(String nombre);

    // Filtro explícito (sin @SQLRestriction): la reposición de stock sí debe ver los eliminados.
    List<Producto> findAllByEliminadoFalse();

    List<Producto> findAllByEliminadoTrue();

    Optional<Producto> findByIdAndEliminadoFalse(Long id);

    /**
     * Descuento atómico: la condición stock >= cantidad la evalúa la BD sobre la fila bloqueada,
     * así dos pedidos concurrentes nunca dejan el stock negativo. Devuelve 0 si no alcanzó, no existe o está eliminado.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad "
            + "WHERE p.id = :id AND p.stock >= :cantidad AND p.eliminado = false")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Sin filtro de eliminado a propósito: cancelar un pedido aceptado no debe perder unidades. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock + :cantidad WHERE p.id = :id")
    int reponerStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
