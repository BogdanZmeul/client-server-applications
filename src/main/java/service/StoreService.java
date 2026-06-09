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
    public void updateProduct(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (product.getId() == null) {
            throw new StoreException("Product not found");
        }

        try {
            productDb.updateProduct(product);
        } catch (DatabaseException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new StoreException("Product already exists");
            }
            if (e.getMessage() != null && e.getMessage().contains("Changes to product have not been applied")) {
                throw new StoreException("Product not found");
            }
            throw e;
        }
    }

    @Override
    public void deleteProduct(int id) {
        try {
            productDb.deleteProduct(id);
        } catch (DatabaseException e) {
            if (e.getMessage() != null && e.getMessage().contains("Changes to product have not been applied")) {
                throw new StoreException("Product not found");
            }
            throw e;
        }
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
    public void takeProductQuantity(int productId, int count) {
        checkCount(count);
        try {
            productDb.takeProductQuantity(productId, count);
        } catch (DatabaseException e) {
            if (e.getMessage() != null && e.getMessage().contains("Changes to product have not been applied")) {
                throw new StoreException("Product not found or not enough quantity");
            }
            throw e;
        }
    }

    @Override
    public void addProductQuantity(int productId, int count) {
        checkCount(count);
        try {
            productDb.addProductQuantity(productId, count);
        } catch (DatabaseException e) {
            if (e.getMessage() != null && e.getMessage().contains("Changes to product have not been applied")) {
                throw new StoreException("Product not found");
            }
            throw e;
        }
    }

    @Override
    public void setProductPrice(int productId, double price) {
        checkPrice(price);
        try {
            productDb.setProductPrice(productId, price);
        } catch (DatabaseException e) {
            if (e.getMessage() != null && e.getMessage().contains("Changes to product have not been applied")) {
                throw new StoreException("Product not found");
            }
            throw e;
        }
    }

    @Override
    public double getProductPrice(int productId) {
        return productDb.getProductPrice(productId);
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
    public void updateGroup(ProductGroup group) {
        checkGroupName(group.getName());

        if (group.getId() == null) {
            throw new StoreException("Group not found");
        }

        try {
            productDb.updateGroup(group);
        } catch (DatabaseException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new StoreException("Group already exists");
            }
            if (e.getMessage() != null && e.getMessage().contains("Changes to group have not been applied")) {
                throw new StoreException("Group not found");
            }
            throw e;
        }
    }

    @Override
    public void deleteGroup(int id) {
        try {
            productDb.deleteGroup(id);
        } catch (DatabaseException e) {
            if (isForeignKeyConstraintViolation(e)) {
                throw new StoreException("Cannot delete group with products");
            }
            if (e.getMessage() != null && e.getMessage().contains("Changes to group have not been applied")) {
                throw new StoreException("Group not found");
            }
            throw e;
        }
    }

    @Override
    public void addProductToGroup(int groupId, int productId) {
        try {
            productDb.addProductToGroup(groupId, productId);
        } catch (DatabaseException e) {
            throw new StoreException("Product not found, group not found or product already in group");
        }
    }

    @Override
    public boolean hasProductsInGroup(int groupId) {
        return productDb.hasProductsInGroup(groupId);
    }

    @Override
    public boolean isProductInGroup(int groupId, int productId) {
        return productDb.isProductInGroup(groupId, productId);
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

    private boolean isUniqueConstraintViolation(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isForeignKeyConstraintViolation(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains("FOREIGN KEY constraint failed")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
