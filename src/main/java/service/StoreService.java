package service;

import db.Product;
import db.ProductGroup;
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
        checkProductExists(product);

        return productDb.getProductCount(product);
    }

    @Override
    public synchronized void takeProduct(String product, int count) {
        checkCount(count);

        Product foundProduct = getProduct(product);
        if (foundProduct.getCount() < count) {
            throw new RuntimeException("Not enough product");
        }

        productDb.takeProduct(product, count);
    }

    @Override
    public synchronized void addProduct(String product, int count) {
        checkCount(count);
        checkProductExists(product);

        productDb.addProduct(product, count);
    }

    @Override
    public synchronized int createGroup(ProductGroup group) {
        checkGroupName(group.getName());

        if (productDb.readGroupByName(group.getName()).isPresent()) {
            throw new RuntimeException("Group already exists");
        }

        return productDb.createGroup(group);
    }

    @Override
    public synchronized int groupsCount() {
        return productDb.groupsCount();
    }

    @Override
    public synchronized List<ProductGroup> readAllGroups() {
        return productDb.readAllGroups();
    }

    @Override
    public synchronized Optional<ProductGroup> readGroup(int id) {
        return productDb.readGroup(id);
    }

    @Override
    public synchronized Optional<ProductGroup> readGroupByName(String name) {
        return productDb.readGroupByName(name);
    }

    @Override
    public synchronized int updateGroup(ProductGroup group) {
        checkGroupName(group.getName());

        if (group.getId() == null || productDb.readGroup(group.getId()).isEmpty()) {
            throw new RuntimeException("Group not found");
        }

        Optional<ProductGroup> groupWithSameName = productDb.readGroupByName(group.getName());
        if (groupWithSameName.isPresent() && !groupWithSameName.get().getId().equals(group.getId())) {
            throw new RuntimeException("Group already exists");
        }

        return productDb.updateGroup(group);
    }

    @Override
    public synchronized void addGroup(String group) {
        createGroup(new ProductGroup(group));
    }

    @Override
    public synchronized int deleteGroup(int id) {
        ProductGroup group = getGroup(id);

        if (productDb.hasProductsInGroup(group.getName())) {
            throw new RuntimeException("Group has products");
        }

        return productDb.deleteGroup(id);
    }

    @Override
    public synchronized int deleteGroup(String group) {
        ProductGroup foundGroup = getGroup(group);

        if (productDb.hasProductsInGroup(foundGroup.getName())) {
            throw new RuntimeException("Group has products");
        }

        return productDb.deleteGroup(foundGroup.getName());
    }

    @Override
    public synchronized void addProductToGroup(String group, String product) {
        if (!productDb.isGroupExists(group)) {
            throw new RuntimeException("Group not found");
        }

        checkProductExists(product);

        if (productDb.isProductInGroup(group, product)) {
            throw new RuntimeException("Product already exists in group");
        }

        productDb.addProductToGroup(group, product);
    }

    @Override
    public synchronized void setPrice(String product, double price) {
        checkPrice(price);
        checkProductExists(product);

        productDb.setPrice(product, price);
    }

    @Override
    public synchronized double getPrice(String product) {
        checkProductExists(product);

        return productDb.getPrice(product);
    }

    @Override
    public synchronized boolean isGroupExists(String group) {
        return productDb.isGroupExists(group);
    }

    @Override
    public synchronized boolean hasProductsInGroup(String group) {
        return productDb.hasProductsInGroup(group);
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

    private void checkGroupName(String group) {
        if (group == null || group.isBlank()) {
            throw new RuntimeException("Group name cannot be empty");
        }
    }

    private void checkProductExists(String product) {
        if (productDb.readByName(product).isEmpty()) {
            throw new RuntimeException("Product not found");
        }
    }

    private Product getProduct(String product) {
        Optional<Product> foundProduct = productDb.readByName(product);
        if (foundProduct.isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        return foundProduct.get();
    }

    private ProductGroup getGroup(int id) {
        Optional<ProductGroup> foundGroup = productDb.readGroup(id);
        if (foundGroup.isEmpty()) {
            throw new RuntimeException("Group not found");
        }

        return foundGroup.get();
    }

    private ProductGroup getGroup(String group) {
        Optional<ProductGroup> foundGroup = productDb.readGroupByName(group);
        if (foundGroup.isEmpty()) {
            throw new RuntimeException("Group not found");
        }

        return foundGroup.get();
    }
}
