package com.github.ffalcinelli.buffalo.models;

import org.json.JSONObject;

/**
 * Settings from NAS functionality.
 * <p>
 * Created by fabio on 10/03/17.
 */
public class NasSettings implements JSONifiable {

    private boolean sambaEnabled;
    private boolean torrentEnabled;
    private boolean dlnaEnabled;
    private boolean webAccessEnabled;
    private String name;

    public NasSettings() {
    }

    public NasSettings(JSONObject jsonObject) {
        fromJSONObject(jsonObject);
    }

    public boolean isSambaEnabled() {
        return sambaEnabled;
    }

    public void setSambaEnabled(boolean sambaEnabled) {
        this.sambaEnabled = sambaEnabled;
    }

    public boolean isTorrentEnabled() {
        return torrentEnabled;
    }

    public void setTorrentEnabled(boolean torrentEnabled) {
        this.torrentEnabled = torrentEnabled;
    }

    public boolean isDlnaEnabled() {
        return dlnaEnabled;
    }

    public void setDlnaEnabled(boolean dlnaEnabled) {
        this.dlnaEnabled = dlnaEnabled;
    }

    public boolean isWebAccessEnabled() {
        return webAccessEnabled;
    }

    public void setWebAccessEnabled(boolean webAccessEnabled) {
        this.webAccessEnabled = webAccessEnabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NasSettings that = (NasSettings) o;

        if (sambaEnabled != that.sambaEnabled) return false;
        if (torrentEnabled != that.torrentEnabled) return false;
        if (dlnaEnabled != that.dlnaEnabled) return false;
        if (webAccessEnabled != that.webAccessEnabled) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = (sambaEnabled ? 1 : 0);
        result = 31 * result + (torrentEnabled ? 1 : 0);
        result = 31 * result + (dlnaEnabled ? 1 : 0);
        result = 31 * result + (webAccessEnabled ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NasSettings" + toJSONObject().toString(4);
    }

    @Override
    public JSONObject toJSONObject() {
        return new JSONObject()
                .put("SAMBA", isSambaEnabled())
                .put("TORRENT", isTorrentEnabled())
                .put("DLNA", isDlnaEnabled())
                .put("WEBAXS", isWebAccessEnabled())
                .put("NASCOMNAME", getName());
    }

    @Override
    public void fromJSONObject(JSONObject jsonObject) {
        setSambaEnabled(jsonObject.optBoolean("SAMBA", isSambaEnabled()));
        setTorrentEnabled(jsonObject.optBoolean("TORRENT", isTorrentEnabled()));
        setDlnaEnabled(jsonObject.optBoolean("DLNA", isDlnaEnabled()));
        setWebAccessEnabled(jsonObject.optBoolean("WEBAXS", isWebAccessEnabled()));
        setName(jsonObject.optString("NASCOMNAME", getName()));
    }
}
