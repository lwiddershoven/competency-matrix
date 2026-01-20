package nl.leonw.competencymatrix.service;

import nl.leonw.competencymatrix.model.*;
import nl.leonw.competencymatrix.repository.*;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.ProficiencyLevel;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.repository.CategoryRepository;
import nl.leonw.competencymatrix.repository.RoleRepository;
import nl.leonw.competencymatrix.repository.RoleSkillRequirementRepository;
import nl.leonw.competencymatrix.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class CompetencyService {

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final RoleSkillRequirementRepository requirementRepository;

    public CompetencyService(RoleRepository roleRepository,
                             CategoryRepository categoryRepository,
                             SkillRepository skillRepository,
                             RoleSkillRequirementRepository requirementRepository) {
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.requirementRepository = requirementRepository;
    }

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
                RoleSkillRequirement req = requirementsBySkillId.get(skill.id());
                if (req != null) {
                    skillsWithReqs.add(new SkillWithRequirement(skill, req.getProficiencyLevel()));
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
