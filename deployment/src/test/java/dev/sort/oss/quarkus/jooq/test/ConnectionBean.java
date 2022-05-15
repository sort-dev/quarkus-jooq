package dev.sort.oss.quarkus.jooq.test;

import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
public class ConnectionBean {
    private static final Logger LOGGER = Logger.getLogger(ConnectionBean.class);

    @Inject
    private DSLContext defaultUsingDefaultDataSource;

    @Inject
    @Named("dslwithdefault")
    private DSLContext namedUsingDefaultDataSource;

    @Inject
    @Named("dslwithdb1")
    private DSLContext namedUsingDb1DataSrouce;

    @Inject
    @Named("dslinjectconfig")
    private DSLContext namedUsingInjectedConfigurator;

    @Inject
    @Named("dslclassconfig")
    private DSLContext namedUsingSpecifiedConfigurator;

    public DSLContext getDefaultUsingDefaultDataSource() {
        return defaultUsingDefaultDataSource;
    }

    public void setDefaultUsingDefaultDataSource(DSLContext defaultUsingDefaultDataSource) {
        this.defaultUsingDefaultDataSource = defaultUsingDefaultDataSource;
    }

    public DSLContext getNamedUsingDefaultDataSource() {
        return namedUsingDefaultDataSource;
    }

    public void setNamedUsingDefaultDataSource(DSLContext namedUsingDefaultDataSource) {
        this.namedUsingDefaultDataSource = namedUsingDefaultDataSource;
    }

    public DSLContext getNamedUsingDb1DataSrouce() {
        return namedUsingDb1DataSrouce;
    }

    public void setNamedUsingDb1DataSrouce(DSLContext namedUsingDb1DataSrouce) {
        this.namedUsingDb1DataSrouce = namedUsingDb1DataSrouce;
    }

    public DSLContext getNamedUsingInjectedConfigurator() {
        return namedUsingInjectedConfigurator;
    }

    public void setNamedUsingInjectedConfigurator(DSLContext namedUsingInjectedConfigurator) {
        this.namedUsingInjectedConfigurator = namedUsingInjectedConfigurator;
    }

    public DSLContext getNamedUsingSpecifiedConfigurator() {
        return namedUsingSpecifiedConfigurator;
    }

    public void setNamedUsingSpecifiedConfigurator(DSLContext namedUsingSpecifiedConfigurator) {
        this.namedUsingSpecifiedConfigurator = namedUsingSpecifiedConfigurator;
    }
}