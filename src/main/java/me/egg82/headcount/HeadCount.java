package me.egg82.headcount;

import java.io.File;
import java.io.IOException;

import com.pi4j.gpio.extension.ads.ADS1115GpioProvider;
import com.pi4j.gpio.extension.ads.ADS1115Pin;
import com.pi4j.gpio.extension.ads.ADS1x15GpioProvider;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.i2c.I2CFactory;
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

        ADS1115GpioProvider provider = null;

        try {
            outer:
            for (int address = 72; address <= 75; address++) {
                for (int bus = 0; bus <= 17; bus++) {
                    try {
                        provider = new ADS1115GpioProvider(bus, address);
                        logger.info("Found enabled bus " + bus + " at address " + address);
                        break outer;
                    } catch (I2CFactory.UnsupportedBusNumberException ignored) {}
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (provider == null) {
            logger.error("No enabled bus found. Is the pinout correct, and I2C enabled in raspi-config?");
            return;
        }

        GpioPinAnalogInput[] inputs = new GpioPinAnalogInput[] {
                controller.provisionAnalogInputPin(provider, ADS1115Pin.INPUT_A0)
        };

        provider.setProgrammableGainAmplifier(ADS1x15GpioProvider.ProgrammableGainAmplifierValue.PGA_4_096V, ADS1115Pin.ALL);
        provider.setEventThreshold(500.0d, ADS1115Pin.ALL);
        provider.setMonitorInterval(100);

        Pi4JEvents.subscribe(inputs[0], GpioPinAnalogValueChangeEvent.class).handler(e -> {
            double value = e.getValue() / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE;
            System.out.println("Output: " + value);
        });

        do {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } while (true);
    }
}
