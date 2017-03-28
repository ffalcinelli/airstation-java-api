package com.github.ffalcinelli.buffalo.airstation;

import com.github.ffalcinelli.buffalo.crypto.JSRsa;
import com.github.ffalcinelli.buffalo.exception.AirStationException;
import com.github.ffalcinelli.buffalo.models.NasSettings;
import com.github.ffalcinelli.buffalo.models.NetworkDevice;
import com.github.ffalcinelli.buffalo.models.WifiSettings;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * An adapter to build AirStation requests and parameters to use in both Synchronous and Asynchronous calls.
 * <p>
 * Created by fabio on 13/03/17.
 */
public class RequestAdapter implements Closeable {

    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded"); // charset=UTF-8");

    public static final String DEFAULT_URL = "http://192.168.11.1";
    public static final String DEFAULT_ENCODING = "utf-8";
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "password";

    private String url;
    private String encoding;
    private String webSessionId;
    private String webSessionNum;

    /**
     * Construct a {@link RequestAdapter} for the given url.
     *
     * @param url      The AirStation url.
     * @param encoding The encoding.
     */
    public RequestAdapter(String url, String encoding) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.encoding = encoding;
    }

    /**
     * Get a default, non persistent, {@link CookieJar} implementation.
     *
     * @return The {@link CookieJar} implementation.
     */
    public static CookieJar getDefaultCookieJar() {
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

    /**
     * Transform a {@link Response} into a {@link Document}.
     *
     * @param response The {@link Response} to parse.
     * @return A {@link Document} representing the response passed.
     * @throws AirStationException Whenever something goes wrong getting or parsing the response.
     */
    static Document responseToDocument(Response response) throws IOException {
        Document doc = null;
        doc = Jsoup.parse(response.body().string());
        Element div = doc.getElementsByAttributeValue("class", "errortxt").first();
        if (div != null)
            throw new AirStationException(div.text());
        return doc;
    }

    /**
     * From a map of String,String pairs, produce a form encoded string (key1=value1&key2=value2&...).
     *
     * @param params   The Map&lt;String, String&gt; to get parameters from.
     * @param encoding The encoding to use.
     * @return A String form encoded.
     * @throws UnsupportedEncodingException If the specified encoding is not supported.
     */
    static String mapToFormEncoded(Map<String, String> params, String encoding) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey(), encoding))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), encoding)).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Get a {@link Request} for the home page.
     *
     * @return The home {@link Request}
     */
    public Request getHomeRequest() {
        return new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=twz", url))
                .build();
    }

    /**
     * After having received the home page, build the subsequent login {@link Request}.
     *
     * @param username     The username to use while forging the request.
     * @param password     The password to use while forging the request.
     * @param homeResponse The response got for a previous call to the home page.
     * @return The login {@link Request}.
     * @throws IOException          Whenever and error occurs while parsing the response.
     * @throws UnsupportedEncodingException If an unsupported encoding is specified to encode FORM parameters.
     */
    public Request doLoginFromHomeResponse(String username, String password, Response homeResponse) throws IOException {
        Document doc = null;
        doc = responseToDocument(homeResponse);
        if (doc.getElementsByTag("title").first().text().equalsIgnoreCase("login")) {
            Map<String, String> params = new HashMap<>();
            params.put("lang", "auto");
            params.put("airstation_uname", username);
            webSessionId = doc.getElementsByAttributeValue("name", "sWebSessionid").first().attr("value");
            webSessionNum = doc.getElementsByAttributeValue("name", "sWebSessionnum").first().attr("value");
            params.put("sWebSessionnum", webSessionNum);
            params.put("sWebSessionid", webSessionId);
            String data = doc.data();
            final String EXP_PATTERN = "exponent = \"";
            final String MOD_PATTERN = "modulus = \"";
            int expIdx = data.indexOf(EXP_PATTERN);
            int modIdx = data.indexOf(MOD_PATTERN);
            JSRsa JSRsa = new JSRsa(data.substring(expIdx + EXP_PATTERN.length(),
                    data.indexOf("\"", expIdx + EXP_PATTERN.length())),
                    data.substring(modIdx + MOD_PATTERN.length(),
                            data.indexOf("\"", modIdx + MOD_PATTERN.length())));
            params.put("encrypted", JSRsa.encrypt("airstation_pass=" + password));
            RequestBody body = RequestBody.create(FORM, mapToFormEncoded(params, encoding));
            return new Request.Builder()
                    .url(String.format("%s/cgi-bin/cgi?req=inp&res=login.html", url))
                    .post(body)
                    .build();
        }
        return null;
    }

    /**
     * Get a logout {@link Request}.
     *
     * @return The logout {@link Request}.
     */
    public Request getLogoutRequest() {
        return new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=twz&frm=logout.html", url))
                .build();
    }

    @Override
    public void close() throws IOException {
        webSessionId = null;
        webSessionNum = null;
    }

    /**
     * Build a {@link Request} to retrieve a given JSON dataset.
     *
     * @param param The dataset to retrieve.
     * @return The {@link Request}.
     */
    public Request getJSONParamRequest(String param) {
        RequestBody requestBody = RequestBody.create(FORM, "");
        return new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=fnc&fnc=%%24{get_json_param(%s,%d)}", url, param, System.currentTimeMillis()))
                .post(requestBody)
                .build();
    }

    /**
     * Build a {@link Request} to perform a given action.
     *
     * @param params The parameters required for the action.
     * @return The {@link Request}.
     * @throws UnsupportedEncodingException If the encoding used to format parameters is not supported.
     */
    public Request getSETRequest(Map<String, String> params) throws UnsupportedEncodingException {
        RequestBody requestBody = RequestBody.create(FORM, mapToFormEncoded(params, encoding));
        return new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=set&t=%d", url, System.currentTimeMillis()))
                .post(requestBody)
                .build();
    }

    /**
     * Build a {@link Request} for a given FORM page.
     *
     * @param frm The FORM to request.
     * @return The {@link Request}.
     */
    public Request getFRMRequest(String frm) {
        return new Request.Builder()
                .url(String.format("%s/cgi-bin/cgi?req=frm&frm=%s&rnd=%d", url, frm, Utils.getRandomInt(8)))
                .build();
    }

    /**
     * Get the required set of parameters to perform Wake On Lan operation.
     *
     * @param macAddress The device MAC address to wake.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getWolParams(String macAddress) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "do_wol_DEVCTRL");
        params.put("mac", macAddress);
        return params;
    }

    /**
     * Get the required set of parameters to start AOSS session.
     *
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getAossParams() {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_AOSS");
        params.put("val", "Start AOSS/WPS");
        return params;
    }

    /**
     * Get the required set of parameters to save informations about a device.
     *
     * @param dev The device informations to save.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getDevCtrlParams(NetworkDevice dev) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_DEVCTRL");
        params.put("name", dev.getName());
        params.put("mac", dev.getMacAddress());
        params.put("img", dev.getImg());
        params.put("qos", String.valueOf(dev.getQos()));
        params.put("parental", dev.isParentalEnabled() ? "1" : "0");
        params.put("disconnect", dev.isDisconnected() ? "1" : "0");
        return params;
    }

    /**
     * Get the required set of parameters to configure wifi. Append the suffix to each key.
     *
     * @param wifi   WiFi settings.
     * @param suffix String to append to keys (e.g. "_a" for WiFi A and "_g" for WiFi G).
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getWifiParams(WifiSettings wifi, String suffix) {
        Map<String, String> params = new HashMap<>();
        params.put("func" + suffix, wifi.isEnabled() ? "1" : "0");
        params.put("ssid" + suffix, wifi.getSsid());
        params.put("enctype" + suffix, wifi.getEncryptionType());
        params.put("key" + suffix, wifi.getKey());
        params.put("ch" + suffix, String.valueOf(wifi.getChannel()));
        params.put("bw" + suffix, String.valueOf(wifi.getBw()));
        return params;
    }

    /**
     * Get the required set of parameters to configure wifi (both A and G).
     *
     * @param a WiFi settings for A radio.
     * @param g WiFi settings for G radio.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getWifiParams(WifiSettings a, WifiSettings g) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_WIRELESS");
        params.putAll(getWifiParams(g, "_g"));
        params.putAll(getWifiParams(a, "_a"));
        return params;
    }

    /**
     * Get the required set of parameters to configure guest wifi.
     *
     * @param guest Guest WiFi settings.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getGuestParams(WifiSettings guest) {
        Map<String, String> params = new HashMap<>();
        params.put("ssid_g", guest.getSsid());
        params.put("enctype_g", guest.getEncryptionType());
        params.put("key_g", guest.getKey());
        params.put("time", String.valueOf(guest.getTime()));
        return params;
    }

    /**
     * Get the required set of parameters to enable/disable NAS functions.
     *
     * @param nas NAS settings to apply.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getNasParams(NasSettings nas) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_NAS");
        params.put("samba", nas.isSambaEnabled() ? "1" : "0");
        params.put("torrent", nas.isTorrentEnabled() ? "1" : "0");
        params.put("dlna", nas.isDlnaEnabled() ? "1" : "0");
        params.put("webaxs", nas.isWebAccessEnabled() ? "1" : "0");
        params.put("nascomname", nas.getName());
        return params;
    }

    /**
     * Get the required set of parameters to enable/disable QOS control mode.
     *
     * @param on Whether to turn off/on the QOS control mode.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getQosParams(boolean on) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_QOS");
        params.put("val", on ? "ON" : "OFF");
        return params;
    }

    /**
     * Get the required set of parameters to set QOS policy.
     *
     * @param policy The QOS policy to set.
     * @param on     Whether the QOS policy must be turned on or off.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getQosPolicyParams(String policy, boolean on) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_QOS");
        params.put("function", on ? "1" : "0");
        params.put("polycy", policy);
        return params;
    }

    /**
     * Get the required set of parameters to set Parental policy.
     *
     * @param policy The parental policy to set.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getParentalPolicyParams(int policy) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "basic_setting_PARENTAL");
        params.put("polycy", String.valueOf(policy));
        return params;
    }

    /**
     * Get the required set of parameters to enable/disable guest wifi.
     *
     * @param on Whether to set to on or off the flag.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getGuestEnabledParams(boolean on) {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_GUEST");
        params.put("val", on ? "ON" : "OFF");
        return params;
    }

    /**
     * Get the required set of parameters to perform a Detect NAS operation.
     *
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getDetectNasParams() {
        Map<String, String> params = new HashMap<>();
        params.put("el", "button_NAS_redetect");
        return params;
    }

    /**
     * Return a {@link JSONArray} with the DHCP reservation table.
     *
     * @param response The {@link Response} containing the DHCP reservation table.
     * @return A {@link JSONArray} with the DHCP reservation table.
     * @throws AirStationException Whenever something goes wrong getting or parsing the response.
     */
    public JSONArray toDhcpEntries(Response response) throws IOException {
        Document doc = responseToDocument(response);
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
     * Get the required set of parameters to perform a DHCP entry edit.
     *
     * @param dev The {@link NetworkDevice} to edit reservation table.
     * @return The required parameters as a {@link Map}.
     */
    public Map<String, String> getDhcpEntryParams(NetworkDevice dev) {
        Map<String, String> params = new HashMap<>();
        String id = String.valueOf(dev.getId());
        params.put("manip" + id, dev.getIpAddress());
        params.put("manmac" + id, dev.getMacAddress());
        params.put("EDITID", id);
        params.put("DOFIX" + id, "Save");
        params.put("sWebSessionnum", webSessionNum);
        params.put("sWebSessionid", webSessionId);
        return params;
    }

    /**
     * Given a {@link Response} object return a {@link JSONObject} with {"RESULT": "OK"} an {@link AirStationException} if something wrong occurs.
     * In case of failure, the exception message is taken from "errortxt" field of original HTML response.
     *
     * @param response The {@link Response} to parse for success or failure response.
     * @return A {@link JSONObject} with {"RESULT": "OK"}
     * @throws IOException Whenever something goes wrong getting or parsing the response.
     */
    JSONObject toJSONResponse(Response response) throws IOException {
        RequestAdapter.responseToDocument(response);
        return new JSONObject().put("RESULT", "OK");
    }

    /**
     * Whether the adapter handles a logged in session.
     *
     * @return True if session is logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return !Utils.isStringEmpty(webSessionId) && !Utils.isStringEmpty(webSessionNum);
    }

    enum JSONFunction {
        AOSS,
        DEVCTRL,
        DEVICE,
        FUNCTION,
        GUEST,
        ICON,
        LANG,
        NAS,
        PARENTAL,
        QOS,
        SYSTEM,
        WIRELESS,
        WPS,
        BUSY,
        DLNA,
        TORRENT,
        SAMBA,
        EXTENDERMONITOR,
        WEB_AXS
    }
}
