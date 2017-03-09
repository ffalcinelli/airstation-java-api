package com.github.ffalcinelli.buffalo.airstation;

import com.github.ffalcinelli.buffalo.crypto.Rsa;
import com.github.ffalcinelli.buffalo.exception.AirStationException;
import com.github.ffalcinelli.buffalo.utils.Utils;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.ffalcinelli.buffalo.utils.Utils.getStringOrDefault;
import static com.github.ffalcinelli.buffalo.utils.Utils.mapToFormEncoded;

/**
 * Created by fabio on 24/02/17.
 */
public class AirStation implements Closeable {

    private String url;
    private String username;
    private String password;
    private String encoding;
    private String webSessionNum;
    private String webSessionId;
    private OkHttpClient client;

    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded"); // charset=UTF-8");

    static CookieJar defaultCookieJar() {
        return new CookieJar() {
            private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url, cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url);
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };
    }

    public AirStation(String url, String username, String password) {
        this(url, username, password, "utf-8", null);
    }

    public AirStation(String url, String username, String password, String encoding) {
        this(url, username, password, encoding, null);
    }

    public AirStation(String url, String username, String password, String encoding, CookieJar cookieJar) {
        this.encoding = encoding;
        this.username = username;
        this.password = password;
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.client = new OkHttpClient.Builder()
                .cookieJar(cookieJar != null ? cookieJar : defaultCookieJar())
                .build();
    }

    private String encryptPassword(String password, Document page) {
        String data = page.data();
        final String EXP_PATTERN = "exponent = \"";
        final String MOD_PATTERN = "modulus = \"";
        int expIdx = data.indexOf(EXP_PATTERN);
        int modIdx = data.indexOf(MOD_PATTERN);
        Rsa rsa = new Rsa(data.substring(expIdx + EXP_PATTERN.length(),
                data.indexOf("\"", expIdx + EXP_PATTERN.length())),
                data.substring(modIdx + MOD_PATTERN.length(),
                        data.indexOf("\"", modIdx + MOD_PATTERN.length())));
        try {
            return rsa.encrypt("airstation_pass=" + password);
        } catch (Exception e) {
            return null;
        }
    }


    Document getDocument(String body) throws AirStationException {
        Document doc = Jsoup.parse(body);
        for (Element div : doc.getElementsByAttributeValue("class", "errortxt")) {
            throw new AirStationException(div.text());
        }
        return doc;
    }

    public void open() throws IOException {
        Request request = new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=twz", url))
                .build();
        Response response = client.newCall(request).execute();
        Document doc = getDocument(response.body().string());

        if (doc.getElementsByTag("title").first().text().equalsIgnoreCase("login")) {
            Map<String, String> params = new HashMap<>();
            params.put("lang", "auto");
            params.put("airstation_uname", username);
            webSessionId = doc.getElementsByAttributeValue("name", "sWebSessionid").first().attr("value");
            webSessionNum = doc.getElementsByAttributeValue("name", "sWebSessionnum").first().attr("value");
            params.put("sWebSessionnum", webSessionNum);
            params.put("sWebSessionid", webSessionId);
            params.put("encrypted", encryptPassword(password, doc));

            RequestBody body = RequestBody.create(FORM, mapToFormEncoded(params, encoding));
            request = new Request.Builder()
                    .url(String.format("%s/cgi-bin/cgi?req=inp&res=login.html", url))
                    .post(body)
                    .build();
            response = client.newCall(request).execute();

            getDocument(response.body().string());
        } else {
            //TODO: use a logger or just ignore?
            System.out.println("Already logged in");
        }
    }

    @Override
    public void close() throws IOException {
        Request request = new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=twz&frm=logout.html", url))
                .build();
        Response response = client.newCall(request).execute();
        Document doc = getDocument(response.body().string());
        webSessionId = null;
        webSessionNum = null;
    }

    private JSONObject getJsonParam(String param) throws IOException {
        if (!isLoggedIn())
            throw new IllegalStateException("Not logged in!");
        RequestBody requestBody = RequestBody.create(FORM, "");
        Request request = new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=fnc&fnc=%%24{get_json_param(%s,%d)}", url, param, System.currentTimeMillis()))
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        String responseText = response.body().string();
        return new JSONObject(responseText);
    }

    private boolean set(Map<String, String> params) throws IOException {
        if (!isLoggedIn())
            throw new IllegalStateException("Not logged in!");
        RequestBody requestBody = RequestBody.create(FORM, mapToFormEncoded(params, encoding));
        Request request = new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=set&t=%d", url, System.currentTimeMillis()))
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        return "OK".equalsIgnoreCase(response.body().string());
    }

    /**
     * Get informations about internet status
     *
     * @return A {@link JSONObject} with the internet status
     * @throws IOException
     */
    public JSONObject getIcon() throws IOException {
        return getJsonParam("ICON");
    }

    /**
     * Get informations about connected devices
     *
     * @return A {@link JSONObject} with the connected devices informations
     * @throws IOException
     */
    public JSONObject getDevCtrl() throws IOException {
        return getJsonParam("DEVCTRL");
    }

    /**
     * Get informations about the AirStation status
     *
     * @return * @return A {@link JSONObject} with the AirStation status
     * @throws IOException
     */
    public JSONObject getDevice() throws IOException {
        return getJsonParam("DEVICE");
    }

    /**
     * Get AOSS function status
     *
     * @return * @return A {@link JSONObject} with the AOSS status
     * @throws IOException
     */
    public JSONObject getAoss() throws IOException {
        return getJsonParam("AOSS");
    }

    /**
     * Get the wireless settings
     *
     * @return * @return A {@link JSONObject} with the wireless settings
     * @throws IOException
     */
    public JSONObject getWireless() throws IOException {
        return getJsonParam("WIRELESS");
    }

    /**
     * Get WPS status
     *
     * @return * @return A {@link JSONObject} with the WPS status
     * @throws IOException
     */
    public JSONObject getWps() throws IOException {
        return getJsonParam("WPS");
    }

    /**
     * Get NAS status
     *
     * @return * @return A {@link JSONObject} with the NAS status
     * @throws IOException
     */
    public JSONObject getNas() throws IOException {
        return getJsonParam("NAS");
    }

    /**
     * Get GUEST wireless settings
     *
     * @return * @return A {@link JSONObject} with the guest wireless settings
     * @throws IOException
     */
    public JSONObject getGuest() throws IOException {
        return getJsonParam("GUEST");
    }

    /**
     * Get QOS settings
     *
     * @return * @return A {@link JSONObject} with the QOS settings
     * @throws IOException
     */
    public JSONObject getQos() throws IOException {
        return getJsonParam("QOS");
    }

    /**
     * Get Parental settings
     *
     * @return * @return A {@link JSONObject} with the parental settings
     * @throws IOException
     */
    public JSONObject getParental() throws IOException {
        return getJsonParam("PARENTAL");
    }

    /**
     * Get System Wide settings
     *
     * @return * @return A {@link JSONObject} with the system settings
     * @throws IOException
     */
    public JSONObject getSystem() throws IOException {
        return getJsonParam("SYSTEM");
    }

    /**
     * Get language settings
     *
     * @return * @return A {@link JSONObject} with the language settings
     * @throws IOException
     */
    public JSONObject getLang() throws IOException {
        return getJsonParam("LANG");
    }

    /**
     * Get device busy status
     *
     * @return * @return A {@link JSONObject} with the device busy status
     * @throws IOException
     */
    public JSONObject getBusy() throws IOException {
        return getJsonParam("BUSY");
    }

    public JSONObject getExtenderMonitor() throws IOException {
        return getJsonParam("EXTENDERMONITOR");
    }

    public JSONObject getDlna() throws IOException {
        return getJsonParam("DLNA");
    }

    public JSONObject getTorrent() throws IOException {
        return getJsonParam("TORRENT");
    }

    public JSONObject getWebAccess() throws IOException {
        return getJsonParam("WEB_AXS");
    }

    public JSONObject getSamba() throws IOException {
        return getJsonParam("SAMBA");
    }

    public boolean isLoggedIn() {
        return !Utils.isStringEmpty(webSessionId) && !Utils.isStringEmpty(webSessionNum);
    }

    /**
     * Wake on Lan the given MAC address
     *
     * @param macAddress The device MAC address to send magic packet to
     * @return true if action has been performed, false otherwise
     * @throws IOException
     */
    public boolean wol(String macAddress) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "do_wol_DEVCTRL");
        params.put("mac", macAddress);
        return set(params);
    }

    /**
     * Wake on Lan the given device, passed as JSON Object with at least MAC key representing the MAC address to wake
     *
     * @param device A {@link JSONObject}  representing the device
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean wol(JSONObject device) throws IOException {
        return wol(device.getString("MAC"));
    }

    /**
     * Put AirStatin in AOSS mode
     *
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean aoss() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_AOSS");
        params.put("val", "Start AOSS/WPS");
        return set(params);
    }

    /**
     * Update informations about a connected device. Device informations are passed as a {@link JSONObject} and must contain
     * <ul>
     * <li>IP: String</li>
     * <li>MAC: String</li>
     * <li>QOS: int</li>
     * <li>PARENTAL: boolean</li>
     * <li>DISCONNECT: boolean</li>
     * </ul>
     *
     * @param device The {@link JSONObject} representing the device info
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean updateDeviceInfo(JSONObject device) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_DEVCTRL");
        params.put("name", device.optString("NAME", "Unknown"));
        params.put("mac", device.getString("MAC"));
        params.put("img", device.optString("IMG", "unknown"));
        params.put("qos", String.valueOf(device.getInt("QOS")));
        params.put("parental", device.getBoolean("PARENTAL") ? "1" : "0");
        params.put("disconnect", device.getBoolean("DISCONNECT") ? "1" : "0");
        return set(params);
    }


    /**
     * Appends a given suffix to the {@link JSONObject} representing the wifi basic settings.
     * This is required to differentiate wifi A and G
     *
     * @param wifi   The {@link JSONObject}  with the Wifi settings
     * @param suffix The suffix to append to keys
     * @return A {@link Map<String, String>} with the modified keys
     */
    private Map<String, String> toWifiParameterMap(JSONObject wifi, String suffix) {
        Map<String, String> params = new HashMap<>();
        for (String k : new String[]{"func", "ssid", "enctype", "key", "ch", "bw"})
            params.put(k + suffix, String.valueOf(wifi.get(k.toUpperCase())));
        return params;
    }

    /**
     * Update the basic wifi settings
     *
     * @param wifiA Settings for wifi A as {@link JSONObject}
     * @param wifiG Settings for wifi G as {@link JSONObject}
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean wirelessBasicSetup(JSONObject wifiA, JSONObject wifiG) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_WIRELESS");
        params.putAll(toWifiParameterMap(wifiG, "_g"));
        params.putAll(toWifiParameterMap(wifiA, "_a"));
        return set(params);
    }

    /**
     * Update basic Guest access settings
     *
     * @param guest
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean guestBasicSetup(JSONObject guest) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_GUEST");
        for (String k : new String[]{"ssid", "enctype", "key"})
            params.put(k + "_g", String.valueOf(guest.get(k.toUpperCase())));
        params.put("time", String.valueOf(guest.get("TIME")));
        return set(params);
    }


    /**
     * Update basic NAS settings
     *
     * @param nas The {@link JSONObject} with the NAS settings
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean nasBasicSetup(JSONObject nas) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_NAS");
        params.put("samba", nas.getBoolean("SAMBA") ? "1" : "0");
        params.put("torrent", nas.getBoolean("TORRENT") ? "1" : "0");
        params.put("dlna", nas.getBoolean("DLNA") ? "1" : "0");
        params.put("webaxs", nas.getBoolean("WEBAXS") ? "1" : "0");
        params.put("nascomname", nas.getString("NASCOMNAME"));
        return set(params);
    }


    /**
     * Enable/Disable QOS function mode
     *
     * @param on If must be enabled or disabled
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean enableQos(boolean on) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_QOS");
        params.put("val", on ? "ON" : "OFF");
        return set(params);
    }

    /**
     * Put router in given QOS policy mode if flag enabled is true
     *
     * @param policy  The policy to use
     * @param enabled Whether to enable or disable the mode
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean setQosPolicy(String policy, boolean enabled) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_QOS");
        params.put("function", enabled ? "1" : "0");
        params.put("polycy", policy);
        return set(params);
    }

    /**
     * Setup the parental policy
     *
     * @param policy The parental policy
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean setParentalPolicy(int policy) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_PARENTAL");
        params.put("polycy", String.valueOf(policy));
        return set(params);
    }

    /**
     * Enable/Disable guest wifi access mode
     *
     * @param on Whether to enable or disable the guest wifi
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean enableGuestWifi(boolean on) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_GUEST");
        params.put("val", on ? "ON" : "OFF");
        return set(params);
    }

    /**
     * Detect Attached Storage devices
     *
     * @return True if the request is received by router
     * @throws IOException
     */
    public boolean detectNas() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_NAS_redetect");
        return set(params);
    }

    /**
     * Get a {@link JSONArray} of current DHCP reserved address entries
     *
     * @return The DHCP address entries as a {@link JSONArray}
     * @throws IOException
     */
    public JSONArray getAddressReservationList() throws IOException {
        if (!isLoggedIn())
            throw new IllegalStateException("Not logged in!");
        Request request = new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=frm&frm=dhcps_lease.html&rnd=%d", url, Utils.getRandomInt(8)))
                .build();
        Response response = client.newCall(request).execute();
        Document doc = Jsoup.parse(response.body().string());
        Element table = doc.getElementsByAttributeValue("class", "AD_LIST").first();
        JSONArray entries = new JSONArray();
        for (Element tr : table.getElementsByTag("tr")) {
            Elements tds = tr.getElementsByTag("td");
            if (tds.size() == 5) {
                entries.put(new JSONObject()
                        .put("IP", tds.get(0).text().replaceAll("\\(\\*\\)", ""))
                        .put("MAC", tds.get(1).text())
                        .put("LEASE", tds.get(2).text())
                        .put("ID", tds.get(4).getElementsByAttributeValue("type", "submit")
                                .first().attr("name").substring(3)));
            }
        }
        return entries;
    }

    /**
     * Edit the DHCP entry. Must contain:
     * <ul>
     * <li>ID: unique identified for this entry as released by the AirStation</li>
     * <li>IP: String</li>
     * <li>MAC: String</li>
     * </ul>
     *
     * @param dhcpEntry The entry to edit
     * @throws IOException
     */
    public void editAddressReservation(JSONObject dhcpEntry) throws IOException {
        Map<String, String> params = new HashMap<>();
        String id = dhcpEntry.getString("ID");
        params.put("sWebSessionnum", webSessionNum);
        params.put("sWebSessionid", webSessionId);
        params.put("manip" + id, dhcpEntry.getString("IP"));
        params.put("manmac" + id, dhcpEntry.getString("MAC"));
        params.put("EDITID", id);
        params.put("DOFIX" + id, "Save");
        set(params);
    }
}
