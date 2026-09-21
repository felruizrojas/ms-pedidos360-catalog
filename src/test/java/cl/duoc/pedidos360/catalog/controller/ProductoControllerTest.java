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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoControllerTest {

    private static final String URL = "/api/catalog/products";
    private static final String VALIDO = "{\"nombre\":\"Teclado\",\"descripcion\":\"USB\",\"precio\":19990.50,\"stock\":5}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    static RequestPostProcessor conScope() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"));
    }

    @Test
    void listarConScopeDevuelve200() throws Exception {
        mockMvc.perform(get(URL).with(conScope()))
                .andExpect(status().isOk());
    }

    @Test
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void listarSinScopeDevuelve403() throws Exception {
        mockMvc.perform(get(URL).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void buscarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get(URL + "/99999").with(conScope()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void idNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get(URL + "/abc").with(conScope()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearValidoDevuelve201() throws Exception {
        mockMvc.perform(post(URL).with(conScope())
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void crearInvalidoDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post(URL).with(conScope())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":-1,\"stock\":-2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").exists());
    }
}
