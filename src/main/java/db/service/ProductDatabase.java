package db.service;

import db.model.Filter;
import db.model.Product;
import db.model.ProductGroup;

import java.util.List;
import java.util.Optional;

public interface ProductDatabase {
    int createProduct(Product product);

    int getProductsCount();

    List<Product> getAllProducts();

    List<Product> searchProducts(Filter filter);

    int countProducts(Filter filter);

    Optional<Product> getProduct(int id);

    void updateProduct(Product product);

    void deleteProduct(int id);

    int deleteAllProducts();

    int getProductQuantity(int productId);

    void takeProductQuantity(int productId, int count);

    void addProductQuantity(int productId, int count);

    void setProductPrice(int productId, double price);

    double getProductPrice(int productId);

    int createGroup(ProductGroup group);

    int getGroupsCount();

    List<ProductGroup> getAllGroups();

    Optional<ProductGroup> getGroup(int id);

    void updateGroup(ProductGroup group);

    void deleteGroup(int id);

    void addProductToGroup(int groupId, int productId);

    boolean hasProductsInGroup(int groupId);

    boolean isProductInGroup(int groupId, int productId);
}
