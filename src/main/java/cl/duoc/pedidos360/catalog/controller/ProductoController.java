package cl.duoc.pedidos360.catalog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.catalog.dto.ActualizarProductoRequest;
import cl.duoc.pedidos360.catalog.dto.ProductoRequest;
import cl.duoc.pedidos360.catalog.dto.ProductoResponse;
import cl.duoc.pedidos360.catalog.dto.StockRequest;
import cl.duoc.pedidos360.catalog.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public List<ProductoResponse> listar() {
        return productoService.listarTodos();
    }

    /** Productos eliminados (borrado lógico). Con ENFORCE_ROLES=true, solo Admin/Operador. */
    @GetMapping("/eliminados")
    public List<ProductoResponse> listarEliminados() {
        return productoService.listarEliminados();
    }

    @GetMapping("/{id}")
    public ProductoResponse buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    /** Borrado lógico: deja de listarse y de poder pedirse, pero la fila se conserva. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }

    /** Deshace el borrado lógico: vuelve a listarse y a poder pedirse. */
    @PutMapping("/{id}/restaurar")
    public ProductoResponse restaurar(@PathVariable Long id) {
        return productoService.restaurar(id);
    }

    /** Usado por ms-pedidos360-orders al aceptar un pedido. 409 si algún ítem no alcanza (sin cambios parciales). */
    @PostMapping("/stock/decrease")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void descontarStock(@Valid @RequestBody StockRequest request) {
        productoService.descontarStock(request);
    }

    /** Usado por ms-pedidos360-orders al cancelar un pedido que ya había descontado stock. */
    @PostMapping("/stock/increase")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reponerStock(@Valid @RequestBody StockRequest request) {
        productoService.reponerStock(request);
    }
}
