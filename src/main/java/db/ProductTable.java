package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ProductTable {
    private final Connection connection;

    ProductTable(Connection connection) {
        this.connection = connection;
    }

    int create(Product product) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO product(name, count, price) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setInt(2, product.getCount());
            ps.setDouble(3, product.getPrice());

            int inserted = ps.executeUpdate();
            if (inserted < 1) {
                throw new RuntimeException("Insert failed");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new RuntimeException("Can't get product id");
        } catch (SQLException e) {
            throw new RuntimeException("Can't create product: " + product, e);
        }
    }

    int count() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM product")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't count products", e);
        }
    }

    List<Product> readAll() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product")) {
            List<Product> products = new ArrayList<>();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(toProduct(rs));
                }
            }

            return products;
        } catch (SQLException e) {
            throw new RuntimeException("Can't read products", e);
        }
    }

    Optional<Product> read(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product WHERE id = ?")) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toProduct(rs));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't read product by id: " + id, e);
        }
    }

    Optional<Product> readByName(String name) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM product WHERE name = ?")) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toProduct(rs));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Can't read product by name: " + name, e);
        }
    }

    int update(Product product) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE product SET name = ?, count = ?, price = ? WHERE id = ?")) {
            ps.setString(1, product.getName());
            ps.setInt(2, product.getCount());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getId());

            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't update product: " + product, e);
        }
    }

    int delete(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete product by id: " + id, e);
        }
    }

    int deleteAll() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete products", e);
        }
    }

    void updateCount(String product, int count) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET count = ? WHERE name = ?")) {
            ps.setInt(1, count);
            ps.setString(2, product);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't update product count: " + product, e);
        }
    }

    void updatePrice(String product, double price) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET price = ? WHERE name = ?")) {
            ps.setDouble(1, price);
            ps.setString(2, product);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't set price for product: " + product, e);
        }
    }

    int getId(String product) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM product WHERE name = ?")) {
            ps.setString(1, product);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

            throw new RuntimeException("Product not found");
        } catch (SQLException e) {
            throw new RuntimeException("Can't get product id: " + product, e);
        }
    }

    private Product toProduct(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("id"), rs.getString("name"), rs.getInt("count"), rs.getDouble("price"));
    }
}
