package me.egg82.headcount.events;

import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import me.egg82.headcount.enums.SQLType;
import me.egg82.headcount.services.CachedConfigValues;
import me.egg82.headcount.services.Configuration;
import me.egg82.headcount.sql.MySQL;
import me.egg82.headcount.sql.SQLite;
import me.egg82.headcount.utils.EventUtil;
import ninja.egg82.events.Pi4JAnalogEventSubscriber;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sensor2Event implements BiConsumer<Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent>, GpioPinAnalogValueChangeEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void accept(Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent> handler, GpioPinAnalogValueChangeEvent event) {
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
            logger.debug("Sensor 2 tripped");
        }

        logger.info("Adding one to count..");

        if (cachedConfig.getSQLType() == SQLType.MySQL) {
            MySQL.add(cachedConfig.getSQL(), config.getNode("storage"), 1);
        } else if (cachedConfig.getSQLType() == SQLType.SQLite) {
            SQLite.add(cachedConfig.getSQL(), config.getNode("storage"), 1);
        }

        // The way the listeners are iterated prevents us from cancelling while in our current thread
        // ConcModExceptions, ahoy!
        // So, instead, we're going to submit the cancellation in a new task and hope for the best
        // Because the Pi4J library is funny that way.
        ForkJoinPool.commonPool().execute(() -> {
            handler.cancel();
            EventUtil.subscribeSensor1(cachedConfig, inputs[cachedConfig.getSensor1Pin()]);
        });
    }
}
