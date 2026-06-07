package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class GroupTable {
    private final Connection connection;

    GroupTable(Connection connection) {
        this.connection = connection;
    }

    int create(ProductGroup group) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO product_group(name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, group.getName());

            int inserted = ps.executeUpdate();
            if (inserted < 1) {
                throw new RuntimeException("Insert failed");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new RuntimeException("Can't get group id");
        } catch (SQLException e) {
            throw new RuntimeException("Can't add group: " + group, e);
        }
    }

    int count() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM product_group")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't count groups", e);
        }
    }

    List<ProductGroup> readAll() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product_group")) {
            List<ProductGroup> groups = new ArrayList<>();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(toProductGroup(rs));
                }
            }

            return groups;
        } catch (SQLException e) {
            throw new RuntimeException("Can't read groups", e);
        }
    }

    Optional<ProductGroup> read(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product_group WHERE id = ?")) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toProductGroup(rs));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't read group by id: " + id, e);
        }
    }

    Optional<ProductGroup> readByName(String name) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product_group WHERE name = ?")) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toProductGroup(rs));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't read group by name: " + name, e);
        }
    }

    int update(ProductGroup group) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product_group SET name = ? WHERE id = ?")) {
            ps.setString(1, group.getName());
            ps.setInt(2, group.getId());

            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't update group: " + group, e);
        }
    }

    int delete(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product_group WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete group by id: " + id, e);
        }
    }

    int delete(String group) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product_group WHERE name = ?")) {
            ps.setString(1, group);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete group: " + group, e);
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

    boolean hasProducts(String group) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM product p
                JOIN product_group pg ON pg.id = p.group_id
                WHERE pg.name = ?
                """)) {
            ps.setString(1, group);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Can't check group products: " + group, e);
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

    private ProductGroup toProductGroup(ResultSet rs) throws SQLException {
        return new ProductGroup(rs.getInt("id"), rs.getString("name"));
    }
}
