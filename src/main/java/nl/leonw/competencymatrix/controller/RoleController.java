package nl.leonw.competencymatrix.controller;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import nl.leonw.competencymatrix.model.ProficiencyLevel;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.service.CompetencyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class RoleController {

    private final CompetencyService competencyService;

    public RoleController(CompetencyService competencyService) {
        this.competencyService = competencyService;
    }

    @ModelAttribute("theme")
    public String theme(@CookieValue(value = "theme", defaultValue = "light") String theme) {
        return theme;
    }

    @GetMapping("/roles/{id}")
    public String roleDetail(@PathVariable Integer id, Model model) {
        Role role = competencyService.getRoleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        model.addAttribute("role", role);
        model.addAttribute("nextRoles", competencyService.getNextRoles(id));
        model.addAttribute("previousRoles", competencyService.getPreviousRoles(id));
        return "role";
    }

    @HxRequest
    @GetMapping("/roles/{id}/categories")
    public String roleCategories(@PathVariable Integer id, Model model) {
        Role role = competencyService.getRoleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        model.addAttribute("skillsByCategory", competencyService.getSkillsByCategoryForRole(id));
        model.addAttribute("roleId", id);
        return "fragments/category-section :: categories(skillsByCategory=${skillsByCategory}, roleId=${roleId})";
    }

    @HxRequest
    @GetMapping("/roles/{roleId}/skills/{skillId}")
    public String skillDetail(@PathVariable Integer roleId,
                              @PathVariable Integer skillId,
                              Model model) {
        Skill skill = competencyService.getSkillById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));

        ProficiencyLevel requiredLevel = competencyService.getRequirementForRoleAndSkill(roleId, skillId)
                .map(req -> req.getProficiencyLevel())
                .orElse(null);

        model.addAttribute("skill", skill);
        model.addAttribute("requiredLevel", requiredLevel);
        return "fragments/skill-modal :: skill-detail(skill=${skill}, requiredLevel=${requiredLevel})";
    }
}
