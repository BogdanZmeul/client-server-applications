package service;

import db.model.Filter;
import db.model.Product;
import db.model.ProductGroup;
import db.model.ProductPage;
import db.DatabaseException;
import db.service.SqliteProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreServiceTest {
    @TempDir
    Path tempDir;

    private SqliteProductService productDb;
    private StoreService storeService;

    @BeforeEach
    void setUp() {
        productDb = new SqliteProductService(tempDir.resolve("store-service.db").toString());
        storeService = new StoreService(productDb);
    }

    @AfterEach
    void close() {
        productDb.close();
    }

    @Test
    void shouldRejectDuplicateProduct() {
        storeService.createProduct(new Product("apple", 10, 5));

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.createProduct(new Product("apple", 20, 7)));

        assertEquals("Product already exists", error.getMessage());
        assertEquals(1, storeService.getProductsCount());
    }

    @Test
    void shouldRejectDuplicateGroup() {
        storeService.createGroup("fruits");

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.createGroup("fruits"));

        assertEquals("Group already exists", error.getMessage());
        assertEquals(1, storeService.getGroupsCount());
    }

    @Test
    void shouldRejectWrongProductValues() {
        StoreException countError = assertThrows(StoreException.class,
                () -> storeService.createProduct(new Product("apple", -1, 5)));
        StoreException priceError = assertThrows(StoreException.class,
                () -> storeService.createProduct(new Product("milk", 1, -5)));

        assertEquals("Count cannot be negative", countError.getMessage());
        assertEquals("Price cannot be negative", priceError.getMessage());
        assertEquals(0, storeService.getProductsCount());
    }

    @Test
    void shouldRejectUpdateProductWithExistingName() {
        int appleId = storeService.createProduct(new Product("apple", 10, 5));
        storeService.createProduct(new Product("milk", 20, 3));

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.updateProduct(new Product(appleId, "milk", 30, 4)));

        assertEquals("Product already exists", error.getMessage());
        assertEquals("apple", storeService.getProduct(appleId).orElseThrow().getName());
    }

    @Test
    void shouldRejectUpdateGroupWithExistingName() {
        int fruitsId = storeService.createGroup(new ProductGroup("fruits"));
        storeService.createGroup("drinks");

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.updateGroup(new ProductGroup(fruitsId, "drinks")));

        assertEquals("Group already exists", error.getMessage());
        assertEquals("fruits", storeService.getGroup(fruitsId).orElseThrow().getName());
    }

    @Test
    void shouldRejectDeletingGroupWithProducts() {
        int productId = storeService.createProduct(new Product("apple", 10, 5));
        int groupId = storeService.createGroup(new ProductGroup("fruits"));
        storeService.addProductToGroup(groupId, productId);

        DatabaseException error = assertThrows(DatabaseException.class,
                () -> storeService.deleteGroup(groupId));

        assertEquals("Changes to group have not been applied", error.getMessage());
        assertTrue(storeService.getGroup(groupId).isPresent());
    }

    @Test
    void shouldRejectAddingProductToWrongGroup() {
        int productId = storeService.createProduct(new Product("apple", 10, 5));

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.addProductToGroup(100, productId));

        assertEquals("Group not found", error.getMessage());
    }

    @Test
    void shouldRejectAddingWrongProductToGroup() {
        int groupId = storeService.createGroup(new ProductGroup("fruits"));

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.addProductToGroup(groupId, 100));

        assertEquals("Product not found", error.getMessage());
    }

    @Test
    void shouldRejectAddingProductToGroupTwice() {
        int productId = storeService.createProduct(new Product("apple", 10, 5));
        int groupId = storeService.createGroup(new ProductGroup("fruits"));
        storeService.addProductToGroup(groupId, productId);

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.addProductToGroup(groupId, productId));

        assertEquals("Product already exists in group", error.getMessage());
    }

    @Test
    void shouldValidateSearchFilter() {
        Filter filter = new Filter();
        filter.minCount = 10;
        filter.maxCount = 5;

        StoreException error = assertThrows(StoreException.class,
                () -> storeService.searchProducts(filter));

        assertEquals("Min count cannot be greater than max count", error.getMessage());
    }

    @Test
    void shouldReturnEmptyPageWhenSearchHasNoResults() {
        storeService.createProduct(new Product("apple", 10, 5));
        Filter filter = new Filter();
        filter.name = "milk";
        filter.page = 1;
        filter.pageSize = 10;

        ProductPage page = storeService.searchProducts(filter);

        assertEquals(0, page.getTotalCount());
        assertTrue(page.getProducts().isEmpty());
    }

    @Test
    void shouldSearchByGroupOnlyAndReturnSecondPage() {
        int firstProductId = storeService.createProduct(new Product("apple", 10, 5));
        int secondProductId = storeService.createProduct(new Product("banana", 20, 7));
        storeService.createProduct(new Product("milk", 30, 3));
        int groupId = storeService.createGroup(new ProductGroup("fruits"));
        storeService.addProductToGroup(groupId, firstProductId);
        storeService.addProductToGroup(groupId, secondProductId);

        Filter filter = new Filter();
        filter.groups = List.of("fruits");
        filter.page = 2;
        filter.pageSize = 1;

        ProductPage page = storeService.searchProducts(filter);

        assertEquals(2, page.getTotalCount());
        assertEquals(1, page.getProducts().size());
        assertEquals("banana", page.getProducts().getFirst().getName());
    }
}
