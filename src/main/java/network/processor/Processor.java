package network.processor;

import data.Message;
import data.Package;
import db.Filter;
import db.Product;
import db.ProductGroup;
import db.ProductService;
import utils.MessageType;

import java.util.Arrays;

public class Processor {
    private final ProductService productService;

    public Processor(ProductService productService) {
        this.productService = productService;
    }

    public Package process(Package pack) {
        if (pack == null) {
            throw new NullPointerException("Package cannot be null");
        }
        if (pack.getMessage() == null) {
            throw new NullPointerException("Message cannot be null");
        }

        Message responseMessage = process(pack.getMessage());
        return new Package(pack.getbSrc(), pack.getbPktId(), responseMessage);
    }

    public Message process(Message message) {
        if (message == null) {
            throw new NullPointerException("Message cannot be null");
        }

        String answer;
        try {
            answer = execute(message);
        } catch (Exception e) {
            answer = "Error:" + e.getMessage();
        }

        return new Message(message.getcType(), message.getbUserId(), answer);
    }

    private String execute(Message message) {
        return switch (message.getcType()) {
            case MessageType.GET_PRODUCT_COUNT -> getProductCount(message);
            case MessageType.TAKE_PRODUCT -> takeProduct(message);
            case MessageType.ADD_PRODUCT -> addProduct(message);
            case MessageType.ADD_GROUP -> addGroup(message);
            case MessageType.ADD_PRODUCT_TO_GROUP -> addProductToGroup(message);
            case MessageType.SET_PRICE -> setPrice(message);
            case MessageType.CREATE_PRODUCT -> createProduct(message);
            case MessageType.GET_PRODUCT -> getProduct(message);
            case MessageType.GET_PRODUCT_BY_NAME -> getProductByName(message);
            case MessageType.GET_ALL_PRODUCTS -> getAllProducts(message);
            case MessageType.UPDATE_PRODUCT -> updateProduct(message);
            case MessageType.DELETE_PRODUCT -> deleteProduct(message);
            case MessageType.DELETE_ALL_PRODUCTS -> deleteAllProducts(message);
            case MessageType.CREATE_GROUP -> createGroup(message);
            case MessageType.GET_GROUP -> getGroup(message);
            case MessageType.GET_GROUP_BY_NAME -> getGroupByName(message);
            case MessageType.GET_ALL_GROUPS -> getAllGroups(message);
            case MessageType.UPDATE_GROUP -> updateGroup(message);
            case MessageType.DELETE_GROUP -> deleteGroup(message);
            case MessageType.DELETE_GROUP_BY_NAME -> deleteGroupByName(message);
            case MessageType.SEARCH_PRODUCTS -> searchProducts(message);
            default -> throw new RuntimeException("Unknown command");
        };
    }

    private String getProductCount(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        return "Ok:" + productService.getProductQuantity(args[0]);
    }

    private String takeProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.takeProductQuantity(args[0], Integer.parseInt(args[1]));

        return "Ok";
    }

    private String addProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.addProductQuantity(args[0], Integer.parseInt(args[1]));

        return "Ok";
    }

    private String addGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        productService.createGroup(args[0]);

        return "Ok";
    }

    private String addProductToGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.addProductToGroup(args[0], args[1]);

        return "Ok";
    }

    private String setPrice(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.setProductPrice(args[0], Double.parseDouble(args[1]));

        return "Ok";
    }

    private String createProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 3);
        int id = productService.createProduct(new Product(args[0], Integer.parseInt(args[1]), Double.parseDouble(args[2])));

        return "Ok:" + id;
    }

    private String getProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        Product product = productService.getProduct(Integer.parseInt(args[0]))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return "Ok:" + product;
    }

    private String getProductByName(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        Product product = productService.getProduct(args[0])
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return "Ok:" + product;
    }

    private String getAllProducts(Message message) {
        getArgs(message.getMessage(), 0);

        return "Ok:" + productService.getAllProducts();
    }

    private String updateProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 4);
        productService.updateProduct(new Product(Integer.parseInt(args[0]), args[1],
                Integer.parseInt(args[2]), Double.parseDouble(args[3])));

        return "Ok";
    }

    private String deleteProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        productService.deleteProduct(Integer.parseInt(args[0]));

        return "Ok";
    }

    private String deleteAllProducts(Message message) {
        getArgs(message.getMessage(), 0);
        int deleted = productService.deleteAllProducts();

        return "Ok:" + deleted;
    }

    private String createGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        int id = productService.createGroup(new ProductGroup(args[0]));

        return "Ok:" + id;
    }

    private String getGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        ProductGroup group = productService.getGroup(Integer.parseInt(args[0]))
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return "Ok:" + group;
    }

    private String getGroupByName(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        ProductGroup group = productService.getGroup(args[0])
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return "Ok:" + group;
    }

    private String getAllGroups(Message message) {
        getArgs(message.getMessage(), 0);

        return "Ok:" + productService.getAllGroups();
    }

    private String updateGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.updateGroup(new ProductGroup(Integer.parseInt(args[0]), args[1]));

        return "Ok";
    }

    private String deleteGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        productService.deleteGroup(Integer.parseInt(args[0]));

        return "Ok";
    }

    private String deleteGroupByName(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        productService.deleteGroup(args[0]);

        return "Ok";
    }

    private String searchProducts(Message message) {
        Filter filter = getFilter(message.getMessage());

        return "Ok:" + productService.searchProducts(filter);
    }

    private Filter getFilter(String text) {
        Filter filter = new Filter();

        if (text == null || text.isBlank()) {
            return filter;
        }

        String[] args = text.split(";");
        for (String arg : args) {
            String[] keyAndValue = arg.split("=", 2);
            if (keyAndValue.length != 2) {
                throw new RuntimeException("Invalid filter");
            }

            setFilterValue(filter, keyAndValue[0].trim(), keyAndValue[1].trim());
        }

        return filter;
    }

    private void setFilterValue(Filter filter, String key, String value) {
        switch (key) {
            case "name" -> filter.name = value;
            case "group", "groups", "category", "categories" -> filter.groups = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .toList();
            case "minCount" -> filter.minCount = Integer.parseInt(value);
            case "maxCount" -> filter.maxCount = Integer.parseInt(value);
            case "minPrice" -> filter.minPrice = Double.parseDouble(value);
            case "maxPrice" -> filter.maxPrice = Double.parseDouble(value);
            case "page" -> filter.page = Integer.parseInt(value);
            case "pageSize" -> filter.pageSize = Integer.parseInt(value);
            default -> throw new RuntimeException("Unknown filter");
        }
    }

    private String[] getArgs(String text, int count) {
        if (count == 0) {
            if (text == null || text.isBlank()) {
                return new String[0];
            }

            throw new RuntimeException("Invalid command");
        }

        if (text == null) {
            throw new RuntimeException("Message cannot be null");
        }

        String[] args = text.split(";");
        if (args.length != count) {
            throw new RuntimeException("Invalid command");
        }

        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].trim();
        }

        return args;
    }
}
