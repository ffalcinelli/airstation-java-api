package com.github.ffalcinelli.buffalo.airstation;

import com.github.ffalcinelli.buffalo.exception.AirStationException;
import junit.framework.TestCase;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.github.ffalcinelli.buffalo.utils.Utils.closeIgnoreException;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by fabio on 28/02/17.
 */
public class AirStationTestCase {

    String[] jsonResources = new String[]{
            "AOSS",
            "DEVCTRL",
            "DEVICE",
            "FUNCTION",
            "GUEST",
            "ICON",
            "LANG",
            "NAS",
            "PARENTAL",
            "QOS",
            "SYSTEM",
            "WIRELESS",
            "WPS",
            "BUSY",
            "DLNA",
            "TORRENT",
            "SAMBA",
            "EXTENDERMONITOR",
            "WEB_AXS"
    };
    String[] formElements = new String[]{
            "do_wol_DEVCTRL",
            "button_AOSS",
            "basic_setting_DEVCTRL",
            "basic_setting_WIRELESS",
            "basic_setting_GUEST",
            "basic_setting_NAS",
            "button_QOS",
            "basic_setting_QOS",
            "basic_setting_PARENTAL",
            "button_GUEST",
            "button_NAS_redetect"
    };
    AirStation airStation;
    HttpUrl baseUrl;

    @Before
    public void setUp() throws IOException {
        // Create a MockWebServer. These are lean enough that you can create a new
        // instance for every unit test.
        MockWebServer server = new MockWebServer();

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                try {
                    if (request.getPath().equals("/cgi-bin/cgi?req=twz")) {
                        if (airStation.isLoggedIn())
                            return mockHtmlResponse("main");
                        else
                            return mockHtmlResponse("login");
                    }
                    if (request.getPath().equals("/cgi-bin/cgi?req=inp&res=login.html")) {
                        if (request.getMethod().equalsIgnoreCase("GET"))
                            return mockHtmlResponse("login");
                    }
                    if (request.getPath().equals("/cgi-bin/cgi?req=twz&frm=logout.html")) {
                        if (request.getMethod().equalsIgnoreCase("GET"))
                            return mockHtmlResponse("login");
                    }
                    if (request.getPath().startsWith("/cgi-bin/cgi?req=frm&frm=dhcps_lease.html&rnd=")) {
                        return mockHtmlResponse("dhcp_reserv");
                    }
                    for (String resource : jsonResources) {
                        if (request.getPath().startsWith(String.format("/cgi-bin/cgi?req=fnc&fnc=%%24{get_json_param(%s,", resource)))
                            return mockJsonResponse(resource);
                    }
                    for (String element : formElements) {
//                        System.out.println(request.getBody().readString(Charset.defaultCharset()));
                        if (request.getPath().startsWith("/cgi-bin/cgi?req=set&t="))
                            return new MockResponse().setBody("OK").setResponseCode(200);
                    }

                    return new MockResponse().setResponseCode(404);
                } catch (IOException e) {
                    return new MockResponse().setResponseCode(500);
                }
            }
        });

        // Start the server.
        server.start();

        // Ask the server for its URL. You'll need this to make HTTP requests.
        baseUrl = server.url("/");

        airStation = new AirStation(baseUrl.toString(), "admin", "password", "utf-8");
        airStation.open();
    }

    @After
    public void tearDown() {
        closeIgnoreException(airStation);
    }

    public MockResponse mockJsonResponse(String name) throws IOException {
        return new MockResponse().setBody(jsonFromFixture(name).toString(4)).setResponseCode(200);
    }

    public MockResponse mockHtmlResponse(String name) throws IOException {
        return new MockResponse().setBody(readFixture(name, "html")).setResponseCode(200).setHeader("Set-Cookie", "mobile=yes");
    }

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

    @Test
    public void open() throws IOException {
        airStation.open();
        assertTrue(airStation.isLoggedIn());
    }

    @Test
    public void close() throws IOException {
        airStation.close();
        assertFalse(airStation.isLoggedIn());
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
    public void throwExceptionWhenErrorText() throws AirStationException {
        airStation.getDocument("<div class=\"errortxt\">This is an error</dv>");
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
        assertTrue(airStation.wol(new JSONObject().put("MAC", "aa:bb:cc:dd:ee:ff")));
    }

    @Test
    public void doAoss() throws IOException {
        assertTrue(airStation.aoss());
    }

    @Test(expected = IllegalStateException.class)
    public void setIllegalStateException() throws IOException{
        airStation.close();
        airStation.aoss();
    }

    @Test
    public void updateDeviceInfo() throws IOException {
        assertTrue(airStation.updateDeviceInfo(new JSONObject()
                .put("MAC", "aa:bb:cc:dd:ee:ff")
                .put("IP", "192.168.11.2")
                .put("ID", "1")
                .put("QOS", 2)
                .put("PARENTAL", true)
                .put("DISCONNECT", false)
        ));
    }

    @Test
    public void wirelessBasicSetup() throws IOException {
        JSONObject wifi = new JSONObject()
                .put("SSID", "ssid")
                .put("FUNC", 1)
                .put("KEY", "secr3t")
                .put("ENCTYPE", "WPA-AES")
                .put("CH", 1)
                .put("BW", 80);

        assertTrue(airStation.wirelessBasicSetup(
                wifi, wifi
        ));
    }

    @Test
    public void guestBasicSetup() throws IOException {
        JSONObject guest = new JSONObject()
                .put("SSID", "ssid")
                .put("KEY", "secr3t")
                .put("ENCTYPE", "WPA-AES")
                .put("TIME", 24);

        assertTrue(airStation.guestBasicSetup(guest));
    }

    @Test
    public void nasBasicSetup() throws IOException {
        assertTrue(airStation.nasBasicSetup(new JSONObject()
                .put("SAMBA", true)
                .put("TORRENT", true)
                .put("DLNA", false)
                .put("WEBAXS", false)
                .put("NASCOMNAME", "NAME")
        ));
    }

    @Test
    public void enableQOS() throws IOException {
        assertTrue(airStation.enableQos(true));
        assertTrue(airStation.enableQos(false));
    }

    @Test
    public void setQosPolicy() throws IOException {
        assertTrue(airStation.setQosPolicy("VIDEO", true));
        assertTrue(airStation.setQosPolicy("VIDEO", false));
    }

    @Test
    public void setParentalPolicy() throws IOException {
        assertTrue(airStation.setParentalPolicy(0));
    }

    @Test
    public void enableGuestWifi() throws IOException {
        assertTrue(airStation.enableGuestWifi(true));
        assertTrue(airStation.enableGuestWifi(false));
    }

    @Test
    public void detectNas() throws IOException {
        assertTrue(airStation.detectNas());
    }

    @Test
    public void addressReservationList() throws IOException {
        JSONArray jsonArray = airStation.getAddressReservationList();
        assertNotEquals(0, jsonArray.length());
        assertEquals("192.168.11.3", jsonArray.getJSONObject(0).get("IP"));
    }

    @Test(expected = IllegalStateException.class)
    public void addressReservationNotLoggedIn() throws IOException {
        airStation.close();
        airStation.getAddressReservationList();
    }

    @Test
    public void editDhcpEntry() throws IOException {
        airStation.editAddressReservation(new JSONObject()
                .put("IP", "192.168.11.2")
                .put("MAC", "aa:bb:cc:dd:ee:ff")
                .put("ID", "1")
        );
    }

}
