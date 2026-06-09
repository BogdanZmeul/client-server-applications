package service;

import db.model.Product;
import db.service.SqliteProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StoreServiceConcurrencyTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldAllowOnlyOneClientToBuySingleProduct() throws Exception {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("buy.db").toString(), 10)) {
            StoreService storeService = new StoreService(productDb);
            int productId = storeService.createProduct(new Product("apple", 1, 5));
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            runInManyThreads(1000, () -> {
                try {
                    storeService.takeProductQuantity(productId, 1);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    errorCount.incrementAndGet();
                }
            });

            assertEquals(1, successCount.get());
            assertEquals(999, errorCount.get());
            assertEquals(0, storeService.getProductQuantity(productId));
        }
    }

    @Test
    void shouldAddProductQuantityInManyThreads() throws Exception {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("add.db").toString(), 10)) {
            StoreService storeService = new StoreService(productDb);
            int productId = storeService.createProduct(new Product("apple", 0, 5));
            AtomicInteger errorCount = new AtomicInteger(0);

            runInManyThreads(1000, () -> {
                try {
                    storeService.addProductQuantity(productId, 1);
                } catch (RuntimeException e) {
                    errorCount.incrementAndGet();
                }
            });

            assertEquals(0, errorCount.get());
            assertEquals(1000, storeService.getProductQuantity(productId));
        }
    }

    @Test
    void shouldUpdateAndReadProductInManyThreads() throws Exception {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("update-read.db").toString(), 10)) {
            StoreService storeService = new StoreService(productDb);
            int productId = storeService.createProduct(new Product("apple", 0, 5));
            AtomicInteger errorCount = new AtomicInteger(0);

            runInManyThreads(1000, index -> {
                try {
                    if (index % 2 == 0) {
                        storeService.updateProduct(new Product(productId, "apple", index, index + 0.5));
                    } else {
                        assertTrue(storeService.getProduct(productId).isPresent());
                    }
                } catch (RuntimeException e) {
                    errorCount.incrementAndGet();
                }
            });

            Optional<Product> product = storeService.getProduct(productId);

            assertEquals(0, errorCount.get());
            assertTrue(product.isPresent());
            assertEquals("apple", product.get().getName());
        }
    }

    @Test
    void shouldAllowOnlyOneClientToDeleteProduct() throws Exception {
        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("delete.db").toString(), 10)) {
            StoreService storeService = new StoreService(productDb);
            int productId = storeService.createProduct(new Product("apple", 10, 5));
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            runInManyThreads(1000, () -> {
                try {
                    storeService.deleteProduct(productId);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    errorCount.incrementAndGet();
                }
            });

            assertEquals(1, successCount.get());
            assertEquals(999, errorCount.get());
            assertTrue(storeService.getProduct(productId).isEmpty());
        }
    }

    private void runInManyThreads(int tasksCount, Runnable task) throws Exception {
        runInManyThreads(tasksCount, index -> task.run());
    }

    private void runInManyThreads(int tasksCount, Task task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(50);

        for (int i = 0; i < tasksCount; i++) {
            int index = i;
            executor.execute(() -> task.run(index));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
    }

    private interface Task {
        void run(int index);
    }
}
