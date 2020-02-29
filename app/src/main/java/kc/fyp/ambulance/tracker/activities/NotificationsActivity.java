package kc.fyp.ambulance.tracker.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.adapters.NotificationAdapter;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.Case;
import kc.fyp.ambulance.tracker.model.Notification;
import kc.fyp.ambulance.tracker.model.User;

public class NotificationsActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout loading;
    private TextView noRecord;
    private RecyclerView notifications;
    private User user;
    private List<Notification> Data;
    private NotificationAdapter notificationAdapter;
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Cases");
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private ValueEventListener notificationListener, bookingListener, userListener;
    private String orderBy;
    private BottomSheetBehavior sheetBehavior;
    private ProgressBar sheetProgress;
    private LinearLayout mainSheet;
    private CircleImageView image;
    private TextView about, name, date, type, status, totalCharge, address, contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        loading = findViewById(R.id.loading);
        noRecord = findViewById(R.id.noRecord);
        notifications = findViewById(R.id.notifiations);
        Session session = new Session(NotificationsActivity.this);
        user = session.getUser();
        Data = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(user.getType(), NotificationsActivity.this);
        notifications.setLayoutManager(new LinearLayoutManager(NotificationsActivity.this));
        notifications.setAdapter(notificationAdapter);

        LinearLayout layoutBottomSheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setPeekHeight(0);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        Button closeSheet = findViewById(R.id.closeSheet);
        closeSheet.setOnClickListener(this);
        sheetProgress = findViewById(R.id.sheetProgress);
        mainSheet = findViewById(R.id.mainSheet);

        image = findViewById(R.id.image);
        about = findViewById(R.id.about);
        name = findViewById(R.id.name);
        date = findViewById(R.id.date);
        type = findViewById(R.id.type);
        status = findViewById(R.id.status);
        totalCharge = findViewById(R.id.totalCharge);
        address = findViewById(R.id.address);
        contact = findViewById(R.id.contact);


        if (user.getType() == 0) {
            orderBy = "userId";
        } else {
            orderBy = "driverId";
        }

        loadNotification();
    }

    private void loadNotification() {
        loading.setVisibility(View.VISIBLE);
        noRecord.setVisibility(View.GONE);
        notifications.setVisibility(View.GONE);
        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Data.clear();
                for (DataSnapshot d : dataSnapshot.getChildren()) {
                    Notification n = d.getValue(Notification.class);
                    if (n != null) {
                        Data.add(n);
                    }
                }
                Collections.reverse(Data);

                if (Data.size() > 0) {
                    notifications.setVisibility(View.VISIBLE);
                    noRecord.setVisibility(View.GONE);
                } else {
                    noRecord.setVisibility(View.VISIBLE);
                    notifications.setVisibility(View.GONE);

                }

                loading.setVisibility(View.GONE);
                notificationAdapter.setData(Data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                loading.setVisibility(View.GONE);
                noRecord.setVisibility(View.VISIBLE);
                notifications.setVisibility(View.GONE);
            }
        };
        reference.orderByChild(orderBy).equalTo(user.getPhone()).addValueEventListener(notificationListener);

    }

    public void showBottomSheet(final Notification notification) {
        sheetBehavior.setHideable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        sheetProgress.setVisibility(View.VISIBLE);
        mainSheet.setVisibility(View.GONE);

        if (user.getType() == 0) {
            about.setText("Ambulance detail");
        } else {
            about.setText("Customer detail");
        }

        bookingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookingReference.child(notification.getCaseId()).removeEventListener(bookingListener);
                bookingReference.removeEventListener(bookingListener);
                final Case booking = dataSnapshot.getValue(Case.class);
                if (booking != null) {
                    sheetProgress.setVisibility(View.GONE);
                    mainSheet.setVisibility(View.VISIBLE);
                    date.setText(booking.getDate());
                    address.setText(booking.getAddress());
                    totalCharge.setText(booking.getAmountCharged() + " RS.");
                    type.setText(booking.getType());
                    status.setText(booking.getStatus());

                    userListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (user.getType() == 0) {
                                userReference.child(booking.getDriverId()).removeEventListener(userListener);
                            } else {
                                userReference.child(booking.getUserId()).removeEventListener(userListener);
                            }
                            userReference.removeEventListener(userListener);
                            User tempUser = dataSnapshot.getValue(User.class);
                            if (tempUser != null) {
                                if (tempUser.getImage() != null && tempUser.getImage().length() > 0) {
                                    Glide.with(NotificationsActivity.this).load(tempUser.getImage()).into(image);
                                } else {
                                    image.setImageDrawable(getResources().getDrawable(R.drawable.user));
                                }
                                String strName = tempUser.getFirstName() + " " + tempUser.getLastName();
                                name.setText(strName);
                                contact.setText(tempUser.getPhone());
                                sheetProgress.setVisibility(View.GONE);
                                mainSheet.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            if (user.getType() == 0) {
                                userReference.child(booking.getDriverId()).removeEventListener(userListener);
                            } else {
                                userReference.child(booking.getUserId()).removeEventListener(userListener);
                            }
                            userReference.removeEventListener(userListener);
                            sheetProgress.setVisibility(View.GONE);
                            mainSheet.setVisibility(View.VISIBLE);
                        }
                    };
                    if (user.getType() == 0) {
                        userReference.child(booking.getDriverId()).addValueEventListener(userListener);
                    } else {
                        userReference.child(booking.getUserId()).addValueEventListener(userListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                bookingReference.child(notification.getCaseId()).removeEventListener(bookingListener);
                bookingReference.removeEventListener(bookingListener);
                sheetProgress.setVisibility(View.GONE);
                mainSheet.setVisibility(View.VISIBLE);
            }
        };
        bookingReference.child(notification.getCaseId()).addValueEventListener(bookingListener);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.closeSheet: {
                sheetBehavior.setHideable(true);
                sheetBehavior.setPeekHeight(0);
                sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setHideable(true);
            sheetBehavior.setPeekHeight(0);
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else
            finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null) {
            bookingReference.removeEventListener(bookingListener);
        }
        if (userListener != null) {
            userReference.removeEventListener(userListener);
        }

        if (notificationListener != null) {
            reference.removeEventListener(notificationListener);
        }
    }
}
