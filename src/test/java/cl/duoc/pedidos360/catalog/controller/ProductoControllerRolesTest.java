package cl.duoc.pedidos360.catalog.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "pedidos360.security.enforce-roles=true")
class ProductoControllerRolesTest {

    private static final String URL = "/api/catalog/products";
    private static final String VALIDO = "{\"nombre\":\"Mouse\",\"precio\":9990,\"stock\":1}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void postSinRolDevuelve403() throws Exception {
        mockMvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void postConRolAdminDevuelve201() throws Exception {
        mockMvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated());
    }
}
