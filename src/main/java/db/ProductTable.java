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

    int createProduct(Product product) {
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

    int getProductsCount() {
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

    List<Product> getAllProducts() {
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

    Optional<Product> getProduct(int id) {
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

    Optional<Product> getProduct(String name) {
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

    void updateProduct(Product product) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE product SET name = ?, count = ?, price = ? WHERE id = ?")) {
            ps.setString(1, product.getName());
            ps.setInt(2, product.getCount());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't update product: " + product, e);
        }
    }

    void deleteProduct(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete product by id: " + id, e);
        }
    }

    int deleteAllProducts() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't delete products", e);
        }
    }

    int getProductQuantity(int productId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT count FROM product WHERE id = ?")) {
            ps.setInt(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't get product count by id: " + productId, e);
        }
    }

    void addProductQuantity(int productId, int count) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET count = count + ? WHERE id = ?")) {
            ps.setInt(1, count);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't add product count: " + productId, e);
        }
    }

    void takeProductQuantity(int productId, int count) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET count = count - ? WHERE id = ?")) {
            ps.setInt(1, count);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't take product count: " + productId, e);
        }
    }

    void setProductPrice(int productId, double price) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET price = ? WHERE id = ?")) {
            ps.setDouble(1, price);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't set price for product: " + productId, e);
        }
    }

    double getProductPrice(int productId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT price FROM product WHERE id = ?")) {
            ps.setInt(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't get product price by id: " + productId, e);
        }
    }

    private Product toProduct(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("id"), rs.getString("name"), rs.getInt("count"), rs.getDouble("price"));
    }
}
