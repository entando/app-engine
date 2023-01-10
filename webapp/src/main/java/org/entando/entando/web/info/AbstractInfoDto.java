package org.entando.entando.web.info;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import org.springframework.util.Assert;

public abstract class AbstractInfoDto {

    private final Properties entries;

    protected AbstractInfoDto(Properties entries) {
        Assert.notNull(entries, "Entries must not be null");
        this.entries = copy(entries);
    }

    public String get(String key) {
        return this.entries.getProperty(key);
    }

    private Properties copy(Properties properties) {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    protected static  void coercePropertyToEpoch(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            properties.setProperty(key, coerceToEpoch(value));
        }
    }

    protected static String coerceToEpoch(String s) {
        Long epoch = parseEpochSecond(s);
        if (epoch != null) {
            return String.valueOf(epoch);
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        try {
            return String.valueOf(format.parse(s, Instant::from).toEpochMilli());
        }
        catch (DateTimeParseException ex) {
            return s;
        }
    }

    protected static Long parseEpochSecond(String s) {
        try {
            return Long.parseLong(s) * 1000;
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

}
