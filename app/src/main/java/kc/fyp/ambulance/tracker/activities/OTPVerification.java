package kc.fyp.ambulance.tracker.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import kc.fyp.ambulance.tracker.model.User;

public class OTPVerification extends AppCompatActivity implements View.OnClickListener{
    private Helpers helpers;
    private Button btnVerify;
    private PinView firstPinView;
    private ProgressBar verifyProgress;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private TextView timer, resend;
    private PhoneAuthCredential credential;
    private String strPhoneNo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification);

        helpers = new Helpers();
        Intent it = getIntent();
        if(it == null){
            finish();
            return;
        }
        Bundle bundle = it.getExtras();
        if(bundle == null){
            finish();
            return;
        }

        // For primitive data type
        strPhoneNo = bundle.getString("phone");
        if(strPhoneNo == null){
            finish();
            return;
        }
        Log.e("PhoneNumber", strPhoneNo);
        verificationId = bundle.getString("verificationId");
        // For non-primitive data type
        resendToken = bundle.getParcelable("resendToken");
        credential = bundle.getParcelable("phoneAuthCredential");


        btnVerify = findViewById(R.id.btnverify);
        firstPinView = findViewById(R.id.firstPinView);
        verifyProgress = findViewById(R.id.verifyProgress);
        timer = findViewById(R.id.timer);
        resend = findViewById(R.id.resend);

        btnVerify.setOnClickListener(this);
        resend.setOnClickListener(this);
        startTimer();
        if(credential == null){
            Log.e("OTP", "Credential Null");
        }
        else{
            Log.e("OTP", "Credential Not Null");
            addUserToFirebase(credential);
        }
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
                if(seconds > 9){
                    time = "0" + minutes + ":" + seconds;
                }
                else{
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
        switch (id){
            case R.id.btnverify:{
                boolean flag = helpers.isConnected(this);
                if (!flag){
                    helpers.showNoInternetError(OTPVerification.this);
                    return;
                }
                if(firstPinView == null || firstPinView.getText() == null){
                    return;
                }
                String otp = firstPinView.getText().toString();
                if(otp.length() != 6){
                    firstPinView.setError(Constants.ERROR_INVALID_OTP);
                }
                else{
                    firstPinView.setError(null);
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
                    addUserToFirebase(credential);
                }

                break;
            }
            case R.id.resend:{
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

    private void addUserToFirebase(PhoneAuthCredential credential){
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


    private void checkUser(){
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        final DatabaseReference reference = db.getReference().child("Users").child(strPhoneNo);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reference.removeEventListener(this);
                verifyProgress.setVisibility(View.GONE);
                btnVerify.setVisibility(View.VISIBLE);
                if(dataSnapshot.getValue() == null){
                    Intent intent = new Intent(OTPVerification.this, UserProfile.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("phone", strPhoneNo);
                    intent.putExtras(bundle);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                else{
                    User user = dataSnapshot.getValue(User.class);
                    Session session = new Session(OTPVerification.this);
                    session.setSession(user);
                    if(user == null){
                        Intent intent = new Intent(OTPVerification.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    else if (user.getType() == 0) {
                        Intent intent = new Intent(OTPVerification.this, Dashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(OTPVerification.this, AmbulanceDashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                reference.removeEventListener(this);
                verifyProgress.setVisibility(View.GONE);
                btnVerify.setVisibility(View.VISIBLE);
                helpers.showError(OTPVerification.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
        });
    }
}
