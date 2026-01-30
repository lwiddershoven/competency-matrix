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
     * Build matrix view model with all skills and roles using map structure.
     * Feature: 004-matrix-overview
     * Uses map of maps with string keys (skillName -> roleName -> cellData)
     * for perfect alignment between headers and cells.
     *
     * @param categoryId Optional category ID to filter skills (null = show all)
     * @return MatrixViewModel with matrix map, ordered roles, and filter state
     */
    public MatrixViewModel buildMatrixViewModel(Integer categoryId) {
        // Fetch skills (filtered or all)
        List<Skill> skills = (categoryId != null)
                ? skillRepository.findByCategoryId(categoryId)
                : skillRepository.findAllOrderByName();

        // Fetch all roles ordered by family + seniority
        List<Role> allRoles = roleRepository.findAllOrderByFamilyAndSeniority();

        // Fetch all requirements once to avoid N+1 queries
        List<RoleSkillRequirement> allRequirements = requirementRepository.findAll();

        // Create lookup map: (skillId, roleId) -> ProficiencyLevel
        Map<String, ProficiencyLevel> requirementsMap = new HashMap<>();
        for (RoleSkillRequirement req : allRequirements) {
            String key = req.skillId() + ":" + req.roleId();
            requirementsMap.put(key, req.getProficiencyLevel());
        }

        // Build rolesInOrder - the canonical order for both headers and cells
        List<nl.leonw.competencymatrix.dto.RoleInfo> rolesInOrder = new ArrayList<>();
        for (Role role : allRoles) {
            rolesInOrder.add(new nl.leonw.competencymatrix.dto.RoleInfo(
                    role.id(), role.name(), role.roleFamily()
            ));
        }

        // Build rolesByFamily - grouped by family for header display
        Map<String, List<nl.leonw.competencymatrix.dto.RoleInfo>> rolesByFamily = new LinkedHashMap<>();
        for (nl.leonw.competencymatrix.dto.RoleInfo roleInfo : rolesInOrder) {
            rolesByFamily
                    .computeIfAbsent(roleInfo.family(), k -> new ArrayList<>())
                    .add(roleInfo);
        }

        // Build matrix: skillName -> (roleName -> cellData)
        Map<String, Map<String, nl.leonw.competencymatrix.dto.CellData>> matrix = new LinkedHashMap<>();
        for (Skill skill : skills) {
            Map<String, nl.leonw.competencymatrix.dto.CellData> skillRow = new LinkedHashMap<>();

            // Create cell for each role in the same order as rolesInOrder
            for (nl.leonw.competencymatrix.dto.RoleInfo roleInfo : rolesInOrder) {
                String key = skill.id() + ":" + roleInfo.id();
                ProficiencyLevel level = requirementsMap.get(key);

                if (level != null) {
                    skillRow.put(roleInfo.name(), nl.leonw.competencymatrix.dto.CellData.withLevel(
                            skill.id().longValue(), roleInfo.id(), level
                    ));
                } else {
                    skillRow.put(roleInfo.name(), nl.leonw.competencymatrix.dto.CellData.empty(
                            skill.id().longValue(), roleInfo.id()
                    ));
                }
            }

            matrix.put(skill.name(), skillRow);
        }

        // Fetch all categories for filter dropdown
        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();

        // Return filtered or unfiltered view model
        if (categoryId != null) {
            return MatrixViewModel.filtered(
                    matrix, rolesInOrder, rolesByFamily, categories, categoryId.toString()
            );
        } else {
            return MatrixViewModel.unfiltered(
                    matrix, rolesInOrder, rolesByFamily, categories
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
