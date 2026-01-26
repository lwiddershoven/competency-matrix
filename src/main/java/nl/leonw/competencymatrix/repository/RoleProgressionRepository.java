package nl.leonw.competencymatrix.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.RoleProgression;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@ApplicationScoped
public class RoleProgressionRepository {

    @Inject
    DataSource dataSource;

    public Optional<RoleProgression> findByFromRoleIdAndToRoleId(Integer fromRoleId, Integer toRoleId) {
        String sql = "SELECT id, from_role_id, to_role_id FROM role_progression WHERE from_role_id = ? AND to_role_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fromRoleId);
            stmt.setInt(2, toRoleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch progression by from and to roles: " + fromRoleId + " -> " + toRoleId, e);
        }
    }

    public Optional<RoleProgression> findById(Integer id) {
        String sql = "SELECT id, from_role_id, to_role_id FROM role_progression WHERE id = ?";
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
            throw new RuntimeException("Failed to fetch progression by id: " + id, e);
        }
    }

    public RoleProgression save(RoleProgression progression) {
        if (progression.id() == null) {
            return insert(progression);
        } else {
            return update(progression);
        }
    }

    private RoleProgression insert(RoleProgression progression) {
        String sql = "INSERT INTO role_progression (from_role_id, to_role_id) VALUES (?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, progression.fromRoleId());
            stmt.setInt(2, progression.toRoleId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RoleProgression(rs.getInt(1), progression.fromRoleId(), progression.toRoleId());
                }
                throw new SQLException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert progression", e);
        }
    }

    private RoleProgression update(RoleProgression progression) {
        String sql = "UPDATE role_progression SET from_role_id = ?, to_role_id = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, progression.fromRoleId());
            stmt.setInt(2, progression.toRoleId());
            stmt.setInt(3, progression.id());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Update failed, progression not found: " + progression.id());
            }
            return progression;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update progression: " + progression.id(), e);
        }
    }

    private RoleProgression mapRow(ResultSet rs) throws SQLException {
        return new RoleProgression(
                rs.getInt("id"),
                rs.getInt("from_role_id"),
                rs.getInt("to_role_id")
        );
    }
}
