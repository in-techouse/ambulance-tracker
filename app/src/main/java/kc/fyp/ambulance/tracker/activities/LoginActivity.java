package kc.fyp.ambulance.tracker.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Constants;
import kc.fyp.ambulance.tracker.director.Helpers;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private Helpers helpers;
    private EditText edtPhoneNo;
    private Button btnLogin;
    private ProgressBar loginProgress;
    private String strPhoneNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        helpers = new Helpers();

        /*
        This activity is created and opened when SplashScreen finishes its animations.
        To ensure a smooth transition between activities, the activity creation animation
        is removed.
        RelativeLayout with EditTexts and Button is animated with a default fade in.
         */

        overridePendingTransition(0, 0);
        View relativeLayout = findViewById(R.id.login_container);
        Animation animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        relativeLayout.startAnimation(animation);

        edtPhoneNo = findViewById(R.id.edtPhoneNo);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgress = findViewById(R.id.loginProgress);


        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnLogin: {
                boolean flag = helpers.isConnected(this);
                if (!flag) {
                    helpers.showNoInternetError(LoginActivity.this);
                    return;
                }
                strPhoneNo = edtPhoneNo.getText().toString();
                if (strPhoneNo.length() != 13) {
                    edtPhoneNo.setError(Constants.ERROR_PHONE);
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                            .setTitle(strPhoneNo)
                            .setMessage(Constants.MESSAGE_PHONE_CORRECT)
                            .setPositiveButton(Constants.MESSAGE_YES, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    verifyUser();
                                }
                            }).setNegativeButton(Constants.MESSAGE_NO_EDIT, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
                break;
            }
        }
    }

    private void verifyUser() {
        Log.e("LOGIN", "Else part");
        loginProgress.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.GONE);
        edtPhoneNo.setError(null);
        PhoneAuthProvider provider = PhoneAuthProvider.getInstance();
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callBack;
        callBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                loginProgress.setVisibility(View.GONE);
                btnLogin.setVisibility(View.VISIBLE);
                Log.e("LOGIN", "Code Sent, Verification Id: " + s);
                Intent it = new Intent(LoginActivity.this, OTPVerification.class);
                Bundle bundle = new Bundle();
                bundle.putString("phone", strPhoneNo);
                bundle.putString("verificationId", s); // Because it's primitive data type
                bundle.putParcelable("resendToken", forceResendingToken); // Because it's non-primitive data type
                it.putExtras(bundle);
                startActivity(it);
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                Log.e("LOGIN", "Verification Completed");
                Intent it = new Intent(LoginActivity.this, OTPVerification.class);
                Bundle bundle = new Bundle();
                bundle.putString("phone", strPhoneNo);
                bundle.putParcelable("phoneAuthCredential", phoneAuthCredential); // Because it's non-primitive data type
                it.putExtras(bundle);
                startActivity(it);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e("LOGIN", "Verification Failed: " + e.getMessage());
                loginProgress.setVisibility(View.GONE);
                btnLogin.setVisibility(View.VISIBLE);
                helpers.showError(LoginActivity.this, Constants.ERROR_SOMETHING_WENT_WRONG + " " + e.getMessage());
            }
        };
        provider.verifyPhoneNumber(strPhoneNo, 120, TimeUnit.SECONDS, this, callBack);
        Log.e("LOGIN", "Code Sent");
    }
}
