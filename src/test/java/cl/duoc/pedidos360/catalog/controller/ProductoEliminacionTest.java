package cl.duoc.pedidos360.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import cl.duoc.pedidos360.catalog.repository.ProductoRepository;

/** Borrado lógico: el producto deja de verse y de poder pedirse, pero la reposición de stock sigue funcionando. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoEliminacionTest {

    private static final String URL = "/api/catalog/products";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductoRepository productoRepository;

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

    private void eliminar(String id) throws Exception {
        mockMvc.perform(delete(URL + "/" + id).with(conScope()))
                .andExpect(status().isNoContent());
    }

    private static String item(String id, int cantidad) {
        return "{\"items\":[{\"productoId\":%s,\"cantidad\":%d}]}".formatted(id, cantidad);
    }

    @Test
    void eliminarDevuelve204YLuegoNoSeEncuentraNiSeLista() throws Exception {
        String id = crear("Eliminar Basico", 5);

        eliminar(id);

        mockMvc.perform(get(URL + "/" + id).with(conScope()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL).with(conScope()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(not(hasItem(Integer.parseInt(id)))));
    }

    @Test
    void eliminarDosVecesDevuelve404() throws Exception {
        String id = crear("Eliminar Doble", 5);
        eliminar(id);

        mockMvc.perform(delete(URL + "/" + id).with(conScope()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete(URL + "/99999").with(conScope()))
                .andExpect(status().isNotFound());
    }

    @Test
    void putSobreEliminadoDevuelve404() throws Exception {
        String id = crear("Eliminar Put", 5);
        eliminar(id);

        mockMvc.perform(put(URL + "/" + id).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precio\":2000,\"stock\":3}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void descuentoConProductoEliminadoDevuelve409SinCambiosParciales() throws Exception {
        String a = crear("Eliminar Stock A", 10);
        String b = crear("Eliminar Stock B", 10);
        eliminar(b);

        // a se procesa primero (id menor) y alcanza; b está eliminado -> rollback también de a.
        mockMvc.perform(post(URL + "/stock/decrease").with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":%s,\"cantidad\":4},{\"productoId\":%s,\"cantidad\":1}]}"
                                .formatted(a, b)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Producto 'Eliminar Stock B' ya no está disponible"))
                .andExpect(jsonPath("$.detalles.productoId").value(Long.parseLong(b)))
                .andExpect(jsonPath("$.detalles.motivo").exists());

        mockMvc.perform(get(URL + "/" + a).with(conScope()))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void reposicionSobreEliminadoDevuelve204YSumaElStock() throws Exception {
        String id = crear("Eliminar Reponer", 3);
        eliminar(id);

        mockMvc.perform(post(URL + "/stock/increase").with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content(item(id, 4)))
                .andExpect(status().isNoContent());

        // Ya no es visible por la API, así que se lee directo del repositorio.
        assertThat(productoRepository.findById(Long.parseLong(id)).orElseThrow().getStock()).isEqualTo(7);
    }

    @Test
    void crearConNombreDeProductoEliminadoDevuelve400() throws Exception {
        String id = crear("Eliminar Nombre", 1);
        eliminar(id);

        mockMvc.perform(post(URL).with(conScope()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"eliminar nombre\",\"precio\":1000,\"stock\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").value("Existe un producto eliminado con este nombre."));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isUnauthorized());
    }
}
