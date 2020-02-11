package kc.fyp.ambulance.tracker.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import java.util.Calendar;
import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Constants;
import kc.fyp.ambulance.tracker.director.Helpers;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.User;

public class UserProfile extends AppCompatActivity implements View.OnClickListener {

    private final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private Helpers helpers;
    private EditText edtPhoneNo, edtFirstName, edtLastName, edtEmail;
    private String strPhoneNo, strFirstName, strLastName, strEmail;
    private Button userProfile;
    private ProgressBar userProfileProgress;
    private ImageView image;
    private Uri imagePath;
    private User user;
    private Session session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        Intent intent = getIntent();
        if(intent == null){
            finish();
            return;
        }

        Bundle bundle = intent.getExtras();
        if(bundle == null){
            finish();
            return;
        }

        strPhoneNo = bundle.getString("phone");

        session = new Session(UserProfile.this);
        user = new User();
        user.setPhone(strPhoneNo);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("AMBULANCE TRACKER");
        setSupportActionBar(toolbar);

        helpers = new Helpers();



        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        image = findViewById(R.id.image);
        image.setImageDrawable(getResources().getDrawable(R.drawable.user));
        edtPhoneNo = findViewById(R.id.edtPhoneNo);
        edtFirstName = findViewById(R.id.edtFirstName);
        edtLastName = findViewById(R.id.edtLastName);
        edtEmail = findViewById(R.id.edtEmail);
        userProfileProgress = findViewById(R.id.userProfileProgress);
        userProfile = findViewById(R.id.userProfile);
        userProfile.setOnClickListener(this);

        edtPhoneNo.setText(strPhoneNo);
        edtPhoneNo.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.fab:{
                boolean flag = hasPermissions(UserProfile.this, PERMISSIONS);
                if(!flag){
                    ActivityCompat.requestPermissions(UserProfile.this, PERMISSIONS, 1);
                }
                else{
                    openGallery();
                }
                break;
            }
            case R.id.userProfile:{
                if(!helpers.isConnected(UserProfile.this)){
                    helpers.showNoInternetError(UserProfile.this);
                    return;
                }

                boolean flag = isValid();
                if(flag){
                    userProfileProgress.setVisibility(View.VISIBLE);
                    userProfile.setVisibility(View.GONE);
                    if(imagePath == null){
                        user.setImage("");
                        saveToDatabase();
                    }
                    else{
                        uploadImage();
                    }
                }
                break;
            }
        }
    }

    private void uploadImage(){
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Users").child(user.getPhone());
        Calendar calendar = Calendar.getInstance();
        Log.e("profile" , "selected Path "+imagePath.toString());
        storageReference.child(calendar.getTimeInMillis()+"").putFile(imagePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.e("Profile" , "in OnSuccess "+uri.toString());
                        user.setImage(uri.toString());
                        userProfileProgress.setVisibility(View.GONE);
                        userProfile.setVisibility(View.VISIBLE);
                        saveToDatabase();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Profile", "Download Url: " + e.getMessage());
                        userProfileProgress.setVisibility(View.GONE);
                        userProfile.setVisibility(View.VISIBLE);
                        helpers.showError(UserProfile.this, "ERROR!Something went wrong.\n Please try again later.");
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Profile", "Upload Image Url: " + e.getMessage());
                userProfileProgress.setVisibility(View.GONE);
                userProfile.setVisibility(View.VISIBLE);
                helpers.showError(UserProfile.this, "ERROR! Something went wrong.\n Please try again later.");            }
        });
    }
    private void saveToDatabase(){
        user.setFirstName(strFirstName);
        user.setLastName(strLastName);
        user.setEmail(strEmail);
        user.setType(0);
        user.setLongitude(0);
        user.setLatitude(0);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference().child("Users").child(strPhoneNo).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        userProfileProgress.setVisibility(View.GONE);
                        userProfile.setVisibility(View.VISIBLE);
                        session.setSession(user);
                        Intent it = new Intent(UserProfile.this, Dashboard.class);
                        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(it);
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                userProfileProgress.setVisibility(View.GONE);
                userProfile.setVisibility(View.VISIBLE);
                helpers.showError(UserProfile.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
        });
    }

    private boolean isValid() {
        boolean flag = true;
        String error = "";
        strFirstName = edtFirstName.getText().toString();
        strLastName = edtLastName.getText().toString();
        strEmail = edtEmail.getText().toString();
        if(strFirstName.length() < 3){
            edtFirstName.setError(Constants.ERROR_FIRST_NAME);
            flag = false;
        }
        else{
            edtFirstName.setError(null);
        }
        if(strLastName.length() < 3){
            edtLastName.setError(Constants.ERROR_LAST_NAME);
            flag = false;
        }
        else{
            edtLastName.setError(null);
        }
        if (strEmail.length() < 7 || !Patterns.EMAIL_ADDRESS.matcher(strEmail).matches()){
            edtEmail.setError(Constants.ERROR_EMAIL);
            flag = false;
        }
        else{
            edtEmail.setError(null);
        }
        return flag;
    }

    private boolean hasPermissions(Context c, String... permission){
        for(String p : permission){
            if(ActivityCompat.checkSelfPermission(c, p) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    public void openGallery(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), 2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
            openGallery();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("Profile", "Gallery Call Back Received in Fragment with Request Code: " + requestCode);
        if (requestCode == 2) {
            Log.e("Profile", "Inside first if");
            if(resultCode == RESULT_OK){
                Log.e("Profile", "Inside second if");
                if(data != null){
                    Log.e("Profile", "Data is not null");
                    Uri img = data.getData();
                    if(img != null){
                        Log.e("Profile", "Img is not null");
                        Glide.with(getApplicationContext()).load(img).into(image);
                        imagePath = img;
                    }
                }
            }
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
