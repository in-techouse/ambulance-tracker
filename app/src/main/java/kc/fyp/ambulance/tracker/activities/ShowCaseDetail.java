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
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Constants;
import kc.fyp.ambulance.tracker.director.Helpers;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.Case;
import kc.fyp.ambulance.tracker.model.Notification;
import kc.fyp.ambulance.tracker.model.User;

public class ShowCaseDetail extends AppCompatActivity implements View.OnClickListener {
    private Case userCase;
    private TextView UserName, user_address, travel, YourAddress;
    private MapView map;
    private GoogleMap googleMap;
    private FusedLocationProviderClient locationProviderClient;
    private Helpers helpers;
    private User user, customer;
    private LinearLayout progress, buttons;
    private CircleImageView userImage;
    private DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Cases");
    private DatabaseReference notificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private ValueEventListener bookingListener, userListener;
    private boolean isFirst = true;
    private boolean isUserFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_case_detail);

        Intent it = getIntent();
        if (it == null) { // if the intent box is empty, finish the activity
            Log.e("CaseDetail", "Intent is NULL");
            finish();
            return;
        }

        Bundle b = it.getExtras();
        if (b == null) { // if the bundle box is empty, finish the activity
            Log.e("CaseDetail", "Extra is NULL");
            finish();
            return;
        }

        userCase = (Case) b.getSerializable("case");
        if (userCase == null) { // if the data is empty, finish the activity
            Log.e("CaseDetail", "Case is NULL");
            finish();
            return;
        }

        Log.e("CaseDetail", "Case Id: " + userCase.getId());
        progress = findViewById(R.id.progress);
        buttons = findViewById(R.id.buttons);
        buttons.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        UserName = findViewById(R.id.userName);
        userImage = findViewById(R.id.userImage);
        user_address = findViewById(R.id.address);
        Button reject = findViewById(R.id.REJECT);
        Button accept = findViewById(R.id.ACCEPT);
        map = findViewById(R.id.map);
        travel = findViewById(R.id.Travel);
        YourAddress = findViewById(R.id.your_address);
        accept.setOnClickListener(this);
        reject.setOnClickListener(this);

        Session session = new Session(ShowCaseDetail.this);
        user = session.getUser();
        helpers = new Helpers();

        // Fetch the detail of user, who posted the case.
        loadUserData();

        locationProviderClient = LocationServices.getFusedLocationProviderClient(ShowCaseDetail.this);
        map.onCreate(savedInstanceState);
        try {
            MapsInitializer.initialize(ShowCaseDetail.this);
            map.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap gM) {
                    Log.e("CaseDetail", "Call back received");

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
            helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }
    }


    private void loadUserData() {
        userListener = new ValueEventListener() {
            // Data fetch success function
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (userListener != null)
                    userReference.child(userCase.getUserId()).removeEventListener(userListener);
                if (userListener != null)
                    userReference.removeEventListener(userListener);
                if (!isUserFirst) {
                    return;
                }
                isUserFirst = false;
                if (dataSnapshot.getValue() != null) {
                    customer = dataSnapshot.getValue(User.class);
                    if (customer != null) {
                        Log.e("CaseDetail", "Customer is not Null");
                        UserName.setText(customer.getFirstName() + " " + customer.getLastName());
                        buttons.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        if (customer.getImage() != null && customer.getImage().length() > 0) {
                            Glide.with(ShowCaseDetail.this).load(customer.getImage()).into(userImage);
                        }
                    } else {
                        Log.e("CaseDetail", "Customer is Null");
                        UserName.setText(""); // Set empty user name, because no data found.
                        buttons.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                    }
                } else {
                    Log.e("CaseDetail", "DataSnapShot is Null");
                    UserName.setText(""); // Set empty user name, because no data found.
                    buttons.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                }
            }

            // Data fetch failure function
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (userListener != null)
                    userReference.child(userCase.getUserId()).removeEventListener(userListener);
                if (userListener != null)
                    userReference.removeEventListener(userListener);
                buttons.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                UserName.setText(""); // Set empty user name, because no data found.
            }
        };

        userReference.child(userCase.getUserId()).addValueEventListener(userListener);
    }

    private boolean askForPermission() { // Check if app have permission to get the current location.
        if (ActivityCompat.checkSelfPermission(ShowCaseDetail.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ShowCaseDetail.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ShowCaseDetail.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            return false;
        }
        return true;
    }

    public void enableLocation() {
        if (askForPermission()) { // Check if app have permission to get the current location.
            googleMap.setMyLocationEnabled(true);
            googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    FusedLocationProviderClient current = LocationServices.getFusedLocationProviderClient(ShowCaseDetail.this);
                    current.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            getDeviceLocation();
                        }
                    });
                    return true;
                }
            });
            getDeviceLocation(); // To get the user location on activity starts
        }
    }

    private void getDeviceLocation() {
        Log.e("Location", "Call received to get device location");
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);

            }
            if (!gps_enabled && !network_enabled) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(ShowCaseDetail.this);
                dialog.setMessage("Oppsss.Your Location Service is off.\nPlease turn on your Location and Try again Later");
                dialog.setPositiveButton("Let me on", new DialogInterface.OnClickListener() {
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
                            googleMap.addMarker(new MarkerOptions().position(me).title("You're Here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            LatLng customerLocation = new LatLng(userCase.getLatitude(), userCase.getLongitude());
                            googleMap.addMarker(new MarkerOptions().position(customerLocation).title("Customer Is Here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 11));
                            Geocoder geocoder = new Geocoder(ShowCaseDetail.this);
                            List<Address> addresses = null;
                            try {
                                // Get Ambulance Driver Current Address
                                addresses = geocoder.getFromLocation(me.latitude, me.longitude, 1);
                                if (addresses != null && addresses.size() > 0) {
                                    Address address = addresses.get(0);
                                    String strAddress = "";
                                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                        strAddress = strAddress + address.getAddressLine(i) + " ";
                                    }
                                    YourAddress.setText(strAddress);
                                }

                                // Get Customer Address
                                addresses = geocoder.getFromLocation(customerLocation.latitude, customerLocation.longitude, 1);
                                if (addresses != null && addresses.size() > 0) {
                                    Address address = addresses.get(0);
                                    String strAddress = "";
                                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                        strAddress = strAddress + address.getAddressLine(i) + " ";
                                    }
                                    user_address.setText(strAddress);
                                } else {
                                    Log.e("CaseDetail", "User Address is Null");
                                }
                                // Calculate difference between user location and driver location
                                double distance = helpers.distance(me.latitude, me.longitude, customerLocation.latitude, customerLocation.longitude);
                                travel.setText(distance + " KM");

                            } catch (Exception exception) {
                                helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                            }
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                }
            });
        } catch (Exception e) {
            helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
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
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.ACCEPT: {
                buttons.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);

                bookingListener = new ValueEventListener() { // Get data from database.
                    // Data fetch success function
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (bookingListener != null)
                            bookingReference.child(userCase.getId()).removeEventListener(bookingListener);
                        if (bookingListener != null)
                            bookingReference.removeEventListener(bookingListener);
                        if (!isFirst) {
                            return;
                        }
                        isFirst = false;
                        if (dataSnapshot.getValue() != null) {
                            Case temp = dataSnapshot.getValue(Case.class);
                            if (temp != null) {
                                if (temp.getDriverId() == null || temp.getDriverId().equals("")) {
                                    temp.setDriverId(user.getPhone());
                                    temp.setStatus("In Progress");
                                    temp.setAmountCharged(100);
                                    acceptBooking(temp); // Mark the booking accepted in database
                                } else {
                                    buttons.setVisibility(View.VISIBLE);
                                    progress.setVisibility(View.GONE);
                                    helpers.showError(ShowCaseDetail.this, "THE CASE HAS BEEN ACCEPTED BY ANOTHER AMBULANCE.");
                                }
                            } else {
                                buttons.setVisibility(View.VISIBLE);
                                progress.setVisibility(View.GONE);
                                helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                            }
                        } else {
                            buttons.setVisibility(View.VISIBLE);
                            progress.setVisibility(View.GONE);
                            helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                        }
                    }

                    // Data fetch failure function
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (bookingListener != null)
                            bookingReference.child(userCase.getId()).removeEventListener(bookingListener);
                        if (bookingListener != null)
                            bookingReference.removeEventListener(bookingListener);
                        buttons.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                    }
                };
                bookingReference.child(userCase.getId()).addValueEventListener(bookingListener);
                break;
            }
            case R.id.REJECT: {
                finish();
                break;
            }
        }
    }

    // Mark the booking accepted in database
    private void acceptBooking(final Case b) {
        Log.e("CaseDetail", "Id: " + b.getId());
        Log.e("CaseDetail", "Provider Id: " + b.getDriverId());
        Log.e("CaseDetail", "Address: " + b.getAddress());
        Log.e("CaseDetail", "Date: " + b.getDate());
        Log.e("CaseDetail", "Status: " + b.getStatus());
        Log.e("CaseDetail", "Type : " + b.getType());
        Log.e("CaseDetail", "Latitude: " + b.getLatitude());
        Log.e("CaseDetail", "Longitude: " + b.getLongitude());
        Log.e("CaseDetail", "User id: " + b.getUserId());
        bookingReference.child(b.getId()).setValue(b)
                // Data update success listener
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        sendNotification(b); // Send notification to the user, whose booking has been accepted by the ambulance driver.
                    }
                })
                // Data update failure listener
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        buttons.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        helpers.showError(ShowCaseDetail.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                    }
                });
    }

    // Send notification to the user, whose booking has been accepted by the ambulance driver.
    private void sendNotification(Case b) {
        Notification notification = new Notification();
        String id = notificationReference.push().getKey();
        notification.setId(id);
        notification.setCaseId(b.getId());
        notification.setUserId(customer.getPhone());
        notification.setDriverId(user.getPhone());
        notification.setRead(false);
        Date d = new Date();
        String date = new SimpleDateFormat("EEE dd, MMM, yyyy HH:mm").format(d);
        notification.setDate(date);
        notification.setUserMessage("Your case has been accepted by " + user.getFirstName() + " " + user.getLastName());
        notification.setDriverMessage("You accepted the case of " + customer.getFirstName() + " " + customer.getLastName());
        notificationReference.child(notification.getId()).setValue(notification)
                // Data save success function
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        buttons.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        finish();
                    }
                })
                // Data save failure function
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        buttons.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        finish();
                    }
                });
    }


    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
        if (bookingListener != null) {
            bookingReference.removeEventListener(bookingListener);
        }
        if (userListener != null) {
            userReference.removeEventListener(userListener);
        }
    }

    @Override
    public void onBackPressed() {
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
}
