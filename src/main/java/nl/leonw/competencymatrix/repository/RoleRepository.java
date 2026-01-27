package nl.leonw.competencymatrix.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.Role;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoleRepository {

    @Inject
    DataSource dataSource;

    public List<Role> findAllOrderByName() {
        String sql = "SELECT id, name, description FROM rolename ORDER BY name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Role> roles = new ArrayList<>();
            while (rs.next()) {
                roles.add(mapRow(rs));
            }
            return roles;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all roles", e);
        }
    }

    public Optional<Role> findById(Integer id) {
        String sql = "SELECT id, name, description FROM rolename WHERE id = ?";
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
            throw new RuntimeException("Failed to fetch role by id: " + id, e);
        }
    }

    public Optional<Role> findByName(String name) {
        String sql = "SELECT id, name, description FROM rolename WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch role by name: " + name, e);
        }
    }

    public Optional<Role> findByNameIgnoreCase(String name) {
        String sql = "SELECT id, name, description FROM rolename WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch role by name (ignore case): " + name, e);
        }
    }

    public List<Role> findNextRoles(Integer roleId) {
        String sql = """
                SELECT r.id, r.name, r.description FROM rolename r
                JOIN role_progression rp ON r.id = rp.to_role_id
                WHERE rp.from_role_id = ?
                ORDER BY r.name
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Role> roles = new ArrayList<>();
                while (rs.next()) {
                    roles.add(mapRow(rs));
                }
                return roles;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch next roles for role: " + roleId, e);
        }
    }

    public List<Role> findPreviousRoles(Integer roleId) {
        String sql = """
                SELECT r.id, r.name, r.description FROM rolename r
                JOIN role_progression rp ON r.id = rp.from_role_id
                WHERE rp.to_role_id = ?
                ORDER BY r.name
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Role> roles = new ArrayList<>();
                while (rs.next()) {
                    roles.add(mapRow(rs));
                }
                return roles;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch previous roles for role: " + roleId, e);
        }
    }

    public Role save(Role role) {
        if (role.id() == null) {
            return insert(role);
        } else {
            return update(role);
        }
    }

    private Role insert(Role role) {
        String sql = "INSERT INTO rolename (name, description, role_family, seniority_order) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());
            stmt.setString(2, role.description());
            stmt.setString(3, role.roleFamily());
            stmt.setInt(4, role.seniorityOrder());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Role(rs.getInt(1), role.name(), role.description(), role.roleFamily(), role.seniorityOrder());
                }
                throw new SQLException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert role: " + role.name(), e);
        }
    }

    private Role update(Role role) {
        String sql = "UPDATE rolename SET name = ?, description = ?, role_family = ?, seniority_order = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());
            stmt.setString(2, role.description());
            stmt.setString(3, role.roleFamily());
            stmt.setInt(4, role.seniorityOrder());
            stmt.setInt(5, role.id());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Update failed, role not found: " + role.id());
            }
            return role;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update role: " + role.id(), e);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM rolename";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all roles", e);
        }
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        return new Role(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("role_family"),
                rs.getInt("seniority_order")
        );
    }
}
