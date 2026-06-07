package db;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    int create(Product product);

    int count();

    List<Product> readAll();

    Optional<Product> read(int id);

    Optional<Product> readByName(String name);

    int update(Product product);

    int delete(int id);

    int deleteAll();

    int getProductCount(String product);

    void takeProduct(String product, int count);

    void addProduct(String product, int count);

    int createGroup(ProductGroup group);

    int groupsCount();

    List<ProductGroup> readAllGroups();

    Optional<ProductGroup> readGroup(int id);

    Optional<ProductGroup> readGroupByName(String name);

    int updateGroup(ProductGroup group);

    void addGroup(String group);

    int deleteGroup(int id);

    int deleteGroup(String group);

    void addProductToGroup(String group, String product);

    void setPrice(String product, double price);

    double getPrice(String product);

    boolean isGroupExists(String group);

    boolean hasProductsInGroup(String group);

    boolean isProductInGroup(String group, String product);
}
