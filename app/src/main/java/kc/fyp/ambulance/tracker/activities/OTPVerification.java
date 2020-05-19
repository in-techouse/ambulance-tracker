package kc.fyp.ambulance.tracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chaos.view.PinView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Constants;
import kc.fyp.ambulance.tracker.director.Helpers;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.Ambulance;
import kc.fyp.ambulance.tracker.model.User;

public class OTPVerification extends AppCompatActivity implements View.OnClickListener {
    private Helpers helpers;
    private Button btnVerify;
    private PinView firstPinView;
    private ProgressBar verifyProgress;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private TextView timer, resend;
    private String strPhoneNo;
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
    private ValueEventListener userValueEventListener, ambulanceValueEventListener;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification);

        helpers = new Helpers();
        Intent it = getIntent();
        if (it == null) {
            finish();
            return;
        }
        Bundle bundle = it.getExtras();
        if (bundle == null) {
            finish();
            return;
        }

        // For primitive data type
        strPhoneNo = bundle.getString("phone");
        if (strPhoneNo == null) {
            finish();
            return;
        }
        Log.e("PhoneNumber", strPhoneNo);
        verificationId = bundle.getString("verificationId");
        // For non-primitive data type
        resendToken = bundle.getParcelable("resendToken");
        PhoneAuthCredential credential = bundle.getParcelable("phoneAuthCredential");


        btnVerify = findViewById(R.id.btnverify);
        firstPinView = findViewById(R.id.firstPinView);
        verifyProgress = findViewById(R.id.verifyProgress);
        timer = findViewById(R.id.timer);
        resend = findViewById(R.id.resend);

        btnVerify.setOnClickListener(this);
        resend.setOnClickListener(this);
        startTimer();
        if (credential == null) {
            Log.e("OTP", "Credential Null");
        } else {
            Log.e("OTP", "Credential Not Null");
            addUserToFirebase(credential);
        }

        session = new Session(getApplicationContext());
    }

    private void startTimer() {
        resend.setEnabled(false);
        new CountDownTimer(120000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisUntilFinished = millisUntilFinished / 1000;
                long seconds = millisUntilFinished % 60;
                long minutes = (millisUntilFinished / 60) % 60;
                String time = "";
                if (seconds > 9) {
                    time = "0" + minutes + ":" + seconds;
                } else {
                    time = "0" + minutes + ":" + "0" + seconds;
                }
                timer.setText(time);
            }

            @Override
            public void onFinish() {
                timer.setText("--:--");
                resend.setEnabled(true);
            }
        }.start();
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnverify: {
                boolean flag = helpers.isConnected(this);
                if (!flag) {
                    helpers.showNoInternetError(OTPVerification.this);
                    return;
                }
                if (firstPinView == null || firstPinView.getText() == null) {
                    return;
                }
                String otp = firstPinView.getText().toString();
                if (otp.length() != 6) {
                    firstPinView.setError(Constants.ERROR_INVALID_OTP);
                } else {
                    firstPinView.setError(null);
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
                    addUserToFirebase(credential);
                }

                break;
            }
            case R.id.resend: {
                verifyProgress.setVisibility(View.VISIBLE);
                resend.setVisibility(View.GONE);
                Log.e("OTP", "Verification Id: " + verificationId);
                Log.e("OTP", "String Phone: " + strPhoneNo);
                Log.e("OTP", "Resend Token: " + resendToken);

                PhoneAuthProvider.OnVerificationStateChangedCallbacks callBack;
                callBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        Log.e("OTP", "Code send successfully");
                        super.onCodeSent(s, forceResendingToken);
                        verifyProgress.setVisibility(View.GONE);
                        resend.setVisibility(View.VISIBLE);
                        verificationId = s;
                        resendToken = forceResendingToken;
                        startTimer();
                    }

                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        Log.e("OTP", "OnVerification Completed");
                        addUserToFirebase(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        verifyProgress.setVisibility(View.GONE);
                        btnVerify.setVisibility(View.VISIBLE);
                        helpers.showError(OTPVerification.this, e.getMessage());
                    }
                };
                PhoneAuthProvider.getInstance().verifyPhoneNumber(strPhoneNo, 120, TimeUnit.SECONDS, this, callBack, resendToken);
                break;
            }
        }
    }

    private void addUserToFirebase(PhoneAuthCredential credential) {
        verifyProgress.setVisibility(View.VISIBLE);
        btnVerify.setVisibility(View.GONE);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        checkUser();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        verifyProgress.setVisibility(View.GONE);
                        btnVerify.setVisibility(View.VISIBLE);
                        helpers.showError(OTPVerification.this, e.getMessage());
                    }
                });
    }


    private void checkUser() {

        userValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (userValueEventListener != null)
                    reference.child("Users").child(strPhoneNo).removeEventListener(userValueEventListener);
                if (userValueEventListener != null)
                    reference.removeEventListener(userValueEventListener);
                if (dataSnapshot.getValue() == null) {
                    verifyProgress.setVisibility(View.GONE);
                    btnVerify.setVisibility(View.VISIBLE);
                    Intent intent = new Intent(OTPVerification.this, UserProfile.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("phone", strPhoneNo);
                    intent.putExtras(bundle);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    User user = dataSnapshot.getValue(User.class);
                    session.setSession(user);
                    if (user == null) {
                        verifyProgress.setVisibility(View.GONE);
                        btnVerify.setVisibility(View.VISIBLE);
                        Intent intent = new Intent(OTPVerification.this, UserProfile.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("phone", strPhoneNo);
                        intent.putExtras(bundle);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else if (user.getType() == 0) {
                        reference.removeEventListener(this);
                        verifyProgress.setVisibility(View.GONE);
                        btnVerify.setVisibility(View.VISIBLE);
                        Intent intent = new Intent(OTPVerification.this, Dashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        getAmbulanceDetail();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (userValueEventListener != null)
                    reference.child("Users").child(strPhoneNo).removeEventListener(userValueEventListener);
                if (userValueEventListener != null)
                    reference.removeEventListener(userValueEventListener);
                verifyProgress.setVisibility(View.GONE);
                btnVerify.setVisibility(View.VISIBLE);
                helpers.showError(OTPVerification.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
        };

        reference.child("Users").child(strPhoneNo).addValueEventListener(userValueEventListener);

//        reference.child("Users").child(strPhoneNo).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.getValue() == null) {
//                    reference.removeEventListener(this);
//                    verifyProgress.setVisibility(View.GONE);
//                    btnVerify.setVisibility(View.VISIBLE);
//                    Intent intent = new Intent(OTPVerification.this, UserProfile.class);
//                    Bundle bundle = new Bundle();
//                    bundle.putString("phone", strPhoneNo);
//                    intent.putExtras(bundle);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
//                } else {
//                    User user = dataSnapshot.getValue(User.class);
//                    session.setSession(user);
//                    if (user == null) {
//                        reference.removeEventListener(this);
//                        verifyProgress.setVisibility(View.GONE);
//                        btnVerify.setVisibility(View.VISIBLE);
//                        Intent intent = new Intent(OTPVerification.this, UserProfile.class);
//                        Bundle bundle = new Bundle();
//                        bundle.putString("phone", strPhoneNo);
//                        intent.putExtras(bundle);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
//                        finish();
//                    } else if (user.getType() == 0) {
//                        reference.removeEventListener(this);
//                        verifyProgress.setVisibility(View.GONE);
//                        btnVerify.setVisibility(View.VISIBLE);
//                        Intent intent = new Intent(OTPVerification.this, Dashboard.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
//                        finish();
//                    } else {
//                        getAmbulanceDetail();
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                reference.removeEventListener(this);
//                verifyProgress.setVisibility(View.GONE);
//                btnVerify.setVisibility(View.VISIBLE);
//                helpers.showError(OTPVerification.this, Constants.ERROR_SOMETHING_WENT_WRONG);
//            }
//        });
    }

    private void getAmbulanceDetail() {
        ambulanceValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (ambulanceValueEventListener != null)
                    reference.child("Ambulances").orderByChild("driverId").equalTo(strPhoneNo).removeEventListener(ambulanceValueEventListener);
                if (ambulanceValueEventListener != null)
                    reference.removeEventListener(ambulanceValueEventListener);
                verifyProgress.setVisibility(View.GONE);
                btnVerify.setVisibility(View.VISIBLE);
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Ambulance ambulance = data.getValue(Ambulance.class);
                    Log.e("OTP", "Ambulance Data Snapshot: " + dataSnapshot.toString());
                    if (ambulance != null) {
                        Log.e("OTP", "Ambulance is not null");
                        Log.e("OTP", "Ambulance Registration: " + ambulance.getRegistrationNumber());
                        Log.e("OTP", "Ambulance Model: " + ambulance.getAmbulanceModel());
                        Log.e("OTP", "Ambulance Driver: " + ambulance.getDriverId());
                        session.setAmbulance(ambulance);
                    }
                }
                Intent intent = new Intent(OTPVerification.this, AmbulanceDashboard.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (ambulanceValueEventListener != null)
                    reference.child("Ambulances").orderByChild("driverId").equalTo(strPhoneNo).removeEventListener(ambulanceValueEventListener);
                if (ambulanceValueEventListener != null)
                    reference.removeEventListener(ambulanceValueEventListener);
                verifyProgress.setVisibility(View.GONE);
                btnVerify.setVisibility(View.VISIBLE);
                helpers.showError(OTPVerification.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
        };

        reference.child("Ambulances").orderByChild("driverId").equalTo(strPhoneNo).addValueEventListener(ambulanceValueEventListener);

//        reference.child("Ambulances").orderByChild("driverId").equalTo(strPhoneNo).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                verifyProgress.setVisibility(View.GONE);
//                btnVerify.setVisibility(View.VISIBLE);
//                for (DataSnapshot data : dataSnapshot.getChildren()) {
//                    Ambulance ambulance = data.getValue(Ambulance.class);
//                    Log.e("OTP", "Ambulance Data Snapshot: " + dataSnapshot.toString());
//                    if (ambulance != null) {
//                        Log.e("OTP", "Ambulance is not null");
//                        Log.e("OTP", "Ambulance Registration: " + ambulance.getRegistrationNumber());
//                        Log.e("OTP", "Ambulance Model: " + ambulance.getAmbulanceModel());
//                        Log.e("OTP", "Ambulance Driver: " + ambulance.getDriverId());
//                        session.setAmbulance(ambulance);
//                    }
//                }
//                Intent intent = new Intent(OTPVerification.this, AmbulanceDashboard.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                finish();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                verifyProgress.setVisibility(View.GONE);
//                btnVerify.setVisibility(View.VISIBLE);
//                helpers.showError(OTPVerification.this, Constants.ERROR_SOMETHING_WENT_WRONG);
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userValueEventListener != null)
            reference.child("Users").child(strPhoneNo).removeEventListener(userValueEventListener);
        if (userValueEventListener != null)
            reference.removeEventListener(userValueEventListener);
        if (ambulanceValueEventListener != null)
            reference.child("Ambulances").orderByChild("driverId").equalTo(strPhoneNo).removeEventListener(ambulanceValueEventListener);
        if (ambulanceValueEventListener != null)
            reference.removeEventListener(ambulanceValueEventListener);
    }
}
