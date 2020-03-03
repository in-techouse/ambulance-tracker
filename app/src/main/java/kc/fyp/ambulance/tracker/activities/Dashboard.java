package kc.fyp.ambulance.tracker.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Constants;
import kc.fyp.ambulance.tracker.director.Helpers;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.Ambulance;
import kc.fyp.ambulance.tracker.model.Case;
import kc.fyp.ambulance.tracker.model.Notification;
import kc.fyp.ambulance.tracker.model.User;

public class Dashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Cases");
    private DatabaseReference ambulanceReference = FirebaseDatabase.getInstance().getReference().child("Ambulances");
    private ValueEventListener providerValueListener, bookingValueListener, bookingsValueListener, providerDetailValueListener, ambulanceValueListener;
    private MapView map;
    private Helpers helpers;
    private Session session;
    private GoogleMap googleMap;
    private DrawerLayout drawer;
    private User user;
    private ImageView providerImage;
    private TextView providerName, ambulanceModel, ambulanceRegistration, providerPhone, bookingAddress, bookingDate, locationAddress, caseTypeTxt;
    private FusedLocationProviderClient locationProviderClient;
    private Marker marker, activeProviderMarker;
    private LinearLayout searching, caseLayout;
    private CardView confirmCard;
    private ProgressBar sheetProgress;
    private RelativeLayout mainSheet;
    private Case activeBooking;
    private User activeProvider;
    private BottomSheetBehavior sheetBehavior;
    private Spinner caseType;
    private CountDownTimer timer;
    private Button cancelBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayout layoutBottomSheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        sheetProgress = findViewById(R.id.sheetProgress);
        mainSheet = findViewById(R.id.mainSheet);
        providerImage = findViewById(R.id.providerImage);
        providerName = findViewById(R.id.providerName);
        ambulanceModel = findViewById(R.id.ambulanceModel);
        ambulanceRegistration = findViewById(R.id.ambulanceRegistration);
        providerPhone = findViewById(R.id.providerPhone);
        bookingAddress = findViewById(R.id.bookingAddress);
        bookingDate = findViewById(R.id.bookingDate);
        cancelBooking = findViewById(R.id.cancelBooking);
        caseTypeTxt = findViewById(R.id.caseTypeTxt);
        RelativeLayout callMe = findViewById(R.id.callMe);
        caseType = findViewById(R.id.caseType);
        caseLayout = findViewById(R.id.caseLayout);
        cancelBooking.setOnClickListener(this);
        callMe.setOnClickListener(this);


        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        List<User> users = new ArrayList<>();


        Button confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener(this);
        searching = findViewById(R.id.searching);
        confirmCard = findViewById(R.id.confirmCard);
        session = new Session(Dashboard.this);
        user = session.getUser();
        helpers = new Helpers();
        locationProviderClient = LocationServices.getFusedLocationProviderClient(Dashboard.this);


        View header = navigationView.getHeaderView(0);
        TextView profile_email = header.findViewById(R.id.profile_email);
        TextView profile_name = header.findViewById(R.id.profile_name);
        CircleImageView profile_image = header.findViewById(R.id.profile_image);
        TextView profile_phone = header.findViewById(R.id.profile_phone);

        String name = user.getFirstName() + " " + user.getLastName();
        profile_name.setText(name);
        profile_email.setText(user.getEmail());
        profile_phone.setText(user.getPhone());
        if (user.getImage() != null && user.getImage().length() > 0) {
            Glide.with(Dashboard.this).load(user.getImage()).into(profile_image);
        }

        locationAddress = findViewById(R.id.locationAddress);


        map = findViewById(R.id.map);
        map.onCreate(savedInstanceState);
        try {
            MapsInitializer.initialize(Dashboard.this);
            map.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap gM) {
                    Log.e("Maps", "Call back received");

                    View locationButton = ((View) map.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
                    RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                    // position on right bottom
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                    rlp.setMargins(0, 350, 100, 0);

                    googleMap = gM;
                    LatLng defaultPosition = new LatLng(31.5204, 74.3487);
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(defaultPosition).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    enableLocation();
                }
            });

        } catch (Exception e) {
            helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    public void enableLocation() {
        boolean flag = hasPermissions(Dashboard.this, PERMISSIONS);
        if (!flag) {
            ActivityCompat.requestPermissions(Dashboard.this, PERMISSIONS, 1);
        } else {
            googleMap.setMyLocationEnabled(true);
            googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    FusedLocationProviderClient current = LocationServices.getFusedLocationProviderClient(Dashboard.this);
                    current.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            getDeviceLocation();
                        }
                    });
                    return true;
                }
            });
            getDeviceLocation();
            getAllAmbulances();
            listenToBookingsChanges();
        }
    }

    private void getDeviceLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);

            }
            if (!gps_enabled && !network_enabled) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(Dashboard.this);
                dialog.setMessage("Oppsss.Your Location Service is off.\n Please turn on your Location and Try again Later");
                dialog.setPositiveButton("Turn On", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);

                    }
                });
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                dialog.show();
                return;
            }

            locationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        Location location = task.getResult();
                        if (location != null) {
                            if (marker != null)
                                marker.remove();
//                            googleMap.clear();
                            LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                            marker = googleMap.addMarker(new MarkerOptions().position(me).title("You're Here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 11));
                            Geocoder geocoder = new Geocoder(Dashboard.this);
                            List<Address> addresses = null;
                            try {
                                addresses = geocoder.getFromLocation(me.latitude, me.longitude, 1);
                                if (addresses != null && addresses.size() > 0) {
                                    Address address = addresses.get(0);
                                    String strAddress = address.getAddressLine(0);
                                    for (int i = 1; i <= address.getMaxAddressLineIndex(); i++) {
                                        strAddress = strAddress + " " + address.getAddressLine(i);
                                    }
                                    locationAddress.setText(strAddress);
                                    updateUserLocation(me.latitude, me.longitude);
                                }
                            } catch (Exception exception) {
                                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                            }
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                }
            });
        } catch (Exception e) {
            helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    private void updateUserLocation(double lat, double lng) {
        user.setLatitude(lat);
        user.setLongitude(lng);
        session.setSession(user);
        userReference.child(user.getPhone()).setValue(user);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            enableLocation();
        }
    }

    private boolean hasPermissions(Context c, String... permission) {
        for (String p : permission) {
            if (ActivityCompat.checkSelfPermission(c, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.confirm: {
                if (!helpers.isConnected(Dashboard.this)) {
                    helpers.showNoInternetError(Dashboard.this);
                    return;
                }
                if (caseType.getSelectedItemPosition() == 0) {
                    helpers.showError(Dashboard.this, "Select your case type first.");
                    return;
                }
                postCase();
                break;
            }
            case R.id.cancelBooking: {
                Log.e("Dashboard", "Cancel button clicked");
                mainSheet.setVisibility(View.GONE);
                sheetProgress.setVisibility(View.VISIBLE);
                activeBooking.setStatus("Cancelled");
                bookingReference.child(activeBooking.getId()).child("status").setValue(activeBooking.getStatus()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("Dashboard", "Booking Cancelled");
                        sendCancelledNotification();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Dashboard", "Booking Cancellation Failed");
                        helpers.showError(Dashboard.this, "something went wrong while cancelling the booking,plz try later");
                        sheetProgress.setVisibility(View.GONE);
                        mainSheet.setVisibility(View.VISIBLE);
                    }
                });
                break;
            }
            case R.id.callMe: {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + activeProvider.getPhone()));
                startActivity(intent);
                break;
            }
        }
    }

    private void sendCancelledNotification() {
        DatabaseReference notificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
        Notification notification = new Notification();
        String id = notificationReference.push().getKey();
        notification.setId(id);
        notification.setCaseId(activeBooking.getId());
        notification.setUserId(activeBooking.getUserId());
        notification.setDriverId(activeBooking.getDriverId());
        notification.setRead(false);
        Date d = new Date();
        String date = new SimpleDateFormat("EEE dd, MMM, yyyy HH:mm").format(d);
        notification.setDate(date);
        notification.setUserMessage("You cancelled your case with " + activeProvider.getFirstName() + " " + activeProvider.getLastName());
        notification.setDriverMessage("Your case has been cancelled by " + user.getFirstName() + " " + user.getLastName());
        notificationReference.child(notification.getId()).setValue(notification);
    }

    private void postCase() {
        searching.setVisibility(View.VISIBLE);
        caseLayout.setVisibility(View.GONE);
        DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Cases");
        String key = bookingReference.push().getKey();
        activeBooking = new Case();
        activeBooking.setId(key);
        activeBooking.setUserId(user.getPhone());
        Date d = new Date();
        String date = new SimpleDateFormat("EEE dd, MMM, yyyy HH:mm").format(d);
        activeBooking.setDate(date);
        activeBooking.setLatitude(marker.getPosition().latitude);
        activeBooking.setLongitude(marker.getPosition().longitude);
        activeBooking.setStatus("New");
        activeBooking.setType(caseType.getSelectedItem().toString());
        activeBooking.setDriverId("");
        activeBooking.setAmountCharged(0);
        activeBooking.setAddress(locationAddress.getText().toString());
        bookingReference.child(activeBooking.getId()).setValue(activeBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                startTimer();
                listenToBookingChanges();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                searching.setVisibility(View.GONE);
                caseLayout.setVisibility(View.VISIBLE);
                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
        });
    }

    private void startTimer() {
        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.e("Dashboard", "Time is ticking, booking status: " + activeBooking.getStatus());
            }

            @Override
            public void onFinish() {
                Log.e("Dashboard", "Time is finished, booking status: " + activeBooking.getStatus());
                if (activeBooking.getStatus().equals("New")) {
                    markBookingReject();
                } else if (activeBooking.getStatus().equals("In Progress")) {
                    onBookingInProgress();
                }
            }
        };
        timer.start();
    }

    private void markBookingReject() {
        activeBooking.setStatus("Rejected");
        DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Cases");
        bookingReference.child(activeBooking.getId()).setValue(activeBooking);
        searching.setVisibility(View.GONE);
        caseLayout.setVisibility(View.VISIBLE);
        helpers.showError(Dashboard.this, "No ambulance available currently.\nPlease try again later.");
        activeBooking = null;
    }

    private void getAllAmbulances() {
        providerValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                googleMap.clear();
                if (marker != null) {
                    marker = googleMap.addMarker(new MarkerOptions().position(marker.getPosition()).title("You're Here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 11));
                }
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    User u = data.getValue(User.class);
                    if (u != null) {
                        LatLng user_location = new LatLng(u.getLatitude(), u.getLongitude());
                        MarkerOptions markerOptions = new MarkerOptions().position(user_location).title(u.getFirstName());
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ambulance_64));
                        Marker marker = googleMap.addMarker(markerOptions);
                        marker.showInfoWindow();
                        marker.setTag(u);
                        Log.e("Dashboard", "Name: " + u.getFirstName() + " Lat: " + u.getLatitude() + " Lng: " + u.getLongitude());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        userReference.orderByChild("type").equalTo(1).addValueEventListener(providerValueListener);
    }

    private void listenToBookingsChanges() {
        bookingsValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookingReference.removeEventListener(bookingsValueListener);
                Log.e("Dashboard", "Bookings Value Event Listener");
                if (activeBooking == null) {
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        Case userCase = d.getValue(Case.class);
                        if (userCase != null) {
                            Log.e("Dashboard", "Cases Value Event Listener, Case found with status: " + userCase.getStatus());
                            if (userCase.getStatus().equals("In Progress") || userCase.getStatus().equals("Started")) {
                                activeBooking = userCase;
                                listenToBookingChanges();
                                onBookingInProgress();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                bookingReference.removeEventListener(bookingsValueListener);
            }
        };

        bookingReference.orderByChild("userId").equalTo(user.getPhone()).addValueEventListener(bookingsValueListener);
    }

    private void listenToBookingChanges() {
        bookingValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("Dashboard", "Booking Value Listener");
                Case userCase = dataSnapshot.getValue(Case.class);
                if (userCase != null) {
                    activeBooking = userCase;
                    switch (activeBooking.getStatus()) {
                        case "In Progress":
                            onBookingInProgress();
                            break;
                        case "Started":
                            cancelBooking.setVisibility(View.GONE);

                            break;
                        case "Cancelled":
                            onBookingCancelled();
                            break;
                        case "Completed":
                            onBookingCompleted();
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        bookingValueListener = bookingReference.child(activeBooking.getId()).addValueEventListener(bookingValueListener);
    }


    private void onBookingInProgress() {
        if (timer != null) {
            timer.cancel();
        }
        googleMap.clear();
        marker = googleMap.addMarker(new MarkerOptions().position(marker.getPosition()).title("You're Here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 11));
        if (providerValueListener != null) {
            userReference.removeEventListener(providerValueListener);
            Log.e("Dashboard", "Provider value event listener removed");
        }
        searching.setVisibility(View.GONE);
        caseLayout.setVisibility(View.VISIBLE);
        confirmCard.setVisibility(View.GONE);
        sheetBehavior.setHideable(false);
        sheetBehavior.setPeekHeight(220);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        sheetProgress.setVisibility(View.VISIBLE);
        mainSheet.setVisibility(View.GONE);

        providerDetailValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                sheetProgress.setVisibility(View.GONE);
                mainSheet.setVisibility(View.VISIBLE);
                userReference.removeEventListener(providerDetailValueListener);
                Log.e("Dashboard", "Provider value event listener called SnapShot: " + dataSnapshot.toString());
                activeProvider = dataSnapshot.getValue(User.class);
                if (activeProvider != null) {
                    if (activeProvider.getImage() != null && activeProvider.getImage().length() > 0) {
                        Glide.with(Dashboard.this).load(activeProvider.getImage()).into(providerImage);
                    }
                    providerName.setText(activeProvider.getFirstName() + " " + activeProvider.getLastName());
                    providerPhone.setText(activeProvider.getPhone());
                    bookingDate.setText(activeBooking.getDate());
                    caseTypeTxt.setText(activeBooking.getType());
                    bookingAddress.setText(activeBooking.getAddress());
                    if (activeProviderMarker != null) {
                        activeProviderMarker.remove();
                    }
                    LatLng latLng = new LatLng(activeProvider.getLatitude(), activeProvider.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(activeProvider.getFirstName());
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ambulance_64));
                    activeProviderMarker = googleMap.addMarker(markerOptions);
                    activeProviderMarker.showInfoWindow();
                    ambulanceValueListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                Log.e("Dashboard", "Ambulances: " + data.toString());
                                Ambulance ambulance = data.getValue(Ambulance.class);
                                if (ambulance != null && ambulance.getDriverId().equals(activeProvider.getPhone())) {
                                    ambulanceModel.setText(ambulance.getAmbulanceModel());
                                    ambulanceRegistration.setText(ambulance.getRegistrationNumber());
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };

                    ambulanceReference.addValueEventListener(ambulanceValueListener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                userReference.removeEventListener(providerDetailValueListener);
                Log.e("Dashboard", "Provider value event listener called");
                sheetProgress.setVisibility(View.GONE);
                mainSheet.setVisibility(View.VISIBLE);
            }
        };

        userReference.child(activeBooking.getDriverId()).addValueEventListener(providerDetailValueListener);
    }

    private void forBothCancelledAndCompleted() {
        if (providerDetailValueListener != null) {
            userReference.removeEventListener(providerValueListener);
        }
        if (bookingValueListener != null) {
            bookingReference.removeEventListener(bookingValueListener);
        }
        if (activeProviderMarker != null) {
            activeProviderMarker.remove();
        }
        sheetProgress.setVisibility(View.GONE);
        mainSheet.setVisibility(View.VISIBLE);
        sheetBehavior.setHideable(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        confirmCard.setVisibility(View.VISIBLE);
        listenToBookingsChanges();
        getAllAmbulances();
    }

    private void onBookingCancelled() {
        forBothCancelledAndCompleted();
    }


    private void onBookingCompleted() {
        forBothCancelledAndCompleted();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.e("MenuItem", "" + id);
        switch (id) {
            case R.id.nav_home: {
                break;
            }
            case R.id.nav_booking: {
                Intent it = new Intent(Dashboard.this, CasesActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_notification: {
                Intent it = new Intent(Dashboard.this, NotificationsActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_userProfile: {
                Intent it = new Intent(Dashboard.this, EditUserProfile.class);
                startActivity(it);
                break;
            }
            case R.id.nav_logout: {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                Session session = new Session(Dashboard.this);
                auth.signOut();
                session.destroySession();
                Intent it = new Intent(Dashboard.this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(it);
                finish();
                break;
            }
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
        if (providerValueListener != null) {
            userReference.removeEventListener(providerValueListener);
        }
        if (providerDetailValueListener != null) {
            userReference.removeEventListener(providerDetailValueListener);
        }
        if (bookingValueListener != null) {
            bookingReference.removeEventListener(bookingValueListener);
        }

        if (bookingsValueListener != null) {
            bookingReference.removeEventListener(bookingsValueListener);
        }

        if (ambulanceValueListener != null) {
            ambulanceReference.removeEventListener(ambulanceValueListener);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
