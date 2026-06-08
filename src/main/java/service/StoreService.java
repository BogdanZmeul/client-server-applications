package service;

import db.Product;
import db.ProductDatabase;
import db.ProductGroup;
import db.ProductService;

import java.util.List;
import java.util.Optional;

public class StoreService implements ProductService {
    private final ProductDatabase productDb;

    public StoreService(ProductDatabase productDb) {
        this.productDb = productDb;
    }

    @Override
    public synchronized int createProduct(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (productDb.getProduct(product.getName()).isPresent()) {
            throw new RuntimeException("Product already exists");
        }

        return productDb.createProduct(product);
    }

    @Override
    public synchronized int getProductsCount() {
        return productDb.getProductsCount();
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        return productDb.getAllProducts();
    }

    @Override
    public synchronized Optional<Product> getProduct(int id) {
        return productDb.getProduct(id);
    }

    @Override
    public synchronized Optional<Product> getProduct(String name) {
        return productDb.getProduct(name);
    }

    @Override
    public synchronized void updateProduct(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (product.getId() == null || productDb.getProduct(product.getId()).isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        Optional<Product> productWithSameName = productDb.getProduct(product.getName());
        if (productWithSameName.isPresent() && !productWithSameName.get().getId().equals(product.getId())) {
            throw new RuntimeException("Product already exists");
        }

        productDb.updateProduct(product);
    }

    @Override
    public synchronized void deleteProduct(int id) {
        checkProductExists(id);
        productDb.deleteProduct(id);
    }

    @Override
    public synchronized int deleteAllProducts() {
        return productDb.deleteAllProducts();
    }

    @Override
    public synchronized int getProductQuantity(int productId) {
        checkProductExists(productId);
        return productDb.getProductQuantity(productId);
    }

    @Override
    public synchronized int getProductQuantity(String product) {
        return getProductQuantity(getProductOrThrow(product).getId());
    }

    @Override
    public synchronized void takeProductQuantity(int productId, int count) {
        checkCount(count);

        Product product = getProductOrThrow(productId);
        if (product.getCount() < count) {
            throw new RuntimeException("Not enough product");
        }

        productDb.takeProductQuantity(productId, count);
    }

    @Override
    public synchronized void takeProductQuantity(String product, int count) {
        takeProductQuantity(getProductOrThrow(product).getId(), count);
    }

    @Override
    public synchronized void addProductQuantity(int productId, int count) {
        checkCount(count);
        checkProductExists(productId);

        productDb.addProductQuantity(productId, count);
    }

    @Override
    public synchronized void addProductQuantity(String product, int count) {
        addProductQuantity(getProductOrThrow(product).getId(), count);
    }

    @Override
    public synchronized void setProductPrice(int productId, double price) {
        checkPrice(price);
        checkProductExists(productId);

        productDb.setProductPrice(productId, price);
    }

    @Override
    public synchronized void setProductPrice(String product, double price) {
        setProductPrice(getProductOrThrow(product).getId(), price);
    }

    @Override
    public synchronized double getProductPrice(int productId) {
        checkProductExists(productId);
        return productDb.getProductPrice(productId);
    }

    @Override
    public synchronized double getProductPrice(String product) {
        return getProductPrice(getProductOrThrow(product).getId());
    }

    @Override
    public synchronized int createGroup(ProductGroup group) {
        checkGroupName(group.getName());

        if (productDb.getGroup(group.getName()).isPresent()) {
            throw new RuntimeException("Group already exists");
        }

        return productDb.createGroup(group);
    }

    @Override
    public synchronized void createGroup(String group) {
        createGroup(new ProductGroup(group));
    }

    @Override
    public synchronized int getGroupsCount() {
        return productDb.getGroupsCount();
    }

    @Override
    public synchronized List<ProductGroup> getAllGroups() {
        return productDb.getAllGroups();
    }

    @Override
    public synchronized Optional<ProductGroup> getGroup(int id) {
        return productDb.getGroup(id);
    }

    @Override
    public synchronized Optional<ProductGroup> getGroup(String name) {
        return productDb.getGroup(name);
    }

    @Override
    public synchronized void updateGroup(ProductGroup group) {
        checkGroupName(group.getName());

        if (group.getId() == null || productDb.getGroup(group.getId()).isEmpty()) {
            throw new RuntimeException("Group not found");
        }

        Optional<ProductGroup> groupWithSameName = productDb.getGroup(group.getName());
        if (groupWithSameName.isPresent() && !groupWithSameName.get().getId().equals(group.getId())) {
            throw new RuntimeException("Group already exists");
        }

        productDb.updateGroup(group);
    }

    @Override
    public synchronized void deleteGroup(int id) {
        ProductGroup group = getGroupOrThrow(id);

        if (productDb.hasProductsInGroup(group.getId())) {
            throw new RuntimeException("Group has products");
        }

        productDb.deleteGroup(group.getId());
    }

    @Override
    public synchronized void deleteGroup(String group) {
        deleteGroup(getGroupOrThrow(group).getId());
    }

    @Override
    public synchronized void addProductToGroup(int groupId, int productId) {
        checkGroupExists(groupId);
        checkProductExists(productId);

        if (productDb.isProductInGroup(groupId, productId)) {
            throw new RuntimeException("Product already exists in group");
        }

        productDb.addProductToGroup(groupId, productId);
    }

    @Override
    public synchronized void addProductToGroup(String group, String product) {
        addProductToGroup(getGroupOrThrow(group).getId(), getProductOrThrow(product).getId());
    }

    @Override
    public synchronized boolean isGroupExists(String group) {
        return productDb.getGroup(group).isPresent();
    }

    @Override
    public synchronized boolean hasProductsInGroup(int groupId) {
        checkGroupExists(groupId);
        return productDb.hasProductsInGroup(groupId);
    }

    @Override
    public synchronized boolean hasProductsInGroup(String group) {
        return hasProductsInGroup(getGroupOrThrow(group).getId());
    }

    @Override
    public synchronized boolean isProductInGroup(int groupId, int productId) {
        checkGroupExists(groupId);
        checkProductExists(productId);

        return productDb.isProductInGroup(groupId, productId);
    }

    @Override
    public synchronized boolean isProductInGroup(String group, String product) {
        return isProductInGroup(getGroupOrThrow(group).getId(), getProductOrThrow(product).getId());
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

    private void checkProductExists(int productId) {
        if (productDb.getProduct(productId).isEmpty()) {
            throw new RuntimeException("Product not found");
        }
    }

    private void checkGroupExists(int groupId) {
        if (productDb.getGroup(groupId).isEmpty()) {
            throw new RuntimeException("Group not found");
        }
    }

    private Product getProductOrThrow(int productId) {
        Optional<Product> foundProduct = productDb.getProduct(productId);
        if (foundProduct.isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        return foundProduct.get();
    }

    private Product getProductOrThrow(String product) {
        Optional<Product> foundProduct = productDb.getProduct(product);
        if (foundProduct.isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        return foundProduct.get();
    }

    private ProductGroup getGroupOrThrow(int id) {
        Optional<ProductGroup> foundGroup = productDb.getGroup(id);
        if (foundGroup.isEmpty()) {
            throw new RuntimeException("Group not found");
        }

        return foundGroup.get();
    }

    private ProductGroup getGroupOrThrow(String group) {
        Optional<ProductGroup> foundGroup = productDb.getGroup(group);
        if (foundGroup.isEmpty()) {
            throw new RuntimeException("Group not found");
        }

        return foundGroup.get();
    }
}
