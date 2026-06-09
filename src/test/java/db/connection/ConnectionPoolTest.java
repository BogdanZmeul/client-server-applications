package db.connection;

import db.DatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionPoolTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldCreateSeveralConnections() {
        try (ConnectionPool connectionPool = new ConnectionPool(tempDir.resolve("pool1.db").toString(), 2)) {
            Connection first = connectionPool.getConnection();
            Connection second = connectionPool.getConnection();

            assertNotNull(first);
            assertNotNull(second);
            assertNotSame(first, second);

            connectionPool.returnConnection(first);
            connectionPool.returnConnection(second);
        }
    }

    @Test
    void shouldReturnConnectionBackToPool() {
        try (ConnectionPool connectionPool = new ConnectionPool(tempDir.resolve("pool2.db").toString(), 1)) {
            Connection first = connectionPool.getConnection();
            connectionPool.returnConnection(first);

            Connection second = connectionPool.getConnection();

            assertSame(first, second);

            connectionPool.returnConnection(second);
        }
    }

    @Test
    void shouldCloseAllConnections() throws Exception {
        Connection first;
        Connection second;

        try (ConnectionPool connectionPool = new ConnectionPool(tempDir.resolve("pool-close.db").toString(), 2)) {
            first = connectionPool.getConnection();
            second = connectionPool.getConnection();

            connectionPool.returnConnection(first);
            connectionPool.returnConnection(second);
        }

        assertTrue(first.isClosed());
        assertTrue(second.isClosed());
    }

    @Test
    void shouldThrowErrorWhenPoolSizeIsWrong() {
        DatabaseException error = assertThrows(DatabaseException.class,
                () -> new ConnectionPool(tempDir.resolve("pool3.db").toString(), 0));

        assertEquals("Pool size cannot be less than 1", error.getMessage());
    }
}
