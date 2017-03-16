package com.github.ffalcinelli.buffalo.models;

import com.github.ffalcinelli.buffalo.AbstractTestCase;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by fabio on 15/03/17.
 */
public class ModelsTestCase extends AbstractTestCase {

    @Test
    public void nasSettingsJson() throws IOException {
        JSONObject jsonObject = new JSONObject()
                .put("SAMBA", true)
                .put("TORRENT", true)
                .put("DLNA", false)
                .put("WEBAXS", true)
                .put("NASCOMNAME", "a_name_here");

        NasSettings nas = new NasSettings();
        nas.fromJSONObject(jsonObject);
        assertTrue(nas.isSambaEnabled());
        assertEquals("a_name_here", nas.getName());
        assertEquals(nas, new NasSettings(jsonObject));
        assertEquals(nas.hashCode(), new NasSettings(jsonObject).hashCode());
        assertTrue(nas.toString().contains(jsonObject.toString(4)));
    }

    @Test
    public void networkDeviceJson() {
        JSONObject jsonObject = new JSONObject()
                .put("ID", 1)
                .put("NAME", "a_name_here")
                .put("IMG", "pc")
                .put("IP", "192.168.11.2")
                .put("MAC", "aa:bb:cc:dd:ee:ff")
                .put("LEASE", 0)
                .put("QOS", 1)
                .put("PARENTAL", false)
                .put("DISCONNECT", false);
        NetworkDevice dev = new NetworkDevice();
        dev.fromJSONObject(jsonObject);
        assertEquals("a_name_here", dev.getName());
        assertEquals(dev, new NetworkDevice(jsonObject));
        assertEquals(dev.hashCode(), new NetworkDevice(jsonObject).hashCode());
        assertTrue(dev.toString().contains(jsonObject.toString(4)));

    }

    @Test
    public void wifiSettingsJson() {
        JSONObject jsonObject = new JSONObject()
                .put("SSID", "ssid1")
                .put("KEY", "secret")
                .put("ENCTYPE", "WPA")
                .put("CH", 1)
                .put("BW", 80)
                .put("FUNC", true)
                .put("TIME", -1);
        WifiSettings wifi = new WifiSettings();
        wifi.fromJSONObject(jsonObject);
        assertEquals("ssid1", wifi.getSsid());
        assertEquals(wifi, new WifiSettings(jsonObject));
        assertEquals(wifi.hashCode(), new WifiSettings(jsonObject).hashCode());
        assertTrue(wifi.toString().contains(jsonObject.toString(4)));
    }
}
