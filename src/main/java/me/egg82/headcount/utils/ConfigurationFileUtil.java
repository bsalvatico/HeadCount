package me.egg82.headcount.utils;

import com.zaxxer.hikari.HikariConfig;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.egg82.headcount.enums.SQLType;
import me.egg82.headcount.services.CachedConfigValues;
import me.egg82.headcount.services.Configuration;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.sql.SQL;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;

public class ConfigurationFileUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationFileUtil.class);

    private ConfigurationFileUtil() {}

    public static void reloadConfig(File currentDirectory) {
        Configuration config;
        try {
            config = getConfig("config.yml", new File(currentDirectory, "config.yml"));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        boolean debug = config.getNode("debug").getBoolean(false);

        if (debug) {
            logger.debug("Debug enabled");
        }

        String sensor2Time = config.getNode("gpio", "sensor2", "time").getString("3seconds");
        Optional<Long> sensor2TimeLong = TimeUtil.getTime(sensor2Time);
        Optional<TimeUnit> sensor2TimeUnit = TimeUtil.getUnit(sensor2Time);
        if (!sensor2TimeLong.isPresent()) {
            logger.warn("gpio.sensor2.time is not a valid time pattern. Using default value.");
            sensor2TimeLong = Optional.of(3L);
            sensor2TimeUnit = Optional.of(TimeUnit.SECONDS);
        }
        if (!sensor2TimeUnit.isPresent()) {
            logger.warn("gpio.sensor2.time is not a valid time pattern. Using default value.");
            sensor2TimeLong = Optional.of(3L);
            sensor2TimeUnit = Optional.of(TimeUnit.SECONDS);
        }

        try {
            destroyServices(ServiceLocator.getOptional(CachedConfigValues.class));
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
        }

        CachedConfigValues cachedValues = CachedConfigValues.builder()
                .debug(debug)
                .sql(getSQL(currentDirectory, config.getNode("storage")))
                .sqlType(config.getNode("storage", "method").getString("sqlite"))
                .setSensor1Pin(getPin(config.getNode("gpio", "sensor1", "pin").getString("a0")))
                .setSensor1Value(clamp(0.0d, 1.0d, config.getNode("gpio", "sensor1", "value").getDouble(0.4d)))
                .setSensor2Pin(getPin(config.getNode("gpio", "sensor2", "pin").getString("a1")))
                .setSensor2Value(clamp(0.0d, 1.0d, config.getNode("gpio", "sensor2", "value").getDouble(0.4d)))
                .setSensor2Time(sensor2TimeLong.get(), sensor2TimeUnit.get())
                .build();

        if (debug) {
            logger.debug("Sensor 1 pin: " + config.getNode("gpio", "sensor1", "pin").getString());
            logger.debug("Sensor 1 trigger: " + cachedValues.getSensor1Value());
            logger.debug("Sensor 2 pin: " + config.getNode("gpio", "sensor2", "pin").getString());
            logger.debug("Sensor 2 trigger: " + cachedValues.getSensor2Value());
        }

        ServiceLocator.register(config);
        ServiceLocator.register(cachedValues);

        if (debug) {
            logger.debug("SQL type: " + cachedValues.getSQLType().name());
        }
    }

    public static Configuration getConfig(String resourcePath, File fileOnDisk) throws IOException {
        File parentDir = fileOnDisk.getParentFile();
        if (parentDir.exists() && !parentDir.isDirectory()) {
            Files.delete(parentDir.toPath());
        }
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }
        if (fileOnDisk.exists() && fileOnDisk.isDirectory()) {
            Files.delete(fileOnDisk.toPath());
        }

        if (!fileOnDisk.exists()) {
            try (InputStreamReader reader = new InputStreamReader(getResource(resourcePath));
                 BufferedReader in = new BufferedReader(reader);
                 FileWriter writer = new FileWriter(fileOnDisk);
                 BufferedWriter out = new BufferedWriter(writer)) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line + System.lineSeparator());
                }
            }
        }

        ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader.builder().setFlowStyle(DumperOptions.FlowStyle.BLOCK).setIndent(2).setFile(fileOnDisk).build();
        ConfigurationNode root = loader.load(ConfigurationOptions.defaults().setHeader("Comments are gone because update :(. Click here for new config + comments: https://www.spigotmc.org/resources/anti-vpn.58291/"));
        Configuration config = new Configuration(root);
        ConfigurationVersionUtil.conformVersion(loader, config, fileOnDisk);

        return config;
    }

    private static void destroyServices(Optional<CachedConfigValues> cachedConfigValues) {
        if (!cachedConfigValues.isPresent()) {
            return;
        }

        cachedConfigValues.get().getSQL().close();
    }

    private static SQL getSQL(File currentDirectory, ConfigurationNode storageConfigNode) {
        SQLType type = SQLType.getByName(storageConfigNode.getNode("method").getString("sqlite"));
        if (type == SQLType.UNKNOWN) {
            logger.warn("storage.method is an unknown value. Using default value.");
            type = SQLType.SQLite;
        }

        HikariConfig hikariConfig = new HikariConfig();
        if (type == SQLType.MySQL) {
            hikariConfig.setJdbcUrl("jdbc:mysql://" + storageConfigNode.getNode("data", "address").getString("127.0.0.1:3306") + "/" + storageConfigNode.getNode("data", "database").getString("avpn"));
            hikariConfig.setConnectionTestQuery("SELECT 1;");
        } else if (type == SQLType.SQLite) {
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + new File(currentDirectory, storageConfigNode.getNode("data", "database").getString("avpn") + ".db").getAbsolutePath());
            hikariConfig.setConnectionTestQuery("SELECT 1;");
        }
        hikariConfig.setUsername(storageConfigNode.getNode("data", "username").getString(""));
        hikariConfig.setPassword(storageConfigNode.getNode("data", "password").getString(""));
        hikariConfig.setMaximumPoolSize(storageConfigNode.getNode("settings", "max-pool-size").getInt(2));
        hikariConfig.setMinimumIdle(storageConfigNode.getNode("settings", "min-idle").getInt(2));
        hikariConfig.setMaxLifetime(storageConfigNode.getNode("settings", "max-lifetime").getLong(1800000L));
        hikariConfig.setConnectionTimeout(storageConfigNode.getNode("settings", "timeout").getLong(5000L));
        hikariConfig.addDataSourceProperty("useUnicode", String.valueOf(storageConfigNode.getNode("settings", "properties", "unicode").getBoolean(true)));
        hikariConfig.addDataSourceProperty("characterEncoding", storageConfigNode.getNode("settings", "properties", "encoding").getString("utf8"));
        hikariConfig.setAutoCommit(true);

        return new SQL(hikariConfig);
    }

    private static InputStream getResource(String filename) {
        try {
            URL url = ConfigurationFileUtil.class.getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    private static double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }

    private static int getPin(String name) {
        if (name == null) {
            return -1;
        }

        if (name.equalsIgnoreCase("a0") || name.equals("0")) {
            return 0;
        } else if (name.equalsIgnoreCase("a1") || name.equals("1")) {
            return 1;
        } else if (name.equalsIgnoreCase("a2") || name.equals("2")) {
            return 1;
        } else if (name.equalsIgnoreCase("a3") || name.equals("3")) {
            return 1;
        }

        return -1;
    }
}
