package cl.duoc.pedidos360.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.pedidos360.catalog.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
