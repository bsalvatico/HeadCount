package me.egg82.headcount.services;

import java.util.concurrent.TimeUnit;
import me.egg82.headcount.enums.SQLType;
import ninja.egg82.sql.SQL;
import ninja.egg82.tuples.longs.LongObjectPair;

public class CachedConfigValues {
    private CachedConfigValues() {}

    private boolean debug = false;
    public boolean getDebug() { return debug; }

    private SQL sql = null;
    public SQL getSQL() { return sql; }

    private SQLType sqlType = SQLType.SQLite;
    public SQLType getSQLType() { return sqlType; }

    private int sensor1Pin = 0;
    public int getSensor1Pin() { return sensor1Pin; }

    private double sensor1Value = 0.4d;
    public double getSensor1Value() { return sensor1Value; }

    private int sensor2Pin = 1;
    public int getSensor2Pin() { return sensor2Pin; }

    private double sensor2Value = 0.4d;
    public double getSensor2Value() { return sensor2Value; }

    private LongObjectPair<TimeUnit> sensor2Time = new LongObjectPair<>(3L, TimeUnit.SECONDS);
    public long getSensor2Time() { return sensor2Time.getSecond().toMillis(sensor2Time.getFirst()); }

    public static CachedConfigValues.Builder builder() { return new CachedConfigValues.Builder(); }

    public static class Builder {
        private final CachedConfigValues values = new CachedConfigValues();

        private Builder() {}

        public CachedConfigValues.Builder debug(boolean value) {
            values.debug = value;
            return this;
        }

        public CachedConfigValues.Builder sql(SQL value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.sql = value;
            return this;
        }

        public CachedConfigValues.Builder sqlType(String value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.sqlType = SQLType.getByName(value);
            return this;
        }

        public CachedConfigValues.Builder setSensor1Pin(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value cannot be < 0");
            }
            if (value > 3) {
                throw new IllegalArgumentException("value cannot be > 3");
            }

            values.sensor1Pin = value;
            return this;
        }

        public CachedConfigValues.Builder setSensor1Value(double value) {
            if (value < 0.0d) {
                throw new IllegalArgumentException("value cannot be < 0");
            }
            if (value > 1.0d) {
                throw new IllegalArgumentException("value cannot be > 1");
            }

            values.sensor1Value = value;
            return this;
        }

        public CachedConfigValues.Builder setSensor2Pin(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value cannot be < 0");
            }
            if (value > 3) {
                throw new IllegalArgumentException("value cannot be > 3");
            }

            values.sensor2Pin = value;
            return this;
        }

        public CachedConfigValues.Builder setSensor2Value(double value) {
            if (value < 0.0d) {
                throw new IllegalArgumentException("value cannot be < 0");
            }
            if (value > 1.0d) {
                throw new IllegalArgumentException("value cannot be > 1");
            }

            values.sensor2Value = value;
            return this;
        }

        public CachedConfigValues.Builder setSensor2Time(long value, TimeUnit unit) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }
            if (unit == null) {
                throw new IllegalArgumentException("unit cannot be null");
            }

            values.sensor2Time = new LongObjectPair<>(value, unit);
            return this;
        }

        public CachedConfigValues build() {
            return values;
        }
    }
}
