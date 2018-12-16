package me.egg82.headcount.utils;

import com.pi4j.gpio.extension.ads.ADS1115GpioProvider;
import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import java.util.concurrent.TimeUnit;
import me.egg82.headcount.events.Sensor1Event;
import me.egg82.headcount.events.Sensor2Event;
import me.egg82.headcount.services.CachedConfigValues;
import ninja.egg82.events.Pi4JEvents;

public class EventUtil {
    private EventUtil() {}

    public static void subscribeSensor1(CachedConfigValues cachedConfig, GpioPinAnalogInput input) {
        if (cachedConfig == null) {
            throw new IllegalArgumentException("cachedConfig cannot be null.");
        }
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null.");
        }

        Pi4JEvents.subscribe(input, GpioPinAnalogValueChangeEvent.class)
                .filter(e -> {
                    double value = e.getValue() / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE;
                    return value >= cachedConfig.getSensor1Value();
                })
                .handler((h, e) -> new Sensor1Event().accept(h, e));
    }

    public static void subscribeSensor2(CachedConfigValues cachedConfig, GpioPinAnalogInput input) {
        if (cachedConfig == null) {
            throw new IllegalArgumentException("cachedConfig cannot be null.");
        }
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null.");
        }

        Pi4JEvents.subscribe(input, GpioPinAnalogValueChangeEvent.class)
                .expireAfter(3L, TimeUnit.SECONDS)
                .filter(e -> {
                    double value = e.getValue() / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE;
                    return value >= cachedConfig.getSensor2Value();
                })
                .handler((h, e) -> new Sensor2Event().accept(h, e));
    }
}
