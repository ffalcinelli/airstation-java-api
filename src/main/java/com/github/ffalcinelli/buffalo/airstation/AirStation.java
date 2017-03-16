package com.github.ffalcinelli.buffalo.airstation;

import com.github.ffalcinelli.buffalo.models.NasSettings;
import com.github.ffalcinelli.buffalo.models.NetworkDevice;
import com.github.ffalcinelli.buffalo.models.WifiSettings;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import static com.github.ffalcinelli.buffalo.airstation.RequestAdapter.*;
import static com.github.ffalcinelli.buffalo.airstation.RequestAdapter.JSONFunction.*;
import static com.github.ffalcinelli.buffalo.utils.Utils.closeIgnoreException;
import static com.github.ffalcinelli.buffalo.utils.Utils.getStringOrDefault;

/**
 * AirStation handles the connection to an AirStation device.
 * <p>
 * Provides a Java API for the functions exposed by the devices.
 * <p>
 * Created by fabio on 24/02/17.
 */
public class AirStation implements Closeable {

    private JSONObject settings;
    private OkHttpClient client;
    private RequestAdapter adapter;

    public AirStation(String url) {
        this(url, getDefaultCookieJar());
    }

    public AirStation(String url, CookieJar cookieJar) {
        this(new JSONObject().put("url", url), cookieJar);
    }

    public AirStation(JSONObject settings) {
        this(settings, null);
    }

    public AirStation(JSONObject settings, CookieJar cookieJar) {
        this.adapter = new RequestAdapter(
                getStringOrDefault(settings, "url", DEFAULT_URL),
                getStringOrDefault(settings, "encoding", DEFAULT_ENCODING)
        );
        this.settings = settings;
        this.client = new OkHttpClient.Builder()
                .cookieJar(cookieJar != null ? cookieJar : RequestAdapter.getDefaultCookieJar())
                .build();

    }

    /**
     * Get configuration settings as a {@link JSONObject}.
     *
     * @return The {@link JSONObject} representing the current settings.
     */
    public JSONObject getSettings() {
        return settings;
    }

    /**
     * Get the AirStation {@link RequestAdapter} in use.
     *
     * @return The Airstation {@link RequestAdapter} in use.
     */
    public RequestAdapter getAdapter() {
        return adapter;
    }

    /**
     * Call the `get_json_param` device function.
     *
     * @param param The JSON param to retrieve.
     * @return A {@link JSONObject} with the data.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getJSONParam(final JSONFunction param) throws IOException {
        if (!adapter.isLoggedIn())
            throw new IllegalStateException("You must be logged in to perform this request.");
        return new JSONObject(client.newCall(adapter.getJSONParamRequest(param.name())).execute().body().string());
    }

    /**
     * Tell the device to perform an action upon the given parameters.
     *
     * @param params The parameters map.
     * @return A {@link JSONObject} containing the device response. Usually a {"RESULT": "OK"} response.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject set(final Map<String, String> params) throws IOException {
        if (!adapter.isLoggedIn())
            throw new IllegalStateException("You must be logged in to perform this request.");
        String response = client.newCall(adapter.getSETRequest(params)).execute().body().string();
        return new JSONObject().put("RESULT", response);
    }

    /**
     * Call the `get_json_param` device function.
     * Asynchronous version of {@link #getJSONParam(RequestAdapter.JSONFunction)} method.
     *
     * @param param    The JSON param to retrieve.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getJSONParam(final JSONFunction param, final AsyncCallback<JSONObject> callback) {
        if (!adapter.isLoggedIn())
            callback.onFailure(new IllegalStateException("You must be logged in to perform this request."));
        else {
            client.newCall(adapter.getJSONParamRequest(param.name())).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    callback.onSuccess(new JSONObject(response.body().string()));
                }
            });
        }
    }

    /**
     * Tell the device to perform an action upon the given parameters.
     * Asynchronous version of {@link #set(Map) set} method.
     *
     * @param params   The parameters map.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void set(final Map<String, String> params, final AsyncCallback<JSONObject> callback) {
        if (!adapter.isLoggedIn())
            callback.onFailure(new IllegalStateException("You must be logged in to perform this request."));
        else {
            try {
                client.newCall(adapter.getSETRequest(params)).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onFailure(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        callback.onSuccess(new JSONObject().put("RESULT", response.body().string()));
                    }
                });
            } catch (UnsupportedEncodingException e) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * Open a session to the device by logging in.
     *
     * @param username The username (usually "admin").
     * @param password The password (if it's not been changed set it to "password").
     * @return a {@link JSONObject} {"RESULT": "OK"} if all went fine, {"RESULT": "FAIL", "REASON": "..."} otherwise.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject login(final String username, final String password) throws IOException {
        if (!adapter.isLoggedIn()) {
            Response response = client.newCall(
                    adapter.doLoginFromHomeResponse(username, password, client.newCall(
                            adapter.getHomeRequest()
                    ).execute())
            ).execute();
            return adapter.toJSONResponse(response, true);
        }
        //TODO: check this up if it's really logged in
        return new JSONObject().put("RESULT", "OK");

    }

    /**
     * Open a session to the device by logging in.
     * Asynchronous version of {@link #login(String, String)} method.
     *
     * @param username The username (usually "admin").
     * @param password The password (if it's not been changed set it to "password").
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void login(final String username, final String password, final AsyncCallback<JSONObject> callback) {
        client.newCall(adapter.getHomeRequest()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Request request = adapter.doLoginFromHomeResponse(username, password, response);
                if (request != null) {
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            callback.onFailure(e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            //TODO: handle invalid username and password. This method should return a JSONObject
                            callback.onSuccess(adapter.toJSONResponse(response, true));
                        }
                    });
                } else {
                    callback.onSuccess(new JSONObject().put("RESULT", "OK"));
                }
            }
        });
    }

    /**
     * Close the session.
     *
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    @Override
    public void close() throws IOException {
        client.newCall(adapter.getLogoutRequest()).execute();
        adapter.close();
    }

    /**
     * Close the session.
     * Asynchronous version of {@link #close()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void close(final AsyncCallback<JSONObject> callback) {
        client.newCall(adapter.getLogoutRequest()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
                closeIgnoreException(adapter);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onSuccess(adapter.toJSONResponse(response));
                closeIgnoreException(adapter);
            }
        });
    }

    /**
     * Get informations about internet status.
     *
     * @return A {@link JSONObject} with the internet status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getIcon() throws IOException {
        return getJSONParam(ICON);
    }

    /**
     * Get informations about internet status.
     * Asynchronous version of {@link #getIcon()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getIcon(AsyncCallback<JSONObject> callback) {
        getJSONParam(ICON, callback);
    }

    /**
     * Get informations about connected devices.
     *
     * @return A {@link JSONObject} with the connected devices informations.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getDevCtrl() throws IOException {
        return getJSONParam(DEVCTRL);
    }

    /**
     * Get informations about connected devices.
     * Asynchronous version of {@link #getDevCtrl()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getDevCtrl(AsyncCallback<JSONObject> callback) {
        getJSONParam(DEVCTRL, callback);
    }

    /**
     * Get informations about the AirStation status.
     *
     * @return A {@link JSONObject} with the AirStation status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getDevice() throws IOException {
        return getJSONParam(DEVICE);
    }

    /**
     * Get informations about the AirStation status.
     * Asynchronous version of {@link #getDevice()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getDevice(AsyncCallback<JSONObject> callback) {
        getJSONParam(DEVICE, callback);
    }

    /**
     * Get AOSS function status.
     *
     * @return A {@link JSONObject} with the AOSS status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getAoss() throws IOException {
        return getJSONParam(AOSS);
    }

    /**
     * Get AOSS function status.
     * Asycnhronous version of {@link #getAoss()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getAoss(AsyncCallback<JSONObject> callback) {
        getJSONParam(AOSS, callback);
    }

    /**
     * Get the wireless settings.
     *
     * @return A {@link JSONObject} with the wireless settings.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getWireless() throws IOException {
        return getJSONParam(WIRELESS);
    }

    /**
     * Get the wireless settings.
     * Asynchronous version of {@link #getWireless()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getWireless(AsyncCallback<JSONObject> callback) {
        getJSONParam(WIRELESS, callback);
    }

    /**
     * Get WPS status.
     *
     * @return A {@link JSONObject} with the WPS status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getWps() throws IOException {
        return getJSONParam(WPS);
    }

    /**
     * Get WPS status.
     * Asynchronous version of {@link #getWps()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getWps(AsyncCallback<JSONObject> callback) {
        getJSONParam(WPS, callback);
    }

    /**
     * Get NAS status.
     *
     * @return A {@link JSONObject} with the NAS status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getNas() throws IOException {
        return getJSONParam(NAS);
    }

    /**
     * Get NAS status.
     * Asynchronous version of {@link #getNas()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getNas(AsyncCallback<JSONObject> callback) {
        getJSONParam(NAS, callback);
    }


    /**
     * Get guest wireless settings.
     *
     * @return A {@link JSONObject} with the guest wireless settings
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getGuest() throws IOException {
        return getJSONParam(GUEST);
    }

    /**
     * Get guest wireless settings.
     * Asynchronous version of {@link #getGuest()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getGuest(AsyncCallback<JSONObject> callback) {
        getJSONParam(GUEST, callback);
    }

    /**
     * Get QOS settings.
     *
     * @return A {@link JSONObject} with the QOS settings.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getQos() throws IOException {
        return getJSONParam(QOS);
    }

    /**
     * Get QOS settings.
     * Asynchronous version of {@link #getQos()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getQos(AsyncCallback<JSONObject> callback) {
        getJSONParam(QOS, callback);
    }

    /**
     * Get Parental settings.
     *
     * @return A {@link JSONObject} with the parental settings.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getParental() throws IOException {
        return getJSONParam(PARENTAL);
    }

    /**
     * Get Parental settings.
     * Asynchronous version of {@link #getParental()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getParental(AsyncCallback<JSONObject> callback) {
        getJSONParam(PARENTAL, callback);
    }

    /**
     * Get System Wide settings.
     *
     * @return A {@link JSONObject} with the system settings.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getSystem() throws IOException {
        return getJSONParam(SYSTEM);
    }

    /**
     * Get System Wide settings.
     * Asynchronous version of {@link #getSystem()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getSystem(AsyncCallback<JSONObject> callback) {
        getJSONParam(SYSTEM, callback);
    }

    /**
     * Get language settings.
     *
     * @return A {@link JSONObject} with the language settings.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getLang() throws IOException {
        return getJSONParam(LANG);
    }

    /**
     * Get language settings.
     * Asynchronous version of {@link #getLang()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getLang(AsyncCallback<JSONObject> callback) {
        getJSONParam(LANG, callback);
    }


    /**
     * Get device busy status.
     *
     * @return A {@link JSONObject} with the device busy status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getBusy() throws IOException {
        return getJSONParam(BUSY);
    }

    /**
     * Get device busy status.
     * Asynchronous version of {@link #getBusy()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getBusy(AsyncCallback<JSONObject> callback) {
        getJSONParam(BUSY, callback);
    }

    /**
     * Get Extender monitor function status.
     *
     * @return A {@link JSONObject} with the extender monitor status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getExtenderMonitor() throws IOException {
        return getJSONParam(EXTENDERMONITOR);
    }

    /**
     * Get Extender monitor function status.
     * Asynchronous version of {@link #getExtenderMonitor()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getExtenderMonitor(AsyncCallback<JSONObject> callback) {
        getJSONParam(EXTENDERMONITOR, callback);
    }

    /**
     * Get DLNA function status.
     *
     * @return A {@link JSONObject} with DLNA status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getDlna() throws IOException {
        return getJSONParam(DLNA);
    }

    /**
     * Get DLNA function status.
     * Asynchronous version of {@link #getDlna()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getDlna(AsyncCallback<JSONObject> callback) {
        getJSONParam(DLNA, callback);
    }

    /**
     * Get Bit Torrent function status.
     *
     * @return A {@link JSONObject} with Bit Torrent status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getTorrent() throws IOException {
        return getJSONParam(TORRENT);
    }

    /**
     * Get Bit Torrent function status.
     * Asynchronous version of {@link #getTorrent()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getTorrent(AsyncCallback<JSONObject> callback) {
        getJSONParam(TORRENT, callback);
    }

    /**
     * Get Web Access function status.
     *
     * @return A {@link JSONObject} with the Web Access status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getWebAccess() throws IOException {
        return getJSONParam(WEB_AXS);
    }

    /**
     * Get Web Access function status.
     * Asynchronous version of {@link #getWebAccess()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getWebAccess(AsyncCallback<JSONObject> callback) {
        getJSONParam(WEB_AXS, callback);
    }

    /**
     * Get SAMBA function status.
     *
     * @return A {@link JSONObject} with the SAMBA status.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject getSamba() throws IOException {
        return getJSONParam(SAMBA);
    }

    /**
     * Get SAMBA function status.
     * Asynchronous version of {@link #getSamba()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getSamba(AsyncCallback<JSONObject> callback) {
        getJSONParam(SAMBA, callback);
    }

    /**
     * Wake on Lan the given MAC address sending magic packets from the AirStation device.
     *
     * @param macAddress The device MAC address to send magic packet to.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject wol(String macAddress) throws IOException {
        return set(adapter.getWolParams(macAddress));
    }

    /**
     * Wake on Lan the given MAC address sending magic packets from the AirStation device.
     * Asynchronous version of {@link #wol(String)} method.
     *
     * @param macAddress The device MAC address to send magic packet to.
     * @param callback   The {@link AsyncCallback} to use either on success or failure events.
     */
    public void wol(String macAddress, AsyncCallback<JSONObject> callback) {
        set(adapter.getWolParams(macAddress), callback);
    }


    /**
     * Put AirStation in AOSS mode.
     *
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject aoss() throws IOException {
        return set(adapter.getAossParams());
    }

    /**
     * Put AirStation in AOSS mode.
     * Asynchronous version of {@link #aoss()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void aoss(AsyncCallback<JSONObject> callback) {
        set(adapter.getAossParams(), callback);
    }


    /**
     * Update informations about a connected device.
     *
     * @param dev The device info to update.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject updateDevCtrl(NetworkDevice dev) throws IOException {
        return set(adapter.getDevCtrlParams(dev));
    }

    /**
     * Update informations about a connected device.
     * Asynchronous version of {@link #updateDevCtrl(NetworkDevice)} method.
     *
     * @param dev      The device info to update.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void updateDevCtrl(NetworkDevice dev,
                              AsyncCallback<JSONObject> callback) {
        set(adapter.getDevCtrlParams(dev), callback);
    }

    /**
     * Update the basic wifi basic settings.
     *
     * @param a Settings for wifi A
     * @param g Settings for wifi G
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject wirelessBasicSetup(WifiSettings a, WifiSettings g) throws IOException {
        return set(adapter.getWifiParams(a, g));
    }

    /**
     * Update the basic wifi basic settings.
     * Asynchronous version of {@link #wirelessBasicSetup(WifiSettings, WifiSettings)} method.
     *
     * @param a        Settings for wifi A
     * @param g        Settings for wifi G
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void wirelessBasicSetup(WifiSettings a, WifiSettings g, AsyncCallback<JSONObject> callback) {
        set(adapter.getWifiParams(a, g), callback);
    }


    /**
     * Update guest wifi basic settings.
     *
     * @param guest Guest wifi settings.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject guestBasicSetup(WifiSettings guest) throws IOException {
        return set(adapter.getGuestParams(guest));
    }

    /**
     * Update guest wifi basic settings.
     * Asynchronous version of {@link #guestBasicSetup(WifiSettings)} method.
     *
     * @param guest    Guest wifi settings.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void guestBasicSetup(WifiSettings guest, AsyncCallback<JSONObject> callback) {
        set(adapter.getGuestParams(guest), callback);
    }


    /**
     * Update NAS basic settings.
     *
     * @param nas NAS function settings.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject nasBasicSetup(NasSettings nas) throws IOException {
        return set(adapter.getNasParams(nas));
    }

    /**
     * Update NAS basic settings.
     * Asynchronous version of {@link #nasBasicSetup(NasSettings)} method.
     *
     * @param nas      NAS function settings.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void nasBasicSetup(NasSettings nas, AsyncCallback<JSONObject> callback) {
        set(adapter.getNasParams(nas), callback);
    }

    /**
     * Enable/Disable QOS function mode.
     *
     * @param on If must be enabled or disabled.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject qos(boolean on) throws IOException {
        return set(adapter.getQosParams(on));
    }

    /**
     * Enable/Disable QOS function mode.
     * Asynchronous version of {@link #qos(boolean)} method.
     *
     * @param on       If must be enabled or disabled.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void qos(boolean on, AsyncCallback<JSONObject> callback) {
        set(adapter.getQosParams(on), callback);
    }

    /**
     * Put device in given QOS policy mode if flag enabled is true.
     *
     * @param policy  The policy to use.
     * @param enabled Whether to enable or disable the mode.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject setQosPolicy(String policy, boolean enabled) throws IOException {
        return set(adapter.getQosPolicyParams(policy, enabled));
    }

    /**
     * Put device in given QOS policy mode if flag enabled is true.
     * Asynchronous version of {@link #setQosPolicy(String, boolean)} method.
     *
     * @param policy   The policy to use.
     * @param on       Whether to enable or disable the mode.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void setQosPolicy(String policy, boolean on, AsyncCallback<JSONObject> callback) {
        set(adapter.getQosPolicyParams(policy, on), callback);
    }

    /**
     * Setup the parental policy.
     *
     * @param policy The parental policy.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject setParentalPolicy(int policy) throws IOException {
        return set(adapter.getParentalPolicyParams(policy));
    }

    /**
     * Setup the parental policy.
     * Asynchronous version of {@link #setParentalPolicy(int)} method.
     *
     * @param policy   The parental policy.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void setParentalPolicy(int policy, AsyncCallback<JSONObject> callback) {
        set(adapter.getParentalPolicyParams(policy), callback);
    }

    /**
     * Enable/Disable guest wifi access mode.
     *
     * @param on Whether to enable or disable the guest wifi.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject guest(boolean on) throws IOException {
        return set(adapter.getGuestEnabledParams(on));
    }

    /**
     * Enable/Disable guest wifi access mode.
     * Asynchronous version of {@link #guest(boolean)} method.
     *
     * @param on       Whether to enable or disable the guest wifi.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void guest(boolean on, AsyncCallback<JSONObject> callback) {
        set(adapter.getGuestEnabledParams(on), callback);
    }

    /**
     * Detect attached storage devices.
     *
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject detectNas() throws IOException {
        return set(adapter.getDetectNasParams());
    }

    /**
     * Detect attached storage devices.
     * Asynchronous version of {@link #detectNas()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void detectNas(AsyncCallback<JSONObject> callback) {
        set(adapter.getDetectNasParams(), callback);
    }


    /**
     * Get a {@link JSONArray} of current DHCP reserved address entries.
     *
     * @return The DHCP address entries as a {@link JSONArray}.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONArray getDhcpReservation() throws IOException {
        if (!adapter.isLoggedIn())
            throw new IllegalStateException("You must be logged in to perform this request.");
        return adapter.toDhcpEntries(client.newCall(adapter.getFRMRequest("dhcps_lease.html")).execute());
    }

    /**
     * Get a {@link JSONArray} of current DHCP reserved address entries.
     * Asynchronous version of {@link #getDhcpReservation()} method.
     *
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void getDhcpReservation(final AsyncCallback<JSONArray> callback) {
        if (!adapter.isLoggedIn())
            callback.onFailure(new IllegalStateException("You must be logged in to perform this request."));
        client.newCall(adapter.getFRMRequest("dhcps_lease.html")).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onSuccess(adapter.toDhcpEntries(response));
            }
        });
    }

    /**
     * Edit the DHCP entry.
     *
     * @param dev The entry to edit.
     * @return A {@link JSONObject} with the result: {"RESULT": "OK"} if device received the command.
     * @throws IOException Whenever something goes wrong communicating with the device.
     */
    public JSONObject updateDhcpReservation(NetworkDevice dev) throws IOException {
        return set(adapter.getDhcpEntryParams(dev));
    }

    /**
     * Edit the DHCP entry.
     * Asynchronous version of {@link #updateDhcpReservation(NetworkDevice)} method.
     *
     * @param dev      The entry to edit.
     * @param callback The {@link AsyncCallback} to use either on success or failure events.
     */
    public void updateDhcpReservation(NetworkDevice dev, AsyncCallback<JSONObject> callback) {
        set(adapter.getDhcpEntryParams(dev), callback);
    }
}
