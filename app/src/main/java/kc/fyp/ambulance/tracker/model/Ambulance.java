package kc.fyp.ambulance.tracker.model;

import java.io.Serializable;

public class Ambulance implements Serializable {
    private String ambulanceModel, driverId, id, registrationNumber;

    public Ambulance() {
    }

    public Ambulance(String ambulanceModel, String driverId, String id, String registrationNumber) {
        this.ambulanceModel = ambulanceModel;
        this.driverId = driverId;
        this.id = id;
        this.registrationNumber = registrationNumber;
    }

    public String getAmbulanceModel() {
        return ambulanceModel;
    }

    public void setAmbulanceModel(String ambulanceModel) {
        this.ambulanceModel = ambulanceModel;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}
