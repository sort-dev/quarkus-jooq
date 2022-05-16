package dev.sort.oss.quarkus.jooq.test;

import dev.sort.oss.quarkus.jooq.runtime.JooqCustomContext;
import org.jboss.logging.Logger;
import org.jooq.Configuration;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class JooqTestCustomizerFactory {

    private static final Logger LOGGER = Logger.getLogger(JooqTestCustomizerFactory.class);

    @PostConstruct
    void onPostConstruct() {
        LOGGER.debug("JooqNamedCustomizer: onPostConstruct");
    }

    @ApplicationScoped
    @Produces
    @Named("customJooqConfigurator")
    public JooqCustomContext create() {
        LOGGER.debug("JooqNamedCustomizer: create");
        return new JooqCustomContext() {
            @Override
            public void apply(Configuration configuration) {
                configuration.data("custom-config", "by factory");
            }
        };
    }
}
