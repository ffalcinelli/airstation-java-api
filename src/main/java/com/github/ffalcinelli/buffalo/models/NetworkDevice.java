package com.github.ffalcinelli.buffalo.models;

import org.json.JSONObject;

/**
 * Informations about connected device.
 * <p>
 * Created by fabio on 10/03/17.
 */
public class NetworkDevice implements JSONifiable {
    private int id;
    private String name = "Unknown";
    private String ipAddress;
    private String macAddress;
    private String img = "Unknown";
    private int qos;
    private boolean parentalEnabled;
    private boolean disconnected;
    private long leaseTime;

    public NetworkDevice() {
    }

    public NetworkDevice(JSONObject jsonObject) {
        fromJSONObject(jsonObject);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public boolean isParentalEnabled() {
        return parentalEnabled;
    }

    public void setParentalEnabled(boolean parentalEnabled) {
        this.parentalEnabled = parentalEnabled;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getLeaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(long leaseTime) {
        this.leaseTime = leaseTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkDevice that = (NetworkDevice) o;

        if (id != that.id) return false;
        if (qos != that.qos) return false;
        if (parentalEnabled != that.parentalEnabled) return false;
        if (disconnected != that.disconnected) return false;
        if (leaseTime != that.leaseTime) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
        if (macAddress != null ? !macAddress.equals(that.macAddress) : that.macAddress != null) return false;
        return img != null ? img.equals(that.img) : that.img == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + (macAddress != null ? macAddress.hashCode() : 0);
        result = 31 * result + (img != null ? img.hashCode() : 0);
        result = 31 * result + qos;
        result = 31 * result + (parentalEnabled ? 1 : 0);
        result = 31 * result + (disconnected ? 1 : 0);
        result = 31 * result + (int) (leaseTime ^ (leaseTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "NetworkDevice" + toJSONObject().toString(4);
    }

    @Override
    public JSONObject toJSONObject() {
        return new JSONObject()
                .put("ID", getId())
                .put("NAME", getName())
                .put("IMG", getImg())
                .put("IP", getIpAddress())
                .put("MAC", getMacAddress())
                .put("LEASE", getLeaseTime())
                .put("QOS", getQos())
                .put("PARENTAL", isParentalEnabled())
                .put("DISCONNECT", isDisconnected());
    }

    @Override
    public void fromJSONObject(JSONObject jsonObject) {
        setId(jsonObject.optInt("ID", getId()));
        setName(jsonObject.optString("NAME", getName()));
        setImg(jsonObject.optString("IMG", getImg()));
        setIpAddress(jsonObject.optString("IP", getIpAddress()));
        setMacAddress(jsonObject.optString("MAC", getMacAddress()));
        setLeaseTime(jsonObject.optLong("LEASE", getLeaseTime()));
        setQos(jsonObject.optInt("QOS", getQos()));
        setParentalEnabled(jsonObject.optBoolean("PARENTAL", isParentalEnabled()));
        setDisconnected(jsonObject.optBoolean("DISCONNECT", isDisconnected()));

    }
}
