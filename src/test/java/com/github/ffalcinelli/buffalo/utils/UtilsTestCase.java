package com.github.ffalcinelli.buffalo.utils;

import org.json.JSONObject;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.github.ffalcinelli.buffalo.utils.Utils.closeIgnoreException;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

/**
 * Created by fabio on 28/02/17.
 */
public class UtilsTestCase {

    @Test
    public void getStringOrDefault() {
        //hack to stimulate class definition to improve code coverage
        new Utils();
        JSONObject json = new JSONObject();
        json.put("here", "there");
        assertEquals("value", Utils.getStringOrDefault(json, "key", "value"));
        assertTrue(json.has("key"));
        assertEquals("value", json.getString("key"));
        assertEquals("there", Utils.getStringOrDefault(json, "here", "value"));
    }

    @Test
    public void mapToForm() throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        params.put("inputName", "inputValue");
        assertEquals("inputName=inputValue", Utils.mapToFormEncoded(params, "utf-8"));
    }

    @Test
    public void isEmpty() {
        assertTrue(Utils.isStringEmpty(null));
        assertTrue(Utils.isStringEmpty(""));
        assertFalse(Utils.isStringEmpty("a"));
    }

    @Test
    public void exceptionIgnored() {
        try {
            closeIgnoreException(new Closeable() {
                @Override
                public void close() throws IOException {
                    throw new IOException("I'll be ignored!");
                }
            });
        } catch (Exception e) {
            fail("Exceptions should not be thrown!");
        }
    }

    @Test
    public void randomInt() {
        assertEquals(8, String.valueOf(Utils.getRandomInt(8)).length());
        assertEquals(1, String.valueOf(Utils.getRandomInt(0)).length());
    }
}
