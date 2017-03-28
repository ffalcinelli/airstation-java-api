package com.github.ffalcinelli.buffalo.airstation;

import com.github.ffalcinelli.buffalo.exception.AirStationException;
import com.github.ffalcinelli.buffalo.exception.AuthenticationException;
import junit.framework.TestCase;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by fabio on 28/02/17.
 */
public class AirStationTestCase extends AbstractAirStationTestCase {


    @Override
    public void setupConnection() throws IOException {
        airStation.login("admin", "password");
    }


    @Test
    public void login() throws IOException {
        airStation.login("admin", "password");
        assertTrue(airStation.getAdapter().isLoggedIn());
    }

    @Test(expected = AuthenticationException.class)
    public void loginFailed() throws IOException {
        denyLogin();
        assertResultNotOk(airStation.login("admin", "wrong_password"));
        assertFalse(isLoggedIn());
    }

    @Test
    public void close() throws IOException {
        airStation.close();
        assertFalse(isLoggedIn());
    }

    @Test
    public void aoss() throws IOException {
        JSONObject response = airStation.getAoss();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void devctrl() throws IOException {
        JSONObject response = airStation.getDevCtrl();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void device() throws IOException {
        JSONObject response = airStation.getDevice();
        assertEquals("BUFFALO INC", response.getString("VENDOR"));
    }

    @Test
    public void guest() throws IOException {
        JSONObject response = airStation.getGuest();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void icon() throws IOException {
        JSONObject response = airStation.getIcon();
        assertTrue(response.getBoolean("INTERNET"));
    }

    @Test
    public void lang() throws IOException {
        JSONObject response = airStation.getLang();
        assertTrue(response.getBoolean("FUNCTION"));
    }

    @Test
    public void nas() throws IOException {
        JSONObject response = airStation.getNas();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void parental() throws IOException {
        JSONObject response = airStation.getParental();
        assertEquals("NORTON", response.getString("MODE"));
    }

    @Test
    public void qos() throws IOException {
        JSONObject response = airStation.getQos();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void system() throws IOException {
        JSONObject response = airStation.getSystem();
        TestCase.assertEquals("admin", response.getString("USERNAME"));
    }

    @Test
    public void wireless() throws IOException {
        JSONObject response = airStation.getWireless();
        assertNotNull(response.getJSONArray("INTERFACE"));
    }

    @Test
    public void wps() throws IOException {
        JSONObject response = airStation.getWps();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void busy() throws IOException {
        JSONObject response = airStation.getBusy();
        assertTrue(response.getBoolean("LOGIN"));
        assertTrue(response.getBoolean("AOSS") ||
                response.getBoolean("AOSS2") ||
                response.getBoolean("SYSTEM"));
    }

    @Test
    public void samba() throws IOException {
        JSONObject response = airStation.getSamba();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void dlna() throws IOException {
        JSONObject response = airStation.getDlna();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void torrent() throws IOException {
        JSONObject response = airStation.getTorrent();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void webAxs() throws IOException {
        JSONObject response = airStation.getWebAccess();
        assertTrue(response.getBoolean("SUPPORT"));
    }

    @Test
    public void extenderMonitor() throws IOException {
        JSONObject response = airStation.getExtenderMonitor();
        assertFalse(response.keys().hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void notLoggedIn() throws IOException {
        airStation.close();
        airStation.getAoss();
    }

    @Test(expected = AirStationException.class)
    public void throwExceptionWhenErrorText() throws IOException {
        Response response = new Response.Builder()
                .request(new Request.Builder().url(baseUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .body(ResponseBody.create(MediaType.parse("text/html"), "<div class=\"errortxt\">This is an error</dv>"))
                .build();
        RequestAdapter.responseToDocument(response);
    }

//    @Test
//    public void constructor() throws IOException {
//        closeIgnoreException(airStation);
//        airStation = new AirStation(new JSONObject().put("url", baseUrl.toString()));
//        airStation.open();
//        assertTrue(airStation.isLoggedIn());
//        closeIgnoreException(airStation);
//        airStation = new AirStation(new JSONObject().put("url", baseUrl.toString()), airStation.defaultCookieJar());
//        airStation.open();
//        assertTrue(airStation.isLoggedIn());
//    }

    @Test
    public void doWol() throws IOException {
        assertResultOk(airStation.wol("aa:bb:cc:dd:ee:ff"));
    }

    @Test
    public void doAoss() throws IOException {
        assertResultOk(airStation.aoss());
    }

    @Test(expected = IllegalStateException.class)
    public void setIllegalStateException() throws IOException {
        airStation.close();
        airStation.aoss();
    }

    @Test
    public void updateDeviceInfo() throws IOException {
        assertResultOk(airStation.updateDevCtrl(this.device));
    }

    @Test
    public void wirelessBasicSetup() throws IOException {
        assertResultOk(airStation.wirelessBasicSetup(
                wifi, wifi
        ));
    }

    @Test
    public void guestBasicSetup() throws IOException {
        assertResultOk(airStation.guestBasicSetup(guest));
    }

    @Test
    public void nasBasicSetup() throws IOException {
        assertResultOk(airStation.nasBasicSetup(nas));
    }

    @Test
    public void enableQOS() throws IOException {
        assertResultOk(airStation.qos(true));
        assertResultOk(airStation.qos(false));
    }

    @Test
    public void setQosPolicy() throws IOException {
        assertResultOk(airStation.setQosPolicy("VIDEO", true));
        assertResultOk(airStation.setQosPolicy("VIDEO", false));
    }

    @Test
    public void setParentalPolicy() throws IOException {
        assertResultOk(airStation.setParentalPolicy(0));
    }

    @Test
    public void enableGuestWifi() throws IOException {
        assertResultOk(airStation.guest(true));
        assertResultOk(airStation.guest(false));
    }

    @Test
    public void detectNas() throws IOException {
        assertResultOk(airStation.detectNas());
    }

    @Test
    public void addressReservationList() throws IOException {
        JSONArray jsonArray = airStation.getDhcpReservation();
        assertNotEquals(0, jsonArray.length());
        assertEquals("192.168.11.3", jsonArray.getJSONObject(0).get("IP"));
    }

    @Test(expected = IllegalStateException.class)
    public void addressReservationNotLoggedIn() throws IOException {
        airStation.close();
        airStation.getDhcpReservation();
    }

    @Test
    public void editDhcpEntry() throws IOException {
        assertResultOk(airStation.updateDhcpReservation(device));
    }
}
