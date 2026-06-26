package com.hotusa;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Singleton que inicializa y gestiona el JedisPool a partir de redis.properties.
 * Llamar a RedisPoolManager.getInstance().getPool() para obtener el pool.
 */
public class RedisPoolManager {

    private static final String PROPERTIES_FILE = "/redis.properties";

    private static volatile RedisPoolManager instance;
    private final JedisPool pool;

    private RedisPoolManager() {
        Properties props = loadProperties();

        String host     = props.getProperty("redis.host", "localhost");
        int    port     = Integer.parseInt(props.getProperty("redis.port", "6379"));
        String password = props.getProperty("redis.password", "").trim();
        int    database = Integer.parseInt(props.getProperty("redis.database", "1"));
        int    connMs   = Integer.parseInt(props.getProperty("redis.pool.connectTimeoutMs", "2000"));
        int    sockMs   = Integer.parseInt(props.getProperty("redis.pool.socketTimeoutMs", "2000"));

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(Integer.parseInt(props.getProperty("redis.pool.maxTotal", "20")));
        config.setMaxIdle(Integer.parseInt(props.getProperty("redis.pool.maxIdle", "5")));
        config.setMinIdle(Integer.parseInt(props.getProperty("redis.pool.minIdle", "1")));
        config.setTestOnBorrow(Boolean.parseBoolean(props.getProperty("redis.pool.testOnBorrow", "true")));
        config.setMaxWait(Duration.ofMillis(connMs));

        if (password.isEmpty()) {
            pool = new JedisPool(config, host, port, connMs, sockMs, null, database, null);
        } else {
            pool = new JedisPool(config, host, port, connMs, sockMs, password, database, null);
        }

        System.out.println("JedisPool inicializado → " + host + ":" + port + " db=" + database);
    }

    public static RedisPoolManager getInstance() {
        if (instance == null) {
            synchronized (RedisPoolManager.class) {
                if (instance == null) {
                    instance = new RedisPoolManager();
                }
            }
        }
        return instance;
    }

    public JedisPool getPool() {
        return pool;
    }

    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    // ------------------------------------------------------------------ //

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new IllegalStateException("No se encontró " + PROPERTIES_FILE + " en el classpath");
            }
            props.load(is);
        } catch (IOException e) {
            System.out.println("Error cargando redis.properties");
            throw new RuntimeException("No se pudo cargar redis.properties", e);
        }
        return props;
    }
}
