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
    public int create(Product product) {
        return productTable.create(product);
    }

    @Override
    public int count() {
        return productTable.count();
    }

    @Override
    public List<Product> readAll() {
        return productTable.readAll();
    }

    @Override
    public Optional<Product> read(int id) {
        return productTable.read(id);
    }

    @Override
    public Optional<Product> readByName(String name) {
        return productTable.readByName(name);
    }

    @Override
    public int update(Product product) {
        return productTable.update(product);
    }

    @Override
    public int delete(int id) {
        return productTable.delete(id);
    }

    @Override
    public int deleteAll() {
        return productTable.deleteAll();
    }

    @Override
    public int getProductCount(String product) {
        Optional<Product> found = readByName(product);
        if (found.isEmpty()) {
            return 0;
        }

        return found.get().getCount();
    }

    @Override
    public void takeProduct(String product, int count) {
        productTable.takeCount(product, count);
    }

    @Override
    public void addProduct(String product, int count) {
        productTable.addCount(product, count);
    }

    @Override
    public int createGroup(ProductGroup group) {
        return groupTable.create(group);
    }

    @Override
    public int groupsCount() {
        return groupTable.count();
    }

    @Override
    public List<ProductGroup> readAllGroups() {
        return groupTable.readAll();
    }

    @Override
    public Optional<ProductGroup> readGroup(int id) {
        return groupTable.read(id);
    }

    @Override
    public Optional<ProductGroup> readGroupByName(String name) {
        return groupTable.readByName(name);
    }

    @Override
    public int updateGroup(ProductGroup group) {
        return groupTable.update(group);
    }

    @Override
    public void addGroup(String group) {
        groupTable.create(new ProductGroup(group));
    }

    @Override
    public int deleteGroup(int id) {
        return groupTable.delete(id);
    }

    @Override
    public int deleteGroup(String group) {
        return groupTable.delete(group);
    }

    @Override
    public void addProductToGroup(String group, String product) {
        int groupId = groupTable.getId(group);
        int productId = productTable.getId(product);
        groupTable.addProductToGroup(groupId, productId);
    }

    @Override
    public void setPrice(String product, double price) {
        productTable.updatePrice(product, price);
    }

    @Override
    public double getPrice(String product) {
        Optional<Product> found = readByName(product);
        if (found.isEmpty()) {
            return 0;
        }

        return found.get().getPrice();
    }

    @Override
    public boolean isGroupExists(String group) {
        return groupTable.exists(group);
    }

    @Override
    public boolean hasProductsInGroup(String group) {
        return groupTable.hasProducts(group);
    }

    @Override
    public boolean isProductInGroup(String group, String product) {
        return groupTable.isProductInGroup(group, product);
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
