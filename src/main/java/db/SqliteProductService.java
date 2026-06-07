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
        checkCount(product.getCount());
        checkPrice(product.getPrice());

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
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (product.getId() == null) {
            throw new RuntimeException("Product id cannot be null");
        }

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
        checkCount(count);

        Optional<Product> found = readByName(product);
        int oldCount = found.map(Product::getCount).orElse(0);
        if (oldCount < count) {
            throw new RuntimeException("Not enough product");
        }

        productTable.updateCount(product, oldCount - count);
    }

    @Override
    public synchronized void addProduct(String product, int count) {
        checkCount(count);

        Optional<Product> found = readByName(product);
        if (found.isEmpty()) {
            productTable.create(new Product(product, count, 0));
            return;
        }

        productTable.updateCount(product, found.get().getCount() + count);
    }

    @Override
    public synchronized void addGroup(String group) {
        groupTable.add(group);
    }

    @Override
    public synchronized void addProductToGroup(String group, String product) {
        int groupId = groupTable.getId(group);
        int productId = productTable.getId(product);
        groupTable.addProductToGroup(groupId, productId);
    }

    @Override
    public synchronized void setPrice(String product, double price) {
        checkPrice(price);

        if (readByName(product).isEmpty()) {
            productTable.create(new Product(product, 0, price));
            return;
        }

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
                        FOREIGN KEY(group_id) REFERENCES product_group(id) ON DELETE SET NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Exception while DB init", e);
        }
    }

    private void checkCount(int count) {
        if (count < 0) {
            throw new RuntimeException("Count cannot be negative");
        }
    }

    private void checkPrice(double price) {
        if (price < 0) {
            throw new RuntimeException("Price cannot be negative");
        }
    }
}
