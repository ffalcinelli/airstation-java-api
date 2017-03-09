package com.github.ffalcinelli.buffalo.utils;

import org.json.JSONObject;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

/**
 * Created by fabio on 28/02/17.
 */
public class Utils {

    public static String getStringOrDefault(JSONObject json, String key, String def) {
        if (json.has(key))
            return json.getString(key);
        json.put(key, def);
        return def;
    }

    public static boolean isStringEmpty(String text) {
        return text == null || "".equals(text);
    }

    public static String mapToFormEncoded(Map<String, String> params, String encoding) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey(), encoding))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), encoding)).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static void closeIgnoreException(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

    public static int getRandomInt(int digits) {
        Random rnd = new Random();
        digits = digits > 1 ? digits - 1 : digits;
        Double base = Math.pow(10, digits);
        Double max = Math.pow(10, digits + 1) - base - 1;
        return base.intValue() + rnd.nextInt(max.intValue());
    }
}
