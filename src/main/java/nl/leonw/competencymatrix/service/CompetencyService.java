package nl.leonw.competencymatrix.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.dto.MatrixViewModel;
import nl.leonw.competencymatrix.model.*;
import nl.leonw.competencymatrix.repository.*;

import java.util.*;

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
     * Task: T012 - Method stub for Phase 3 implementation
     *
     * @param categoryId Optional category ID to filter skills (null = show all)
     * @return MatrixViewModel with rows, column headers, and filter state
     */
    public MatrixViewModel buildMatrixViewModel(Integer categoryId) {
        // TODO: Implement in Phase 3 (T013-T027)
        // Will build:
        // 1. Fetch all skills (or filtered by categoryId)
        // 2. Fetch all roles ordered by roleFamily + seniorityOrder
        // 3. Group roles by family (Developer, Architect, Operations)
        // 4. For each skill, create MatrixRow with cells for each role
        // 5. Return MatrixViewModel.filtered() or .unfiltered()
        throw new UnsupportedOperationException("buildMatrixViewModel not yet implemented - Phase 3");
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
