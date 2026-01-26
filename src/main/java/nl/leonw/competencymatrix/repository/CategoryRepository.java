package nl.leonw.competencymatrix.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.CompetencyCategory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CategoryRepository {

    @Inject
    DataSource dataSource;

    public List<CompetencyCategory> findAllOrderByDisplayOrder() {
        String sql = "SELECT id, name, display_order FROM competency_category ORDER BY display_order, name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<CompetencyCategory> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
            return categories;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all categories", e);
        }
    }

    public Optional<CompetencyCategory> findById(Integer id) {
        String sql = "SELECT id, name, display_order FROM competency_category WHERE id = ?";
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
            throw new RuntimeException("Failed to fetch category by id: " + id, e);
        }
    }

    public Optional<CompetencyCategory> findByName(String name) {
        String sql = "SELECT id, name, display_order FROM competency_category WHERE name = ?";
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
            throw new RuntimeException("Failed to fetch category by name: " + name, e);
        }
    }

    public CompetencyCategory save(CompetencyCategory category) {
        if (category.id() == null) {
            return insert(category);
        } else {
            return update(category);
        }
    }

    private CompetencyCategory insert(CompetencyCategory category) {
        String sql = "INSERT INTO competency_category (name, display_order) VALUES (?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category.name());
            stmt.setInt(2, category.displayOrder());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CompetencyCategory(rs.getInt(1), category.name(), category.displayOrder());
                }
                throw new SQLException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert category: " + category.name(), e);
        }
    }

    private CompetencyCategory update(CompetencyCategory category) {
        String sql = "UPDATE competency_category SET name = ?, display_order = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category.name());
            stmt.setInt(2, category.displayOrder());
            stmt.setInt(3, category.id());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Update failed, category not found: " + category.id());
            }
            return category;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update category: " + category.id(), e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM competency_category";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count categories", e);
        }
    }

    private CompetencyCategory mapRow(ResultSet rs) throws SQLException {
        return new CompetencyCategory(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("display_order")
        );
    }
}
