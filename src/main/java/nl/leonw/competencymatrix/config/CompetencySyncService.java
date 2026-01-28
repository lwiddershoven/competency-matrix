package nl.leonw.competencymatrix.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.RoleProgression;
import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.repository.CategoryRepository;
import nl.leonw.competencymatrix.repository.RoleProgressionRepository;
import nl.leonw.competencymatrix.repository.RoleRepository;
import nl.leonw.competencymatrix.repository.RoleSkillRequirementRepository;
import nl.leonw.competencymatrix.repository.SkillRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Core service for synchronizing competencies from YAML to database.
 * Handles parsing, validation, and normalization operations.
 */
@ApplicationScoped
public class CompetencySyncService {

    private static final Logger log = LoggerFactory.getLogger(CompetencySyncService.class);
    private static final String COMPETENCIES_PATH = "seed/competencies.yaml";

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    SkillRepository skillRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    RoleSkillRequirementRepository requirementRepository;

    @Inject
    RoleProgressionRepository progressionRepository;

    @ConfigProperty(name = "competency.sync.mode")
    Optional<String> syncModeValue;

    /**
     * Entry point for startup synchronization based on configured sync mode.
     * Reads configuration, loads YAML data, and executes sync operations.
     *
     * @return SyncResult summary of changes
     */
    @Transactional
    public SyncResult syncFromConfiguration() {
        String configuredValue = syncModeValue.orElse(null);
        if (configuredValue == null) {
            log.warn("Competency sync mode not configured, defaulting to 'none'. Set competency.sync.mode property.");
        }

        SyncMode mode = resolveSyncMode(configuredValue);
        if (mode == SyncMode.NONE) {
            log.info("Competency sync mode set to none; skipping sync.");
            return emptyResult();
        }

        log.info("Starting competency sync - mode: {}", mode.name().toLowerCase());

        try {
            YamlCompetencyData data = loadYamlData();
            SyncResult result = switch (mode) {
                case MERGE -> syncMerge(data);
                case REPLACE -> syncReplace(data);
                case NONE -> emptyResult();
            };

            log.info(result.formatSummary());
            return result;
        } catch (RuntimeException e) {
            log.error("Competency sync failed", e);
            throw e;
        }
    }

    /**
     * Parses competencies.yaml file into in-memory data structures.
     *
     * @param inputStream YAML file input stream
     * @return Parsed competency data
     * @throws RuntimeException if YAML parsing fails
     */
    @SuppressWarnings("unchecked")
    public YamlCompetencyData parseYaml(InputStream inputStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);

            List<YamlCompetencyData.CategoryData> categories = parseCategories(
                    (List<Map<String, Object>>) data.get("categories"));
            List<YamlCompetencyData.RoleData> roles = parseRoles(
                    (List<Map<String, Object>>) data.get("roles"));
            List<YamlCompetencyData.ProgressionData> progressions = parseProgressions(
                    (List<Map<String, Object>>) data.get("progressions"));

            return new YamlCompetencyData(categories, roles, progressions);
        } catch (Exception e) {
            log.error("Failed to parse YAML file", e);
            throw new RuntimeException("Failed to parse competencies.yaml: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the structure and content of parsed YAML data.
     *
     * @param data Parsed YAML data to validate
     * @throws RuntimeException if validation fails
     */
    public void validateYaml(YamlCompetencyData data) {
        if (data.categories() == null || data.roles() == null || data.progressions() == null) {
            throw new RuntimeException("Invalid YAML: missing required top-level keys (categories, roles, progressions)");
        }

        // Validate categories
        for (YamlCompetencyData.CategoryData category : data.categories()) {
            if (category.name() == null || category.name().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Category name is required");
            }
            if (category.skills() == null) {
                throw new RuntimeException("Invalid YAML: Category '" + category.name() + "' is missing 'skills' list");
            }

            // Validate skills
            for (YamlCompetencyData.SkillData skill : category.skills()) {
                if (skill.name() == null || skill.name().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Skill name is required in category '" + category.name() + "'");
                }
                if (skill.levels() == null || skill.levels().size() != 4) {
                    throw new RuntimeException("Invalid YAML: Skill '" + skill.name() + "' must have all four levels (basic, decent, good, excellent)");
                }
                if (!skill.levels().containsKey("basic") || !skill.levels().containsKey("decent") ||
                    !skill.levels().containsKey("good") || !skill.levels().containsKey("excellent")) {
                    throw new RuntimeException("Invalid YAML: Skill '" + skill.name() + "' must have levels: basic, decent, good, excellent");
                }
            }
        }

        // Validate roles
        for (YamlCompetencyData.RoleData role : data.roles()) {
            if (role.name() == null || role.name().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Role name is required");
            }
            if (role.description() == null) {
                throw new RuntimeException("Invalid YAML: Role '" + role.name() + "' is missing 'description'");
            }
            if (role.requirements() == null) {
                throw new RuntimeException("Invalid YAML: Role '" + role.name() + "' is missing 'requirements' list");
            }

            // Validate requirements
            for (YamlCompetencyData.RequirementData req : role.requirements()) {
                if (req.skillName() == null || req.skillName().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Requirement in role '" + role.name() + "' missing skill name");
                }
                if (req.categoryName() == null || req.categoryName().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Requirement in role '" + role.name() + "' missing category name");
                }
                if (req.level() == null || req.level().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Requirement in role '" + role.name() + "' missing level");
                }
            }
        }

        // Validate progressions
        for (YamlCompetencyData.ProgressionData progression : data.progressions()) {
            if (progression.fromRoleName() == null || progression.fromRoleName().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Progression missing 'from' role name");
            }
            if (progression.toRoleName() == null || progression.toRoleName().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Progression missing 'to' role name");
            }
        }

        log.debug("YAML validation passed");
    }

    /**
     * Resolves the configured sync mode value into a SyncMode enum.
     * Placeholder implementation to support TDD tests; will be expanded with
     * actual configuration logic in subsequent tasks.
     *
     * @param configuredValue raw configuration value
     * @return resolved sync mode
     */
    public SyncMode resolveSyncMode(String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return SyncMode.NONE;
        }

        String normalized = configuredValue.trim().toLowerCase();
        return switch (normalized) {
            case "none" -> SyncMode.NONE;
            case "merge" -> SyncMode.MERGE;
            case "replace" -> SyncMode.REPLACE;
            default -> throw new IllegalArgumentException(
                    "Invalid competency.sync.mode value '" + configuredValue + "': must be one of [none, merge, replace]");
        };
    }

    /**
     * Synchronizes competency data using merge mode with provided YAML input.
     * Intended for tests or custom invocations without configuration.
     *
     * @param inputStream YAML input stream
     * @return SyncResult summary of changes
     */
    public SyncResult syncMerge(InputStream inputStream) {
        if (inputStream == null) {
            throw new RuntimeException("Competencies YAML input stream is required");
        }
        YamlCompetencyData data = parseYaml(inputStream);
        return syncMerge(data);
    }

    /**
     * Synchronizes competency data using merge mode with provided YAML data.
     *
     * @param data Parsed YAML data
     * @return SyncResult summary of changes
     */
    public SyncResult syncMerge(YamlCompetencyData data) {
        validateYaml(data);

        SyncCounters counters = new SyncCounters();
        Map<String, CompetencyCategory> categoryIndex = processCategories(data.categories(), counters);
        Map<String, Skill> skillIndex = processSkills(data.categories(), categoryIndex, counters);
        Map<String, Role> roleIndex = processRoles(data.roles(), counters);
        processRequirements(data.roles(), roleIndex, categoryIndex, skillIndex, counters);
        processProgressions(data.progressions(), roleIndex, counters);

        return counters.toResult();
    }

    /**
     * Synchronizes competency data using replace mode with provided YAML data.
     * Deletes all existing competency data before applying the YAML contents.
     *
     * @param data Parsed YAML data
     * @return SyncResult summary of deletions and additions
     */
    public SyncResult syncReplace(YamlCompetencyData data) {
        validateYaml(data);

        SyncCounters counters = new SyncCounters();
        deleteAllEntities(counters);

        Map<String, CompetencyCategory> categoryIndex = processCategories(data.categories(), counters);
        Map<String, Skill> skillIndex = processSkills(data.categories(), categoryIndex, counters);
        Map<String, Role> roleIndex = processRoles(data.roles(), counters);
        processRequirements(data.roles(), roleIndex, categoryIndex, skillIndex, counters);
        processProgressions(data.progressions(), roleIndex, counters);

        return counters.toResult();
    }

    /**
     * Synchronization method for none mode - skips all sync operations.
     * Returns an empty result indicating no changes were made.
     *
     * @param data Parsed YAML data (ignored in none mode)
     * @return Empty SyncResult indicating no operations performed
     */
    public SyncResult syncNone(YamlCompetencyData data) {
        log.info("Sync mode set to none; skipping all synchronization operations.");
        return emptyResult();
    }

    /**
     * Normalizes a string for case/space-insensitive matching.
     * Converts to lowercase, trims, and collapses multiple spaces.
     *
     * @param value String to normalize
     * @return Normalized string (lowercase, trimmed, single spaces)
     */
    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /**
     * Load competency data from multiple YAML files (JAR-compatible).
     * T017-T022: Multi-file loading with error reporting and logging
     *
     * @return merged competency data from all files
     */
    private YamlCompetencyData loadYamlData() {
        log.info("Loading competency data from split YAML files");

        List<YamlCompetencyData.CategoryData> allCategories = new ArrayList<>();
        List<YamlCompetencyData.RoleData> allRoles = new ArrayList<>();
        List<YamlCompetencyData.ProgressionData> allProgressions = new ArrayList<>();

        Map<String, String> categoryFileMap = new HashMap<>();
        Map<String, String> roleFileMap = new HashMap<>();

        // T019: Discover and load category resources from classpath
        List<String> categoryResources = discoverYamlResources("seed/categories");
        log.info("Discovered {} category files", categoryResources.size());

        for (String resourcePath : categoryResources) {
            try {
                String filename = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                log.info("Loading category file: {}", filename);
                YamlCompetencyData data = parseYamlResource(resourcePath);

                // T020: Merge categories using List.addAll()
                if (data.categories() != null && !data.categories().isEmpty()) {
                    for (YamlCompetencyData.CategoryData category : data.categories()) {
                        categoryFileMap.put(category.name(), filename);
                    }
                    allCategories.addAll(data.categories());
                }
            } catch (Exception e) {
                // T021: Per-file error reporting with filename context
                String filename = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                String error = String.format("Failed to load category file %s: %s", filename, e.getMessage());
                log.error(error, e);
                throw new RuntimeException(error, e);
            }
        }

        // T018: Detect duplicate categories across files
        detectDuplicateCategories(allCategories, categoryFileMap);

        // Load role resources from classpath
        List<String> roleResources = discoverYamlResources("seed/roles");
        log.info("Discovered {} role files", roleResources.size());

        for (String resourcePath : roleResources) {
            try {
                String filename = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                log.info("Loading role file: {}", filename);
                YamlCompetencyData data = parseYamlResource(resourcePath);

                if (data.roles() != null && !data.roles().isEmpty()) {
                    for (YamlCompetencyData.RoleData role : data.roles()) {
                        roleFileMap.put(role.name(), filename);
                    }
                    allRoles.addAll(data.roles());
                }
            } catch (Exception e) {
                String filename = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                String error = String.format("Failed to load role file %s: %s", filename, e.getMessage());
                log.error(error, e);
                throw new RuntimeException(error, e);
            }
        }

        // Detect duplicate roles across files
        detectDuplicateRoles(allRoles, roleFileMap);

        // Load progressions from single file (classpath resource)
        String progressionsPath = "seed/progressions.yaml";
        if (getClass().getClassLoader().getResource(progressionsPath) != null) {
            try {
                log.info("Loading progressions from progressions.yaml");
                YamlCompetencyData data = parseYamlResource(progressionsPath);

                if (data.progressions() != null) {
                    allProgressions.addAll(data.progressions());
                }
            } catch (Exception e) {
                log.warn("Failed to load progressions.yaml: {}", e.getMessage());
                // Progressions are optional, continue without them
            }
        } else {
            log.warn("progressions.yaml not found, no progressions will be loaded");
        }

        // T022: Log summary of loaded data
        log.info("Loaded {} categories, {} roles, {} progressions from split files",
                allCategories.size(), allRoles.size(), allProgressions.size());

        return new YamlCompetencyData(allCategories, allRoles, allProgressions);
    }

    private Map<String, CompetencyCategory> processCategories(List<YamlCompetencyData.CategoryData> categories,
                                                              SyncCounters counters) {
        Map<String, CompetencyCategory> categoryIndex = new HashMap<>();
        for (CompetencyCategory existing : categoryRepository.findAllOrderByDisplayOrder()) {
            categoryIndex.put(normalize(existing.name()), existing);
        }

        for (YamlCompetencyData.CategoryData yamlCategory : categories) {
            String normalizedName = normalize(yamlCategory.name());
            CompetencyCategory existing = categoryIndex.get(normalizedName);

            if (existing == null) {
                CompetencyCategory created = categoryRepository.save(
                        new CompetencyCategory(yamlCategory.name(), yamlCategory.displayOrder()));
                categoryIndex.put(normalizedName, created);
                counters.categoriesAdded++;
                log.info("Category added: {}", created.name());
            } else if (categoryNeedsUpdate(existing, yamlCategory)) {
                CompetencyCategory updated = categoryRepository.save(new CompetencyCategory(
                        existing.id(), yamlCategory.name(), yamlCategory.displayOrder()));
                categoryIndex.put(normalizedName, updated);
                counters.categoriesUpdated++;
                log.info("Category updated: {}", updated.name());
            } else {
                categoryIndex.put(normalizedName, existing);
            }
        }

        return categoryIndex;
    }

    private Map<String, Skill> processSkills(List<YamlCompetencyData.CategoryData> categories,
                                             Map<String, CompetencyCategory> categoryIndex,
                                             SyncCounters counters) {
        Map<String, Skill> skillIndex = new HashMap<>();

        for (YamlCompetencyData.CategoryData yamlCategory : categories) {
            CompetencyCategory category = resolveCategory(yamlCategory.name(), categoryIndex);

            Map<String, Skill> existingSkillsByName = new HashMap<>();
            for (Skill existing : skillRepository.findByCategoryId(category.id())) {
                existingSkillsByName.put(normalize(existing.name()), existing);
                skillIndex.put(skillKey(category.name(), existing.name()), existing);
            }

            for (YamlCompetencyData.SkillData yamlSkill : yamlCategory.skills()) {
                String normalizedSkill = normalize(yamlSkill.name());
                Skill existing = existingSkillsByName.get(normalizedSkill);
                Map<String, String> levels = yamlSkill.levels();
                String basic = levels.get("basic");
                String decent = levels.get("decent");
                String good = levels.get("good");
                String excellent = levels.get("excellent");

                if (existing == null) {
                    Skill created = skillRepository.save(new Skill(
                            yamlSkill.name(), category.id(), basic, decent, good, excellent));
                    skillIndex.put(skillKey(category.name(), created.name()), created);
                    counters.skillsAdded++;
                    log.info("Skill added: {} in category {}", created.name(), category.name());
                } else if (skillNeedsUpdate(existing, yamlSkill)) {
                    Skill updated = skillRepository.save(new Skill(
                            existing.id(), yamlSkill.name(), category.id(), basic, decent, good, excellent));
                    skillIndex.put(skillKey(category.name(), updated.name()), updated);
                    counters.skillsUpdated++;
                    log.info("Skill updated: {} in category {}", updated.name(), category.name());
                } else {
                    skillIndex.put(skillKey(category.name(), existing.name()), existing);
                }
            }
        }

        return skillIndex;
    }

    private Map<String, Role> processRoles(List<YamlCompetencyData.RoleData> roles, SyncCounters counters) {
        Map<String, Role> roleIndex = new HashMap<>();
        for (Role existing : roleRepository.findAllOrderByName()) {
            roleIndex.put(normalize(existing.name()), existing);
        }

        for (YamlCompetencyData.RoleData yamlRole : roles) {
            String normalizedName = normalize(yamlRole.name());
            Role existing = roleIndex.get(normalizedName);

            if (existing == null) {
                Role created = roleRepository.save(new Role(yamlRole.name(), yamlRole.description()));
                roleIndex.put(normalizedName, created);
                counters.rolesAdded++;
                log.info("Role added: {}", created.name());
            } else if (roleNeedsUpdate(existing, yamlRole)) {
                Role updated = roleRepository.save(new Role(existing.id(), yamlRole.name(), yamlRole.description(),
                    yamlRole.roleFamily(), yamlRole.seniorityOrder()));
                roleIndex.put(normalizedName, updated);
                counters.rolesUpdated++;
                log.info("Role updated: {}", updated.name());
            } else {
                roleIndex.put(normalizedName, existing);
            }
        }

        return roleIndex;
    }

    private void processRequirements(List<YamlCompetencyData.RoleData> roles,
                                     Map<String, Role> roleIndex,
                                     Map<String, CompetencyCategory> categoryIndex,
                                     Map<String, Skill> skillIndex,
                                     SyncCounters counters) {
        for (YamlCompetencyData.RoleData yamlRole : roles) {
            Role role = resolveRole(yamlRole.name(), roleIndex);

            for (YamlCompetencyData.RequirementData requirement : yamlRole.requirements()) {
                CompetencyCategory category = resolveCategory(requirement.categoryName(), categoryIndex);
                Skill skill = resolveSkill(requirement, category, skillIndex);

                String requiredLevel = requirement.level().toUpperCase();
                Optional<RoleSkillRequirement> existing =
                        requirementRepository.findByRoleIdAndSkillId(role.id(), skill.id());

                if (existing.isPresent()) {
                    RoleSkillRequirement current = existing.get();
                    if (requirementNeedsUpdate(current, requiredLevel)) {
                        requirementRepository.save(new RoleSkillRequirement(
                                current.id(), role.id(), skill.id(), requiredLevel));
                        counters.requirementsUpdated++;
                        log.info("Requirement updated: {} -> {} at {}", role.name(), skill.name(), requiredLevel);
                    }
                } else {
                    requirementRepository.save(new RoleSkillRequirement(role.id(), skill.id(), requiredLevel));
                    counters.requirementsAdded++;
                    log.info("Requirement added: {} -> {} at {}", role.name(), skill.name(), requiredLevel);
                }
            }
        }
    }

    private void processProgressions(List<YamlCompetencyData.ProgressionData> progressions,
                                     Map<String, Role> roleIndex,
                                     SyncCounters counters) {
        for (YamlCompetencyData.ProgressionData progression : progressions) {
            Role fromRole = resolveRole(progression.fromRoleName(), roleIndex);
            Role toRole = resolveRole(progression.toRoleName(), roleIndex);

            Optional<RoleProgression> existing =
                    progressionRepository.findByFromRoleIdAndToRoleId(fromRole.id(), toRole.id());
            if (existing.isEmpty()) {
                progressionRepository.save(new RoleProgression(fromRole.id(), toRole.id()));
                counters.progressionsAdded++;
                log.info("Progression added: {} -> {}", fromRole.name(), toRole.name());
            }
        }
    }

    private boolean categoryNeedsUpdate(CompetencyCategory existing, YamlCompetencyData.CategoryData yamlCategory) {
        return !existing.name().equals(yamlCategory.name())
                || existing.displayOrder() != yamlCategory.displayOrder();
    }

    private boolean skillNeedsUpdate(Skill existing, YamlCompetencyData.SkillData yamlSkill) {
        Map<String, String> levels = yamlSkill.levels();
        return !existing.name().equals(yamlSkill.name())
                || !Objects.equals(existing.basicDescription(), levels.get("basic"))
                || !Objects.equals(existing.decentDescription(), levels.get("decent"))
                || !Objects.equals(existing.goodDescription(), levels.get("good"))
                || !Objects.equals(existing.excellentDescription(), levels.get("excellent"));
    }

    private boolean roleNeedsUpdate(Role existing, YamlCompetencyData.RoleData yamlRole) {
        return !existing.name().equals(yamlRole.name())
                || !Objects.equals(existing.description(), yamlRole.description());
    }

    private boolean requirementNeedsUpdate(RoleSkillRequirement existing, String requiredLevel) {
        return !existing.requiredLevel().equalsIgnoreCase(requiredLevel);
    }

    private CompetencyCategory resolveCategory(String categoryName,
                                               Map<String, CompetencyCategory> categoryIndex) {
        String normalized = normalize(categoryName);
        CompetencyCategory category = categoryIndex.get(normalized);
        if (category != null) {
            return category;
        }
        return categoryRepository.findByNameIgnoreCase(categoryName)
                .orElseThrow(() -> {
                    String message = "Category '" + categoryName + "' does not exist in database or YAML";
                    log.error(message);
                    return new RuntimeException(message);
                });
    }

    private Skill resolveSkill(YamlCompetencyData.RequirementData requirement,
                               CompetencyCategory category,
                               Map<String, Skill> skillIndex) {
        String key = skillKey(category.name(), requirement.skillName());
        Skill skill = skillIndex.get(key);
        if (skill != null) {
            return skill;
        }

        Optional<Skill> existing = skillRepository
                .findByNameAndCategoryIdIgnoreCase(requirement.skillName(), category.id());
        if (existing.isPresent()) {
            skillIndex.put(key, existing.get());
            return existing.get();
        }

        String message = "Role requirement references skill '" + requirement.skillName()
                + "' in category '" + requirement.categoryName()
                + "' which does not exist in database or YAML";
        log.error(message);
        throw new RuntimeException(message);
    }

    private Role resolveRole(String roleName, Map<String, Role> roleIndex) {
        String normalized = normalize(roleName);
        Role role = roleIndex.get(normalized);
        if (role != null) {
            return role;
        }
        return roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> {
                    String message = "Role '" + roleName + "' does not exist in database or YAML";
                    log.error(message);
                    return new RuntimeException(message);
                });
    }

    private String skillKey(String categoryName, String skillName) {
        return normalize(categoryName) + "::" + normalize(skillName);
    }

    private void deleteAllEntities(SyncCounters counters) {
        int progressionsDeleted = progressionRepository.deleteAll();
        int requirementsDeleted = requirementRepository.deleteAll();
        int skillsDeleted = skillRepository.deleteAll();
        int rolesDeleted = roleRepository.deleteAll();
        int categoriesDeleted = categoryRepository.deleteAll();

        counters.progressionsDeleted = progressionsDeleted;
        counters.requirementsDeleted = requirementsDeleted;
        counters.skillsDeleted = skillsDeleted;
        counters.rolesDeleted = rolesDeleted;
        counters.categoriesDeleted = categoriesDeleted;

        log.info("Deleted {} progressions", progressionsDeleted);
        log.info("Deleted {} requirements", requirementsDeleted);
        log.info("Deleted {} skills", skillsDeleted);
        log.info("Deleted {} roles", rolesDeleted);
        log.info("Deleted {} categories", categoriesDeleted);
    }

    private SyncResult emptyResult() {
        return new SyncResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static final class SyncCounters {
        private int categoriesAdded;
        private int categoriesUpdated;
        private int skillsAdded;
        private int skillsUpdated;
        private int rolesAdded;
        private int rolesUpdated;
        private int requirementsAdded;
        private int requirementsUpdated;
        private int progressionsAdded;
        private int progressionsUpdated;
        private int categoriesDeleted;
        private int skillsDeleted;
        private int rolesDeleted;
        private int requirementsDeleted;
        private int progressionsDeleted;

        private SyncResult toResult() {
            return new SyncResult(
                    categoriesAdded,
                    categoriesUpdated,
                    skillsAdded,
                    skillsUpdated,
                    rolesAdded,
                    rolesUpdated,
                    requirementsAdded,
                    requirementsUpdated,
                    progressionsAdded,
                    progressionsUpdated,
                    categoriesDeleted,
                    skillsDeleted,
                    rolesDeleted,
                    requirementsDeleted,
                    progressionsDeleted
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<YamlCompetencyData.CategoryData> parseCategories(List<Map<String, Object>> categoriesList) {
        if (categoriesList == null) {
            return List.of();
        }

        List<YamlCompetencyData.CategoryData> categories = new ArrayList<>();
        int displayOrder = 0;

        for (Map<String, Object> categoryMap : categoriesList) {
            String name = (String) categoryMap.get("name");
            List<Map<String, Object>> skillsList = (List<Map<String, Object>>) categoryMap.get("skills");

            List<YamlCompetencyData.SkillData> skills = new ArrayList<>();
            if (skillsList != null) {
                for (Map<String, Object> skillMap : skillsList) {
                    String skillName = (String) skillMap.get("name");
                    Map<String, String> levels = (Map<String, String>) skillMap.get("levels");

                    skills.add(new YamlCompetencyData.SkillData(skillName, name, levels));
                }
            }

            categories.add(new YamlCompetencyData.CategoryData(name, displayOrder++, skills));
        }

        return categories;
    }

    @SuppressWarnings("unchecked")
    private List<YamlCompetencyData.RoleData> parseRoles(List<Map<String, Object>> rolesList) {
        if (rolesList == null) {
            return List.of();
        }

        List<YamlCompetencyData.RoleData> roles = new ArrayList<>();

        for (Map<String, Object> roleMap : rolesList) {
            String name = (String) roleMap.get("name");
            String description = (String) roleMap.get("description");
            String roleFamily = (String) roleMap.get("roleFamily");
            Integer seniorityOrder = (Integer) roleMap.get("seniorityOrder");
            List<Map<String, Object>> requirementsList = (List<Map<String, Object>>) roleMap.get("requirements");

            List<YamlCompetencyData.RequirementData> requirements = new ArrayList<>();
            if (requirementsList != null) {
                for (Map<String, Object> reqMap : requirementsList) {
                    String skillName = (String) reqMap.get("skill");
                    String categoryName = (String) reqMap.get("category");
                    String level = (String) reqMap.get("level");

                    requirements.add(new YamlCompetencyData.RequirementData(skillName, categoryName, level));
                }
            }

            roles.add(new YamlCompetencyData.RoleData(name, description, roleFamily, seniorityOrder, requirements));
        }

        return roles;
    }

    @SuppressWarnings("unchecked")
    private List<YamlCompetencyData.ProgressionData> parseProgressions(List<Map<String, Object>> progressionsList) {
        if (progressionsList == null) {
            return List.of();
        }

        List<YamlCompetencyData.ProgressionData> progressions = new ArrayList<>();

        for (Map<String, Object> progressionMap : progressionsList) {
            String from = (String) progressionMap.get("from");
            String to = (String) progressionMap.get("to");

            progressions.add(new YamlCompetencyData.ProgressionData(from, to));
        }

        return progressions;
    }

    /**
     * Discovers YAML resources from the classpath (JAR-compatible).
     * Reads an index.txt file in the directory that lists all YAML files.
     * This approach works in both development (file system) and production (JAR).
     *
     * @param directoryPath directory path relative to classpath resources (e.g., "seed/categories")
     * @return list of resource paths that exist on the classpath
     */
    private List<String> discoverYamlResources(String directoryPath) {
        String indexPath = directoryPath + "/index.txt";
        List<String> foundResources = new ArrayList<>();

        try (InputStream indexStream = getClass().getClassLoader().getResourceAsStream(indexPath)) {
            if (indexStream == null) {
                log.warn("Index file not found: {}", indexPath);
                return foundResources;
            }

            // Read index file line by line
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(indexStream, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    String resourcePath = directoryPath + "/" + line;

                    // Verify the resource exists
                    if (getClass().getClassLoader().getResource(resourcePath) != null) {
                        foundResources.add(resourcePath);
                        log.debug("Found resource: {}", resourcePath);
                    } else {
                        log.warn("Resource listed in index but not found: {}", resourcePath);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to read index file: {}", indexPath, e);
        }

        return foundResources;
    }


    /**
     * Parses a single YAML resource from the classpath (JAR-compatible).
     * T006: Filename-to-entity mapping helper method
     * Handles both single entity files (category/role files) and full competencies files (progressions)
     *
     * @param resourcePath classpath resource path (e.g., "seed/categories/programming.yaml")
     * @return parsed YamlCompetencyData
     * @throws RuntimeException if parsing fails
     */
    @SuppressWarnings("unchecked")
    private YamlCompetencyData parseYamlResource(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }

            Yaml yaml = new Yaml();
            Object rawData = yaml.load(inputStream);

            String filename = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);

            // Handle single entity files (categories/roles) vs progressions list
            if (rawData instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) rawData;

                // Check if this is a category file (has 'skills' key)
                if (map.containsKey("skills")) {
                    // Single category entity
                    List<YamlCompetencyData.CategoryData> categories = parseCategories(List.of(map));
                    return new YamlCompetencyData(categories, List.of(), List.of());
                }
                // Check if this is a role file (has 'requirements' key)
                else if (map.containsKey("requirements") || map.containsKey("name")) {
                    // Single role entity
                    List<YamlCompetencyData.RoleData> roles = parseRoles(List.of(map));
                    return new YamlCompetencyData(List.of(), roles, List.of());
                }
            }
            // Handle progressions list (array of progression objects)
            else if (rawData instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) rawData;
                List<YamlCompetencyData.ProgressionData> progressions = parseProgressions(list);
                return new YamlCompetencyData(List.of(), List.of(), progressions);
            }

            throw new RuntimeException("Unrecognized YAML structure in " + filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse " + resourcePath + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("YAML syntax error in " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Detects duplicate category names across multiple files.
     * T007: Duplicate detection for categories
     *
     * @param categories list of all category data from all files
     * @param fileMap    map of category name to source filename
     * @throws IllegalStateException if duplicates found
     */
    private void detectDuplicateCategories(List<YamlCompetencyData.CategoryData> categories,
                                           Map<String, String> fileMap) {
        Map<String, String> seen = new HashMap<>();

        for (YamlCompetencyData.CategoryData category : categories) {
            String name = category.name();

            if (seen.containsKey(name)) {
                String error = String.format(
                        "Duplicate category '%s' found in files: %s and %s",
                        name, seen.get(name), fileMap.get(name)
                );
                log.error(error);
                throw new IllegalStateException(error);
            }

            seen.put(name, fileMap.get(name));
        }
    }

    /**
     * Detects duplicate role names across multiple files.
     * T008: Duplicate detection for roles
     *
     * @param roles    list of all role data from all files
     * @param fileMap  map of role name to source filename
     * @throws IllegalStateException if duplicates found
     */
    private void detectDuplicateRoles(List<YamlCompetencyData.RoleData> roles,
                                      Map<String, String> fileMap) {
        Map<String, String> seen = new HashMap<>();

        for (YamlCompetencyData.RoleData role : roles) {
            String name = role.name();

            if (seen.containsKey(name)) {
                String error = String.format(
                        "Duplicate role '%s' found in files: %s and %s",
                        name, seen.get(name), fileMap.get(name)
                );
                log.error(error);
                throw new IllegalStateException(error);
            }

            seen.put(name, fileMap.get(name));
        }
    }
}
