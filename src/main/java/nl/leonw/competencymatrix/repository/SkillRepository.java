package nl.leonw.competencymatrix.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.Skill;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SkillRepository {

    @Inject
    DataSource dataSource;

    public List<Skill> findByCategoryId(Integer categoryId) {
        String sql = "SELECT id, name, category_id, basic_description, decent_description, good_description, excellent_description FROM skill WHERE category_id = ? ORDER BY name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Skill> skills = new ArrayList<>();
                while (rs.next()) {
                    skills.add(mapRow(rs));
                }
                return skills;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch skills by category: " + categoryId, e);
        }
    }

    public Optional<Skill> findById(Integer id) {
        String sql = "SELECT id, name, category_id, basic_description, decent_description, good_description, excellent_description FROM skill WHERE id = ?";
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
            throw new RuntimeException("Failed to fetch skill by id: " + id, e);
        }
    }

    public Optional<Skill> findByNameAndCategoryId(String name, Integer categoryId) {
        String sql = "SELECT id, name, category_id, basic_description, decent_description, good_description, excellent_description FROM skill WHERE name = ? AND category_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setInt(2, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch skill by name and category: " + name + ", " + categoryId, e);
        }
    }

    public List<Skill> findByRoleId(Integer roleId) {
        String sql = """
                SELECT s.id, s.name, s.category_id, s.basic_description, s.decent_description, s.good_description, s.excellent_description
                FROM skill s
                JOIN role_skill_requirement rsr ON s.id = rsr.skill_id
                WHERE rsr.role_id = ?
                ORDER BY s.name
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Skill> skills = new ArrayList<>();
                while (rs.next()) {
                    skills.add(mapRow(rs));
                }
                return skills;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch skills by role: " + roleId, e);
        }
    }

    public Skill save(Skill skill) {
        if (skill.id() == null) {
            return insert(skill);
        } else {
            return update(skill);
        }
    }

    private Skill insert(Skill skill) {
        String sql = "INSERT INTO skill (name, category_id, basic_description, decent_description, good_description, excellent_description) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, skill.name());
            stmt.setInt(2, skill.categoryId());
            stmt.setString(3, skill.basicDescription());
            stmt.setString(4, skill.decentDescription());
            stmt.setString(5, skill.goodDescription());
            stmt.setString(6, skill.excellentDescription());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Skill(rs.getInt(1), skill.name(), skill.categoryId(),
                            skill.basicDescription(), skill.decentDescription(),
                            skill.goodDescription(), skill.excellentDescription());
                }
                throw new SQLException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert skill: " + skill.name(), e);
        }
    }

    private Skill update(Skill skill) {
        String sql = "UPDATE skill SET name = ?, category_id = ?, basic_description = ?, decent_description = ?, good_description = ?, excellent_description = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, skill.name());
            stmt.setInt(2, skill.categoryId());
            stmt.setString(3, skill.basicDescription());
            stmt.setString(4, skill.decentDescription());
            stmt.setString(5, skill.goodDescription());
            stmt.setString(6, skill.excellentDescription());
            stmt.setInt(7, skill.id());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Update failed, skill not found: " + skill.id());
            }
            return skill;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update skill: " + skill.id(), e);
        }
    }

    private Skill mapRow(ResultSet rs) throws SQLException {
        return new Skill(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("category_id"),
                rs.getString("basic_description"),
                rs.getString("decent_description"),
                rs.getString("good_description"),
                rs.getString("excellent_description")
        );
    }
}
