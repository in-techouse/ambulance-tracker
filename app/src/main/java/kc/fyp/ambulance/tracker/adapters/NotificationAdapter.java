package kc.fyp.ambulance.tracker.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.activities.NotificationsActivity;
import kc.fyp.ambulance.tracker.model.Notification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationHolder> {
    private List<Notification> Data;
    private int role;
    private NotificationsActivity notificationActivity;

    public NotificationAdapter(int r, NotificationsActivity n) {
        Data = new ArrayList<>();
        role = r;
        notificationActivity = n;
    }

    public void setData(List<Notification> data) {
        Data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View V = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationHolder(V);

    }

    @Override
    public void onBindViewHolder(@NonNull NotificationHolder holder, int position) {
        final Notification n = Data.get(position);
        if (n != null) {
            Log.e("Notification", "Date: " + n.getDate());
            Log.e("Notification", "User Message: " + n.getUserMessage());
            Log.e("Notification", "Provider Message: " + n.getDriverMessage());
            holder.date.setText(n.getDate());
            if (role == 0)
                holder.message.setText(n.getUserMessage());
            else
                holder.message.setText(n.getDriverMessage());

            holder.mainCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("Notification", "Notification Clicked");
                    notificationActivity.showBottomSheet(n);
                }
            });
        } else {
            Log.e("Notification", "Notification obj is null");
        }

    }

    @Override
    public int getItemCount() {
        return Data.size();

    }

    class NotificationHolder extends RecyclerView.ViewHolder {
        TextView date;
        TextView message;
        CardView mainCard;

        public NotificationHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.Date);
            message = itemView.findViewById(R.id.message);
            mainCard = itemView.findViewById(R.id.mainCard);

        }
    }
}

