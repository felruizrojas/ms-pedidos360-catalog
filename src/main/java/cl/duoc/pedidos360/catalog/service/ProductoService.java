package cl.duoc.pedidos360.catalog.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.catalog.dto.ActualizarProductoRequest;
import cl.duoc.pedidos360.catalog.dto.ProductoMapper;
import cl.duoc.pedidos360.catalog.dto.ProductoRequest;
import cl.duoc.pedidos360.catalog.dto.ProductoResponse;
import cl.duoc.pedidos360.catalog.dto.StockItemRequest;
import cl.duoc.pedidos360.catalog.dto.StockRequest;
import cl.duoc.pedidos360.catalog.exception.ProductoDuplicadoException;
import cl.duoc.pedidos360.catalog.exception.ProductoNoDisponibleException;
import cl.duoc.pedidos360.catalog.exception.ProductoNotFoundException;
import cl.duoc.pedidos360.catalog.exception.StockInsuficienteException;
import cl.duoc.pedidos360.catalog.model.Producto;
import cl.duoc.pedidos360.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAllByEliminadoFalse().stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    /** Eliminados (borrado lógico), para que Admin pueda deshacer la eliminación. */
    public List<ProductoResponse> listarEliminados() {
        return productoRepository.findAllByEliminadoTrue().stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    public ProductoResponse buscarPorId(Long id) {
        return ProductoMapper.toResponse(buscarVigente(id));
    }

    public ProductoResponse crear(ProductoRequest request) {
        String nombre = request.getNombre().trim();

        // Chequeo previo: da un 400 claro y evita gastar un round-trip fallido a la BD
        // en el caso normal (sin condición de carrera).
        if (productoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ProductoDuplicadoException(nombre, productoRepository.existsByNombreIgnoreCaseAndEliminadoTrue(nombre));
        }

        Producto producto = ProductoMapper.toEntity(request);
        producto.setNombre(nombre);
        try {
            Producto guardado = productoRepository.save(producto);
            return ProductoMapper.toResponse(guardado);
        } catch (DataIntegrityViolationException ex) {
            // Última barrera: dos requests concurrentes pasaron el chequeo anterior casi a la vez
            // y solo uno pudo insertar gracias al constraint unique de la tabla.
            throw new ProductoDuplicadoException(nombre);
        }
    }

    public ProductoResponse actualizar(Long id, ActualizarProductoRequest request) {
        Producto producto = buscarVigente(id);
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        return ProductoMapper.toResponse(productoRepository.save(producto));
    }

    /** Borrado lógico: la fila se conserva porque los pedidos ya creados la referencian. 404 si ya estaba eliminado. */
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarVigente(id);
        producto.setEliminado(true);
        producto.setEliminadoEn(Instant.now());
        productoRepository.save(producto);
    }

    /** Deshace el borrado lógico. Idempotente: si ya estaba vigente lo devuelve igual. 404 si no existe. */
    @Transactional
    public ProductoResponse restaurar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        if (producto.isEliminado()) {
            producto.setEliminado(false);
            producto.setEliminadoEn(null);
            producto = productoRepository.save(producto);
        }
        return ProductoMapper.toResponse(producto);
    }

    /**
     * Todo o nada: si un ítem no alcanza, no existe o fue eliminado se lanza la excepción y @Transactional
     * revierte los descuentos ya hechos a los ítems anteriores.
     */
    @Transactional
    public void descontarStock(StockRequest request) {
        agrupar(request).forEach((id, cantidad) -> {
            if (productoRepository.descontarStock(id, cantidad) == 0) {
                // El UPDATE devolvió 0: se distingue el motivo para responder 404 o 409.
                Producto producto = productoRepository.findById(id)
                        .orElseThrow(() -> new ProductoNotFoundException(id));
                if (producto.isEliminado()) {
                    throw new ProductoNoDisponibleException(id, producto.getNombre());
                }
                throw new StockInsuficienteException(id, producto.getNombre(), cantidad, producto.getStock());
            }
        });
    }

    @Transactional
    public void reponerStock(StockRequest request) {
        agrupar(request).forEach((id, cantidad) -> {
            if (productoRepository.reponerStock(id, cantidad) == 0) {
                throw new ProductoNotFoundException(id);
            }
        });
    }

    /** Un producto eliminado se trata igual que uno inexistente (404). */
    private Producto buscarVigente(Long id) {
        return productoRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    /**
     * Suma las cantidades de un mismo producto (un pedido puede repetirlo) y ordena por id, para que
     * dos transacciones concurrentes bloqueen las filas en el mismo orden y no se produzca un deadlock.
     */
    private static Map<Long, Integer> agrupar(StockRequest request) {
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (StockItemRequest item : request.getItems()) {
            cantidades.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }
        return cantidades;
    }
}
