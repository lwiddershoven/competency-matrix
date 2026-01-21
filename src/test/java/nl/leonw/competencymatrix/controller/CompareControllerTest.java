package nl.leonw.competencymatrix.controller;

import nl.leonw.competencymatrix.TestcontainersConfiguration;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
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
class CompareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    private Role fromRole;
    private Role toRole;

    @BeforeEach
    void setUp() {
        fromRole = roleRepository.findByName("Junior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Junior Developer", "Entry level")));
        toRole = roleRepository.findByName("Senior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Senior Developer", "Senior level")));
    }

    @Test
    void shouldLoadComparePage() throws Exception {
        mockMvc.perform(get("/compare")
                        .param("from", fromRole.id().toString())
                        .param("to", toRole.id().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("compare"))
                .andExpect(model().attributeExists("fromRole", "toRole", "allRoles"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Compare Roles")));
    }

    @Test
    void shouldReturn404ForNonExistentFromRole() throws Exception {
        mockMvc.perform(get("/compare")
                        .param("from", "99999")
                        .param("to", toRole.id().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLoadComparisonFragment() throws Exception {
        mockMvc.perform(get("/compare/skills")
                        .param("from", fromRole.id().toString())
                        .param("to", toRole.id().toString())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk());
    }
}
