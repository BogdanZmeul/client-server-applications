package network.processor;

import data.Message;
import data.Package;
import db.service.SqliteProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.StoreService;
import utils.MessageType;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorCrudTest {
    @TempDir
    Path tempDir;

    private SqliteProductService productDb;
    private Processor processor;

    @BeforeEach
    void setUp() {
        productDb = new SqliteProductService(tempDir.resolve("processor-crud.db").toString());
        processor = new Processor(new StoreService(productDb));
    }

    @AfterEach
    void close() {
        productDb.close();
    }

    @Test
    void shouldProcessProductCrudCommands() {
        int productId = getId(process(MessageType.CREATE_PRODUCT, "apple;10;5.5"));

        assertTrue(process(MessageType.GET_PRODUCT, String.valueOf(productId)).contains("apple"));
        assertTrue(process(MessageType.GET_ALL_PRODUCTS, "").contains("apple"));

        assertEquals("Ok", process(MessageType.UPDATE_PRODUCT, productId + ";green apple;20;7.5"));
        assertTrue(process(MessageType.GET_PRODUCT, String.valueOf(productId)).contains("green apple"));

        assertEquals("Ok", process(MessageType.DELETE_PRODUCT, String.valueOf(productId)));
        assertEquals("Error:Product not found", process(MessageType.GET_PRODUCT, String.valueOf(productId)));
    }

    @Test
    void shouldProcessDeleteAllProductsCommand() {
        process(MessageType.CREATE_PRODUCT, "apple;10;5.5");
        process(MessageType.CREATE_PRODUCT, "milk;20;3.0");

        String answer = process(MessageType.DELETE_ALL_PRODUCTS, "");

        assertEquals("Ok:2", answer);
        assertEquals("Ok:[]", process(MessageType.GET_ALL_PRODUCTS, ""));
    }

    @Test
    void shouldProcessGroupCrudCommands() {
        int groupId = getId(process(MessageType.CREATE_GROUP, "fruits"));

        assertTrue(process(MessageType.GET_GROUP, String.valueOf(groupId)).contains("fruits"));
        assertTrue(process(MessageType.GET_ALL_GROUPS, "").contains("fruits"));

        assertEquals("Ok", process(MessageType.UPDATE_GROUP, groupId + ";fresh fruits"));
        assertTrue(process(MessageType.GET_GROUP, String.valueOf(groupId)).contains("fresh fruits"));

        assertEquals("Ok", process(MessageType.DELETE_GROUP, String.valueOf(groupId)));
        assertEquals("Error:Group not found", process(MessageType.GET_GROUP, String.valueOf(groupId)));
    }

    @Test
    void shouldProcessSearchProductsCommand() {
        int appleId = getId(process(MessageType.CREATE_PRODUCT, "apple;10;5.5"));
        int greenAppleId = getId(process(MessageType.CREATE_PRODUCT, "green apple;50;7.2"));
        int milkId = getId(process(MessageType.CREATE_PRODUCT, "milk;20;3.0"));
        int fruitsId = getId(process(MessageType.CREATE_GROUP, "fruits"));
        int drinksId = getId(process(MessageType.CREATE_GROUP, "drinks"));
        process(MessageType.ADD_PRODUCT_TO_GROUP, fruitsId + ";" + appleId);
        process(MessageType.ADD_PRODUCT_TO_GROUP, fruitsId + ";" + greenAppleId);
        process(MessageType.ADD_PRODUCT_TO_GROUP, drinksId + ";" + milkId);

        String answer = process(MessageType.SEARCH_PRODUCTS,
                "name=apple;groups=fruits;minCount=5;maxCount=20;minPrice=5;maxPrice=6;page=1;pageSize=10");

        assertTrue(answer.startsWith("Ok:ProductPage"));
        assertTrue(answer.contains("apple"));
        assertFalse(answer.contains("green apple"));
        assertFalse(answer.contains("milk"));
    }

    @Test
    void shouldReturnErrorWhenSearchFilterIsWrong() {
        String answer = process(MessageType.SEARCH_PRODUCTS, "minPrice=10;maxPrice=1");

        assertEquals("Error:Min price cannot be greater than max price", answer);
    }

    @Test
    void shouldReturnErrorWhenSearchFilterIsUnknown() {
        String answer = process(MessageType.SEARCH_PRODUCTS, "wrong=apple");

        assertEquals("Error:Unknown filter", answer);
    }

    @Test
    void shouldReturnErrorWhenProductCrudCommandHasWrongArgumentsCount() {
        assertEquals("Error:Invalid command", process(MessageType.CREATE_PRODUCT, "apple;10"));
        assertEquals("Error:Invalid command", process(MessageType.UPDATE_PRODUCT, "1;apple;10"));
        assertEquals("Error:Invalid command", process(MessageType.DELETE_PRODUCT, "1;2"));
        assertEquals("Error:Invalid command", process(MessageType.GET_ALL_PRODUCTS, "apple"));
    }

    @Test
    void shouldReturnErrorWhenGroupCrudCommandHasWrongArgumentsCount() {
        assertEquals("Error:Invalid command", process(MessageType.CREATE_GROUP, "fruits;extra"));
        assertEquals("Error:Invalid command", process(MessageType.UPDATE_GROUP, "1"));
        assertEquals("Error:Invalid command", process(MessageType.DELETE_GROUP, "1;2"));
        assertEquals("Error:Invalid command", process(MessageType.GET_ALL_GROUPS, "fruits"));
    }

    private String process(int type, String text) {
        Package answer = processor.process(new Package((byte) 1, 1, new Message(type, 1, text)));
        return answer.getMessage().getMessage();
    }

    private int getId(String answer) {
        assertTrue(answer.startsWith("Ok:"));
        return Integer.parseInt(answer.substring(3));
    }
}
