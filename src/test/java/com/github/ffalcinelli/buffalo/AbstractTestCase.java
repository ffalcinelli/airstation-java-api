package com.github.ffalcinelli.buffalo;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by fabio on 15/03/17.
 */
public abstract class AbstractTestCase {

    public JSONObject jsonFromFixture(String name) throws IOException {
        return new JSONObject(readFixture(name, "json"));
    }

    public String readFixture(String name, String ext) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        ClassLoader.getSystemClassLoader().getResourceAsStream(name.toLowerCase() + "." + ext)));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public void assertResultOk(JSONObject jsonObject) {
        assertTrue(jsonObject.has("RESULT"));
        assertEquals("OK", jsonObject.getString("RESULT"));
    }

    public void assertResultNotOk(JSONObject jsonObject) {
        assertTrue(jsonObject.has("RESULT"));
        assertNotEquals("OK", jsonObject.getString("RESULT"));
    }
}
