package cl.duoc.pedidos360.catalog.seed;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.catalog.model.Producto;
import cl.duoc.pedidos360.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        if (productoRepository.count() > 0) {
            return;
        }

        try (InputStream inputStream = getClass().getResourceAsStream("/data/productos-seed.json")) {
            if (inputStream == null) {
                log.warn("No se encontró el archivo de seed productos-seed.json");
                return;
            }
            List<Producto> productos = objectMapper.readValue(inputStream, new com.fasterxml.jackson.core.type.TypeReference<List<Producto>>() {
            });
            productoRepository.saveAll(productos);
            log.info("Seed de catálogo cargado: {} productos insertados", productos.size());
        }
    }
}
