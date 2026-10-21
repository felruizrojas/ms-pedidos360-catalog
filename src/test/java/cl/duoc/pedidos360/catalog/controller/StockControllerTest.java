package cl.duoc.pedidos360.catalog.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Descuento y reposición de stock usados por ms-pedidos360-orders al aceptar/cancelar pedidos. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockControllerTest {

    private static final String URL = "/api/catalog/products";
    private static final String DECREASE = URL + "/stock/decrease";
    private static final String INCREASE = URL + "/stock/increase";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    static RequestPostProcessor conScope() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"));
    }

    private String crear(String nombre, int stock) throws Exception {
        String creado = mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\",\"precio\":1000,\"stock\":%d}".formatted(nombre, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return creado.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    private static String items(String... pares) {
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < pares.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"productoId\":").append(pares[i]).append(",\"cantidad\":").append(pares[i + 1]).append('}');
        }
        return sb.append("]}").toString();
    }

    private void esperarStock(String id, int stock) throws Exception {
        mockMvc.perform(get(URL + "/" + id).with(conScope()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(stock));
    }

    @Test
    void descuentoCorrectoBajaElStockDeTodosLosItems() throws Exception {
        String a = crear("Stock Descuento A", 10);
        String b = crear("Stock Descuento B", 5);

        mockMvc.perform(post(DECREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(items(a, "3", b, "5")))
                .andExpect(status().isNoContent());

        esperarStock(a, 7);
        esperarStock(b, 0);
    }

    @Test
    void mismoProductoRepetidoSeSumaAntesDeDescontar() throws Exception {
        String a = crear("Stock Repetido", 4);

        mockMvc.perform(post(DECREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(items(a, "3", a, "2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.solicitado").value(5));

        esperarStock(a, 4);
    }

    @Test
    void stockInsuficienteDevuelve409SinCambiosParciales() throws Exception {
        String a = crear("Stock Parcial A", 10);
        String b = crear("Stock Parcial B", 2);

        // a se procesa primero (id menor) y alcanza; b no alcanza -> rollback también de a.
        mockMvc.perform(post(DECREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(items(a, "4", b, "5")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.detalles.productoId").value(Long.parseLong(b)))
                .andExpect(jsonPath("$.detalles.solicitado").value(5))
                .andExpect(jsonPath("$.detalles.disponible").value(2));

        esperarStock(a, 10);
        esperarStock(b, 2);
    }

    @Test
    void productoInexistenteDevuelve404SinCambiosParciales() throws Exception {
        String a = crear("Stock Inexistente", 10);

        mockMvc.perform(post(DECREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(items(a, "1", "99999", "1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists());

        esperarStock(a, 10);
    }

    @Test
    void reposicionSubeElStock() throws Exception {
        String a = crear("Stock Reposicion", 3);

        mockMvc.perform(post(INCREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(items(a, "4")))
                .andExpect(status().isNoContent());

        esperarStock(a, 7);
    }

    @Test
    void reposicionDeProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post(INCREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(items("99999", "1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void bodyInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post(DECREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.items").exists());

        mockMvc.perform(post(DECREASE).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(items("1", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles['items[0].cantidad']").exists());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(post(DECREASE).contentType(MediaType.APPLICATION_JSON).content(items("1", "1")))
                .andExpect(status().isUnauthorized());
    }
}
