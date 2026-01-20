package nl.leonw.competencymatrix.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import nl.leonw.competencymatrix.service.CompetencyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    private final CompetencyService competencyService;

    public HomeController(CompetencyService competencyService) {
        this.competencyService = competencyService;
    }

    @ModelAttribute("theme")
    public String theme(@CookieValue(value = "theme", defaultValue = "light") String theme) {
        return theme;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("roles", competencyService.getAllRoles());
        return "index";
    }

    @PostMapping("/theme")
    public String toggleTheme(@CookieValue(value = "theme", defaultValue = "light") String currentTheme,
                              HttpServletResponse response) {
        String newTheme = "dark".equals(currentTheme) ? "light" : "dark";
        Cookie cookie = new Cookie("theme", newTheme);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 365); // 1 year
        response.addCookie(cookie);
        return "redirect:/";
    }
}
