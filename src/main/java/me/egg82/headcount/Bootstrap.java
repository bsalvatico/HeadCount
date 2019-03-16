package me.egg82.headcount;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import me.egg82.headcount.utils.JarUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {
    public static void main(String[] args) { new Bootstrap(); }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Bootstrap() {
        logger.info("Starting..");

        try {
            loadJars(new File(getDirectory(), "external"), (URLClassLoader) getClass().getClassLoader());
        }  catch (ClassCastException | IOException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        new HeadCount(getDirectory());
    }

    private void loadJars(File jarsFolder, URLClassLoader classLoader) throws IOException, IllegalAccessException, InvocationTargetException {
        if (jarsFolder.exists() && !jarsFolder.isDirectory()) {
            Files.delete(jarsFolder.toPath());
        }
        if (!jarsFolder.exists()) {
            if (!jarsFolder.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }

        logger.info("Loading dep Guava");
        JarUtil.loadJar("https://search.maven.org/remotecontent?filepath=com/google/guava/guava/27.1-jre/guava-27.1-jre.jar",
                new File(jarsFolder, "guava-27.1-jre.jar"),
                classLoader);

        logger.info("Loading dep Caffeine");
        JarUtil.loadJar("https://search.maven.org/remotecontent?filepath=com/github/ben-manes/caffeine/caffeine/2.7.0/caffeine-2.7.0.jar",
                new File(jarsFolder, "caffeine-2.7.0.jar"),
                classLoader);

        logger.info("Loading dep HikariCP");
        JarUtil.loadJar("https://search.maven.org/remotecontent?filepath=com/zaxxer/HikariCP/3.3.1/HikariCP-3.3.1.jar",
                new File(jarsFolder, "HikariCP-3.3.1.jar"),
                classLoader);

        logger.info("Loading dep Javassist");
        JarUtil.loadJar("https://search.maven.org/remotecontent?filepath=org/javassist/javassist/3.24.1-GA/javassist-3.24.1-GA.jar",
                new File(jarsFolder, "javassist-3.24.1-GA.jar"),
                classLoader);

        logger.info("Loading dep SQLite");
        JarUtil.loadJar("https://search.maven.org/remotecontent?filepath=org/xerial/sqlite-jdbc/3.27.2/sqlite-jdbc-3.27.2.jar",
                new File(jarsFolder, "sqlite-jdbc-3.27.2.jar"),
                classLoader);

        try {
            DriverManager.registerDriver((Driver) Class.forName("org.sqlite.JDBC", true, classLoader).newInstance());
        } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        logger.info("Loading dep MySQL");
        JarUtil.loadJar("https://search.maven.org/remotecontent?filepath=mysql/mysql-connector-java/8.0.15/mysql-connector-java-8.0.15.jar",
                new File(jarsFolder, "mysql-connector-java-8.0.15.jar"),
                classLoader);

        try {
            DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver", true, classLoader).newInstance());
        } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private File getDirectory() {
        try {
            return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return null;
    }
}
