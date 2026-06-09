package db.connection;

import db.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool implements AutoCloseable {
    private final BlockingQueue<Connection> freeConnections;
    private final List<Connection> allConnections = new ArrayList<>();

    public ConnectionPool(String dbName, int poolSize) {
        if (poolSize < 1) {
            throw new DatabaseException("Pool size cannot be less than 1");
        }

        freeConnections = new ArrayBlockingQueue<>(poolSize);

        try {
            for (int i = 0; i < poolSize; i++) {
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
                try (java.sql.Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL;");
                    stmt.execute("PRAGMA synchronous=NORMAL;");
                    stmt.execute("PRAGMA busy_timeout=3000;");
                }
                freeConnections.add(connection);
                allConnections.add(connection);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Can't create SQLite connection pool", e);
        }
    }

    public Connection getConnection() {
        try {
            return freeConnections.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Can't get connection from pool", e);
        }
    }

    public void returnConnection(Connection connection) {
        if (connection != null) {
            freeConnections.add(connection);
        }
    }

    @Override
    public void close() {
        for (Connection connection : allConnections) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new DatabaseException("Can't close SQLite DB", e);
            }
        }
    }
}
