package network.storage;

import db.Product;
import db.ProductGroup;
import db.ProductService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProductStorage implements ProductService {
    private final Map<Integer, Product> products = new HashMap<>();
    private final Map<Integer, ProductGroup> groups = new HashMap<>();
    private final Map<Integer, Set<Integer>> groupProducts = new HashMap<>();
    private int productId = 1;
    private int groupId = 1;

    @Override
    public synchronized int createProduct(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        int id = productId++;
        products.put(id, new Product(id, product.getName(), product.getCount(), product.getPrice()));

        return id;
    }

    @Override
    public synchronized int getProductsCount() {
        return products.size();
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    @Override
    public synchronized Optional<Product> getProduct(int id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public synchronized Optional<Product> getProduct(String name) {
        for (Product product : products.values()) {
            if (product.getName().equals(name)) {
                return Optional.of(product);
            }
        }

        return Optional.empty();
    }

    @Override
    public synchronized void updateProduct(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (product.getId() != null) {
            products.put(product.getId(), product);
        }
    }

    @Override
    public synchronized void deleteProduct(int id) {
        products.remove(id);

        for (Set<Integer> productIds : groupProducts.values()) {
            productIds.remove(id);
        }
    }

    @Override
    public synchronized int deleteAllProducts() {
        int count = products.size();
        products.clear();

        for (Set<Integer> productIds : groupProducts.values()) {
            productIds.clear();
        }

        return count;
    }

    @Override
    public synchronized int getProductQuantity(int productId) {
        Product product = products.get(productId);
        if (product == null) {
            return 0;
        }

        return product.getCount();
    }

    @Override
    public synchronized int getProductQuantity(String product) {
        Optional<Product> foundProduct = getProduct(product);
        if (foundProduct.isEmpty()) {
            return 0;
        }

        return foundProduct.get().getCount();
    }

    @Override
    public synchronized void takeProductQuantity(int productId, int count) {
        checkCount(count);

        Product product = products.get(productId);
        if (product == null || product.getCount() < count) {
            throw new RuntimeException("Not enough product");
        }

        product.setCount(product.getCount() - count);
    }

    @Override
    public synchronized void takeProductQuantity(String product, int count) {
        takeProductQuantity(getProductId(product), count);
    }

    @Override
    public synchronized void addProductQuantity(int productId, int count) {
        checkCount(count);

        Product product = products.get(productId);
        if (product != null) {
            product.setCount(product.getCount() + count);
        }
    }

    @Override
    public synchronized void addProductQuantity(String product, int count) {
        addProductQuantity(getProductId(product), count);
    }

    @Override
    public synchronized void setProductPrice(int productId, double price) {
        checkPrice(price);

        Product product = products.get(productId);
        if (product != null) {
            product.setPrice(price);
        }
    }

    @Override
    public synchronized void setProductPrice(String product, double price) {
        setProductPrice(getProductId(product), price);
    }

    @Override
    public synchronized double getProductPrice(int productId) {
        Product product = products.get(productId);
        if (product == null) {
            return 0;
        }

        return product.getPrice();
    }

    @Override
    public synchronized double getProductPrice(String product) {
        return getProductPrice(getProductId(product));
    }

    @Override
    public synchronized int createGroup(ProductGroup group) {
        int id = groupId++;
        groups.put(id, new ProductGroup(id, group.getName()));
        groupProducts.put(id, new HashSet<>());

        return id;
    }

    @Override
    public synchronized void createGroup(String group) {
        createGroup(new ProductGroup(group));
    }

    @Override
    public synchronized int getGroupsCount() {
        return groups.size();
    }

    @Override
    public synchronized List<ProductGroup> getAllGroups() {
        return new ArrayList<>(groups.values());
    }

    @Override
    public synchronized Optional<ProductGroup> getGroup(int id) {
        return Optional.ofNullable(groups.get(id));
    }

    @Override
    public synchronized Optional<ProductGroup> getGroup(String name) {
        for (ProductGroup group : groups.values()) {
            if (group.getName().equals(name)) {
                return Optional.of(group);
            }
        }

        return Optional.empty();
    }

    @Override
    public synchronized void updateGroup(ProductGroup group) {
        if (group.getId() != null) {
            groups.put(group.getId(), group);
        }
    }

    @Override
    public synchronized void deleteGroup(int id) {
        groups.remove(id);
        groupProducts.remove(id);
    }

    @Override
    public synchronized void deleteGroup(String group) {
        deleteGroup(getGroupId(group));
    }

    @Override
    public synchronized void addProductToGroup(int groupId, int productId) {
        if (groupProducts.containsKey(groupId) && products.containsKey(productId)) {
            groupProducts.get(groupId).add(productId);
        }
    }

    @Override
    public synchronized void addProductToGroup(String group, String product) {
        addProductToGroup(getGroupId(group), getProductId(product));
    }

    @Override
    public synchronized boolean isGroupExists(String group) {
        return getGroup(group).isPresent();
    }

    @Override
    public synchronized boolean hasProductsInGroup(int groupId) {
        if (!groupProducts.containsKey(groupId)) {
            return false;
        }

        return !groupProducts.get(groupId).isEmpty();
    }

    @Override
    public synchronized boolean hasProductsInGroup(String group) {
        return hasProductsInGroup(getGroupId(group));
    }

    @Override
    public synchronized boolean isProductInGroup(int groupId, int productId) {
        if (!groupProducts.containsKey(groupId)) {
            return false;
        }

        return groupProducts.get(groupId).contains(productId);
    }

    @Override
    public synchronized boolean isProductInGroup(String group, String product) {
        return isProductInGroup(getGroupId(group), getProductId(product));
    }

    private int getProductId(String product) {
        Optional<Product> foundProduct = getProduct(product);
        if (foundProduct.isEmpty()) {
            return -1;
        }

        return foundProduct.get().getId();
    }

    private int getGroupId(String group) {
        Optional<ProductGroup> foundGroup = getGroup(group);
        if (foundGroup.isEmpty()) {
            return -1;
        }

        return foundGroup.get().getId();
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
