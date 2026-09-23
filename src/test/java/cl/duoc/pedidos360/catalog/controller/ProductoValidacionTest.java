package cl.duoc.pedidos360.catalog.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Mismas reglas que valida el formulario del dashboard (nombre solo letras/máx. 25,
 * descripción letras+números/máx. 50, precio/stock con tope, nombre único), verificadas
 * acá para que un cliente que no pase por el formulario (Postman, curl, otro front) no
 * pueda saltárselas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoValidacionTest {

    private static final String URL = "/api/catalog/products";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    static RequestPostProcessor conScope() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"));
    }

    private static String producto(String nombre, String descripcion, String precio, String stock) {
        return "{\"nombre\":\"%s\",\"descripcion\":\"%s\",\"precio\":%s,\"stock\":%s}"
                .formatted(nombre, descripcion, precio, stock);
    }

    @Test
    void nombreConNumerosDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Producto1", "Válido", "100", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").exists());
    }

    @Test
    void nombreMasDe25CaracteresDevuelve400() throws Exception {
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Un Nombre Larguisimo De Producto", "Válido", "100", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").exists());
    }

    @Test
    void descripcionConSimbolosDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Validacion Uno", "Con símbolo #raro!", "100", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.descripcion").exists());
    }

    @Test
    void precioSuperaElMaximoDevuelve400() throws Exception {
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Validacion Dos", "Valida", "10000000000000000000000000000", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.precio").exists());
    }

    @Test
    void stockSuperaElMaximoDevuelve400() throws Exception {
        // Un valor que sí cabe en un Integer (a diferencia del precio, que es BigDecimal y admite
        // cualquier tamaño) pero que supera el tope de negocio: dispara @Max, no un error de parseo.
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Validacion Tres", "Valida", "100", "999999999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.stock").exists());
    }

    @Test
    void stockConValorImposibleDeParsearDevuelve400() throws Exception {
        // Un entero que ni siquiera cabe en un Integer de Java (30 dígitos): falla al parsear el
        // JSON antes de llegar a la validación de negocio. Igual de seguro (400), aunque sin detalle
        // por campo, porque el request ni siquiera es un Producto válido a nivel de tipos.
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Validacion Cuatro", "Valida", "100", "10000000000000000000000000000")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nombreDuplicadoDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Nombre Repetido", "Primera vez", "100", "1")))
                .andExpect(status().isCreated());

        // Mismo nombre con distinto uso de mayúsculas y espacios: igual se considera duplicado.
        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("nombre repetido", "Segunda vez", "200", "2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").exists());
    }
}
