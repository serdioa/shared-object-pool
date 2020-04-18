package de.serdioa.common.pool.test;

import java.util.Properties;

import lombok.Getter;
import lombok.Setter;


/**
 * Configuration for tests.
 */
public class Configuration {

    public static final String PROP_POOL_TYPE = "poolType";
    public static final String PROP_OBJECT_TYPE = "objectType";
    public static final String PROP_THREADS = "threads";
    public static final String PROP_OBJECTS = "objects";
    public static final String PROP_RUN_MILLIS = "runMillis";

    @Getter
    @Setter
    private PoolType poolType;

    @Getter
    @Setter
    private ObjectType objectType;

    @Getter
    @Setter
    private int threads;

    @Getter
    @Setter
    private int objects;

    @Getter
    @Setter
    private long runMillis;


    public static Configuration fromProperties(Properties properties) {
        PoolType poolType = getEnumProperty(properties, PROP_POOL_TYPE, PoolType.class, PoolType.CONCURRENT);
        ObjectType objectType =
                getEnumProperty(properties, PROP_OBJECT_TYPE, ObjectType.class, ObjectType.REFLECTION_LOCKING);
        int threads = getIntProperty(properties, PROP_THREADS, 4);
        int objects = getIntProperty(properties, PROP_OBJECTS, 4);
        long runMillis = getLongProperty(properties, PROP_RUN_MILLIS, 10_000L);

        Configuration config = new Configuration();
        config.setPoolType(poolType);
        config.setObjectType(objectType);
        config.setThreads(threads);
        config.setObjects(objects);
        config.setRunMillis(runMillis);

        return config;
    }


    private static int getIntProperty(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(value);
        }
    }


    private static long getLongProperty(Properties properties, String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        } else {
            return Long.parseLong(value);
        }
    }


    private static <E extends Enum<E>> E getEnumProperty(Properties properties, String key, Class<E> type, E defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        } else {
            return Enum.valueOf(type, value);
        }
    }
}
