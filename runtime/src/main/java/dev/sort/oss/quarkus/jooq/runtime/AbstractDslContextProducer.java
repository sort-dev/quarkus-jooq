package dev.sort.oss.quarkus.jooq.runtime;

import io.agroal.api.AgroalDataSource;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import javax.inject.Qualifier;
import java.lang.annotation.*;
import java.util.Objects;

public abstract class AbstractDslContextProducer {
    private static final Logger log = Logger.getLogger(AbstractDslContextProducer.class);

    public DSLContext createDslContext(String sqlDialect, AgroalDataSource dataSource, String customConfigurationClassName) {
        Objects.requireNonNull(sqlDialect, "sqlDialect");
        Objects.requireNonNull(dataSource, "dataSource");

        try {
            JooqCustomContext contextCustomizer = loadCustomContext(customConfigurationClassName);
            return createDslContext(sqlDialect, dataSource, contextCustomizer);
        } catch (Exception e) {
            log.error(customConfigurationClassName, e);
            throw new RuntimeException(e);
        }
    }

    public DSLContext createDslContext(String sqlDialect, AgroalDataSource dataSource, JooqCustomContext contextCustomizer) {
        Objects.requireNonNull(sqlDialect, "sqlDialect");
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(contextCustomizer, "contextCustomizer");
        return DslContextFactory.create(sqlDialect, dataSource, contextCustomizer);
    }

    private JooqCustomContext loadCustomContext(String customClassName) throws Exception {
        if (customClassName == null || customClassName.isEmpty()) {
            return new JooqCustomContext() {};
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = JooqCustomContext.class.getClassLoader();
        Class<?> clazz = cl.loadClass(customClassName);
        return (JooqCustomContext) clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * CDI: Ambiguous dependencies
     */
    @Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier
    public @interface DslContextQualifier {
        String value();
    }
}
