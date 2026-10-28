package cl.duoc.pedidos360.catalog.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private static final String ACTUALIZACION = "{\"precio\":8990,\"stock\":2}";

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

    @Test
    void putSinRolDevuelve403() throws Exception {
        mockMvc.perform(put(URL + "/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user")))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION))
                .andExpect(status().isForbidden());
    }

    @Test
    void putConRolClienteDevuelve403() throws Exception {
        mockMvc.perform(put(URL + "/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Cliente")))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION))
                .andExpect(status().isForbidden());
    }

    @Test
    void putConRolAdminPasaLaSeguridad() throws Exception {
        // 404 (no 403): la seguridad lo dejó pasar y el controller respondió que el id no existe.
        mockMvc.perform(put(URL + "/99999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION))
                .andExpect(status().isNotFound());
    }

    @Test
    void putConRolOperadorDevuelve403() throws Exception {
        mockMvc.perform(put(URL + "/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Operador")))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION))
                .andExpect(status().isForbidden());
    }

    @Test
    void postConRolOperadorDevuelve403() throws Exception {
        mockMvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Operador")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminadosConRolClienteDevuelve403() throws Exception {
        mockMvc.perform(get(URL + "/eliminados")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminadosConRolAdminDevuelve200() throws Exception {
        mockMvc.perform(get(URL + "/eliminados")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk());
    }

    @Test
    void eliminadosConRolOperadorDevuelve403() throws Exception {
        mockMvc.perform(get(URL + "/eliminados")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Operador"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void restaurarConRolClienteDevuelve403() throws Exception {
        mockMvc.perform(put(URL + "/1/restaurar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());
    }

    private static final String STOCK = "{\"items\":[{\"productoId\":99999,\"cantidad\":1}]}";

    @Test
    void stockDecreaseConRolClienteDevuelve403() throws Exception {
        mockMvc.perform(post(URL + "/stock/decrease")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Cliente")))
                        .contentType(MediaType.APPLICATION_JSON).content(STOCK))
                .andExpect(status().isForbidden());
    }

    @Test
    void stockIncreaseConRolClienteDevuelve403() throws Exception {
        mockMvc.perform(post(URL + "/stock/increase")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Cliente")))
                        .contentType(MediaType.APPLICATION_JSON).content(STOCK))
                .andExpect(status().isForbidden());
    }

    @Test
    void stockDecreaseConRolOperadorPasaLaSeguridad() throws Exception {
        // 404 (no 403): la seguridad lo dejó pasar y el servicio respondió que el producto no existe.
        mockMvc.perform(post(URL + "/stock/decrease")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Operador")))
                        .contentType(MediaType.APPLICATION_JSON).content(STOCK))
                .andExpect(status().isNotFound());
    }

    @Test
    void stockIncreaseConRolOperadorPasaLaSeguridad() throws Exception {
        mockMvc.perform(post(URL + "/stock/increase")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Operador")))
                        .contentType(MediaType.APPLICATION_JSON).content(STOCK))
                .andExpect(status().isNotFound());
    }

    @Test
    void stockDecreaseConRolAdminDevuelve403() throws Exception {
        // El stock solo se mueve al aceptar/cancelar pedidos (Operador vía orders), no desde el dashboard.
        mockMvc.perform(post(URL + "/stock/decrease")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON).content(STOCK))
                .andExpect(status().isForbidden());
    }

    private String crearComoAdmin(String nombre) throws Exception {
        String creado = mockMvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\",\"precio\":1000,\"stock\":1}".formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return creado.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    @Test
    void deleteSinTokenDevuelve401() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteConRolClienteDevuelve403() throws Exception {
        mockMvc.perform(delete(URL + "/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteConRolAdminDevuelve204() throws Exception {
        String id = crearComoAdmin("Roles Borrar Admin");
        mockMvc.perform(delete(URL + "/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteConRolOperadorDevuelve403() throws Exception {
        String id = crearComoAdmin("Roles Borrar Operador");
        mockMvc.perform(delete(URL + "/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"),
                                new SimpleGrantedAuthority("ROLE_Operador"))))
                .andExpect(status().isForbidden());
    }
}
