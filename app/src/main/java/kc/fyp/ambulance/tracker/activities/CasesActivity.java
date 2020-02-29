package kc.fyp.ambulance.tracker.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import kc.fyp.ambulance.tracker.adapters.CaseAdapter;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.Case;
import kc.fyp.ambulance.tracker.model.User;

public class CasesActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout loading;
    private TextView noCase;
    private RecyclerView cases;
    private User user;
    private List<Case> data;
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Cases");
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private ValueEventListener bookingListener, userListener;
    private CaseAdapter adapter;
    private String orderBy;
    private BottomSheetBehavior sheetBehavior;
    private ProgressBar sheetProgress;
    private LinearLayout mainSheet;
    private CircleImageView image;
    private TextView about, name, contact, date, type, status, totalCharge, address;
    private RelativeLayout userLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cases);
        loading = findViewById(R.id.loading);
        noCase = findViewById(R.id.noBooking);
        cases = findViewById(R.id.bookings);
        Session session = new Session(CasesActivity.this);
        user = session.getUser();
        adapter = new CaseAdapter(CasesActivity.this);
        cases.setLayoutManager(new LinearLayoutManager(CasesActivity.this));
        cases.setAdapter(adapter);
        data = new ArrayList<>();

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
        contact = findViewById(R.id.contact);
        date = findViewById(R.id.date);
        type = findViewById(R.id.type);
        status = findViewById(R.id.status);
        totalCharge = findViewById(R.id.totalCharge);
        address = findViewById(R.id.address);
        userLayout = findViewById(R.id.userLayout);

        if (user.getType() == 0) {
            orderBy = "userId";
        } else {
            orderBy = "driverId";
        }
        loadCases();
    }

    private void loadCases() {
        loading.setVisibility(View.VISIBLE);
        noCase.setVisibility(View.GONE);
        cases.setVisibility(View.GONE);

        bookingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("Case", "Data Snap Shot: " + dataSnapshot.toString());
                data.clear();
                for (DataSnapshot d : dataSnapshot.getChildren()) {
                    Case c = d.getValue(Case.class);
                    if (c != null) {
                        data.add(c);
                    }
                }
                Collections.reverse(data);
                Log.e("Case", "Data List Size: " + data.size());
                if (data.size() > 0) {
                    Log.e("Case", "If, list visible");
                    cases.setVisibility(View.VISIBLE);
                    noCase.setVisibility(View.GONE);
                } else {
                    Log.e("Case", "Else, list invisible");
                    noCase.setVisibility(View.VISIBLE);
                    cases.setVisibility(View.GONE);
                }
                loading.setVisibility(View.GONE);
                adapter.setData(data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                loading.setVisibility(View.GONE);
                noCase.setVisibility(View.VISIBLE);
                cases.setVisibility(View.GONE);
            }
        };
        reference.orderByChild(orderBy).equalTo(user.getPhone()).addValueEventListener(bookingListener);
    }

    public void showBottomSheet(final Case userCase) {
        sheetBehavior.setHideable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        sheetProgress.setVisibility(View.VISIBLE);
        mainSheet.setVisibility(View.GONE);

        if (user.getType() == 0) {
            about.setText("Ambulance detail");
        } else {
            about.setText("Customer detail");
        }
        date.setText(userCase.getDate());
        address.setText(userCase.getAddress());
        totalCharge.setText(userCase.getAmountCharged() + " RS.");
        type.setText(userCase.getType());
        status.setText(userCase.getStatus());

        if (userCase.getDriverId().equals("")) {
            about.setVisibility(View.GONE);
            userLayout.setVisibility(View.GONE);
            sheetProgress.setVisibility(View.GONE);
            mainSheet.setVisibility(View.VISIBLE);
            return;
        }

        about.setVisibility(View.VISIBLE);
        userLayout.setVisibility(View.VISIBLE);

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (user.getType() == 0) {
                    userReference.child(userCase.getDriverId()).removeEventListener(userListener);
                } else {
                    userReference.child(userCase.getUserId()).removeEventListener(userListener);
                }
                userReference.removeEventListener(userListener);
                User tempUser = dataSnapshot.getValue(User.class);
                if (tempUser != null) {
                    if (tempUser.getImage() != null && tempUser.getImage().length() > 0) {
                        Glide.with(getApplicationContext()).load(tempUser.getImage()).into(image);
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
                    userReference.child(userCase.getDriverId()).removeEventListener(userListener);
                } else {
                    userReference.child(userCase.getUserId()).removeEventListener(userListener);
                }
                userReference.removeEventListener(userListener);
                sheetProgress.setVisibility(View.GONE);
                mainSheet.setVisibility(View.VISIBLE);
            }
        };
        if (user.getType() == 0) {
            userReference.child(userCase.getDriverId()).addValueEventListener(userListener);
        } else {
            userReference.child(userCase.getUserId()).addValueEventListener(userListener);
        }
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
            reference.removeEventListener(bookingListener);
        }
        if (userListener != null) {
            userReference.removeEventListener(userListener);
        }
    }
}
