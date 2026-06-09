package db.service;

import db.model.Filter;
import db.model.Product;
import db.model.ProductGroup;
import db.model.ProductPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.StoreService;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteProductServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldCreateReadUpdateAndDeleteProduct() {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("products.db").toString())) {
            int productId = productDb.createProduct(new Product("apple", 10, 5.5));

            Product product = productDb.getProduct(productId).orElseThrow();
            assertEquals("apple", product.getName());
            assertEquals(10, product.getCount());
            assertEquals(5.5, product.getPrice());

            productDb.updateProduct(new Product(productId, "green apple", 20, 7.5));

            Product updatedProduct = productDb.getProduct(productId).orElseThrow();
            assertEquals("green apple", updatedProduct.getName());
            assertEquals(20, updatedProduct.getCount());
            assertEquals(7.5, updatedProduct.getPrice());

            productDb.deleteProduct(productId);

            assertTrue(productDb.getProduct(productId).isEmpty());
        }
    }

    @Test
    void shouldCreateReadUpdateAndDeleteGroup() {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("groups.db").toString())) {
            int groupId = productDb.createGroup(new ProductGroup("fruits"));

            ProductGroup group = productDb.getGroup(groupId).orElseThrow();
            assertEquals("fruits", group.getName());

            productDb.updateGroup(new ProductGroup(groupId, "fresh fruits"));

            ProductGroup updatedGroup = productDb.getGroup(groupId).orElseThrow();
            assertEquals("fresh fruits", updatedGroup.getName());

            productDb.deleteGroup(groupId);

            assertTrue(productDb.getGroup(groupId).isEmpty());
        }
    }

    @Test
    void shouldAddProductToGroup() {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("product-group.db").toString())) {
            int productId = productDb.createProduct(new Product("apple", 10, 5.5));
            int groupId = productDb.createGroup(new ProductGroup("fruits"));

            productDb.addProductToGroup(groupId, productId);

            assertTrue(productDb.isProductInGroup(groupId, productId));
            assertTrue(productDb.hasProductsInGroup(groupId));
        }
    }

    @Test
    void shouldSearchProductsWithDynamicFiltersAndPagination() {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("search.db").toString())) {
            StoreService storeService = new StoreService(productDb);
            int appleId = storeService.createProduct(new Product("apple", 10, 5.5));
            int greenAppleId = storeService.createProduct(new Product("green apple", 50, 7.2));
            int milkId = storeService.createProduct(new Product("milk", 20, 3.0));
            int fruitsId = storeService.createGroup(new ProductGroup("fruits"));
            int drinksId = storeService.createGroup(new ProductGroup("drinks"));

            storeService.addProductToGroup(fruitsId, appleId);
            storeService.addProductToGroup(fruitsId, greenAppleId);
            storeService.addProductToGroup(drinksId, milkId);

            Filter filter = new Filter();
            filter.name = "apple";
            filter.groups = List.of("fruits");
            filter.minCount = 5;
            filter.maxCount = 20;
            filter.minPrice = 5.0;
            filter.maxPrice = 6.0;
            filter.page = 1;
            filter.pageSize = 10;

            ProductPage page = storeService.searchProducts(filter);

            assertEquals(1, page.getTotalCount());
            assertEquals(1, page.getProducts().size());
            assertEquals("apple", page.getProducts().getFirst().getName());
        }
    }

    @Test
    void shouldSearchProductsOnlyByPriceAndReturnRequestedPage() {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("search-page.db").toString())) {
            StoreService storeService = new StoreService(productDb);
            storeService.createProduct(new Product("apple", 10, 5.5));
            storeService.createProduct(new Product("green apple", 50, 7.2));
            storeService.createProduct(new Product("milk", 20, 3.0));

            Filter filter = new Filter();
            filter.minPrice = 4.0;
            filter.page = 1;
            filter.pageSize = 1;

            ProductPage page = storeService.searchProducts(filter);

            assertEquals(2, page.getTotalCount());
            assertEquals(1, page.getProducts().size());
            assertEquals(1, page.getPage());
            assertEquals(1, page.getPageSize());
        }
    }
}
