package service;

import db.model.Filter;
import db.model.Product;
import db.service.ProductDatabase;
import db.model.ProductGroup;
import db.model.ProductPage;
import db.service.ProductService;
import db.DatabaseException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StoreService implements ProductService {
    private final ProductDatabase productDb;

    public StoreService(ProductDatabase productDb) {
        this.productDb = productDb;
    }

    @Override
    public int createProduct(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        try {
            return productDb.createProduct(product);
        } catch (DatabaseException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new StoreException("Product already exists");
            }
            throw e;
        }
    }

    @Override
    public int getProductsCount() {
        return productDb.getProductsCount();
    }

    @Override
    public List<Product> getAllProducts() {
        return productDb.getAllProducts();
    }

    @Override
    public ProductPage searchProducts(Filter filter) {
        checkFilter(filter);

        List<Product> products = productDb.searchProducts(filter);
        int totalCount = productDb.countProducts(filter);

        return new ProductPage(products, totalCount, filter.page, filter.pageSize);
    }

    @Override
    public Optional<Product> getProduct(int id) {
        return productDb.getProduct(id);
    }

    @Override
    public Optional<Product> getProduct(String name) {
        return productDb.getProduct(name);
    }

    @Override
    public void updateProduct(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (product.getId() == null || productDb.getProduct(product.getId()).isEmpty()) {
            throw new StoreException("Product not found");
        }

        try {
            productDb.updateProduct(product);
        } catch (DatabaseException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new StoreException("Product already exists");
            }
            throw e;
        }
    }

    @Override
    public void deleteProduct(int id) {
        productDb.deleteProduct(id);
    }

    @Override
    public int deleteAllProducts() {
        return productDb.deleteAllProducts();
    }

    @Override
    public int getProductQuantity(int productId) {
        return productDb.getProductQuantity(productId);
    }

    @Override
    public int getProductQuantity(String product) {
        return getProductQuantity(getProductOrThrow(product).getId());
    }

    @Override
    public void takeProductQuantity(int productId, int count) {
        checkCount(count);
        productDb.takeProductQuantity(productId, count);
    }

    @Override
    public void takeProductQuantity(String product, int count) {
        takeProductQuantity(getProductOrThrow(product).getId(), count);
    }

    @Override
    public void addProductQuantity(int productId, int count) {
        checkCount(count);
        productDb.addProductQuantity(productId, count);
    }

    @Override
    public void addProductQuantity(String product, int count) {
        addProductQuantity(getProductOrThrow(product).getId(), count);
    }

    @Override
    public void setProductPrice(int productId, double price) {
        checkPrice(price);
        productDb.setProductPrice(productId, price);
    }

    @Override
    public void setProductPrice(String product, double price) {
        setProductPrice(getProductOrThrow(product).getId(), price);
    }

    @Override
    public double getProductPrice(int productId) {
        return productDb.getProductPrice(productId);
    }

    @Override
    public double getProductPrice(String product) {
        return getProductPrice(getProductOrThrow(product).getId());
    }

    @Override
    public int createGroup(ProductGroup group) {
        checkGroupName(group.getName());

        try {
            return productDb.createGroup(group);
        } catch (DatabaseException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new StoreException("Group already exists");
            }
            throw e;
        }
    }

    @Override
    public void createGroup(String group) {
        createGroup(new ProductGroup(group));
    }

    @Override
    public int getGroupsCount() {
        return productDb.getGroupsCount();
    }

    @Override
    public List<ProductGroup> getAllGroups() {
        return productDb.getAllGroups();
    }

    @Override
    public Optional<ProductGroup> getGroup(int id) {
        return productDb.getGroup(id);
    }

    @Override
    public Optional<ProductGroup> getGroup(String name) {
        return productDb.getGroup(name);
    }

    @Override
    public void updateGroup(ProductGroup group) {
        checkGroupName(group.getName());

        if (group.getId() == null || productDb.getGroup(group.getId()).isEmpty()) {
            throw new StoreException("Group not found");
        }

        try {
            productDb.updateGroup(group);
        } catch (DatabaseException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new StoreException("Group already exists");
            }
            throw e;
        }
    }

    @Override
    public void deleteGroup(int id) {
        productDb.deleteGroup(id);
    }

    @Override
    public void deleteGroup(String group) {
        deleteGroup(getGroupOrThrow(group).getId());
    }

    @Override
    public void addProductToGroup(int groupId, int productId) {
        checkGroupExists(groupId);
        checkProductExists(productId);

        if (productDb.isProductInGroup(groupId, productId)) {
            throw new StoreException("Product already exists in group");
        }

        productDb.addProductToGroup(groupId, productId);
    }

    @Override
    public void addProductToGroup(String group, String product) {
        addProductToGroup(getGroupOrThrow(group).getId(), getProductOrThrow(product).getId());
    }

    @Override
    public boolean isGroupExists(String group) {
        return productDb.getGroup(group).isPresent();
    }

    @Override
    public boolean hasProductsInGroup(int groupId) {
        checkGroupExists(groupId);
        return productDb.hasProductsInGroup(groupId);
    }

    @Override
    public boolean hasProductsInGroup(String group) {
        return hasProductsInGroup(getGroupOrThrow(group).getId());
    }

    @Override
    public boolean isProductInGroup(int groupId, int productId) {
        checkGroupExists(groupId);
        checkProductExists(productId);

        return productDb.isProductInGroup(groupId, productId);
    }

    @Override
    public boolean isProductInGroup(String group, String product) {
        return isProductInGroup(getGroupOrThrow(group).getId(), getProductOrThrow(product).getId());
    }

    private void checkCount(int count) {
        if (count < 0) {
            throw new StoreException("Count cannot be negative");
        }
    }

    private void checkPrice(double price) {
        if (price < 0) {
            throw new StoreException("Price cannot be negative");
        }
    }

    private void checkFilter(Filter filter) {
        if (filter == null) {
            throw new StoreException("Filter cannot be null");
        }

        if (filter.name != null && filter.name.isBlank()) {
            filter.name = null;
        }

        if (filter.groups != null) {
            filter.groups = filter.groups.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(group -> !group.isBlank())
                    .toList();

            if (filter.groups.isEmpty()) {
                filter.groups = null;
            }
        }

        if (filter.page != null && filter.page < 1) {
            throw new StoreException("Page cannot be less than 1");
        }

        if (filter.pageSize != null && filter.pageSize < 1) {
            throw new StoreException("Page size cannot be less than 1");
        }

        if (filter.minCount != null) {
            checkCount(filter.minCount);
        }

        if (filter.maxCount != null) {
            checkCount(filter.maxCount);
        }

        if (filter.minCount != null && filter.maxCount != null && filter.minCount > filter.maxCount) {
            throw new StoreException("Min count cannot be greater than max count");
        }

        if (filter.minPrice != null) {
            checkPrice(filter.minPrice);
        }

        if (filter.maxPrice != null) {
            checkPrice(filter.maxPrice);
        }

        if (filter.minPrice != null && filter.maxPrice != null && filter.minPrice > filter.maxPrice) {
            throw new StoreException("Min price cannot be greater than max price");
        }
    }

    private void checkGroupName(String group) {
        if (group == null || group.isBlank()) {
            throw new StoreException("Group name cannot be empty");
        }
    }

    private void checkProductExists(int productId) {
        if (productDb.getProduct(productId).isEmpty()) {
            throw new StoreException("Product not found");
        }
    }

    private void checkGroupExists(int groupId) {
        if (productDb.getGroup(groupId).isEmpty()) {
            throw new StoreException("Group not found");
        }
    }

    private Product getProductOrThrow(int productId) {
        Optional<Product> foundProduct = productDb.getProduct(productId);
        if (foundProduct.isEmpty()) {
            throw new StoreException("Product not found");
        }

        return foundProduct.get();
    }

    private Product getProductOrThrow(String product) {
        Optional<Product> foundProduct = productDb.getProduct(product);
        if (foundProduct.isEmpty()) {
            throw new StoreException("Product not found");
        }

        return foundProduct.get();
    }

    private ProductGroup getGroupOrThrow(int id) {
        Optional<ProductGroup> foundGroup = productDb.getGroup(id);
        if (foundGroup.isEmpty()) {
            throw new StoreException("Group not found");
        }

        return foundGroup.get();
    }

    private ProductGroup getGroupOrThrow(String group) {
        Optional<ProductGroup> foundGroup = productDb.getGroup(group);
        if (foundGroup.isEmpty()) {
            throw new StoreException("Group not found");
        }

        return foundGroup.get();
    }

    private boolean isUniqueConstraintViolation(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains("UNIQUE constraint failed")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
