package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class SqliteProductService implements ProductService, AutoCloseable {
    private final Connection connection;
    private final ProductTable productTable;
    private final GroupTable groupTable;

    public SqliteProductService(String dbName) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            this.connection.createStatement().execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            throw new RuntimeException("Can't create SQLite DB", e);
        }

        init();
        productTable = new ProductTable(connection);
        groupTable = new GroupTable(connection);
    }

    @Override
    public synchronized int create(Product product) {
        return productTable.create(product);
    }

    @Override
    public synchronized int count() {
        return productTable.count();
    }

    @Override
    public synchronized List<Product> readAll() {
        return productTable.readAll();
    }

    @Override
    public synchronized Optional<Product> read(int id) {
        return productTable.read(id);
    }

    @Override
    public synchronized Optional<Product> readByName(String name) {
        return productTable.readByName(name);
    }

    @Override
    public synchronized int update(Product product) {
        return productTable.update(product);
    }

    @Override
    public synchronized int delete(int id) {
        return productTable.delete(id);
    }

    @Override
    public synchronized int deleteAll() {
        return productTable.deleteAll();
    }

    @Override
    public synchronized int getProductCount(String product) {
        Optional<Product> found = readByName(product);
        if (found.isEmpty()) {
            return 0;
        }

        return found.get().getCount();
    }

    @Override
    public synchronized void takeProduct(String product, int count) {
        productTable.takeCount(product, count);
    }

    @Override
    public synchronized void addProduct(String product, int count) {
        productTable.addCount(product, count);
    }

    @Override
    public synchronized int createGroup(ProductGroup group) {
        return groupTable.create(group);
    }

    @Override
    public synchronized int groupsCount() {
        return groupTable.count();
    }

    @Override
    public synchronized List<ProductGroup> readAllGroups() {
        return groupTable.readAll();
    }

    @Override
    public synchronized Optional<ProductGroup> readGroup(int id) {
        return groupTable.read(id);
    }

    @Override
    public synchronized Optional<ProductGroup> readGroupByName(String name) {
        return groupTable.readByName(name);
    }

    @Override
    public synchronized int updateGroup(ProductGroup group) {
        return groupTable.update(group);
    }

    @Override
    public synchronized void addGroup(String group) {
        groupTable.create(new ProductGroup(group));
    }

    @Override
    public synchronized int deleteGroup(int id) {
        return groupTable.delete(id);
    }

    @Override
    public synchronized int deleteGroup(String group) {
        return groupTable.delete(group);
    }

    @Override
    public synchronized void addProductToGroup(String group, String product) {
        int groupId = groupTable.getId(group);
        int productId = productTable.getId(product);
        groupTable.addProductToGroup(groupId, productId);
    }

    @Override
    public synchronized void setPrice(String product, double price) {
        productTable.updatePrice(product, price);
    }

    @Override
    public synchronized double getPrice(String product) {
        Optional<Product> found = readByName(product);
        if (found.isEmpty()) {
            return 0;
        }

        return found.get().getPrice();
    }

    @Override
    public synchronized boolean isGroupExists(String group) {
        return groupTable.exists(group);
    }

    @Override
    public synchronized boolean hasProductsInGroup(String group) {
        return groupTable.hasProducts(group);
    }

    @Override
    public synchronized boolean isProductInGroup(String group, String product) {
        return groupTable.isProductInGroup(group, product);
    }

    @Override
    public synchronized void close() {
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
