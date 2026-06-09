package db.table;

import db.DatabaseException;
import db.model.Filter;
import db.model.Product;
import db.model.ProductGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductTableTest {
    @TempDir
    Path tempDir;

    private Connection connection;
    private ProductTable productTable;
    private GroupTable groupTable;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("product-table.db"));
        initDb();
        productTable = new ProductTable(connection);
        groupTable = new GroupTable(connection);
    }

    @AfterEach
    void close() throws Exception {
        connection.close();
    }

    @Test
    void shouldCreateReadUpdateAndDeleteProduct() {
        int productId = productTable.createProduct(new Product("apple", 10, 5.5));

        Product product = productTable.getProduct(productId).orElseThrow();
        assertEquals("apple", product.getName());
        assertEquals(10, product.getCount());
        assertEquals(5.5, product.getPrice());
        assertEquals(1, productTable.getProductsCount());
        assertEquals(1, productTable.getAllProducts().size());
        assertTrue(productTable.getProduct("apple").isPresent());

        productTable.updateProduct(new Product(productId, "green apple", 20, 7.5));

        Product updatedProduct = productTable.getProduct(productId).orElseThrow();
        assertEquals("green apple", updatedProduct.getName());
        assertEquals(20, updatedProduct.getCount());
        assertEquals(7.5, updatedProduct.getPrice());

        productTable.deleteProduct(productId);

        assertEquals(0, productTable.getProductsCount());
        assertTrue(productTable.getProduct(productId).isEmpty());
    }

    @Test
    void shouldUpdateProductQuantityAndPrice() {
        int productId = productTable.createProduct(new Product("apple", 10, 5.5));

        productTable.addProductQuantity(productId, 5);
        productTable.takeProductQuantity(productId, 3);
        productTable.setProductPrice(productId, 8.5);

        assertEquals(12, productTable.getProductQuantity(productId));
        assertEquals(8.5, productTable.getProductPrice(productId));
    }

    @Test
    void shouldDeleteAllProducts() {
        productTable.createProduct(new Product("apple", 10, 5.5));
        productTable.createProduct(new Product("milk", 20, 3.0));

        int deleted = productTable.deleteAllProducts();

        assertEquals(2, deleted);
        assertEquals(0, productTable.getProductsCount());
        assertTrue(productTable.getAllProducts().isEmpty());
    }

    @Test
    void shouldSearchProductsWithDynamicFilters() {
        int appleId = productTable.createProduct(new Product("apple", 10, 5.5));
        int greenAppleId = productTable.createProduct(new Product("green apple", 50, 7.2));
        int milkId = productTable.createProduct(new Product("milk", 20, 3.0));
        int fruitsId = groupTable.createGroup(new ProductGroup("fruits"));
        int drinksId = groupTable.createGroup(new ProductGroup("drinks"));

        groupTable.addProductToGroup(fruitsId, appleId);
        groupTable.addProductToGroup(fruitsId, greenAppleId);
        groupTable.addProductToGroup(drinksId, milkId);

        Filter filter = new Filter();
        filter.name = "apple";
        filter.groups = List.of("fruits");
        filter.minCount = 5;
        filter.maxCount = 20;
        filter.minPrice = 5.0;
        filter.maxPrice = 6.0;
        filter.page = 1;
        filter.pageSize = 10;

        List<Product> products = productTable.searchProducts(filter);
        int count = productTable.countProducts(filter);

        assertEquals(1, count);
        assertEquals(1, products.size());
        assertEquals("apple", products.getFirst().getName());
    }

    @Test
    void shouldSearchProductsWithEmptyFilterAndPagination() {
        productTable.createProduct(new Product("apple", 10, 5.5));
        productTable.createProduct(new Product("banana", 20, 7.0));
        productTable.createProduct(new Product("milk", 30, 3.0));

        Filter filter = new Filter();
        filter.page = 2;
        filter.pageSize = 1;

        List<Product> products = productTable.searchProducts(filter);
        int count = productTable.countProducts(filter);

        assertEquals(3, count);
        assertEquals(1, products.size());
        assertEquals("banana", products.getFirst().getName());
    }

    @Test
    void shouldReturnEmptyOptionalWhenProductDoesNotExist() {
        assertTrue(productTable.getProduct(100).isEmpty());
        assertTrue(productTable.getProduct("apple").isEmpty());
    }

    @Test
    void shouldThrowErrorWhenProductChangesAreNotApplied() {
        int productId = productTable.createProduct(new Product("apple", 10, 5.5));

        DatabaseException updateError = assertThrows(DatabaseException.class,
                () -> productTable.updateProduct(new Product(100, "milk", 10, 5)));
        DatabaseException deleteError = assertThrows(DatabaseException.class,
                () -> productTable.deleteProduct(100));
        DatabaseException addCountError = assertThrows(DatabaseException.class,
                () -> productTable.addProductQuantity(100, 1));
        DatabaseException takeCountError = assertThrows(DatabaseException.class,
                () -> productTable.takeProductQuantity(productId, 100));
        DatabaseException priceError = assertThrows(DatabaseException.class,
                () -> productTable.setProductPrice(100, 10));

        assertEquals("Changes to product have not been applied", updateError.getMessage());
        assertEquals("Changes to product have not been applied", deleteError.getMessage());
        assertEquals("Changes to product have not been applied", addCountError.getMessage());
        assertEquals("Changes to product have not been applied", takeCountError.getMessage());
        assertEquals("Changes to product have not been applied", priceError.getMessage());
    }

    @Test
    void shouldThrowErrorWhenProductQuantityOrPriceDoesNotExist() {
        DatabaseException countError = assertThrows(DatabaseException.class,
                () -> productTable.getProductQuantity(100));
        DatabaseException priceError = assertThrows(DatabaseException.class,
                () -> productTable.getProductPrice(100));

        assertEquals("Product not found", countError.getMessage());
        assertEquals("Product not found", priceError.getMessage());
    }

    @Test
    void shouldRejectDuplicateProductNameOnDatabaseLevel() {
        productTable.createProduct(new Product("apple", 10, 5.5));

        DatabaseException error = assertThrows(DatabaseException.class,
                () -> productTable.createProduct(new Product("apple", 20, 7.0)));

        assertTrue(error.getMessage().contains("Can't create product"));
        assertEquals(1, productTable.getProductsCount());
    }

    private void initDb() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE product_group (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE product (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        count INT NOT NULL,
                        price REAL NOT NULL,
                        group_id INT,
                        FOREIGN KEY(group_id) REFERENCES product_group(id) ON DELETE RESTRICT
                    )
                    """);
        }
    }
}
