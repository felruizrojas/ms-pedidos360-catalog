package cl.duoc.pedidos360.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.catalog.dto.ProductoMapper;
import cl.duoc.pedidos360.catalog.dto.ProductoRequest;
import cl.duoc.pedidos360.catalog.dto.ProductoResponse;
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
        Producto producto = ProductoMapper.toEntity(request);
        Producto guardado = productoRepository.save(producto);
        return ProductoMapper.toResponse(guardado);
    }
}
