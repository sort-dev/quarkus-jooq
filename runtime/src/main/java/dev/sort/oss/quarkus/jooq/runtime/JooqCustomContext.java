package dev.sort.oss.quarkus.jooq.runtime;

import org.jboss.logging.Logger;
import org.jooq.Configuration;

public interface JooqCustomContext {
    Logger LOGGER = Logger.getLogger(JooqCustomContext.class);

    default void apply(Configuration configuration) {}
}
