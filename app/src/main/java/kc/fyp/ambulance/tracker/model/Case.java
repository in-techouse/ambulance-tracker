package kc.fyp.ambulance.tracker.model;

import java.io.Serializable;

public class Case implements Serializable {
    private String userId, driverId, status, type, date, id, address;
    private double latitude, longitude;
    private int amountCharged;

    public Case() {
    }

    public Case(String userId, String driverId, String status, String type, String date, String id, String address, double latitude, double longitude, int amountCharged) {
        this.userId = userId;
        this.driverId = driverId;
        this.status = status;
        this.type = type;
        this.date = date;
        this.id = id;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.amountCharged = amountCharged;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getAmountCharged() {
        return amountCharged;
    }

    public void setAmountCharged(int amountCharged) {
        this.amountCharged = amountCharged;
    }
}
