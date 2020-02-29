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

    private DatabaseReference bookingsReference = FirebaseDatabase.getInstance().getReference().child("Case");
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
        Button cancelBooking = findViewById(R.id.cancelBooking);
        Button completeBooking = findViewById(R.id.mark_complete);

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
            googleMap.setMyLocationEnabled(true);
            googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    FusedLocationProviderClient current = LocationServices.getFusedLocationProviderClient(AmbulanceDashboard.this);
                    current.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            getDeviceLocation();
                        }
                    });
                    return true;
                }
            });
            getDeviceLocation();
            listenToBookings();
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
                dialog.setMessage("Oppsss.Your Location Service is off.\n Please turn on your Location and Try again Later");
                dialog.setPositiveButton("Let me On", new DialogInterface.OnClickListener() {
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
                                    String strAddress = "";
                                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
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

    private void listenToBookings() {
        bookingsValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("AmbulanceDashboard", "Bookings value event Listener");
                if (activeBooking == null) {
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        Case userCase = d.getValue(Case.class);
                        if (userCase != null) {
                            Log.e("AmbulanceDashboard", "Bookings value event Listener, booking found with status: " + userCase.getStatus());
                            if (userCase.getStatus().equals("New")) {
//                                showBookingDialog(userCase);
                            } else if (userCase.getStatus().equals("In Progress")) {
                                activeBooking = userCase;
//                                onBookingInProgress();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        bookingsReference.orderByChild("type").equalTo(user.getType()).addValueEventListener(bookingsValueListener);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
//            case R.id.cancelBooking: {
//                Log.e("AmbulanceDashboard", "button clicked");
//                mainsheet.setVisibility(View.GONE);
//                sheetprogress.setVisibility(View.VISIBLE);
//                activeBooking.setStatus("Cancelled");
//                bookingsReference.child(activeBooking.getId()).child("status").setValue(activeBooking.getStatus()).addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.e("AmbulanceDashboard", "Cancelled");
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.e("AmbulanceDashboard", "Cancellation Failed");
//                        helpers.showError(AmbulanceDashboard.this, "something went wrong while cancelling the booking,plz try later");
//                        sheetprogress.setVisibility(View.GONE);
//                        mainsheet.setVisibility(View.VISIBLE);
//                    }
//                });
//                break;
//            }
//            case R.id.mark_complete: {
//                Log.e("AmbulanceDashboard", "button clicked");
//                mainsheet.setVisibility(View.GONE);
//                amountLayout.setVisibility(View.VISIBLE);
//                break;
//            }
//
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
