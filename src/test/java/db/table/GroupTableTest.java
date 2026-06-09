package db.table;

import db.model.Product;
import db.model.ProductGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class GroupTableTest {
    @TempDir
    Path tempDir;

    private Connection connection;
    private ProductTable productTable;
    private GroupTable groupTable;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("group-table.db"));
        initDb();
        productTable = new ProductTable(connection);
        groupTable = new GroupTable(connection);
    }

    @AfterEach
    void close() throws Exception {
        connection.close();
    }

    @Test
    void shouldCreateReadUpdateAndDeleteGroup() {
        int groupId = groupTable.createGroup(new ProductGroup("fruits"));

        ProductGroup group = groupTable.getGroup(groupId).orElseThrow();
        assertEquals("fruits", group.getName());
        assertEquals(1, groupTable.getGroupsCount());
        assertEquals(1, groupTable.getAllGroups().size());
        assertTrue(groupTable.getGroup("fruits").isPresent());

        groupTable.updateGroup(new ProductGroup(groupId, "fresh fruits"));

        ProductGroup updatedGroup = groupTable.getGroup(groupId).orElseThrow();
        assertEquals("fresh fruits", updatedGroup.getName());

        groupTable.deleteGroup(groupId);

        assertEquals(0, groupTable.getGroupsCount());
        assertTrue(groupTable.getGroup(groupId).isEmpty());
    }

    @Test
    void shouldAddProductToGroupAndCheckIt() {
        int productId = productTable.createProduct(new Product("apple", 10, 5.5));
        int groupId = groupTable.createGroup(new ProductGroup("fruits"));

        groupTable.addProductToGroup(groupId, productId);

        assertTrue(groupTable.hasProductsInGroup(groupId));
        assertTrue(groupTable.isProductInGroup(groupId, productId));
    }

    @Test
    void shouldReturnFalseWhenGroupHasNoProducts() {
        int productId = productTable.createProduct(new Product("apple", 10, 5.5));
        int groupId = groupTable.createGroup(new ProductGroup("fruits"));

        assertFalse(groupTable.hasProductsInGroup(groupId));
        assertFalse(groupTable.isProductInGroup(groupId, productId));
    }

    @Test
    void shouldReturnEmptyOptionalWhenGroupDoesNotExist() {
        assertTrue(groupTable.getGroup(100).isEmpty());
        assertTrue(groupTable.getGroup("fruits").isEmpty());
    }

    @Test
    void shouldRejectDeletingGroupWithProducts() {
        int productId = productTable.createProduct(new Product("apple", 10, 5.5));
        int groupId = groupTable.createGroup(new ProductGroup("fruits"));
        groupTable.addProductToGroup(groupId, productId);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> groupTable.deleteGroup(groupId));

        assertEquals("Changes to group have not been applied", error.getMessage());
        assertTrue(groupTable.getGroup(groupId).isPresent());
    }

    @Test
    void shouldThrowErrorWhenGroupChangesAreNotApplied() {
        int productId = productTable.createProduct(new Product("apple", 10, 5.5));

        RuntimeException updateError = assertThrows(RuntimeException.class,
                () -> groupTable.updateGroup(new ProductGroup(100, "fruits")));
        RuntimeException deleteError = assertThrows(RuntimeException.class,
                () -> groupTable.deleteGroup(100));
        RuntimeException wrongGroupError = assertThrows(RuntimeException.class,
                () -> groupTable.addProductToGroup(100, productId));
        RuntimeException wrongProductError = assertThrows(RuntimeException.class,
                () -> groupTable.addProductToGroup(groupTable.createGroup(new ProductGroup("fruits")), 100));

        assertEquals("Changes to group have not been applied", updateError.getMessage());
        assertEquals("Changes to group have not been applied", deleteError.getMessage());
        assertEquals("Changes to group have not been applied", wrongGroupError.getMessage());
        assertEquals("Changes to group have not been applied", wrongProductError.getMessage());
    }

    @Test
    void shouldRejectDuplicateGroupNameOnDatabaseLevel() {
        groupTable.createGroup(new ProductGroup("fruits"));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> groupTable.createGroup(new ProductGroup("fruits")));

        assertTrue(error.getMessage().contains("Can't add group"));
        assertEquals(1, groupTable.getGroupsCount());
    }

    private void initDb() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE product_group (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE product (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        count INT NOT NULL,
                        price REAL NOT NULL,
                        group_id INT,
                        FOREIGN KEY(group_id) REFERENCES product_group(id) ON DELETE RESTRICT
                    )
                    """);
        }
    }
}
