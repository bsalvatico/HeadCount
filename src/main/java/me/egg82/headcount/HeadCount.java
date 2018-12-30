package me.egg82.headcount;

import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import java.io.File;
import java.io.IOException;

import com.pi4j.gpio.extension.ads.ADS1115GpioProvider;
import com.pi4j.gpio.extension.ads.ADS1115Pin;
import com.pi4j.io.gpio.*;
import com.pi4j.io.i2c.I2CFactory;
import java.util.concurrent.atomic.AtomicLong;
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
        loadGPIO();

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

    private void loadGPIO() {
        logger.info("Loading GPIO..");

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
                controller.provisionAnalogInputPin(provider, ADS1115Pin.INPUT_A0),
                controller.provisionAnalogInputPin(provider, ADS1115Pin.INPUT_A1),
                controller.provisionAnalogInputPin(provider, ADS1115Pin.INPUT_A2),
                controller.provisionAnalogInputPin(provider, ADS1115Pin.INPUT_A3)
        };

        provider.setProgrammableGainAmplifier(ADS1115GpioProvider.ProgrammableGainAmplifierValue.PGA_2_048V, ADS1115Pin.ALL);

        provider.setEventThreshold(250.0d, ADS1115Pin.ALL);
        provider.setMonitorInterval(100);

        ServiceLocator.register(controller);
        ServiceLocator.register(provider);
        ServiceLocator.register(inputs);
    }

    private void start() {
        logger.info("Starting..");

        Configuration config;
        CachedConfigValues cachedConfig;
        GpioPinAnalogInput[] inputs;

        try {
            config = ServiceLocator.get(Configuration.class);
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            inputs = ServiceLocator.get(GpioPinAnalogInput[].class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getDebug()) {
            Pi4JEvents.subscribe(inputs[cachedConfig.getSensor1Pin()], GpioPinAnalogValueChangeEvent.class).handler(e -> logger.debug("Sensor 1: " + (e.getValue() / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE)));
            Pi4JEvents.subscribe(inputs[cachedConfig.getSensor2Pin()], GpioPinAnalogValueChangeEvent.class).handler(e -> logger.debug("Sensor 2: " + (e.getValue() / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE)));
        }

        AtomicLong sensor1TripTime = new AtomicLong(-1L);

        Pi4JEvents.subscribe(inputs[cachedConfig.getSensor1Pin()], GpioPinAnalogValueChangeEvent.class)
                .filter(e -> {
                    double value = e.getValue() / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE;
                    return value >= cachedConfig.getSensor1Value();
                })
                .handler(e -> {
                    if (cachedConfig.getDebug()) {
                        logger.debug("Sensor 1 tripped");
                    }

                    sensor1TripTime.set(System.currentTimeMillis());
                });

        Pi4JEvents.subscribe(inputs[cachedConfig.getSensor2Pin()], GpioPinAnalogValueChangeEvent.class)
                .filter(e -> System.currentTimeMillis() < Math.addExact(sensor1TripTime.get(), cachedConfig.getSensor2Time()))
                .filter(e -> {
                    double value = e.getValue() / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE;
                    return value >= cachedConfig.getSensor2Value();
                })
                .handler(e -> {
                    if (cachedConfig.getDebug()) {
                        logger.debug("Sensor 2 tripped");
                    }

                    logger.info("Adding one to count..");

                    if (cachedConfig.getSQLType() == SQLType.MySQL) {
                        MySQL.add(cachedConfig.getSQL(), config.getNode("storage"), 1);
                    } else if (cachedConfig.getSQLType() == SQLType.SQLite) {
                        SQLite.add(cachedConfig.getSQL(), config.getNode("storage"), 1);
                    }

                    sensor1TripTime.set(-1L);
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
