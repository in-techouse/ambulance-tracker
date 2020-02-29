package kc.fyp.ambulance.tracker.model;

import java.io.Serializable;

public class Notification implements Serializable {
    private String id, userId, driverId, caseId, userMessage, driverMessage, date;
    private boolean read;

    public Notification() {
    }

    public Notification(String id, String userId, String driverId, String caseId, String userMessage, String driverMessage, String date, boolean read) {
        this.id = id;
        this.userId = userId;
        this.driverId = driverId;
        this.caseId = caseId;
        this.userMessage = userMessage;
        this.driverMessage = driverMessage;
        this.date = date;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getDriverMessage() {
        return driverMessage;
    }

    public void setDriverMessage(String driverMessage) {
        this.driverMessage = driverMessage;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
