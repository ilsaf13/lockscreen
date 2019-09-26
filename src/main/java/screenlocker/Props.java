package screenlocker;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Props {
    private final Properties properties;

    public Props(String filename) throws IOException {
        properties = new Properties();
        properties.load(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
    }

    public Props(Properties properties) {
        this.properties = properties;
    }

    public Props(Map<String, String> map) {
        properties = new Properties();
        properties.putAll(map);
    }

    public void store(FileWriter fw) throws IOException {
        properties.store(fw, "");
    }

    public ConcurrentHashMap<String, String> toConcurrentHashMap() {
        return new ConcurrentHashMap(properties);
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public int getInt(String key, String defaultValue) {
        return Integer.parseInt(getString(key, defaultValue));
    }

    public long getLong(String key, long defaultValue) {
        if (properties.containsKey(key)) {
            return Long.parseLong(get(key));
        } else {
            return defaultValue;
        }
    }

    public Color getColor(String key, Color defaultColor) {
        if (properties.containsKey(key)) {
            return new Color(Integer.decode(properties.getProperty(key)));
        } else {
            return defaultColor;
        }
    }

    public Font getFont(String key, String defaultValue) {
        return Font.decode(getString(key, defaultValue));
    }

    public float getFraction(String name, String defaultValue) {
        String value = getString(name, defaultValue).trim();
        if (value.endsWith("%")) {
            return Float.parseFloat(value.substring(0, value.length() - 1)) / 100;
        } else {
            return Float.parseFloat(value);
        }
    }
}
