package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class GroupTable {
    private final Connection connection;

    GroupTable(Connection connection) {
        this.connection = connection;
    }

    void add(String group) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO product_group(name) VALUES (?)")) {
            ps.setString(1, group);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't add group: " + group, e);
        }
    }

    void addProductToGroup(int groupId, int productId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE product SET group_id = ? WHERE id = ?")) {
            ps.setInt(1, groupId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't add product to group", e);
        }
    }

    boolean exists(String group) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM product_group WHERE name = ?")) {
            ps.setString(1, group);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Can't check group: " + group, e);
        }
    }

    boolean isProductInGroup(String group, String product) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM product p
                JOIN product_group pg ON pg.id = p.group_id
                WHERE pg.name = ? AND p.name = ?
                """)) {
            ps.setString(1, group);
            ps.setString(2, product);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Can't check product group", e);
        }
    }

    int getId(String group) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM product_group WHERE name = ?")) {
            ps.setString(1, group);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

            throw new RuntimeException("Group not found");
        } catch (SQLException e) {
            throw new RuntimeException("Can't get group id: " + group, e);
        }
    }
}
