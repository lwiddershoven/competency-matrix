package nl.leonw.competencymatrix.controller;

import nl.leonw.competencymatrix.TestcontainersConfiguration;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldLoadRoleDetailPage() throws Exception {
        Role role = roleRepository.findByName("Junior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Junior Developer", "Entry level")));

        mockMvc.perform(get("/roles/{id}", role.id()))
                .andExpect(status().isOk())
                .andExpect(view().name("role"))
                .andExpect(model().attributeExists("role"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Junior Developer")));
    }

    @Test
    void shouldReturn404ForNonExistentRole() throws Exception {
        mockMvc.perform(get("/roles/{id}", 99999))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLoadCategoriesFragment() throws Exception {
        Role role = roleRepository.findByName("Junior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Junior Developer", "Entry level")));

        mockMvc.perform(get("/roles/{id}/categories", role.id())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk());
    }
}
