package dev.sort.oss.quarkus.jooq.deployment;

import dev.sort.oss.quarkus.jooq.runtime.*;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.*;
import io.quarkus.runtime.util.HashUtil;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.tools.LoggerListener;
import org.objectweb.asm.Opcodes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class JooqProcessor {
    private static final Logger log = Logger.getLogger(JooqProcessor.class);

    private static final String FEATURE = "jooq";

    private static final DotName DSL_CONTEXT_QUALIFIER =
            DotName.createSimple(AbstractDslContextProducer.DslContextQualifier.class.getName());

    private final String dslContextProducerClassName =
            AbstractDslContextProducer.class.getPackage().getName() + "." + "DslContextProducer";

    @Record(ExecutionTime.STATIC_INIT)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    protected void build(RecorderContext recorder,
                         JooqRecorder template,
                         BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                         BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
                         JooqConfig jooqConfig,
                         BuildProducer<GeneratedBeanBuildItem> generatedBean,
                         DataSourcesBuildTimeConfig dataSourceConfig,
                         List<JdbcDataSourceBuildItem> jdbcDataSourcesBuildItem) {
        if (isUnconfigured(jooqConfig)) {
            return;
        }

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, AbstractDslContextProducer.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, LoggerListener.class));

        if (!isPresentDialect(jooqConfig.defaultConfig)) {
            log.warn("No default sql-dialect been defined");
        }

        createDslContextProducerBean(
                generatedBean,
                unremovableBeans,
                jooqConfig,
                dataSourceConfig,
                jdbcDataSourcesBuildItem,
                AbstractDslContextProducer.class
        );
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureDataSource(JooqRecorder template,
                             BuildProducer<JooqInitializedBuildItem> jooqInitialized,
                             JooqConfig jooqConfig) {
        if (isUnconfigured(jooqConfig)) {
            return;
        }
        jooqInitialized.produce(new JooqInitializedBuildItem());
    }

    protected boolean isUnconfigured(JooqConfig jooqConfig) {
        if (!isPresentDialect(jooqConfig.defaultConfig) && jooqConfig.namedConfig.isEmpty()) {
            // No jOOQ has been configured so bail out
            log.debug("No jOOQ has been configured");
            return true;
        } else {
            return false;
        }
    }

    protected void createDslContextProducerBean(BuildProducer<GeneratedBeanBuildItem> generatedBean,
                                                BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
                                                JooqConfig jooqConfig,
                                                DataSourcesBuildTimeConfig dataSourceConfig,
                                                List<JdbcDataSourceBuildItem> jdbcDataSourcesBuildItem, Class<?> producerClass) {
        ClassOutput classOutput = (name, data) -> generatedBean.produce(new GeneratedBeanBuildItem(name, data));
        unremovableBeans.produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNameExclusion(dslContextProducerClassName)));

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput).className(dslContextProducerClassName)
                .superClass(producerClass).build();
        classCreator.addAnnotation(ApplicationScoped.class);

        JooqItemConfig defaultConfig = jooqConfig.defaultConfig;
        if (isPresentDialect(defaultConfig)) {
            Optional<JdbcDataSourceBuildItem> defaultJdbcDataSourceBuildItem = jdbcDataSourcesBuildItem.stream()
                    .filter(JdbcDataSourceBuildItem::isDefault)
                    .findFirst();

            if (!defaultJdbcDataSourceBuildItem.isPresent()) { //dataSourceConfig.defaultDataSource.dbKind
                log.warn("Default dataSource not found");
                System.err.println(">>> Default dataSource not found");
            }
            if (defaultConfig.datasource.isPresent()
                    && !DataSourceUtil.DEFAULT_DATASOURCE_NAME.equals(defaultConfig.datasource.get())) {
                log.warn("Skip default dataSource name: " + defaultConfig.datasource.get());
            }
            String dsVarName = "defaultDataSource";

            FieldCreator defaultDataSourceCreator = classCreator.getFieldCreator(dsVarName, AgroalDataSource.class)
                    .setModifiers(Opcodes.ACC_MODULE);

            defaultDataSourceCreator.addAnnotation(Default.class);
            defaultDataSourceCreator.addAnnotation(Inject.class);

            //
            String dialect = defaultConfig.dialect;
            MethodCreator defaultDslContextMethodCreator = classCreator.getMethodCreator("createDefaultDslContext",
                    DSLContext.class);

            defaultDslContextMethodCreator.addAnnotation(Singleton.class);
            defaultDslContextMethodCreator.addAnnotation(Produces.class);
            defaultDslContextMethodCreator.addAnnotation(Default.class);

            ResultHandle dialectRH = defaultDslContextMethodCreator.load(dialect);

            ResultHandle dataSourceRH = defaultDslContextMethodCreator.readInstanceField(
                    FieldDescriptor.of(classCreator.getClassName(), dsVarName, AgroalDataSource.class.getName()),
                    defaultDslContextMethodCreator.getThis());

            if (defaultConfig.configurationInject.isPresent()) {
                String configurationInjectName = defaultConfig.configurationInject.get();
                String injectVarName = "configuration_" + HashUtil.sha1(configurationInjectName);

                FieldCreator configurationCreator = classCreator.getFieldCreator(injectVarName, JooqCustomContext.class)
                        .setModifiers(Opcodes.ACC_MODULE);

                configurationCreator.addAnnotation(Inject.class);
                configurationCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                        new AnnotationValue[]{AnnotationValue.createStringValue("value", configurationInjectName)}));

                ResultHandle configurationRH = defaultDslContextMethodCreator.readInstanceField(
                        FieldDescriptor.of(classCreator.getClassName(), injectVarName, JooqCustomContext.class.getName()),
                        defaultDslContextMethodCreator.getThis());

                defaultDslContextMethodCreator.returnValue( //
                        defaultDslContextMethodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(producerClass, "createDslContext",
                                        DSLContext.class, String.class, AgroalDataSource.class,
                                        JooqCustomContext.class),
                                defaultDslContextMethodCreator.getThis(), dialectRH, dataSourceRH, configurationRH));
            } else {
                ResultHandle configurationRH = defaultConfig.configuration.isPresent()
                        ? defaultDslContextMethodCreator.load(defaultConfig.configuration.get())
                        : defaultDslContextMethodCreator.loadNull();

                defaultConfig.configuration
                        .ifPresent(s -> unremovableBeans.produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNameExclusion(s))));

                defaultDslContextMethodCreator.returnValue(defaultDslContextMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(producerClass, "createDslContext", DSLContext.class,
                                String.class, AgroalDataSource.class, String.class),
                        defaultDslContextMethodCreator.getThis(), dialectRH, dataSourceRH, configurationRH));
            }
        }

        for (Map.Entry<String, JooqItemConfig> configEntry : jooqConfig.namedConfig.entrySet()) {
            String named = configEntry.getKey();
            JooqItemConfig namedConfig = configEntry.getValue();
            if (!isPresentDialect(namedConfig)) {
                log.warnv("!isPresentDialect(namedConfig), named: {0}, namedConfig: {1}", named, namedConfig);
                continue;
            }

            if (!namedConfig.datasource.isPresent()) {
                log.warnv("(!config.datasource.isPresent()), named: {0}, namedConfig: {1}", named, namedConfig);
                continue;
            }

            String dataSourceName = namedConfig.datasource.get();
            Optional<JdbcDataSourceBuildItem> namedJdbcDataSourceBuildItem = jdbcDataSourcesBuildItem.stream()
                    .filter(j -> j.getName().equals(dataSourceName))
                    .findFirst();

            if (!namedJdbcDataSourceBuildItem.isPresent()) {
                log.warnv("Named: '{0}' dataSource not found", dataSourceName);
                System.err.println(">>> Named: '" + dataSourceName + "' dataSource not found");
            }

            String suffix = HashUtil.sha1(named);
            String dsVarName = "dataSource_" + suffix;

            FieldCreator dataSourceCreator = classCreator.getFieldCreator(dsVarName, AgroalDataSource.class)
                    .setModifiers(Opcodes.ACC_MODULE);
            dataSourceCreator.addAnnotation(Inject.class);
            if (namedJdbcDataSourceBuildItem.get().isDefault()) {
                dataSourceCreator.addAnnotation(Default.class);
            } else {
                dataSourceCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                        new AnnotationValue[]{AnnotationValue.createStringValue("value", dataSourceName)}));
            }

            MethodCreator namedDslContextMethodCreator = classCreator.getMethodCreator("createNamedDslContext_" + suffix,
                    DSLContext.class.getName());

            namedDslContextMethodCreator.addAnnotation(ApplicationScoped.class);
            namedDslContextMethodCreator.addAnnotation(Produces.class);
            namedDslContextMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                    new AnnotationValue[]{AnnotationValue.createStringValue("value", named)}));
            namedDslContextMethodCreator.addAnnotation(AnnotationInstance.create(DSL_CONTEXT_QUALIFIER, null,
                    new AnnotationValue[]{AnnotationValue.createStringValue("value", named)}));

            ResultHandle dialectRH = namedDslContextMethodCreator.load(namedConfig.dialect);

            ResultHandle dataSourceRH = namedDslContextMethodCreator.readInstanceField(
                    FieldDescriptor.of(classCreator.getClassName(), dsVarName, AgroalDataSource.class.getName()),
                    namedDslContextMethodCreator.getThis());

            if (namedConfig.configurationInject.isPresent()) {
                String configurationInjectName = namedConfig.configurationInject.get();
                String injectVarName = "configurationInjectName" + HashUtil.sha1(configurationInjectName);

                FieldCreator configurationCreator = classCreator.getFieldCreator(injectVarName, JooqCustomContext.class)
                        .setModifiers(Opcodes.ACC_MODULE);

                configurationCreator.addAnnotation(Inject.class);
                configurationCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                        new AnnotationValue[]{AnnotationValue.createStringValue("value", configurationInjectName)}));

                ResultHandle configurationRH = namedDslContextMethodCreator.readInstanceField(FieldDescriptor
                                .of(classCreator.getClassName(), injectVarName, JooqCustomContext.class.getName()),
                        namedDslContextMethodCreator.getThis());

                namedDslContextMethodCreator.returnValue(namedDslContextMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(producerClass, "createDslContext",
                                DSLContext.class, String.class, AgroalDataSource.class, JooqCustomContext.class),
                        namedDslContextMethodCreator.getThis(), dialectRH, dataSourceRH, configurationRH));
            } else {
                ResultHandle configurationRH = namedConfig.configuration.isPresent()
                        ? namedDslContextMethodCreator.load(namedConfig.configuration.get())
                        : namedDslContextMethodCreator.loadNull();

                namedConfig.configuration
                        .ifPresent(s -> unremovableBeans.produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNameExclusion(s))));

                namedDslContextMethodCreator.returnValue(namedDslContextMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(producerClass, "createDslContext", DSLContext.class,
                                String.class, AgroalDataSource.class, String.class),
                        namedDslContextMethodCreator.getThis(), dialectRH, dataSourceRH, configurationRH));
            }
        }

        classCreator.close();
    }

    protected boolean isPresentDialect(JooqItemConfig itemConfig) {
        return itemConfig.dialect != null && !itemConfig.dialect.isEmpty();
    }
}
