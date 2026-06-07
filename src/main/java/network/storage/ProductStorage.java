package network.storage;

import db.Product;
import db.ProductGroup;
import db.ProductService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProductStorage implements ProductService {
    private final Map<String, Integer> products = new HashMap<>();
    private final Map<String, Set<String>> groups = new HashMap<>();
    private final Map<String, Double> prices = new HashMap<>();
    private final Map<Integer, Product> productsById = new HashMap<>();
    private final Map<Integer, ProductGroup> groupsById = new HashMap<>();
    private int idCounter = 1;
    private int groupIdCounter = 1;

    @Override
    public synchronized int create(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        int id = idCounter++;
        Product savedProduct = new Product(id, product.getName(), product.getCount(), product.getPrice());
        productsById.put(id, savedProduct);
        products.put(product.getName(), product.getCount());
        prices.put(product.getName(), product.getPrice());

        return id;
    }

    @Override
    public synchronized int count() {
        return productsById.size();
    }

    @Override
    public synchronized List<Product> readAll() {
        return new ArrayList<>(productsById.values());
    }

    @Override
    public synchronized Optional<Product> read(int id) {
        return Optional.ofNullable(productsById.get(id));
    }

    @Override
    public synchronized Optional<Product> readByName(String name) {
        for (Product product : productsById.values()) {
            if (product.getName().equals(name)) {
                return Optional.of(product);
            }
        }

        return Optional.empty();
    }

    @Override
    public synchronized int update(Product product) {
        checkCount(product.getCount());
        checkPrice(product.getPrice());

        if (product.getId() == null || !productsById.containsKey(product.getId())) {
            return 0;
        }

        productsById.put(product.getId(), product);
        products.put(product.getName(), product.getCount());
        prices.put(product.getName(), product.getPrice());

        return 1;
    }

    @Override
    public synchronized int delete(int id) {
        Product product = productsById.remove(id);
        if (product == null) {
            return 0;
        }

        products.remove(product.getName());
        prices.remove(product.getName());

        for (Set<String> groupProducts : groups.values()) {
            groupProducts.remove(product.getName());
        }

        return 1;
    }

    @Override
    public synchronized int deleteAll() {
        int count = productsById.size();
        productsById.clear();
        products.clear();
        prices.clear();

        for (Set<String> groupProducts : groups.values()) {
            groupProducts.clear();
        }

        return count;
    }

    @Override
    public synchronized int getProductCount(String product) {
        if (!products.containsKey(product)) {
            return 0;
        }

        return products.get(product);
    }

    @Override
    public synchronized void takeProduct(String product, int count) {
        checkCount(count);

        int oldCount = getProductCount(product);
        if (oldCount < count) {
            throw new RuntimeException("Not enough product");
        }

        products.put(product, oldCount - count);
        updateProductModel(product, oldCount - count, getPrice(product));
    }

    @Override
    public synchronized void addProduct(String product, int count) {
        checkCount(count);

        int oldCount = getProductCount(product);
        products.put(product, oldCount + count);
        updateProductModel(product, oldCount + count, getPrice(product));
    }

    @Override
    public synchronized int createGroup(ProductGroup group) {
        int id = groupIdCounter++;
        ProductGroup savedGroup = new ProductGroup(id, group.getName());
        groupsById.put(id, savedGroup);
        groups.put(group.getName(), new HashSet<>());

        return id;
    }

    @Override
    public synchronized int groupsCount() {
        return groupsById.size();
    }

    @Override
    public synchronized List<ProductGroup> readAllGroups() {
        return new ArrayList<>(groupsById.values());
    }

    @Override
    public synchronized Optional<ProductGroup> readGroup(int id) {
        return Optional.ofNullable(groupsById.get(id));
    }

    @Override
    public synchronized Optional<ProductGroup> readGroupByName(String name) {
        for (ProductGroup group : groupsById.values()) {
            if (group.getName().equals(name)) {
                return Optional.of(group);
            }
        }

        return Optional.empty();
    }

    @Override
    public synchronized int updateGroup(ProductGroup group) {
        if (group.getId() == null || !groupsById.containsKey(group.getId())) {
            return 0;
        }

        ProductGroup oldGroup = groupsById.get(group.getId());
        Set<String> productsInGroup = groups.remove(oldGroup.getName());
        groups.put(group.getName(), productsInGroup);
        groupsById.put(group.getId(), group);

        return 1;
    }

    @Override
    public synchronized void addGroup(String group) {
        if (readGroupByName(group).isEmpty()) {
            createGroup(new ProductGroup(group));
        }
    }

    @Override
    public synchronized int deleteGroup(int id) {
        ProductGroup group = groupsById.remove(id);
        if (group == null) {
            return 0;
        }

        groups.remove(group.getName());
        return 1;
    }

    @Override
    public synchronized int deleteGroup(String group) {
        Optional<ProductGroup> foundGroup = readGroupByName(group);
        if (foundGroup.isEmpty()) {
            return 0;
        }

        return deleteGroup(foundGroup.get().getId());
    }

    @Override
    public synchronized void addProductToGroup(String group, String product) {
        addGroup(group);
        groups.get(group).add(product);
    }

    @Override
    public synchronized void setPrice(String product, double price) {
        checkPrice(price);

        prices.put(product, price);
        updateProductModel(product, getProductCount(product), price);
    }

    @Override
    public synchronized double getPrice(String product) {
        if (!prices.containsKey(product)) {
            return 0;
        }

        return prices.get(product);
    }

    @Override
    public synchronized boolean isGroupExists(String group) {
        return readGroupByName(group).isPresent();
    }

    @Override
    public synchronized boolean hasProductsInGroup(String group) {
        if (!groups.containsKey(group)) {
            return false;
        }

        return !groups.get(group).isEmpty();
    }

    @Override
    public synchronized boolean isProductInGroup(String group, String product) {
        if (!groups.containsKey(group)) {
            return false;
        }

        return groups.get(group).contains(product);
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

    private void updateProductModel(String name, int count, double price) {
        Optional<Product> found = readByName(name);
        if (found.isPresent()) {
            Product product = found.get();
            product.setCount(count);
            product.setPrice(price);
            productsById.put(product.getId(), product);
            return;
        }

        int id = idCounter++;
        productsById.put(id, new Product(id, name, count, price));
    }
}
