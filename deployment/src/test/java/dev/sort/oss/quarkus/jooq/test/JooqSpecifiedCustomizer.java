package dev.sort.oss.quarkus.jooq.test;

import dev.sort.oss.quarkus.jooq.runtime.JooqCustomContext;
import org.jooq.Configuration;

public class JooqSpecifiedCustomizer implements JooqCustomContext {
    @Override
    public void apply(Configuration configuration) {
        configuration.data("custom-config", "by classname");
    }
}
