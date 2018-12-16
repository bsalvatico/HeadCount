package me.egg82.headcount.events;

import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import me.egg82.headcount.services.CachedConfigValues;
import me.egg82.headcount.utils.EventUtil;
import ninja.egg82.events.Pi4JAnalogEventSubscriber;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sensor1Event implements BiConsumer<Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent>, GpioPinAnalogValueChangeEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void accept(Pi4JAnalogEventSubscriber<GpioPinAnalogValueChangeEvent> handler, GpioPinAnalogValueChangeEvent event) {
        CachedConfigValues cachedConfig;
        GpioPinAnalogInput[] inputs;

        try {
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            inputs = ServiceLocator.get(GpioPinAnalogInput[].class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getDebug()) {
            logger.debug("Sensor 1 tripped");
        }

        // The way the listeners are iterated prevents us from cancelling while in our current thread
        // ConcModExceptions, ahoy!
        // So, instead, we're going to submit the cancellation in a new task and hope for the best
        // Because the Pi4J library is funny that way.
        ForkJoinPool.commonPool().execute(() -> {
            handler.cancel();
            EventUtil.subscribeSensor2(cachedConfig, inputs[cachedConfig.getSensor2Pin()]);
        });
    }
}
