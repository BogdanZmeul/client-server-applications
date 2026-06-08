package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class SqliteProductService implements ProductDatabase, AutoCloseable {
    private final Connection connection;
    private final ProductTable productTable;
    private final GroupTable groupTable;

    public SqliteProductService(String dbName) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create SQLite DB", e);
        }

        init();
        productTable = new ProductTable(connection);
        groupTable = new GroupTable(connection);
    }

    @Override
    public int createProduct(Product product) {
        return productTable.createProduct(product);
    }

    @Override
    public int getProductsCount() {
        return productTable.getProductsCount();
    }

    @Override
    public List<Product> getAllProducts() {
        return productTable.getAllProducts();
    }

    @Override
    public Optional<Product> getProduct(int id) {
        return productTable.getProduct(id);
    }

    @Override
    public Optional<Product> getProduct(String name) {
        return productTable.getProduct(name);
    }

    @Override
    public void updateProduct(Product product) {
        productTable.updateProduct(product);
    }

    @Override
    public void deleteProduct(int id) {
        productTable.deleteProduct(id);
    }

    @Override
    public int deleteAllProducts() {
        return productTable.deleteAllProducts();
    }

    @Override
    public int getProductQuantity(int productId) {
        return productTable.getProductQuantity(productId);
    }

    @Override
    public void takeProductQuantity(int productId, int count) {
        productTable.takeProductQuantity(productId, count);
    }

    @Override
    public void addProductQuantity(int productId, int count) {
        productTable.addProductQuantity(productId, count);
    }

    @Override
    public void setProductPrice(int productId, double price) {
        productTable.setProductPrice(productId, price);
    }

    @Override
    public double getProductPrice(int productId) {
        return productTable.getProductPrice(productId);
    }

    @Override
    public int createGroup(ProductGroup group) {
        return groupTable.createGroup(group);
    }

    @Override
    public int getGroupsCount() {
        return groupTable.getGroupsCount();
    }

    @Override
    public List<ProductGroup> getAllGroups() {
        return groupTable.getAllGroups();
    }

    @Override
    public Optional<ProductGroup> getGroup(int id) {
        return groupTable.getGroup(id);
    }

    @Override
    public Optional<ProductGroup> getGroup(String name) {
        return groupTable.getGroup(name);
    }

    @Override
    public void updateGroup(ProductGroup group) {
        groupTable.updateGroup(group);
    }

    @Override
    public void deleteGroup(int id) {
        groupTable.deleteGroup(id);
    }

    @Override
    public void addProductToGroup(int groupId, int productId) {
        groupTable.addProductToGroup(groupId, productId);
    }

    @Override
    public boolean hasProductsInGroup(int groupId) {
        return groupTable.hasProductsInGroup(groupId);
    }

    @Override
    public boolean isProductInGroup(int groupId, int productId) {
        return groupTable.isProductInGroup(groupId, productId);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Can't close SQLite DB", e);
        }
    }

    private void init() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS product_group (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS product (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        count INT NOT NULL,
                        price REAL NOT NULL,
                        group_id INT,
                        FOREIGN KEY(group_id) REFERENCES product_group(id) ON DELETE RESTRICT
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Exception while DB init", e);
        }
    }

}
