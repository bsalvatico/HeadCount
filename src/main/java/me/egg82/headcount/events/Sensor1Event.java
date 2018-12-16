package me.egg82.headcount.events;

import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import me.egg82.headcount.services.CachedConfigValues;
import ninja.egg82.events.Pi4JAnalogEventSubscriber;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sensor1Event implements BiConsumer<Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent>, GpioPinAnalogValueChangeEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean sensorState;
    private final AtomicLong currentTime;

    public Sensor1Event(AtomicBoolean sensorState, AtomicLong currentTime) {
        this.sensorState = sensorState;
        this.currentTime = currentTime;
    }

    public void accept(Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent> handler, GpioPinAnalogValueChangeEvent event) {
        CachedConfigValues cachedConfig;

        try {
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getDebug()) {
            logger.debug("Sensor 1 tripped");
        }

        currentTime.set(System.currentTimeMillis());
        sensorState.set(true);
    }
}
