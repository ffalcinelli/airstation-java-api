package com.github.ffalcinelli.buffalo.models;

import org.json.JSONObject;

/**
 * Settings about WIFI modules.
 * <p>
 * Created by fabio on 10/03/17.
 */
public class WifiSettings implements JSONifiable {

    private String ssid;
    private String key;
    private String encryptionType;
    private int channel;
    private int bw;
    private boolean enabled;
    private int time = -1;

    public WifiSettings() {
    }

    public WifiSettings(JSONObject jsonObject) {
        fromJSONObject(jsonObject);
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(String encryptionType) {
        this.encryptionType = encryptionType;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getBw() {
        return bw;
    }

    public void setBw(int bw) {
        this.bw = bw;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WifiSettings that = (WifiSettings) o;

        if (channel != that.channel) return false;
        if (bw != that.bw) return false;
        if (enabled != that.enabled) return false;
        if (time != that.time) return false;
        if (ssid != null ? !ssid.equals(that.ssid) : that.ssid != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return encryptionType != null ? encryptionType.equals(that.encryptionType) : that.encryptionType == null;
    }

    @Override
    public int hashCode() {
        int result = ssid != null ? ssid.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (encryptionType != null ? encryptionType.hashCode() : 0);
        result = 31 * result + channel;
        result = 31 * result + bw;
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + time;
        return result;
    }

    @Override
    public String toString() {
        return "WifiSettings" + toJSONObject().toString(4);
    }

    @Override
    public JSONObject toJSONObject() {
        return new JSONObject()
                .put("SSID", getSsid())
                .put("KEY", getKey())
                .put("ENCTYPE", getEncryptionType())
                .put("CH", getChannel())
                .put("BW", getBw())
                .put("FUNC", isEnabled())
                .put("TIME", getTime());
    }

    @Override
    public void fromJSONObject(JSONObject jsonObject) {
        setSsid(jsonObject.optString("SSID", getSsid()));
        setKey(jsonObject.optString("KEY", getKey()));
        setEncryptionType(jsonObject.optString("ENCTYPE", getEncryptionType()));
        setChannel(jsonObject.optInt("CH", getChannel()));
        setBw(jsonObject.optInt("BW", getBw()));
        setEnabled(jsonObject.optBoolean("FUNC", isEnabled()));
        setTime(jsonObject.optInt("TIME", getTime()));
    }
}
