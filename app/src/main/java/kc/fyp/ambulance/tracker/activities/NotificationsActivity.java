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
    private NotificationAdapter notificationAdapter; // Attach RecyclerView and Data to display all the items.
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Cases");
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private ValueEventListener notificationListener, bookingListener, userListener;
    private String orderBy;
    // Case Detail Bottom Sheet Variables
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

        // Bottom Sheet initialization
        LinearLayout layoutBottomSheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setPeekHeight(0);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        Button closeSheet = findViewById(R.id.closeSheet);
        closeSheet.setOnClickListener(this);
        sheetProgress = findViewById(R.id.sheetProgress); // Loading bar, will be displayed until the data is not loaded.
        mainSheet = findViewById(R.id.mainSheet); // Will be displayed when data is loaded.

        image = findViewById(R.id.image);
        about = findViewById(R.id.about);
        name = findViewById(R.id.name);
        date = findViewById(R.id.date);
        type = findViewById(R.id.type);
        status = findViewById(R.id.status);
        totalCharge = findViewById(R.id.totalCharge);
        address = findViewById(R.id.address);
        contact = findViewById(R.id.contact);

        // Check the user is customer or ambulance driver.
        if (user.getType() == 0) { // it's customer.
            orderBy = "userId";
        } else { // It's Driver
            orderBy = "driverId";
        }

        loadNotification();
    }

    private void loadNotification() {
        loading.setVisibility(View.VISIBLE);
        noRecord.setVisibility(View.GONE);
        notifications.setVisibility(View.GONE);

        notificationListener = new ValueEventListener() {
            // Fetch data success function
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Data.clear(); // Remove data, to avoid duplication
                for (DataSnapshot d : dataSnapshot.getChildren()) {
                    Notification n = d.getValue(Notification.class);
                    if (n != null) {
                        Data.add(n);
                    }
                }
                Collections.reverse(Data); // Reverse the data list, to display the latest notifcation on top.

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
            // Fetch data failure function
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                loading.setVisibility(View.GONE);
                noRecord.setVisibility(View.VISIBLE);
                notifications.setVisibility(View.GONE);
            }
        };

        reference
                .orderByChild(orderBy) // Telling about the column, userId or driverId
                .equalTo(user.getPhone()) // Telling about the value,
                .addValueEventListener(notificationListener);
    }

    public void showBottomSheet(final Notification notification) {
        sheetBehavior.setHideable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Display the sheet.
        sheetProgress.setVisibility(View.VISIBLE); // Loading bar inside the bottom sheet, will be visible until the user data is not loaded.
        mainSheet.setVisibility(View.GONE); // The main view, holding all the views to display data.

        // Check the user is customer or ambulance driver.
        if (user.getType() == 0) { // It's customer
            about.setText("Ambulance detail");
        } else {
            about.setText("Customer detail");
        }

        // Loading detail of case.
        bookingListener = new ValueEventListener() {
            // Data fetch success function.
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Remove listener.
                bookingReference.child(notification.getCaseId()).removeEventListener(bookingListener);
                bookingReference.removeEventListener(bookingListener);
                final Case booking = dataSnapshot.getValue(Case.class);
                if (booking != null) {
                    // Display data of case
                    sheetProgress.setVisibility(View.GONE);
                    mainSheet.setVisibility(View.VISIBLE);
                    date.setText(booking.getDate());
                    address.setText(booking.getAddress());
                    totalCharge.setText(booking.getAmountCharged() + " RS.");
                    type.setText(booking.getType());
                    status.setText(booking.getStatus());

                    // Get the detail of customer or ambulance driver, using the userId or driverId.
                    userListener = new ValueEventListener() {
                        // Data fetch success function
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // Remove database listener.
                            if (user.getType() == 0) {
                                userReference.child(booking.getDriverId()).removeEventListener(userListener);
                            } else {
                                userReference.child(booking.getUserId()).removeEventListener(userListener);
                            }
                            userReference.removeEventListener(userListener);
                            User tempUser = dataSnapshot.getValue(User.class);
                            if (tempUser != null) {
                                // Display user data.
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

                        // Data fetch failure function
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Remove database listener.
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
                    // Attach listener to fetch data.
                    if (user.getType() == 0) {
                        userReference.child(booking.getDriverId()).addValueEventListener(userListener);
                    } else {
                        userReference.child(booking.getUserId()).addValueEventListener(userListener);
                    }
                }
            }

            // Data fetch failure function.
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Remove listener.
                bookingReference.child(notification.getCaseId()).removeEventListener(bookingListener);
                bookingReference.removeEventListener(bookingListener);
                // Loading bar inside the bottom sheet, hide it, data loading failed.
                sheetProgress.setVisibility(View.GONE);
                // The main view, holding all the views to display data.
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
