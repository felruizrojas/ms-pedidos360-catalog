package cl.duoc.pedidos360.catalog.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.catalog.dto.ProductoMapper;
import cl.duoc.pedidos360.catalog.dto.ProductoRequest;
import cl.duoc.pedidos360.catalog.dto.ProductoResponse;
import cl.duoc.pedidos360.catalog.exception.ProductoDuplicadoException;
import cl.duoc.pedidos360.catalog.exception.ProductoNotFoundException;
import cl.duoc.pedidos360.catalog.model.Producto;
import cl.duoc.pedidos360.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    public ProductoResponse buscarPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        return ProductoMapper.toResponse(producto);
    }

    public ProductoResponse crear(ProductoRequest request) {
        String nombre = request.getNombre().trim();

        // Chequeo previo: da un 400 claro y evita gastar un round-trip fallido a la BD
        // en el caso normal (sin condición de carrera).
        if (productoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ProductoDuplicadoException(nombre);
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
}
