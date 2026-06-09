package db.service;

import db.connection.ConnectionPool;
import db.model.Filter;
import db.model.Product;
import db.model.ProductGroup;
import db.table.GroupTable;
import db.table.ProductTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SqliteProductService implements ProductDatabase, AutoCloseable {
    private static final int MAX_ATTEMPTS = 30;
    private static final int RETRY_SLEEP_MILLIS = 50;

    public SqliteProductService(String dbName) {
        this(dbName, 5);
    }

    private final ConnectionPool connectionPool;

    public SqliteProductService(String dbName, int poolSize) {
        connectionPool = new ConnectionPool(dbName, poolSize);
        init();
    }

    @Override
    public int createProduct(Product product) {
        return withProductTable(productTable -> productTable.createProduct(product));
    }

    @Override
    public int getProductsCount() {
        return withProductTable(ProductTable::getProductsCount);
    }

    @Override
    public List<Product> getAllProducts() {
        return withProductTable(ProductTable::getAllProducts);
    }

    @Override
    public List<Product> searchProducts(Filter filter) {
        return withProductTable(productTable -> productTable.searchProducts(filter));
    }

    @Override
    public int countProducts(Filter filter) {
        return withProductTable(productTable -> productTable.countProducts(filter));
    }

    @Override
    public Optional<Product> getProduct(int id) {
        return withProductTable(productTable -> productTable.getProduct(id));
    }

    @Override
    public Optional<Product> getProduct(String name) {
        return withProductTable(productTable -> productTable.getProduct(name));
    }

    @Override
    public void updateProduct(Product product) {
        withProductTable(productTable -> {
            productTable.updateProduct(product);
            return null;
        });
    }

    @Override
    public void deleteProduct(int id) {
        withProductTable(productTable -> {
            productTable.deleteProduct(id);
            return null;
        });
    }

    @Override
    public int deleteAllProducts() {
        return withProductTable(ProductTable::deleteAllProducts);
    }

    @Override
    public int getProductQuantity(int productId) {
        return withProductTable(productTable -> productTable.getProductQuantity(productId));
    }

    @Override
    public void takeProductQuantity(int productId, int count) {
        withProductTable(productTable -> {
            productTable.takeProductQuantity(productId, count);
            return null;
        });
    }

    @Override
    public void addProductQuantity(int productId, int count) {
        withProductTable(productTable -> {
            productTable.addProductQuantity(productId, count);
            return null;
        });
    }

    @Override
    public void setProductPrice(int productId, double price) {
        withProductTable(productTable -> {
            productTable.setProductPrice(productId, price);
            return null;
        });
    }

    @Override
    public double getProductPrice(int productId) {
        return withProductTable(productTable -> productTable.getProductPrice(productId));
    }

    @Override
    public int createGroup(ProductGroup group) {
        return withGroupTable(groupTable -> groupTable.createGroup(group));
    }

    @Override
    public int getGroupsCount() {
        return withGroupTable(GroupTable::getGroupsCount);
    }

    @Override
    public List<ProductGroup> getAllGroups() {
        return withGroupTable(GroupTable::getAllGroups);
    }

    @Override
    public Optional<ProductGroup> getGroup(int id) {
        return withGroupTable(groupTable -> groupTable.getGroup(id));
    }

    @Override
    public Optional<ProductGroup> getGroup(String name) {
        return withGroupTable(groupTable -> groupTable.getGroup(name));
    }

    @Override
    public void updateGroup(ProductGroup group) {
        withGroupTable(groupTable -> {
            groupTable.updateGroup(group);
            return null;
        });
    }

    @Override
    public void deleteGroup(int id) {
        withGroupTable(groupTable -> {
            groupTable.deleteGroup(id);
            return null;
        });
    }

    @Override
    public void addProductToGroup(int groupId, int productId) {
        withGroupTable(groupTable -> {
            groupTable.addProductToGroup(groupId, productId);
            return null;
        });
    }

    @Override
    public boolean hasProductsInGroup(int groupId) {
        return withGroupTable(groupTable -> groupTable.hasProductsInGroup(groupId));
    }

    @Override
    public boolean isProductInGroup(int groupId, int productId) {
        return withGroupTable(groupTable -> groupTable.isProductInGroup(groupId, productId));
    }

    @Override
    public void close() {
        connectionPool.close();
    }

    private void init() {
        Connection connection = connectionPool.getConnection();

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
        } finally {
            connectionPool.returnConnection(connection);
        }
    }

    private <T> T withProductTable(Function<ProductTable, T> operation) {
        return withConnection(connection -> operation.apply(new ProductTable(connection)));
    }

    private <T> T withGroupTable(Function<GroupTable, T> operation) {
        return withConnection(connection -> operation.apply(new GroupTable(connection)));
    }

    private <T> T withConnection(Function<Connection, T> operation) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            Connection connection = connectionPool.getConnection();
            boolean needRetry = false;

            try {
                return operation.apply(connection);
            } catch (RuntimeException e) {
                if (!isDatabaseLocked(e) || i == MAX_ATTEMPTS - 1) {
                    throw e;
                }

                needRetry = true;
            } finally {
                connectionPool.returnConnection(connection);
            }

            if (needRetry) {
                sleepBeforeRetry();
            }
        }

        throw new RuntimeException("Database is locked");
    }

    private boolean isDatabaseLocked(Throwable e) {
        Throwable current = e;

        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("database is locked") || message.contains("SQLITE_BUSY"))) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_SLEEP_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", e);
        }
    }

}
