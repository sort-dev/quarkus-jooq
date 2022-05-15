package dev.sort.oss.quarkus.jooq.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.Map;

@ConfigRoot(name = "jooq", phase = ConfigPhase.BUILD_TIME)
public class JooqConfig {

    /**
     * The default config.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public JooqItemConfig defaultConfig;

    /**
     * Additional configs.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, JooqItemConfig> namedConfig;

}
