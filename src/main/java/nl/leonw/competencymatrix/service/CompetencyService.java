package nl.leonw.competencymatrix.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.dto.MatrixViewModel;
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
     * Build matrix view model with all skills and roles.
     * Feature: 004-matrix-overview
     * Tasks: T018-T020 - Implementation for Phase 3 User Story 1
     *
     * @param categoryId Optional category ID to filter skills (null = show all)
     * @return MatrixViewModel with rows, column headers, and filter state
     */
    public MatrixViewModel buildMatrixViewModel(Integer categoryId) {
        // T018: Core logic - fetch data and build structure
        // 1. Fetch skills (filtered or all)
        List<Skill> skills = (categoryId != null)
                ? skillRepository.findByCategoryId(categoryId)
                : skillRepository.findAllOrderByName();

        // 2. Fetch all roles ordered by family + seniority
        List<Role> allRoles = roleRepository.findAllOrderByFamilyAndSeniority();

        // 3. Fetch all requirements once to avoid N+1 queries
        List<RoleSkillRequirement> allRequirements = requirementRepository.findAll();

        // Create lookup map: (skillId, roleId) -> ProficiencyLevel
        Map<String, nl.leonw.competencymatrix.model.ProficiencyLevel> requirementsMap = new HashMap<>();
        for (RoleSkillRequirement req : allRequirements) {
            String key = req.skillId() + ":" + req.roleId();
            requirementsMap.put(key, req.getProficiencyLevel());
        }

        // T019: Role grouping and sorting - group roles by family
        Map<String, List<nl.leonw.competencymatrix.dto.MatrixColumnHeader>> rolesByFamily = new LinkedHashMap<>();
        for (Role role : allRoles) {
            rolesByFamily
                    .computeIfAbsent(role.roleFamily(), k -> new ArrayList<>())
                    .add(nl.leonw.competencymatrix.dto.MatrixColumnHeader.from(role));
        }

        // T020: MatrixRow construction with empty cell handling
        List<nl.leonw.competencymatrix.dto.MatrixRow> rows = new ArrayList<>();
        for (Skill skill : skills) {
            List<nl.leonw.competencymatrix.dto.MatrixCell> cells = new ArrayList<>();

            // Create cell for each role (empty if no requirement exists)
            for (Role role : allRoles) {
                String key = skill.id() + ":" + role.id();
                nl.leonw.competencymatrix.model.ProficiencyLevel level = requirementsMap.get(key);

                if (level != null) {
                    cells.add(nl.leonw.competencymatrix.dto.MatrixCell.withLevel(
                            skill.id().longValue(), role.id(), level
                    ));
                } else {
                    cells.add(nl.leonw.competencymatrix.dto.MatrixCell.empty(
                            skill.id().longValue(), role.id()
                    ));
                }
            }

            rows.add(new nl.leonw.competencymatrix.dto.MatrixRow(skill, cells));
        }

        // Fetch all categories for filter dropdown
        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();

        // Return filtered or unfiltered view model
        if (categoryId != null) {
            return nl.leonw.competencymatrix.dto.MatrixViewModel.filtered(
                    rows, rolesByFamily, categories, categoryId.toString()
            );
        } else {
            return nl.leonw.competencymatrix.dto.MatrixViewModel.unfiltered(
                    rows, rolesByFamily, categories
            );
        }
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
