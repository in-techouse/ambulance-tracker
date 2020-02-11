package kc.fyp.ambulance.tracker.director;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
import kc.fyp.ambulance.tracker.model.User;

public class Session {
    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public Session(Context c){
        context = c;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
        gson = new Gson();
    }

    public void setSession(User user){
        String value = gson.toJson(user);
        editor.putString("user", value);
        editor.commit();
    }

    public void destroySession(){
        editor.remove("user");
        editor.commit();
    }

    public User getUser(){
        User user = new User();
        try{

            String value = preferences.getString("user", "*");

            if(value.equals("*")){
                user = null;
            }
            else{
                user = gson.fromJson(value, User.class);
            }
        }
        catch (Exception e){
            user = null;
        }
        return user;
    }
}
