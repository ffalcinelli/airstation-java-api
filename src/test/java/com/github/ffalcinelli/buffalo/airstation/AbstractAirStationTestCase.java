package com.github.ffalcinelli.buffalo.airstation;

import com.github.ffalcinelli.buffalo.AbstractTestCase;
import com.github.ffalcinelli.buffalo.models.NasSettings;
import com.github.ffalcinelli.buffalo.models.NetworkDevice;
import com.github.ffalcinelli.buffalo.models.WifiSettings;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.github.ffalcinelli.buffalo.utils.Utils.closeIgnoreException;
import static org.junit.Assert.assertEquals;

/**
 * Created by fabio on 13/03/17.
 */
public abstract class AbstractAirStationTestCase extends AbstractTestCase {

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

    HttpUrl baseUrl;
    JSONObject settings;
    NetworkDevice device;
    WifiSettings wifi, guest;
    NasSettings nas;
    MockWebServer server;
    AirStation airStation;

    @Before
    public void setUp() throws IOException {
        // Create a MockWebServer. These are lean enough that you can create a new
        // instance for every unit test.
        server = new MockWebServer();

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                try {
                    if (request.getPath().equals("/cgi-bin/cgi?req=twz")) {
                        if (isLoggedIn())
                            return mockHtmlResponse("main");
                        else
                            return mockHtmlResponse("login");
                    }
                    if (request.getPath().equals("/cgi-bin/cgi?req=inp&res=login.html")) {
                        if (request.getMethod().equalsIgnoreCase("GET"))
                            return mockHtmlResponse("login");
                        else {
                            String url = baseUrl.toString();
                            url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
                            return new MockResponse().setResponseCode(302).setHeader("Location", url + "/cgi-bin/cgi?req=twz");
                        }
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


        prepareFixtures();

        airStation = new AirStation(baseUrl.toString());
        settings = airStation.getSettings();

        setupConnection();
    }

    @After
    public void tearDown() {
        closeIgnoreException(airStation);
        closeIgnoreException(server);
    }

    public void prepareFixtures() {
        device = new NetworkDevice();
        device.setMacAddress("aa:bb:cc:dd:ee:ff");
        device.setIpAddress("192.168.11.2");
        device.setId(1);
        device.setQos(2);
        device.setParentalEnabled(true);
        device.setDisconnected(false);
        wifi = new WifiSettings();
        wifi.setSsid("ssid");
        wifi.setEnabled(true);
        wifi.setKey("secr3t");
        wifi.setEncryptionType("WPA-AES");
        wifi.setBw(80);
        wifi.setChannel(1);
        guest = new WifiSettings();
        guest.setSsid("guest_ssid");
        guest.setEncryptionType("WPA-AES");
        guest.setKey("guest_s3cr3t");
        guest.setTime(24);
        nas = new NasSettings();
        nas.setDlnaEnabled(true);
        nas.setName("NAS NAME");

    }

    public abstract void setupConnection() throws IOException;

    public MockResponse mockJsonResponse(String name) throws IOException {
        return new MockResponse().setBody(jsonFromFixture(name).toString(4)).setResponseCode(200);
    }

    public MockResponse mockHtmlResponse(String name) throws IOException {
        return new MockResponse().setBody(readFixture(name, "html"))
                .setResponseCode(200)
                .setHeader("Set-Cookie", "mobile=yes");
    }

    public boolean isLoggedIn() {
        return airStation.getAdapter().isLoggedIn();
    }

    @Test
    public void constructorSettings() {
        assertEquals(settings, new AirStation(settings).getSettings());
    }

    public void denyLogin() throws IOException {
        tearDown();
        server = new MockWebServer();
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                try {
                    if (request.getMethod().equalsIgnoreCase("GET"))
                        return mockHtmlResponse("login");
                    else
                        return mockHtmlResponse("login_failed");
                } catch (IOException e) {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });
        server.start();
        baseUrl = server.url("/");
        airStation = new AirStation(baseUrl.toString());
        prepareFixtures();
        setupConnection();
    }
}
