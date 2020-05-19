package kc.fyp.ambulance.tracker.director;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import kc.fyp.ambulance.tracker.model.Ambulance;
import kc.fyp.ambulance.tracker.model.User;

public class Session {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public Session(Context c) {
        preferences = PreferenceManager.getDefaultSharedPreferences(c);
        editor = preferences.edit();
        gson = new Gson();
    }

    public void setSession(User user) {
        String value = gson.toJson(user);
        editor.putString("user", value);
        editor.commit();
    }

    public void setAmbulance(Ambulance ambulance) {
        String value = gson.toJson(ambulance);
        editor.putString("ambulance", value);
        editor.commit();
    }

    public void destroySession() {
        editor.remove("user");
        editor.remove("ambulance");
        editor.commit();
    }

    public User getUser() {
        User user = new User();
        try {
            String value = preferences.getString("user", "*");
            if (value.equals("*")) {
                user = null;
            } else {
                user = gson.fromJson(value, User.class);
            }
        } catch (Exception e) {
            user = null;
        }
        return user;
    }

    public Ambulance getAmbulance() {
        Ambulance ambulance = new Ambulance();
        try {
            String value = preferences.getString("ambulance", "*");
            if (value.equals("*")) {
                ambulance = null;
            } else {
                ambulance = gson.fromJson(value, Ambulance.class);
            }
        } catch (Exception e) {
            ambulance = null;
        }
        return ambulance;
    }
}
