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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.shreyaspatil.MaterialDialog.MaterialDialog;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Constants;
import kc.fyp.ambulance.tracker.director.Helpers;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.Ambulance;
import kc.fyp.ambulance.tracker.model.Case;
import kc.fyp.ambulance.tracker.model.User;

public class AmbulanceDashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private DatabaseReference bookingsReference = FirebaseDatabase.getInstance().getReference().child("Cases");
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private ValueEventListener bookingsValueListener, bookingValueListener, userValueListener;
    private MapView map;
    private Helpers helpers;
    private GoogleMap googleMap;
    private DrawerLayout drawer;
    private User user, activeCustomer;
    private FusedLocationProviderClient locationProviderClient;
    private Marker marker, customerMarker;
    private LinearLayout amountLayout;
    private BottomSheetBehavior sheetbehavoior;
    private ProgressBar sheetprogress;
    private RelativeLayout mainsheet;
    private Case activeBooking;
    private TextView locationAddress, profileName, profileEmail, profilePhone, providerName, providerPhone, bookingAddress, bookingDate;
    private CircleImageView providerImage;
    private EditText totalCharge;
    private Session session;
    private boolean isStarted = false;
    private Button cancelBooking, completeBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance_dashboard);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayout layoutBottomSheet = findViewById(R.id.bottom_sheet);
        sheetbehavoior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetbehavoior.setHideable(true);
        sheetbehavoior.setPeekHeight(0);
        sheetbehavoior.setState(BottomSheetBehavior.STATE_HIDDEN);
        sheetprogress = findViewById(R.id.sheetProgress);
        mainsheet = findViewById(R.id.mainSheet);
        amountLayout = findViewById(R.id.amountLayout);
        providerImage = findViewById(R.id.providerImage);
        providerName = findViewById(R.id.providerName);
        providerPhone = findViewById(R.id.providerPhone);
        RelativeLayout callMe = findViewById(R.id.callMe);
        bookingAddress = findViewById(R.id.bookingAddress);
        bookingDate = findViewById(R.id.bookingDate);
        cancelBooking = findViewById(R.id.cancelBooking);
        completeBooking = findViewById(R.id.mark_complete);

        totalCharge = findViewById(R.id.totalCharge);
        Button amountSubmit = findViewById(R.id.amountSubmit);

        cancelBooking.setOnClickListener(this);
        completeBooking.setOnClickListener(this);
        amountSubmit.setOnClickListener(this);
        callMe.setOnClickListener(this);


        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        session = new Session(AmbulanceDashboard.this);
        user = session.getUser();
        helpers = new Helpers();
        locationProviderClient = LocationServices.getFusedLocationProviderClient(AmbulanceDashboard.this);

        // Drawer Header code.
        View header = navigationView.getHeaderView(0);
        TextView profile_email = header.findViewById(R.id.profile_email);
        TextView profile_name = header.findViewById(R.id.profile_name);
        CircleImageView profile_image = header.findViewById(R.id.profile_image);
        TextView profile_phone = header.findViewById(R.id.profile_phone);
        TextView ambulanceModel = header.findViewById(R.id.profile_type);
        TextView ambulanceRegistrationNumber = header.findViewById(R.id.profile_experience);
        String name = user.getFirstName() + " " + user.getLastName();
        profile_name.setText(name);
        profile_email.setText(user.getEmail());
        profile_phone.setText(user.getPhone());

        Ambulance ambulance = session.getAmbulance();
        if (ambulance != null) {
            Log.e("AmbulanceDashboard", "Ambulance is not null");
            Log.e("AmbulanceDashboard", "Ambulance Registration Number: " + ambulance.getRegistrationNumber());
            ambulanceRegistrationNumber.setText(ambulance.getRegistrationNumber());
            ambulanceModel.setText(ambulance.getAmbulanceModel());
        }
        if (user.getImage() != null && user.getImage().length() > 0) {
            Glide.with(getApplicationContext()).load(user.getImage()).into(profile_image);
        }

        locationAddress = findViewById(R.id.locationAddress);

        // Initialize Map View
        map = findViewById(R.id.map);
        map.onCreate(savedInstanceState);
        try {
            MapsInitializer.initialize(AmbulanceDashboard.this);
            map.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap gM) {
                    Log.e("AmbulanceDashboard", "Call back received");

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
            helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    private boolean askForPermission() {
        if (ActivityCompat.checkSelfPermission(AmbulanceDashboard.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(AmbulanceDashboard.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AmbulanceDashboard.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            return false;
        }
        return true;
    }

    public void enableLocation() {
        if (askForPermission()) {
            googleMap.setMyLocationEnabled(true);  // Show Location button
            // Location button click listener
            googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    FusedLocationProviderClient current = LocationServices.getFusedLocationProviderClient(AmbulanceDashboard.this);
                    current.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            getDeviceLocation();  // Get user location on button pressed
                        }
                    });
                    return true;
                }
            });
            getDeviceLocation(); // Get user location on application start
            listenToBookings(); // Listen to bookings, and show the driver a dialog, if a user post a new booking/case
        }
    }

    private void getDeviceLocation() {
        Log.e("AmbulanceDashboard", "Call received to get device location");
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);

            }
            if (!gps_enabled && !network_enabled) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(AmbulanceDashboard.this);
                dialog.setMessage("Oppsss.Your Location Service is off.\nPlease turn on your Location and Try again Later");
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
                            googleMap.clear();
                            LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                            marker = googleMap.addMarker(new MarkerOptions().position(me).title("You're Here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 11));
                            Geocoder geocoder = new Geocoder(AmbulanceDashboard.this);
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
                                helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                            }
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                }
            });
        } catch (Exception e) {
            helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        Log.e("MenuItem", "" + id);
        switch (id) {
            case R.id.nav_home: {
                break;
            }
            case R.id.nav_booking: {
                Intent it = new Intent(AmbulanceDashboard.this, CasesActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_notification: {
                Intent it = new Intent(AmbulanceDashboard.this, NotificationsActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_userProfile: {
                Intent it = new Intent(AmbulanceDashboard.this, EditUserProfile.class);
                startActivity(it);
                break;
            }
            case R.id.nav_logout: {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                Session session = new Session(AmbulanceDashboard.this);
                auth.signOut();
                session.destroySession();
                Intent it = new Intent(AmbulanceDashboard.this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(it);
                finish();
                break;
            }
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateUserLocation(double lat, double lng) {
        Log.e("AmbulanceDashboard", "Lat: " + lat + " Lng: " + lng);
        user.setLatitude(lat);
        user.setLongitude(lng);
        session.setSession(user);
        userReference.child(user.getPhone()).setValue(user);
    }

    // Listen to bookings, and show the driver a dialog, if a user post a new booking/case
    private void listenToBookings() {
        Log.e("AmbulanceDashboard", "Bookings value event Listener registered");
        bookingsValueListener = new ValueEventListener() {
            // Fetch data success function
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("AmbulanceDashboard", "Bookings value event Listener");
                if (activeBooking == null) {
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        Case userCase = d.getValue(Case.class);
                        if (userCase != null) {
                            Log.e("AmbulanceDashboard", "Bookings value event Listener, booking found with status: " + userCase.getStatus());
                            if (userCase.getStatus().equals("New")) { // If the status of case is new, that's a new case/booking. show the driver a dialog
                                showBookingDialog(userCase); // Show the driver a dialog
                            } else if (userCase.getStatus().equals("In Progress") || userCase.getStatus().equals("Started")) { // Handle, if the driver already has an active booking
                                activeBooking = userCase;
                                onBookingInProgress(); // Show the bottom sheet and customer details
                            }
                        }
                    }
                }
            }
            // Fetch data failure function
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        bookingsReference.addValueEventListener(bookingsValueListener);
    }

    private void showBookingDialog(final Case userCase) {
        helpers.showNotification(AmbulanceDashboard.this, "New Case", "We have a new case for you. It's time to help someone.");

        final MaterialDialog dialog = new MaterialDialog.Builder(AmbulanceDashboard.this)
                .setTitle("NEW CASE")
                .setMessage("We have a new case for you. It's time to help someone.")
                .setCancelable(false)
                .setPositiveButton("DETAILS", R.drawable.ic_okay, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        Intent it = new Intent(AmbulanceDashboard.this, ShowCaseDetail.class);
                        // To pass data to new activity.
                        Bundle bundle = new Bundle();
                        // Passing data to activity
                        bundle.putSerializable("case", userCase); // Usecase is the data object
                        // Passing bundle to the intent
                        it.putExtras(bundle);
                        startActivity(it);
                    }
                })
                .setNegativeButton("REJECT", R.drawable.ic_close, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .build();
        dialog.show();
    }

    private void onBookingInProgress() {
        bookingsReference.removeEventListener(bookingsValueListener);
        sheetbehavoior.setHideable(false);
        sheetbehavoior.setPeekHeight(220);
        sheetbehavoior.setState(BottomSheetBehavior.STATE_EXPANDED);
        sheetprogress.setVisibility(View.VISIBLE);
        mainsheet.setVisibility(View.GONE);
        amountLayout.setVisibility(View.GONE);
        listenToBookingChanges();

        userValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userReference.removeEventListener(userValueListener);
                Log.e("AmbulanceDashboard", "User value event listener called SnapShot: " + dataSnapshot.toString());
                sheetprogress.setVisibility(View.GONE);
                mainsheet.setVisibility(View.VISIBLE);
                activeCustomer = dataSnapshot.getValue(User.class);
                if (activeCustomer != null && activeBooking != null) {
                    if (activeCustomer.getImage() != null && activeCustomer.getImage().length() > 0) {
                        Glide.with(AmbulanceDashboard.this).load(activeCustomer.getImage()).into(providerImage);
                    }
                    providerName.setText(activeCustomer.getFirstName() + " " + activeCustomer.getLastName());
                    providerPhone.setText(activeCustomer.getPhone());
                    bookingAddress.setText(activeBooking.getAddress());
                    bookingDate.setText(activeBooking.getDate());

                    if (customerMarker != null) {
                        customerMarker.remove();
                    }
                    LatLng latLng = new LatLng(activeCustomer.getLatitude(), activeCustomer.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(activeCustomer.getFirstName());
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    customerMarker = googleMap.addMarker(markerOptions);
                    customerMarker.showInfoWindow();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                userReference.removeEventListener(userValueListener);
                Log.e("AmbulanceDashboard", "User value event listener called");
                sheetprogress.setVisibility(View.GONE);
                mainsheet.setVisibility(View.VISIBLE);
            }
        };

        userReference.child(activeBooking.getUserId()).addValueEventListener(userValueListener);

    }

    private void listenToBookingChanges() {
        bookingValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("AmbulanceDashboard", "Booking Value Listener");
                Case booking = dataSnapshot.getValue(Case.class);
                if (activeBooking != null && booking != null) {
                    activeBooking = booking;
                    if (activeBooking != null && activeBooking.getStatus() != null) {
                        switch (activeBooking.getStatus()) {
                            case "Started":
                                if (!isStarted)
                                    calculateFare();
                                isStarted = true;
                                cancelBooking.setVisibility(View.GONE);
                                completeBooking.setText("MARK COMPLETE");

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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        bookingValueListener = bookingsReference.child(activeBooking.getId()).addValueEventListener(bookingValueListener);
    }

    private void calculateFare() {
        if (activeBooking != null && !activeBooking.getStatus().equals("Started")) {
            return;
        }
        new CountDownTimer(12000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.e("AmbulanceDashboard", "calculateFare, OnTick");
            }

            @Override
            public void onFinish() {
                if (activeBooking != null && activeBooking.getStatus().equals("Started")) {
                    activeBooking.setAmountCharged(activeBooking.getAmountCharged() + 17);
                    bookingsReference.child(activeBooking.getId()).child("amountCharged").setValue(activeBooking.getAmountCharged());
                    calculateFare();
                }
            }
        }.start();
    }

    private void onBookingCancelled() {
        forBothCancelledAndCompleted();
    }

    private void onBookingCompleted() {
        forBothCancelledAndCompleted();
    }

    private void forBothCancelledAndCompleted() {
        if (activeBooking.getAmountCharged() > 0) {
            helpers.showCollectionDialog(AmbulanceDashboard.this, "Total amount to be collected from " + activeCustomer.getFirstName() + " is " + activeBooking.getAmountCharged() + " RS.");
        }
        sheetprogress.setVisibility(View.VISIBLE);
        mainsheet.setVisibility(View.GONE);
        if (userValueListener != null) {
            userReference.removeEventListener(userValueListener);
        }
        if (bookingValueListener != null) {
            bookingsReference.addValueEventListener(bookingValueListener);
        }
        if (customerMarker != null) {
            customerMarker.remove();
        }
        sheetprogress.setVisibility(View.GONE);
        mainsheet.setVisibility(View.VISIBLE);
        sheetbehavoior.setHideable(true);
        sheetbehavoior.setState(BottomSheetBehavior.STATE_HIDDEN);
        activeBooking = null;
        listenToBookings();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.cancelBooking: {
                Log.e("AmbulanceDashboard", "button clicked");
                mainsheet.setVisibility(View.GONE);
                sheetprogress.setVisibility(View.VISIBLE);
                activeBooking.setStatus("Cancelled");
                bookingsReference.child(activeBooking.getId()).child("status").setValue(activeBooking.getStatus()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("AmbulanceDashboard", "Cancelled");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("AmbulanceDashboard", "Cancellation Failed");
                        helpers.showError(AmbulanceDashboard.this, "something went wrong while cancelling the booking,plz try later");
                        sheetprogress.setVisibility(View.GONE);
                        mainsheet.setVisibility(View.VISIBLE);
                    }
                });
                break;
            }
            case R.id.mark_complete: {
                Log.e("AmbulanceDashboard", "button clicked");
                if (isStarted) {
                    // Mark Complete
                    activeBooking.setStatus("Completed");
                    bookingsReference.child(activeBooking.getId()).child("status").setValue(activeBooking.getStatus()).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            isStarted = false;
                            Log.e("AmbulanceDashboard", "Cancelled");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("AmbulanceDashboard", "Cancellation Failed");
                            helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                        }
                    });
                } else {
                    // Start the ride
                    activeBooking.setStatus("Started");
                    bookingsReference.child(activeBooking.getId()).child("status").setValue(activeBooking.getStatus()).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.e("AmbulanceDashboard", "Cancelled");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("AmbulanceDashboard", "Cancellation Failed");
                            helpers.showError(AmbulanceDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                        }
                    });
                }
//                mainsheet.setVisibility(View.GONE);
//                amountLayout.setVisibility(View.VISIBLE);
                break;
            }
//            case R.id.amountSubmit: {
//                String strTotalCharge = totalCharge.getText().toString();
//                if (strTotalCharge.length() < 1) {
//                    totalCharge.setError("Enter some valid amount.");
//                    return;
//                }
//                int amount = 0;
//                try {
//                    amount = Integer.parseInt(strTotalCharge);
//                } catch (Exception e) {
//                    totalCharge.setError("Enter some valid amount.");
//                    return;
//                }
//
//                mainsheet.setVisibility(View.GONE);
//                amountLayout.setVisibility(View.GONE);
//                sheetprogress.setVisibility(View.VISIBLE);
//                activeBooking.setStatus("Completed");
//                activeBooking.setAmountCharged(amount);
//                totalCharge.setText("");
//                bookingsReference.child(activeBooking.getId()).setValue(activeBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.e("AmbulanceDashboard", "Completed");
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.e("AmbulanceDashboard", "Complete");
//                        sheetprogress.setVisibility(View.GONE);
//                        mainsheet.setVisibility(View.VISIBLE);
//                    }
//                });
//            }
            case R.id.callMe: {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + activeCustomer.getPhone()));
                startActivity(intent);
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
        if (bookingsValueListener != null) {
            bookingsReference.removeEventListener(bookingsValueListener);
        }
        if (bookingValueListener != null) {
            bookingsReference.removeEventListener(bookingValueListener);
        }

        if (userValueListener != null) {
            userReference.removeEventListener(userValueListener);
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
