package cl.duoc.pedidos360.catalog.seed;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.catalog.model.Producto;
import cl.duoc.pedidos360.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final ObjectMapper objectMapper;

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
            List<Producto> productos = objectMapper.readValue(inputStream, new TypeReference<List<Producto>>() {
            });
            productoRepository.saveAll(productos);
            log.info("Seed de catálogo cargado: {} productos insertados", productos.size());
        }
    }
}
