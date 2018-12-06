package me.egg82.headcount;

import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.exception.UnsupportedPinModeException;
import java.io.File;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import me.egg82.headcount.enums.SQLType;
import me.egg82.headcount.services.CachedConfigValues;
import me.egg82.headcount.services.Configuration;
import me.egg82.headcount.sql.MySQL;
import me.egg82.headcount.sql.SQLite;
import me.egg82.headcount.utils.ConfigurationFileUtil;
import ninja.egg82.events.Pi4JEvents;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeadCount {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final File currentDirectory;

    public HeadCount(File currentDirectory) {
        this.currentDirectory = currentDirectory;

        loadServices();
        loadSQL();

        start();
    }

    private void loadServices() {
        logger.info("Loading services..");
        ConfigurationFileUtil.reloadConfig(currentDirectory);
    }

    private void loadSQL() {
        logger.info("Loading SQL..");
        Configuration config;
        CachedConfigValues cachedConfig;

        try {
            config = ServiceLocator.get(Configuration.class);
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getSQLType() == SQLType.MySQL) {
            MySQL.createTables(cachedConfig.getSQL(), config.getNode("storage"));
        } else if (cachedConfig.getSQLType() == SQLType.SQLite) {
            SQLite.createTables(cachedConfig.getSQL(), config.getNode("storage"));
        }
    }

    private void start() {
        logger.info("Starting..");

        GpioController controller = GpioFactory.getInstance();

        /*GpioPinAnalogInput photosensorIn = controller.provisionAnalogInputPin(RaspiPin.GPIO_01);
        Pi4JEvents.subscribe(photosensorIn, GpioPinAnalogValueChangeEvent.class).handler(e -> {
            System.out.println("Output: " + e.getValue());
        });*/

        GpioPinDigitalInput photosensorIn = controller.provisionDigitalInputPin(RaspiPin.GPIO_01);
        Pi4JEvents.subscribe(photosensorIn, GpioPinDigitalStateChangeEvent.class).handler(e -> {
            System.out.println("Value 1: " + e.getState().getValue());
        });

        GpioPinDigitalInput photosensor2In = controller.provisionDigitalInputPin(RaspiPin.GPIO_26);
        Pi4JEvents.subscribe(photosensor2In, GpioPinDigitalStateChangeEvent.class).handler(e -> {
            System.out.println("Value 2: " + e.getState().getValue());
        });

        do {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ignored) {}
        } while (true);
    }
}
