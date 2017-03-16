package com.github.ffalcinelli.buffalo.airstation;

import com.github.ffalcinelli.buffalo.exception.AirStationException;
import junit.framework.TestCase;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by fabio on 13/03/17.
 */
public class AirStationAsyncTestCase extends AbstractAirStationTestCase {

    Future<JSONObject> future;

    @Override
    public void setupConnection() throws IOException {
        Future<JSONObject> f = new Future<>();
        airStation.login("admin", "password", f);
        future = new Future<>();
        try {
            f.result();
        } catch (Throwable t) {
            fail("Cannot initialize test airStation");
        }
    }

    @Test
    public void login() throws Throwable {
        airStation.login("admin", "password", future);
        assertResultOk(future.result());
        assertTrue(isLoggedIn());
    }

    @Test
    public void loginFailed() throws Throwable {
        denyLogin();
        airStation.login("admin", "wrong_password", future);
        assertResultNotOk(future.result());
        assertFalse(isLoggedIn());
    }

    @Test(expected = ConnectException.class)
    public void loginConnectException() throws Throwable {
        server.close();
        airStation.login("admin", "password", future);
        assertResultOk(future.result());
        assertTrue(isLoggedIn());
    }

    @Test
    public void close() throws Throwable {
        airStation.close(future);
        assertResultOk(future.result());
        assertFalse(isLoggedIn());
    }

    @Test(expected = ConnectException.class)
    public void closeNotConnected() throws Throwable {
        server.close();
        airStation.close(future);
        try {
            future.result();
        } finally {
            assertFalse(isLoggedIn());
        }
    }

    @Test
    public void aoss() throws Throwable {
        airStation.getAoss(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void devctrl() throws Throwable {
        airStation.getDevCtrl(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void device() throws Throwable {
        airStation.getDevice(future);
        assertEquals("BUFFALO INC", future.result().getString("VENDOR"));
    }

    @Test
    public void guest() throws Throwable {
        airStation.getGuest(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void icon() throws Throwable {
        airStation.getIcon(future);
        assertTrue(future.result().getBoolean("INTERNET"));
    }

    @Test
    public void lang() throws Throwable {
        airStation.getLang(future);
        assertTrue(future.result().getBoolean("FUNCTION"));
    }

    @Test
    public void nas() throws Throwable {
        airStation.getNas(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void parental() throws Throwable {
        airStation.getParental(future);
        assertEquals("NORTON", future.result().getString("MODE"));
    }

    @Test
    public void qos() throws Throwable {
        airStation.getQos(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void system() throws Throwable {
        airStation.getSystem(future);
        TestCase.assertEquals("admin", future.result().getString("USERNAME"));
    }

    @Test
    public void wireless() throws Throwable {
        airStation.getWireless(future);
        assertNotNull(future.result().getJSONArray("INTERFACE"));
    }

    @Test
    public void wps() throws Throwable {
        airStation.getWps(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void busy() throws Throwable {
        airStation.getBusy(future);
        JSONObject response = future.result();
        assertTrue(response.getBoolean("LOGIN"));
        assertTrue(response.getBoolean("AOSS") ||
                response.getBoolean("AOSS2") ||
                response.getBoolean("SYSTEM"));
    }

    @Test
    public void samba() throws Throwable {
        airStation.getSamba(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void dlna() throws Throwable {
        airStation.getDlna(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void torrent() throws Throwable {
        airStation.getTorrent(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void webAxs() throws Throwable {
        airStation.getWebAccess(future);
        assertTrue(future.result().getBoolean("SUPPORT"));
    }

    @Test
    public void extenderMonitor() throws Throwable {
        airStation.getExtenderMonitor(future);
        assertFalse(future.result().keys().hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void notLoggedIn() throws Throwable {
        airStation.close();
        airStation.getAoss(future);
        future.result();
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

    @Test
    public void doWol() throws Throwable {
        airStation.wol("aa:bb:cc:dd:ee:ff", future);
        assertResultOk(future.result());
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
    public void doAoss() throws Throwable {
        airStation.aoss(future);
        assertResultOk(future.result());
    }

    @Test
    public void updateDeviceInfo() throws Throwable {
        airStation.updateDevCtrl(this.device, future);
        assertResultOk(future.result());
    }

    @Test
    public void wirelessBasicSetup() throws Throwable {
        airStation.wirelessBasicSetup(wifi, wifi, future);
        assertResultOk(future.result());
    }

    @Test
    public void guestBasicSetup() throws Throwable {
        airStation.guestBasicSetup(guest, future);
        assertResultOk(future.result());
    }

    @Test
    public void nasBasicSetup() throws Throwable {
        airStation.nasBasicSetup(nas, future);
        assertResultOk(future.result());
    }

    @Test
    public void enableQOS() throws Throwable {
        airStation.qos(true, future);
        assertResultOk(future.result());
    }

    @Test
    public void setQosPolicy() throws Throwable {
        airStation.setQosPolicy("VIDEO", true, future);
        assertResultOk(future.result());
    }

    @Test
    public void setParentalPolicy() throws Throwable {
        airStation.setParentalPolicy(0, future);
        assertResultOk(future.result());
    }

    @Test
    public void enableGuestWifi() throws Throwable {
        airStation.guest(true, future);
        assertResultOk(future.result());
    }

    @Test
    public void detectNas() throws Throwable {
        airStation.detectNas(future);
        assertResultOk(future.result());
    }

    @Test
    public void addressReservationList() throws Throwable {
        Future<JSONArray> jsonArrayFuture = new Future<>();
        airStation.getDhcpReservation(jsonArrayFuture);
        JSONArray jsonArray = jsonArrayFuture.result();
        assertNotEquals(0, jsonArray.length());
        assertEquals("192.168.11.3", jsonArray.getJSONObject(0).get("IP"));
    }

    @Test(expected = IllegalStateException.class)
    public void addressReservationNotLoggedIn() throws Throwable {
        Future<JSONArray> jsonArrayFuture = new Future<>();
        airStation.getAdapter().close();
        airStation.getDhcpReservation(jsonArrayFuture);
        jsonArrayFuture.result();
    }

    @Test(expected = ConnectException.class)
    public void addressReservationNotConnected() throws Throwable {
        Future<JSONArray> jsonArrayFuture = new Future<>();
        server.close();
        airStation.getAdapter().close();
        airStation.getDhcpReservation(jsonArrayFuture);
        jsonArrayFuture.result();
    }

    @Test
    public void editDhcpEntry() throws Throwable {
        airStation.updateDhcpReservation(device, future);
        assertResultOk(future.result());
    }

    @Test(expected = ConnectException.class)
    public void setConnectionException() throws Throwable {
        server.close();
        Map<String, String> params = new HashMap<>();
        params.put("testkey", "testvalue");
        airStation.set(params, future);
        assertResultOk(future.result());
    }

    @Test(expected = IllegalStateException.class)
    public void setNotLoggedIn() throws Throwable {
        airStation.getAdapter().close();
        Map<String, String> params = new HashMap<>();
        params.put("testkey", "testvalue");
        airStation.set(params, future);
        assertResultNotOk(future.result());
    }


    class Future<T> implements AsyncCallback<T> {
        Lock lock = new ReentrantLock();
        Condition ready = lock.newCondition();
        T result;
        Throwable t;

        T result() throws Throwable {
            lock.lock();
            try {
                ready.await(1, TimeUnit.SECONDS);
                if (t != null)
                    throw t;
                return result;
            } finally {
                lock.unlock();
            }
        }

        public void onFailure(Throwable t) {
            lock.lock();
            try {
                this.t = t;
                ready.signal();
            } finally {
                lock.unlock();
            }
        }

        public void onSuccess(T result) {
            lock.lock();
            try {
                this.result = result;
                ready.signal();
            } finally {
                lock.unlock();
            }
        }
    }
}
