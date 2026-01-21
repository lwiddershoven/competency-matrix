package nl.leonw.competencymatrix.controller;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.service.CompetencyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CompareController {

    private final CompetencyService competencyService;

    public CompareController(CompetencyService competencyService) {
        this.competencyService = competencyService;
    }

    @ModelAttribute("theme")
    public String theme(@CookieValue(value = "theme", defaultValue = "light") String theme) {
        return theme;
    }

    @GetMapping("/compare")
    public String compare(@RequestParam Integer from,
                         @RequestParam Integer to,
                         Model model) {
        Role fromRole = competencyService.getRoleById(from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "From role not found"));
        Role toRole = competencyService.getRoleById(to)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To role not found"));

        model.addAttribute("fromRole", fromRole);
        model.addAttribute("toRole", toRole);
        model.addAttribute("allRoles", competencyService.getAllRoles());
        return "compare";
    }

    @HxRequest
    @GetMapping("/compare/skills")
    public String compareSkills(@RequestParam Integer from,
                               @RequestParam Integer to,
                               Model model) {
        Role fromRole = competencyService.getRoleById(from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "From role not found"));
        Role toRole = competencyService.getRoleById(to)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To role not found"));

        model.addAttribute("comparisons", competencyService.compareRoles(from, to));
        model.addAttribute("fromRole", fromRole);
        model.addAttribute("toRole", toRole);
        return "fragments/comparison-table :: comparison(comparisons=${comparisons}, fromRole=${fromRole}, toRole=${toRole})";
    }
}
