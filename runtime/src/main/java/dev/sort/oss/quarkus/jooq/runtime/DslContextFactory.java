package dev.sort.oss.quarkus.jooq.runtime;

import io.agroal.api.AgroalDataSource;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 *
 *
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
public class DslContextFactory {
    static {
        System.setProperty("org.jooq.no-logo", String.valueOf(true)); // -Dorg.jooq.no-logo=true
    }

    static public DSLContext create(String sqlDialect, AgroalDataSource ds, JooqCustomContext customContext) {
        final DSLContext context = DSL.using(ds, SQLDialect.valueOf(sqlDialect.toUpperCase()));
        customContext.apply(context.configuration());
        return context;
    }
}
