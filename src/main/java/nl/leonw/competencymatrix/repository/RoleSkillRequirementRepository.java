package nl.leonw.competencymatrix.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.RoleSkillRequirement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoleSkillRequirementRepository {

    @Inject
    DataSource dataSource;

    /**
     * Find all role-skill requirements.
     * Used for matrix overview (Feature 004).
     */
    public List<RoleSkillRequirement> findAll() {
        String sql = "SELECT id, role_id, skill_id, required_level FROM role_skill_requirement";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<RoleSkillRequirement> requirements = new ArrayList<>();
            while (rs.next()) {
                requirements.add(mapRow(rs));
            }
            return requirements;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all requirements", e);
        }
    }

    public List<RoleSkillRequirement> findByRoleId(Integer roleId) {
        String sql = "SELECT id, role_id, skill_id, required_level FROM role_skill_requirement WHERE role_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<RoleSkillRequirement> requirements = new ArrayList<>();
                while (rs.next()) {
                    requirements.add(mapRow(rs));
                }
                return requirements;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch requirements by role: " + roleId, e);
        }
    }

    public Optional<RoleSkillRequirement> findById(Integer id) {
        String sql = "SELECT id, role_id, skill_id, required_level FROM role_skill_requirement WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch requirement by id: " + id, e);
        }
    }

    public Optional<RoleSkillRequirement> findByRoleIdAndSkillId(Integer roleId, Integer skillId) {
        String sql = "SELECT id, role_id, skill_id, required_level FROM role_skill_requirement WHERE role_id = ? AND skill_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            stmt.setInt(2, skillId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch requirement by role and skill: " + roleId + ", " + skillId, e);
        }
    }

    public Optional<RoleSkillRequirement> findByRoleIdAndSkillIdAndRequiredLevel(Integer roleId, Integer skillId, String requiredLevel) {
        String sql = "SELECT id, role_id, skill_id, required_level FROM role_skill_requirement WHERE role_id = ? AND skill_id = ? AND required_level = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            stmt.setInt(2, skillId);
            stmt.setString(3, requiredLevel);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch requirement by role, skill, and level", e);
        }
    }

    public RoleSkillRequirement save(RoleSkillRequirement requirement) {
        if (requirement.id() == null) {
            return insert(requirement);
        } else {
            return update(requirement);
        }
    }

    private RoleSkillRequirement insert(RoleSkillRequirement requirement) {
        String sql = "INSERT INTO role_skill_requirement (role_id, skill_id, required_level) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, requirement.roleId());
            stmt.setInt(2, requirement.skillId());
            stmt.setString(3, requirement.requiredLevel());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new RoleSkillRequirement(rs.getInt(1), requirement.roleId(),
                            requirement.skillId(), requirement.requiredLevel());
                }
                throw new SQLException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert requirement", e);
        }
    }

    private RoleSkillRequirement update(RoleSkillRequirement requirement) {
        String sql = "UPDATE role_skill_requirement SET role_id = ?, skill_id = ?, required_level = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requirement.roleId());
            stmt.setInt(2, requirement.skillId());
            stmt.setString(3, requirement.requiredLevel());
            stmt.setInt(4, requirement.id());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Update failed, requirement not found: " + requirement.id());
            }
            return requirement;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update requirement: " + requirement.id(), e);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM role_skill_requirement";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all requirements", e);
        }
    }

    private RoleSkillRequirement mapRow(ResultSet rs) throws SQLException {
        return new RoleSkillRequirement(
                rs.getInt("id"),
                rs.getInt("role_id"),
                rs.getInt("skill_id"),
                rs.getString("required_level")
        );
    }
}
