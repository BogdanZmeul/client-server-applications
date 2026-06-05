package network.processor;

import data.Message;
import data.Package;
import db.ProductService;
import utils.MessageType;

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
            default -> throw new RuntimeException("Unknown command");
        };
    }

    private String getProductCount(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        return "Ok:" + productService.getProductCount(args[0]);
    }

    private String takeProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.takeProduct(args[0], Integer.parseInt(args[1]));

        return "Ok";
    }

    private String addProduct(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.addProduct(args[0], Integer.parseInt(args[1]));

        return "Ok";
    }

    private String addGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 1);
        productService.addGroup(args[0]);

        return "Ok";
    }

    private String addProductToGroup(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.addProductToGroup(args[0], args[1]);

        return "Ok";
    }

    private String setPrice(Message message) {
        String[] args = getArgs(message.getMessage(), 2);
        productService.setPrice(args[0], Double.parseDouble(args[1]));

        return "Ok";
    }

    private String[] getArgs(String text, int count) {
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
