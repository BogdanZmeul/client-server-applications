package network.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProductStorage {
    private final Map<String, Integer> products = new HashMap<>();
    private final Map<String, Set<String>> groups = new HashMap<>();
    private final Map<String, Double> prices = new HashMap<>();

    public synchronized int getProductCount(String product) {
        if (!products.containsKey(product)) {
            return 0;
        }

        return products.get(product);
    }

    public synchronized void takeProduct(String product, int count) {
        checkCount(count);

        int oldCount = getProductCount(product);
        if (oldCount < count) {
            throw new RuntimeException("Not enough product");
        }

        products.put(product, oldCount - count);
    }

    public synchronized void addProduct(String product, int count) {
        checkCount(count);

        int oldCount = getProductCount(product);
        products.put(product, oldCount + count);
    }

    public synchronized void addGroup(String group) {
        if (!groups.containsKey(group)) {
            groups.put(group, new HashSet<>());
        }
    }

    public synchronized void addProductToGroup(String group, String product) {
        addGroup(group);
        groups.get(group).add(product);
    }

    public synchronized void setPrice(String product, double price) {
        if (price < 0) {
            throw new RuntimeException("Price cannot be negative");
        }

        prices.put(product, price);
    }

    public synchronized double getPrice(String product) {
        if (!prices.containsKey(product)) {
            return 0;
        }

        return prices.get(product);
    }

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
}
