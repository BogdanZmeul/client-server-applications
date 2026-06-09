package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    List<Product> searchProducts(Filter filter) {
        SqlWrapper query = getSearchQuery(filter);

        try (PreparedStatement ps = connection.prepareStatement(query.sql.toString())) {
            setParams(ps, query.params);

            List<Product> products = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(toProduct(rs));
                }
            }

            return products;
        } catch (SQLException e) {
            throw new RuntimeException("Can't search products", e);
        }
    }

    int countProducts(Filter filter) {
        SqlWrapper query = getCountQuery(filter);

        try (PreparedStatement ps = connection.prepareStatement(query.sql.toString())) {
            setParams(ps, query.params);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Can't count filtered products", e);
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

            int updated = ps.executeUpdate();
            if (updated < 1) {
                throw new RuntimeException("Changes to product have not been applied");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't update product: " + product, e);
        }
    }

    void deleteProduct(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM product WHERE id = ?")) {
            ps.setInt(1, id);

            int deleted = ps.executeUpdate();
            if (deleted < 1) {
                throw new RuntimeException("Changes to product have not been applied");
            }
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

            throw new RuntimeException("Product not found");
        } catch (SQLException e) {
            throw new RuntimeException("Can't get product count by id: " + productId, e);
        }
    }

    void addProductQuantity(int productId, int count) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET count = count + ? WHERE id = ?")) {
            ps.setInt(1, count);
            ps.setInt(2, productId);

            int updated = ps.executeUpdate();
            if (updated < 1) {
                throw new RuntimeException("Changes to product have not been applied");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't add product count: " + productId, e);
        }
    }

    void takeProductQuantity(int productId, int count) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE product SET count = count - ? WHERE id = ? AND count >= ?")) {
            ps.setInt(1, count);
            ps.setInt(2, productId);
            ps.setInt(3, count);

            int updated = ps.executeUpdate();
            if (updated < 1) {
                throw new RuntimeException("Changes to product have not been applied");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't take product count: " + productId, e);
        }
    }

    void setProductPrice(int productId, double price) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE product SET price = ? WHERE id = ?")) {
            ps.setDouble(1, price);
            ps.setInt(2, productId);

            int updated = ps.executeUpdate();
            if (updated < 1) {
                throw new RuntimeException("Changes to product have not been applied");
            }
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

            throw new RuntimeException("Product not found");
        } catch (SQLException e) {
            throw new RuntimeException("Can't get product price by id: " + productId, e);
        }
    }

    private Product toProduct(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("id"), rs.getString("name"), rs.getInt("count"), rs.getDouble("price"));
    }

    private SqlWrapper getSearchQuery(Filter filter) {
        SqlWrapper query = getQuery("""
                SELECT p.*
                FROM product p
                LEFT JOIN product_group g ON p.group_id = g.id
                """, filter);

        query.sql.append("""
                ORDER BY p.id
                LIMIT ?
                OFFSET ?
                """);
        query.params.add(filter.pageSize);
        query.params.add((filter.page - 1) * filter.pageSize);

        return query;
    }

    private SqlWrapper getCountQuery(Filter filter) {
        return getQuery("""
                SELECT COUNT(*)
                FROM product p
                LEFT JOIN product_group g ON p.group_id = g.id
                """, filter);
    }

    private SqlWrapper getQuery(String selectSql, Filter filter) {
        SqlWrapper filterSql = getFilterSql(filter);

        if (filterSql.sql == null) {
            filterSql.sql = new StringBuilder(selectSql);
        } else {
            filterSql.sql.insert(0, selectSql);
        }

        return filterSql;
    }

    private SqlWrapper getFilterSql(Filter filter) {
        SqlWrapper sqlWrapper = new SqlWrapper();

        String where = Stream.of(
                stringLike("p.name", filter.name, sqlWrapper.params),
                stringIn("g.name", filter.groups, sqlWrapper.params),
                numberMoreOrEquals("p.count", filter.minCount, sqlWrapper.params),
                numberLessOrEquals("p.count", filter.maxCount, sqlWrapper.params),
                numberMoreOrEquals("p.price", filter.minPrice, sqlWrapper.params),
                numberLessOrEquals("p.price", filter.maxPrice, sqlWrapper.params)
        )
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" AND "));

        if (!where.isBlank()) {
            sqlWrapper.sql = new StringBuilder();
            sqlWrapper.sql.append("WHERE ");
            sqlWrapper.sql.append(where);
            sqlWrapper.sql.append("\n");
        }

        return sqlWrapper;
    }

    private String stringLike(String columnName, String value, List<Object> params) {
        if (value == null || value.isBlank()) {
            return null;
        }

        params.add("%" + value + "%");
        return columnName + " LIKE ?";
    }

    private String stringIn(String columnName, List<String> values, List<Object> params) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        params.addAll(values);

        String questionMarks = values.stream()
                .map(value -> "?")
                .collect(Collectors.joining(", "));

        return columnName + " IN (" + questionMarks + ")";
    }

    private String numberMoreOrEquals(String columnName, Number value, List<Object> params) {
        if (value == null) {
            return null;
        }

        params.add(value);
        return columnName + " >= ?";
    }

    private String numberLessOrEquals(String columnName, Number value, List<Object> params) {
        if (value == null) {
            return null;
        }

        params.add(value);
        return columnName + " <= ?";
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static class SqlWrapper {
        private StringBuilder sql;
        private final List<Object> params = new ArrayList<>();
    }
}
