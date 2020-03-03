package kc.fyp.ambulance.tracker.director;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import com.shreyaspatil.MaterialDialog.MaterialDialog;

import kc.fyp.ambulance.tracker.R;

public class Helpers {

    public boolean isConnected(Context context) {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;
        return connected;
    }

    public void showNoInternetError(Activity activity) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .setTitle(Constants.ERROR)
                .setMessage(Constants.ERROR_NO_INTERNET)
                .setCancelable(false)
                .setPositiveButton(Constants.MESSAGES_OKAY, R.drawable.ic_okay, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(Constants.MESSAGE_CLOSE, R.drawable.ic_close, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .build();
        // Show Dialog
        dialog.show();
    }

    public void showError(Activity activity, String message) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .setTitle(Constants.ERROR)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(Constants.MESSAGES_OKAY, R.drawable.ic_okay, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(Constants.MESSAGE_CLOSE, R.drawable.ic_close, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .build();
        // Show Dialog
        dialog.show();
    }

    public void showCollectionDialog(Activity activity, String message) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .setTitle("AMOUNT TO BE COLLECTED")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(Constants.MESSAGES_OKAY, R.drawable.ic_okay, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(Constants.MESSAGE_CLOSE, R.drawable.ic_close, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .build();
        // Show Dialog
        dialog.show();
    }

    public void showNotification(Activity activity, String text, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, "1");
        builder.setTicker(text);
        builder.setAutoCancel(true);
        builder.setChannelId("1");
        builder.setContentInfo(text);
        builder.setContentTitle(text);
        builder.setContentText(message);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        builder.build();
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(10, builder.build());
        }
    }

    public double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        long factor = (long) Math.pow(10, 3);
        dist = dist * factor;
        double temp = Math.round(dist);
        return (temp / factor);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}
