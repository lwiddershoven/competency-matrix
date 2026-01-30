package nl.leonw.competencymatrix.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.dto.CellData;
import nl.leonw.competencymatrix.dto.CompetencyMatrix;
import nl.leonw.competencymatrix.dto.MatrixViewModel;
import nl.leonw.competencymatrix.dto.RoleInfo;
import nl.leonw.competencymatrix.dto.SkillInfo;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.ProficiencyLevel;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.repository.CategoryRepository;
import nl.leonw.competencymatrix.repository.RoleRepository;
import nl.leonw.competencymatrix.repository.RoleSkillRequirementRepository;
import nl.leonw.competencymatrix.repository.SkillRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class CompetencyService {

    @Inject
    RoleRepository roleRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    SkillRepository skillRepository;

    @Inject
    RoleSkillRequirementRepository requirementRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findAllOrderByName();
    }

    public Optional<Role> getRoleById(Integer id) {
        return roleRepository.findById(id);
    }

    public List<CompetencyCategory> getAllCategories() {
        return categoryRepository.findAllOrderByDisplayOrder();
    }

    public Optional<CompetencyCategory> getCategoryById(Integer id) {
        return categoryRepository.findById(id);
    }

    public Optional<Skill> getSkillById(Integer id) {
        return skillRepository.findById(id);
    }

    public List<Skill> getSkillsByCategory(Integer categoryId) {
        return skillRepository.findByCategoryId(categoryId);
    }

    public List<Skill> getSkillsForRole(Integer roleId) {
        return skillRepository.findByRoleId(roleId);
    }

    public List<RoleSkillRequirement> getRequirementsForRole(Integer roleId) {
        return requirementRepository.findByRoleId(roleId);
    }

    public Optional<RoleSkillRequirement> getRequirementForRoleAndSkill(Integer roleId, Integer skillId) {
        return requirementRepository.findByRoleIdAndSkillId(roleId, skillId);
    }

    public List<Role> getNextRoles(Integer roleId) {
        return roleRepository.findNextRoles(roleId);
    }

    public List<Role> getPreviousRoles(Integer roleId) {
        return roleRepository.findPreviousRoles(roleId);
    }

    /**
     * Get skills grouped by category for a specific role
     */
    public Map<CompetencyCategory, List<SkillWithRequirement>> getSkillsByCategoryForRole(Integer roleId) {
        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();
        List<RoleSkillRequirement> requirements = requirementRepository.findByRoleId(roleId);

        Map<Integer, RoleSkillRequirement> requirementsBySkillId = new HashMap<>();
        for (RoleSkillRequirement req : requirements) {
            requirementsBySkillId.put(req.skillId(), req);
        }

        Map<CompetencyCategory, List<SkillWithRequirement>> result = new LinkedHashMap<>();
        for (CompetencyCategory category : categories) {
            List<Skill> skills = skillRepository.findByCategoryId(category.id());
            List<SkillWithRequirement> skillsWithReqs = new ArrayList<>();

            for (Skill skill : skills) {
                if (skill != null) {
                    RoleSkillRequirement req = requirementsBySkillId.get(skill.id());
                    if (req != null) {
                        skillsWithReqs.add(new SkillWithRequirement(skill, req.getProficiencyLevel()));
                    }
                }
            }

            if (!skillsWithReqs.isEmpty()) {
                result.put(category, skillsWithReqs);
            }
        }
        return result;
    }

    /**
     * Get all skills grouped by category with optional category filtering
     */
    public Map<CompetencyCategory, List<Skill>> getAllSkillsByCategory(Integer categoryId) {
        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();

        Map<CompetencyCategory, List<Skill>> result = new LinkedHashMap<>();

        for (CompetencyCategory category : categories) {
            if (categoryId == null || categoryId.equals(category.id())) {
                List<Skill> skills = skillRepository.findByCategoryId(category.id());

                // Sort skills alphabetically (case-insensitive)
                skills.sort((s1, s2) -> s1.name().compareToIgnoreCase(s2.name()));

                if (!skills.isEmpty()) {
                    result.put(category, skills);
                }
            }
        }

        return result;
    }

    /**
     * Compare skill requirements between two roles
     */
    public List<SkillComparison> compareRoles(Integer fromRoleId, Integer toRoleId) {
        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();
        Map<Integer, RoleSkillRequirement> fromReqs = toMap(requirementRepository.findByRoleId(fromRoleId));
        Map<Integer, RoleSkillRequirement> toReqs = toMap(requirementRepository.findByRoleId(toRoleId));

        Set<Integer> allSkillIds = new HashSet<>();
        allSkillIds.addAll(fromReqs.keySet());
        allSkillIds.addAll(toReqs.keySet());

        List<SkillComparison> comparisons = new ArrayList<>();
        for (CompetencyCategory category : categories) {
            List<Skill> skills = skillRepository.findByCategoryId(category.id());
            for (Skill skill : skills) {
                if (allSkillIds.contains(skill.id())) {
                    RoleSkillRequirement fromReq = fromReqs.get(skill.id());
                    RoleSkillRequirement toReq = toReqs.get(skill.id());

                    ProficiencyLevel fromLevel = fromReq != null ? fromReq.getProficiencyLevel() : null;
                    ProficiencyLevel toLevel = toReq != null ? toReq.getProficiencyLevel() : null;

                    comparisons.add(new SkillComparison(category, skill, fromLevel, toLevel));
                }
            }
        }
        return comparisons;
    }

    private Map<Integer, RoleSkillRequirement> toMap(List<RoleSkillRequirement> requirements) {
        Map<Integer, RoleSkillRequirement> map = new HashMap<>();
        for (RoleSkillRequirement req : requirements) {
            map.put(req.skillId(), req);
        }
        return map;
    }

    /**
     * Build matrix view model with all skills and roles using explicit lookup.
     * Feature: 004-matrix-overview
     * Uses CompetencyMatrix with get(roleName, skillName) for explicit cell lookups.
     *
     * @param categoryId Optional category ID to filter skills (null = show all)
     * @return MatrixViewModel with matrix, ordered roles/skills, and filter state
     */
    public MatrixViewModel buildMatrixViewModel(Integer categoryId) {
        // Load data in correct order
        List<Skill> skills = loadSkillsAlphabetical(categoryId);
        List<Role> roles = loadRolesOrderedByFamilyAndSeniority();

        // Build ordered lists with IDs
        List<SkillInfo> skillsInOrder = buildSkillInfos(skills);
        List<RoleInfo> rolesInOrder = buildRoleInfos(roles);

        // Build matrix with role->skill->level mapping
        CompetencyMatrix matrix = buildCompetencyMatrix(roles, skills);

        // Build auxiliary data structures
        Map<String, List<RoleInfo>> rolesByFamily = groupRolesByFamily(rolesInOrder);
        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();

        return buildMatrixViewModel(categoryId, matrix, skillsInOrder, rolesInOrder, rolesByFamily, categories);
    }

    /**
     * Load skills in alphabetical order (pure alphabetical, no category grouping).
     */
    private List<Skill> loadSkillsAlphabetical(Integer categoryId) {
        List<Skill> skills;
        if (categoryId == null) {
            skills = skillRepository.findAllOrderByName();
        } else {
            skills = skillRepository.findByCategoryId(categoryId);
        }
        // Ensure alphabetical ordering (case-insensitive)
        skills.sort((s1, s2) -> s1.name().compareToIgnoreCase(s2.name()));
        return skills;
    }

    /**
     * Load roles ordered by role family (alphabetical), then seniority order (ascending).
     */
    private List<Role> loadRolesOrderedByFamilyAndSeniority() {
        return roleRepository.findAllOrderByFamilyAndSeniority();
    }

    private List<SkillInfo> buildSkillInfos(List<Skill> skills) {
        List<SkillInfo> skillInfos = new ArrayList<>();
        for (Skill skill : skills) {
            skillInfos.add(new SkillInfo(skill.id(), skill.name()));
        }
        return skillInfos;
    }

    private List<RoleInfo> buildRoleInfos(List<Role> roles) {
        List<RoleInfo> roleInfos = new ArrayList<>();
        for (Role role : roles) {
            roleInfos.add(new RoleInfo(role.id(), role.name(), role.roleFamily()));
        }
        return roleInfos;
    }

    private Map<String, List<RoleInfo>> groupRolesByFamily(List<RoleInfo> rolesInOrder) {
        Map<String, List<RoleInfo>> rolesByFamily = new LinkedHashMap<>();
        for (RoleInfo roleInfo : rolesInOrder) {
            rolesByFamily
                    .computeIfAbsent(roleInfo.family(), k -> new ArrayList<>())
                    .add(roleInfo);
        }
        return rolesByFamily;
    }

    /**
     * Build CompetencyMatrix with role->skill->level mappings.
     * Uses explicit role and skill names for lookups.
     */
    private CompetencyMatrix buildCompetencyMatrix(List<Role> roles, List<Skill> skills) {
        // Load all requirements into a map for efficient lookup
        List<RoleSkillRequirement> allRequirements = requirementRepository.findAll();
        Map<String, ProficiencyLevel> requirementsMap = new HashMap<>();
        for (RoleSkillRequirement req : allRequirements) {
            requirementsMap.put(requirementKey(req.skillId(), req.roleId()), req.getProficiencyLevel());
        }

        // Build role->skill->level map
        Map<String, Map<String, String>> matrixData = new HashMap<>();

        for (Role role : roles) {
            Map<String, String> skillLevels = new HashMap<>();
            for (Skill skill : skills) {
                String key = requirementKey(skill.id(), role.id());
                ProficiencyLevel level = requirementsMap.get(key);
                if (level != null) {
                    skillLevels.put(skill.name(), level.getDisplayName());
                }
            }
            matrixData.put(role.name(), skillLevels);
        }

        return new CompetencyMatrix(matrixData);
    }

    private String requirementKey(Integer skillId, Integer roleId) {
        return skillId + ":" + roleId;
    }

    private MatrixViewModel buildMatrixViewModel(
            Integer categoryId,
            CompetencyMatrix matrix,
            List<SkillInfo> skillsInOrder,
            List<RoleInfo> rolesInOrder,
            Map<String, List<RoleInfo>> rolesByFamily,
            List<CompetencyCategory> categories
    ) {
        if (categoryId != null) {
            return MatrixViewModel.filtered(
                    matrix, skillsInOrder, rolesInOrder, rolesByFamily, categories, categoryId.toString()
            );
        }
        return MatrixViewModel.unfiltered(
                matrix, skillsInOrder, rolesInOrder, rolesByFamily, categories
        );
    }

    public record SkillWithRequirement(Skill skill, ProficiencyLevel requiredLevel) {}

    public record SkillComparison(
            CompetencyCategory category,
            Skill skill,
            ProficiencyLevel fromLevel,
            ProficiencyLevel toLevel
    ) {
        public boolean hasChanged() {
            return !Objects.equals(fromLevel, toLevel);
        }

        public boolean isUpgrade() {
            if (fromLevel == null && toLevel != null) return true;
            if (fromLevel == null || toLevel == null) return false;
            return toLevel.ordinal() > fromLevel.ordinal();
        }

        public boolean isDowngrade() {
            if (toLevel == null && fromLevel != null) return true;
            if (fromLevel == null || toLevel == null) return false;
            return toLevel.ordinal() < fromLevel.ordinal();
        }

        public boolean isNew() {
            return fromLevel == null && toLevel != null;
        }

        public boolean isRemoved() {
            return fromLevel != null && toLevel == null;
        }
    }
}
