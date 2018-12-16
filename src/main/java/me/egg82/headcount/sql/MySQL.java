package me.egg82.headcount.sql;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;
import ninja.egg82.core.SQLQueryResult;
import ninja.egg82.sql.SQL;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQL {
    private static final Logger logger = LoggerFactory.getLogger(MySQL.class);

    private MySQL() {}

    public static CompletableFuture<Void> createTables(SQL sql, ConfigurationNode storageConfigNode) {
        String databaseName = storageConfigNode.getNode("data", "database").getString();
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "headcount_";

        return CompletableFuture.runAsync(() -> {
            try {
                SQLQueryResult query = sql.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='" + tablePrefix.substring(0, tablePrefix.length() - 1) + "';", databaseName);
                if (query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0) {
                    return;
                }

                sql.execute("CREATE TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ("
                        + "`count` BIGINT NOT NULL,"
                        + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ");");
                sql.execute("ALTER TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ADD UNIQUE (`created`);");
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    public static CompletableFuture<Boolean> add(SQL sql, ConfigurationNode storageConfigNode, int count) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "headcount_";

        return CompletableFuture.supplyAsync(() -> {
            try {
                sql.execute("INSERT INTO `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` (`count`) VALUES(?) ON DUPLICATE KEY UPDATE `count`=`count`+?;", count, count);
                return Boolean.TRUE;
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return Boolean.FALSE;
        });
    }

    public static CompletableFuture<Long> getCurrentTime(SQL sql) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                SQLQueryResult query = sql.query("SELECT CURRENT_TIMESTAMP();");

                for (Object[] o : query.getData()) {
                    return ((Timestamp) o[0]).getTime() + (System.currentTimeMillis() - start);
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return -1L;
        });
    }
}
