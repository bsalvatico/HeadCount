package me.egg82.headcount.services;

import me.egg82.headcount.enums.SQLType;
import ninja.egg82.sql.SQL;

public class CachedConfigValues {
    private CachedConfigValues() {}

    private boolean debug = false;
    public boolean getDebug() { return debug; }

    private SQL sql = null;
    public SQL getSQL() { return sql; }

    private SQLType sqlType = SQLType.SQLite;
    public SQLType getSQLType() { return sqlType; }

    private double sensor1Value = 0.4d;
    public double getSensor1Value() { return sensor1Value; }

    private double sensor2Value = 0.4d;
    public double getSensor2Value() { return sensor2Value; }

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

        public CachedConfigValues build() {
            return values;
        }
    }
}
