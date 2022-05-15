package dev.sort.oss.quarkus.jooq.test;

import io.quarkus.test.QuarkusUnitTest;
import org.h2.tools.Server;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class JooqTests {
    private static final Logger LOGGER = Logger.getLogger(JooqTests.class);

    static private Server server;

    @BeforeAll
    static public void startDatabase() {
        LOGGER.debug("Start H2 server..." + server);
        try {
            server = Server.createTcpServer(new String[]{"-trace", "-tcp", "-tcpAllowOthers", "-tcpPort", "19092", "-ifNotExists"})
                    .start();
            LOGGER.debug("H2 server URL = " + server.getURL());
            Thread.sleep(1000 * 1); // waiting for database to be ready
        } catch (Exception e) {
            LOGGER.error("Start H2 server failed", e);
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static public void stopDatabase() {
        LOGGER.debug("Stop H2 server..." + server);
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @AfterEach
    public void clean() {
        if (db != null) {
            dropTableIfExists(db.getDefaultUsingDefaultDataSource());
            dropTableIfExists(db.getNamedUsingDefaultDataSource());
            dropTableIfExists(db.getNamedUsingDb1DataSrouce());
            dropTableIfExists(db.getNamedUsingSpecifiedConfigurator());
            dropTableIfExists(db.getNamedUsingInjectedConfigurator());
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties", "application.properties")
                    .addClasses(
                            ConnectionBean.class,
                            JooqSpecifiedCustomizer.class,
                            JooqTestCustomizerFactory.class
                    )
            );

    @Inject
    ConnectionBean db;

    @Test
    void testDefaultContext() {
        allTests(db.getDefaultUsingDefaultDataSource(), null);
    }

    @Test
    void testNamedContextUsingDefaultDataSource() {
        allTests(db.getNamedUsingDefaultDataSource(), null);
    }

    @Test
    void testNamedContextUsingDb1DataSrouce() {
        allTests(db.getNamedUsingDb1DataSrouce(), null);
    }

    @Test
    void testNamedContextUsingInjectedConfigurator() {
        basicCheck(db.getNamedUsingInjectedConfigurator(), "by factory");
    }

    @Test
    void testNamedContextUsingSpecifiedConfigurator() {
        basicCheck(db.getNamedUsingSpecifiedConfigurator(), "by classname");
    }

    @Test
    void testDataSourcesAreCorrect() {
        // first write to one datasource
        createTable(db.getDefaultUsingDefaultDataSource());
        insertIntoTable(db.getDefaultUsingDefaultDataSource(), 1);
        checkTableValue(db.getDefaultUsingDefaultDataSource(), 1, 1);
        checkTableSize(db.getDefaultUsingDefaultDataSource(), 1);
        // and we don't see that from the other
        try {
            checkTableSize(db.getNamedUsingDb1DataSrouce(), 0);
            Assertions.fail("The table shouldn't even exist");
        } catch (DataAccessException ex) {
            // as expected
        }

        // and we have clean inserts here
        createTable(db.getNamedUsingDb1DataSrouce());

        // old value from other datasource is not here
        checkTableValue(db.getNamedUsingDb1DataSrouce(), 1, 0);
        // no values are in the table yet
        checkTableSize(db.getNamedUsingDb1DataSrouce(), 0);

        // set our new value for this datasource and check it
        insertIntoTable(db.getNamedUsingDb1DataSrouce(), 10);
        checkTableValue(db.getNamedUsingDb1DataSrouce(), 10, 1);
        checkTableSize(db.getNamedUsingDb1DataSrouce(), 1);

        // and back to original to make sure nothing changed
        checkTableValue(db.getDefaultUsingDefaultDataSource(), 10, 0);
        checkTableValue(db.getDefaultUsingDefaultDataSource(), 1, 1);
        checkTableSize(db.getDefaultUsingDefaultDataSource(), 1);

        dropTable(db.getDefaultUsingDefaultDataSource());
        dropTable(db.getNamedUsingDb1DataSrouce());
    }

    void allTests(DSLContext context, String checkCustomConfigData) {
        basicCheck(context, checkCustomConfigData);
        createTable(context);
        insertIntoTable(context, 1);
        insertIntoTableTransactional(context, 2);
        checkTableValue(context, 1, 1);
        checkTableValue(context, 2, 1);
        checkTableSize(context, 2);
        dropTable(context);
    }

    void basicCheck(DSLContext context, String checkCustomConfigData) {
        if (checkCustomConfigData != null && !checkCustomConfigData.isBlank()) {
            Assertions.assertEquals(checkCustomConfigData, context.configuration().data("custom-config"));
        } else {
            Assertions.assertNull(context.configuration().data("custom-config"));
        }

        context.execute("SELECT 1");
    }

    void createTable(DSLContext context) {
        context.execute("CREATE TABLE foo (id INT NOT NULL, PRIMARY KEY (id))");
    }

    void insertIntoTable(DSLContext context, int value) {
        context.execute("INSERT INTO foo (id) VALUES (?)", value);
    }

    @Transactional
    void insertIntoTableTransactional(DSLContext context, int value) {
        context.execute("INSERT INTO foo (id) VALUES (?)", value);
    }

    void checkTableValue(DSLContext context, int value, int expectedCount) {
        int actualCount = context.resultQuery("SELECT id FROM foo WHERE id = ?", value).fetch().size();
        Assertions.assertEquals(expectedCount, actualCount);
    }

    void checkTableSize(DSLContext context, int expectedCount) {
        int actualCount = context.resultQuery("SELECT id FROM foo").fetch().size();
        Assertions.assertEquals(expectedCount, actualCount);
    }

    void dropTable(DSLContext context) {
        context.execute("DROP TABLE foo");
    }

    void dropTableIfExists(DSLContext context) {
        try {
            context.execute("DROP TABLE foo");
        } catch (Throwable ex) {
            // noop
        }
    }
}
