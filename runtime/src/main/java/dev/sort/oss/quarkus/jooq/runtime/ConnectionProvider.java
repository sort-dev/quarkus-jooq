package dev.sort.oss.quarkus.jooq.runtime;

import io.agroal.api.AgroalDataSource;
import org.jboss.logging.Logger;

import javax.inject.Provider;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionProvider implements Provider<Connection> {
    private static final Logger LOGGER = Logger.getLogger(ConnectionProvider.class);

    final private AgroalDataSource dataSource;

    public ConnectionProvider(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection get() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            LOGGER.error("Error occurred creating connection.", e);
            throw new RuntimeException(e);
        }
    }
}
