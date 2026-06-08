package db;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    int createProduct(Product product);

    int getProductsCount();

    List<Product> getAllProducts();

    ProductPage searchProducts(Filter filter);

    Optional<Product> getProduct(int id);

    Optional<Product> getProduct(String name);

    void updateProduct(Product product);

    void deleteProduct(int id);

    int deleteAllProducts();

    int getProductQuantity(int productId);

    int getProductQuantity(String product);

    void takeProductQuantity(int productId, int count);

    void takeProductQuantity(String product, int count);

    void addProductQuantity(int productId, int count);

    void addProductQuantity(String product, int count);

    void setProductPrice(int productId, double price);

    void setProductPrice(String product, double price);

    double getProductPrice(int productId);

    double getProductPrice(String product);

    int createGroup(ProductGroup group);

    void createGroup(String group);

    int getGroupsCount();

    List<ProductGroup> getAllGroups();

    Optional<ProductGroup> getGroup(int id);

    Optional<ProductGroup> getGroup(String name);

    void updateGroup(ProductGroup group);

    void deleteGroup(int id);

    void deleteGroup(String group);

    void addProductToGroup(int groupId, int productId);

    void addProductToGroup(String group, String product);

    boolean isGroupExists(String group);

    boolean hasProductsInGroup(int groupId);

    boolean hasProductsInGroup(String group);

    boolean isProductInGroup(int groupId, int productId);

    boolean isProductInGroup(String group, String product);
}
