package service;

import db.Product;
import db.ProductService;

import java.util.List;
import java.util.Optional;

public class StoreService implements ProductService {
    private final ProductService productDb;

    public StoreService(ProductService productDb) {
        this.productDb = productDb;
    }

    @Override
    public synchronized int create(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (productDb.readByName(product.getName()).isPresent()) {
            throw new RuntimeException("Product already exists");
        }

        return productDb.create(product);
    }

    @Override
    public synchronized int count() {
        return productDb.count();
    }

    @Override
    public synchronized List<Product> readAll() {
        return productDb.readAll();
    }

    @Override
    public synchronized Optional<Product> read(int id) {
        return productDb.read(id);
    }

    @Override
    public synchronized Optional<Product> readByName(String name) {
        return productDb.readByName(name);
    }

    @Override
    public synchronized int update(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (product.getId() == null || productDb.read(product.getId()).isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        Optional<Product> productWithSameName = productDb.readByName(product.getName());
        if (productWithSameName.isPresent() && !productWithSameName.get().getId().equals(product.getId())) {
            throw new RuntimeException("Product already exists");
        }

        return productDb.update(product);
    }

    @Override
    public synchronized int delete(int id) {
        if (productDb.read(id).isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        return productDb.delete(id);
    }

    @Override
    public synchronized int deleteAll() {
        return productDb.deleteAll();
    }

    @Override
    public synchronized int getProductCount(String product) {
        return productDb.getProductCount(product);
    }

    @Override
    public synchronized void takeProduct(String product, int count) {
        checkCount(count);

        if (productDb.getProductCount(product) < count) {
            throw new RuntimeException("Not enough product");
        }

        productDb.takeProduct(product, count);
    }

    @Override
    public synchronized void addProduct(String product, int count) {
        checkCount(count);

        productDb.addProduct(product, count);
    }

    @Override
    public synchronized void addGroup(String group) {
        if (productDb.isGroupExists(group)) {
            throw new RuntimeException("Group already exists");
        }

        productDb.addGroup(group);
    }

    @Override
    public synchronized void addProductToGroup(String group, String product) {
        if (!productDb.isGroupExists(group)) {
            throw new RuntimeException("Group not found");
        }

        if (productDb.readByName(product).isEmpty()) {
            productDb.create(new Product(product, 0, 0));
        }

        if (productDb.isProductInGroup(group, product)) {
            throw new RuntimeException("Product already exists in group");
        }

        productDb.addProductToGroup(group, product);
    }

    @Override
    public synchronized void setPrice(String product, double price) {
        checkPrice(price);

        productDb.setPrice(product, price);
    }

    @Override
    public synchronized double getPrice(String product) {
        return productDb.getPrice(product);
    }

    @Override
    public synchronized boolean isGroupExists(String group) {
        return productDb.isGroupExists(group);
    }

    @Override
    public synchronized boolean isProductInGroup(String group, String product) {
        return productDb.isProductInGroup(group, product);
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
