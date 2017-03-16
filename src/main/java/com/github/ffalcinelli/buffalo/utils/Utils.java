package com.github.ffalcinelli.buffalo.utils;

import org.json.JSONObject;

import java.io.Closeable;
import java.util.Random;

/**
 * A class for utilities and shortcuts. DRY.
 * <p>
 * Created by fabio on 28/02/17.
 */
public class Utils {

    /**
     * Given a {@link JSONObject} retrieves the value associated with the given key parameter. If
     * the {@link JSONObject} has no such key, then the default value will be returned and the key, value pair got set
     * into the {@link JSONObject} itself
     *
     * @param json The {@link JSONObject}
     * @param key  The key to retrieve
     * @param def  The default value to use if no such key exists
     * @return The value either default or actually associated with key
     */
    public static String getStringOrDefault(JSONObject json, String key, String def) {
        if (json.has(key))
            return json.getString(key);
        json.put(key, def);
        return def;
    }

    /**
     * Checks whether the passed string is null or empty
     *
     * @param text The String to test
     * @return true if text is null or empty, false otherwise
     */
    public static boolean isStringEmpty(String text) {
        return text == null || "".equals(text);
    }

    /**
     * Close a list of {@link Closeable} instances ignoring every eventual {@link Exception}.
     *
     * @param closeables The object to close by invoking {@link Closeable#close()} method.
     */
    public static void closeIgnoreException(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Returns a random int with the given digits length.
     *
     * @param digits The length in digits
     * @return The random number
     */
    public static int getRandomInt(int digits) {
        Random rnd = new Random();
        digits = digits > 1 ? digits - 1 : digits;
        Double base = Math.pow(10, digits);
        Double max = Math.pow(10, digits + 1) - base - 1;
        return base.intValue() + rnd.nextInt(max.intValue());
    }
}
