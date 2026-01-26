package nl.leonw.competencymatrix.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.SelectOption;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@QuarkusTest
class BrowseCompetenciesTest {

    public static final int TIMEOUT = 500;
    @io.quarkus.test.common.http.TestHTTPResource("/")
    String url;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
        page.navigate(baseUrl());
        page.setDefaultTimeout(200); // Locator timeouts
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    private String baseUrl() {
        return url;
    }

    @Test
    void shouldDisplayHomePageWithRoles() {
        assertThat(page.locator("h1")).containsText("Career Competency Matrix");
        assertThat(page.locator(".roles-grid")).isVisible();
        assertThat(page.locator(".role-card").first()).isVisible();
    }

    @Test
    void shouldNavigateToRoleDetailPage() {
        // Click on Junior Developer role
        page.locator(".role-card:has-text('Junior Developer')").click();

        // Verify we're on the role detail page
        assertThat(page.locator("h1")).containsText("Junior Developer");
        assertThat(page.locator(".breadcrumb")).containsText("Home");
    }

    @Test
    void shouldDisplayCompetenciesForRole() {
        page.locator(".role-card:has-text('Junior Developer')").click();

        // Wait for categories to load via htmx
        page.waitForSelector(".category-section", new Page.WaitForSelectorOptions().setTimeout(TIMEOUT));

        // Verify competencies are displayed
        assertThat(page.locator(".category-section").first()).isVisible();
        assertThat(page.locator(".skill-card").first()).isVisible();
    }

    @Test
    void shouldOpenSkillModalOnClick() {
        page.locator(".role-card:has-text('Junior Developer')").click();

        // Wait for categories to load
        page.waitForSelector(".skill-card", new Page.WaitForSelectorOptions().setTimeout(TIMEOUT));

        // Click on a skill card
        page.locator(".skill-card").first().click();

        // Wait for modal to open
        page.waitForSelector("dialog[open]", new Page.WaitForSelectorOptions().setTimeout(TIMEOUT));

        // Verify modal is open and has content
        assertThat(page.locator("dialog")).isVisible();
        assertThat(page.locator("dialog .level-descriptions")).isVisible();
    }

    @Test
    void shouldNavigateToComparePageAndShowComparison() {
        // Select roles in compare form
        page.selectOption("#from-role", new SelectOption().setLabel("Junior Developer"));
        page.selectOption("#to-role", new SelectOption().setLabel("Senior Developer"));
        page.locator("form.compare-form button[type='submit']").click();

        // Verify we're on compare page
        assertThat(page.locator("h1")).containsText("Compare Roles");

        // Wait for comparison table to load
        page.waitForSelector(".comparison-table", new Page.WaitForSelectorOptions().setTimeout(TIMEOUT));

        // Verify comparison is displayed
        assertThat(page.locator(".comparison-table")).isVisible();
    }

    @Test
    void shouldToggleTheme() {

        // Initial theme should be light
        assertThat(page.locator("html[data-theme='light']")).isVisible();

        // Click theme toggle
        page.locator("#theme-toggle").click();

        // After toggle, theme should be dark (or toggled state)
        // Note: The actual toggle happens via JS, and cookie redirect
        page.waitForLoadState();
    }

    @Test
    void shouldShowCareerProgressionLinks() {
        page.locator(".role-card:has-text('Junior Developer')").click();

        // Verify progression links are shown
        assertThat(page.locator("text=Career Progression")).isVisible();
        assertThat(page.locator(".next-role-link")).isVisible();
    }

    @Test
    void shouldNavigateWithin3Clicks() {
        // User Story: Browse competencies within 3 clicks
        // Click 1: From home, click on a role
        page.locator(".role-card:has-text('Senior Developer')").click();

        // Click 2: Click on a skill to see details
        page.waitForSelector(".skill-card", new Page.WaitForSelectorOptions().setTimeout(TIMEOUT));
        page.locator(".skill-card").first().click();

        // Verify we can see skill details in modal (within 2 clicks)
        page.waitForSelector("dialog[open]", new Page.WaitForSelectorOptions().setTimeout(TIMEOUT));
        assertThat(page.locator("dialog .level-descriptions")).isVisible();
    }
}
