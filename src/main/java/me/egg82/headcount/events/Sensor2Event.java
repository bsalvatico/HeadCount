package me.egg82.headcount.events;

import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import me.egg82.headcount.enums.SQLType;
import me.egg82.headcount.services.CachedConfigValues;
import me.egg82.headcount.services.Configuration;
import me.egg82.headcount.sql.MySQL;
import me.egg82.headcount.sql.SQLite;
import ninja.egg82.events.Pi4JAnalogEventSubscriber;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sensor2Event implements BiConsumer<Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent>, GpioPinAnalogValueChangeEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean sensorState;

    public Sensor2Event(AtomicBoolean sensorState) {
        this.sensorState = sensorState;
    }

    public void accept(Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent> handler, GpioPinAnalogValueChangeEvent event) {
        Configuration config;
        CachedConfigValues cachedConfig;

        try {
            config = ServiceLocator.get(Configuration.class);
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getDebug()) {
            logger.debug("Sensor 2 tripped");
        }

        logger.info("Adding one to count..");

        if (cachedConfig.getSQLType() == SQLType.MySQL) {
            MySQL.add(cachedConfig.getSQL(), config.getNode("storage"), 1);
        } else if (cachedConfig.getSQLType() == SQLType.SQLite) {
            SQLite.add(cachedConfig.getSQL(), config.getNode("storage"), 1);
        }

        sensorState.set(false);
    }
}
