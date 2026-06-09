package db.table;

import db.DatabaseException;
import db.model.ProductGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupTable {
    private final Connection connection;

    public GroupTable(Connection connection) {
        this.connection = connection;
    }

    public int createGroup(ProductGroup group) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO product_group(name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, group.getName());

            int inserted = ps.executeUpdate();
            if (inserted < 1) {
                throw new DatabaseException("Insert failed");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new DatabaseException("Can't get group id");
        } catch (SQLException e) {
            throw new DatabaseException("Can't add group: " + group, e);
        }
    }

    public int getGroupsCount() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM product_group")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Can't count groups", e);
        }
    }

    public List<ProductGroup> getAllGroups() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product_group")) {
            List<ProductGroup> groups = new ArrayList<>();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(toProductGroup(rs));
                }
            }

            return groups;
        } catch (SQLException e) {
            throw new DatabaseException("Can't read groups", e);
        }
    }

    public Optional<ProductGroup> getGroup(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product_group WHERE id = ?")) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toProductGroup(rs));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Can't read group by id: " + id, e);
        }
    }

    public Optional<ProductGroup> getGroup(String name) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product_group WHERE name = ?")) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toProductGroup(rs));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Can't read group by name: " + name, e);
        }
    }

    public void updateGroup(ProductGroup group) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product_group SET name = ? WHERE id = ?")) {
            ps.setString(1, group.getName());
            ps.setInt(2, group.getId());

            int updated = ps.executeUpdate();
            if (updated < 1) {
                throw new DatabaseException("Changes to group have not been applied");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Can't update group: " + group, e);
        }
    }

    public void deleteGroup(int id) {
        try (PreparedStatement ps = connection.prepareStatement("""
                DELETE FROM product_group
                WHERE id = ? AND NOT EXISTS (
                    SELECT 1
                    FROM product
                    WHERE group_id = ?
                )
                """)) {
            ps.setInt(1, id);
            ps.setInt(2, id);

            int deleted = ps.executeUpdate();
            if (deleted < 1) {
                throw new DatabaseException("Changes to group have not been applied");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Can't delete group by id: " + id, e);
        }
    }

    public void addProductToGroup(int groupId, int productId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE product SET group_id = ? WHERE id = ? AND EXISTS (SELECT 1 FROM product_group WHERE id = ?)")) {
            ps.setInt(1, groupId);
            ps.setInt(2, productId);
            ps.setInt(3, groupId);

            int updated = ps.executeUpdate();
            if (updated < 1) {
                throw new DatabaseException("Changes to group have not been applied");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Can't add product to group", e);
        }
    }

    public boolean hasProductsInGroup(int groupId) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM product
                WHERE group_id = ?
                """)) {
            ps.setInt(1, groupId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;
        } catch (SQLException e) {
            throw new DatabaseException("Can't check group products: " + groupId, e);
        }
    }

    public boolean isProductInGroup(int groupId, int productId) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM product
                WHERE group_id = ? AND id = ?
                """)) {
            ps.setInt(1, groupId);
            ps.setInt(2, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;
        } catch (SQLException e) {
            throw new DatabaseException("Can't check product group", e);
        }
    }

    private ProductGroup toProductGroup(ResultSet rs) throws SQLException {
        return new ProductGroup(rs.getInt("id"), rs.getString("name"));
    }
}
