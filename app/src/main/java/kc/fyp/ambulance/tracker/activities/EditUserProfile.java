package kc.fyp.ambulance.tracker.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Calendar;

import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Constants;
import kc.fyp.ambulance.tracker.director.Helpers;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.User;

public class EditUserProfile extends AppCompatActivity implements View.OnClickListener {
    private ImageView img;
    private Helpers helpers;
    private Session session;
    private User user;
    private Uri imagePath; // To hold the selected image.
    private boolean isImage; // image not selected => false, if image is selected => true
    private EditText edtPhoneNo, edtFirstName, edtLastName, edtEmail;
    private String strPhoneNo, strFirstName, strLastName, strEmail;
    private Button userProfile;
    private ProgressBar userProfileProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        session = new Session(EditUserProfile.this);
        user = session.getUser();
        helpers = new Helpers();
        // Loading data for user profile.
        img = findViewById(R.id.img);
        if (user.getImage() != null && !user.getImage().equalsIgnoreCase("")) {
            Glide.with(EditUserProfile.this).load(user.getImage()).into(img);
        } else {
            img.setImageDrawable(getResources().getDrawable(R.drawable.user));
        }

        if (getSupportActionBar() != null) // Enabled the back arrow.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // intializing all variables.
        edtPhoneNo = findViewById(R.id.edtPhoneNo);
        edtFirstName = findViewById(R.id.edtFirstName);
        edtLastName = findViewById(R.id.edtLastName);
        edtEmail = findViewById(R.id.edtEmail);
        userProfileProgress = findViewById(R.id.userProfileProgress);
        userProfile = findViewById(R.id.userProfile);
        userProfileProgress.setVisibility(View.GONE);
        userProfile.setOnClickListener(this);


        // Loading all values to user profile
        edtPhoneNo.setText(user.getPhone());
        edtFirstName.setText(user.getFirstName());
        edtLastName.setText(user.getLastName());
        edtEmail.setText(user.getEmail());

        // A button to open gallery, so user can select and update the profile image.
        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (askForPermission()) { // Check if the app have permission to access the user gallery.
                    openGallery(); // Permission is granted, open gallery.
                }
            }
        });
    }

    // Will return the select image.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    Log.e("profile", "data null");
                    return;
                }
                Uri image = data.getData();
                if (image != null) {
                    // Display the image in profile
                    Glide.with(EditUserProfile.this).load(image).into(img);
                    imagePath = image;
                    isImage = true; // Set to true, which represents that the image is select.
                }
            }
        }
    }

    // Check if the app have permission to access the user gallery.
    private boolean askForPermission() {
        if (ActivityCompat.checkSelfPermission(EditUserProfile.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(EditUserProfile.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EditUserProfile.this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 10);
            return false;
        }
        return true;
    }

    // Permission is granted, open gallery.
    public void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            openGallery();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.userProfile: {
                // Check internet connection
                if (!helpers.isConnected(EditUserProfile.this)) {
                    // Show error if not connected
                    helpers.showNoInternetError(EditUserProfile.this);
                    return;
                }
                boolean flag = isValid(); // Perform validation on all inputs.
                if (flag) {
                    Log.e("profile", "is image value " + isImage);
                    if (isImage) { // it's true, upload the image first.
                        Log.e("Profile", "Image Found");
                        uploadImage();
                    } else {
                        Log.e("Profile", "No Image Found");
                        saveToDatabase();
                    }
                }
            }
        }
    }

    private void uploadImage() {
        userProfileProgress.setVisibility(View.VISIBLE);
        userProfile.setVisibility(View.GONE);
        // Firebase Storage.
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Users").child(user.getPhone());
        Uri selectedMediaUri = Uri.parse(imagePath.toString());

        // Converting image into a file.
        File file = new File(selectedMediaUri.getPath());
        Log.e("file", "in file object value " + file.toString());
        Log.e("Profile", "Uri: " + selectedMediaUri.getPath() + " File: " + file.exists());

        // Using to generate a unique name.
        Calendar calendar = Calendar.getInstance();

        Log.e("profile", "selected Path " + imagePath.toString());
        storageReference
                .child(calendar.getTimeInMillis() + "") // Setting image name
                .putFile(imagePath) // Putting the file. (Saving)
                // Data save success function
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get the download url of image.
                        taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                // get download url success function
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Log.e("Profile", "in OnSuccess " + uri.toString());
                                        user.setImage(uri.toString()); // Set the download url to current use data.
                                        userProfileProgress.setVisibility(View.GONE);
                                        userProfile.setVisibility(View.VISIBLE);
                                        saveToDatabase();
                                    }
                                })
                                // get download url failure function
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("Profile", "Download Url: " + e.getMessage());
                                        userProfileProgress.setVisibility(View.GONE);
                                        userProfile.setVisibility(View.VISIBLE);
                                        helpers.showError(EditUserProfile.this, "ERROR!Something went wrong.\n Please try again later.");
                                    }
                                });
                    }
                })
                // Data save failure function
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Profile", "Upload Image Url: " + e.getMessage());
                        userProfileProgress.setVisibility(View.GONE);
                        userProfile.setVisibility(View.VISIBLE);
                        helpers.showError(EditUserProfile.this, "ERROR! Something went wrong.\n Please try again later.");
                    }
                });
    }

    private void saveToDatabase() {
        userProfileProgress.setVisibility(View.VISIBLE);
        userProfile.setVisibility(View.GONE);
        // Get all values from the fields.
        strFirstName = edtFirstName.getText().toString();
        strLastName = edtLastName.getText().toString();
        strPhoneNo = edtPhoneNo.getText().toString();
        strEmail = edtEmail.getText().toString();
        // Set updated data to current user
        user.setFirstName(strFirstName);
        user.setLastName(strLastName);
        user.setEmail(strEmail);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference().child("Users").child(strPhoneNo).setValue(user)
                // Data save success function
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        userProfileProgress.setVisibility(View.GONE);
                        userProfile.setVisibility(View.VISIBLE);
                        session.setSession(user);
                        Intent intent = new Intent(EditUserProfile.this, Dashboard.class);
                        if (user.getType() == 1) // It's ambulance driver, move to ambulance dashboard
                            intent = new Intent(EditUserProfile.this, AmbulanceDashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                // Data save failure function
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        userProfileProgress.setVisibility(View.GONE);
                        userProfile.setVisibility(View.VISIBLE);
                        helpers.showError(EditUserProfile.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                    }
                });

    }

    // Perform validation on all inputs.
    private boolean isValid() {
        boolean flag = true;
        strFirstName = edtFirstName.getText().toString();
        strLastName = edtLastName.getText().toString();
        strEmail = edtEmail.getText().toString();
        if (strFirstName.length() < 3) {
            edtFirstName.setError(Constants.ERROR_FIRST_NAME);
            flag = false;
        } else {
            edtFirstName.setError(null);
        }
        if (strLastName.length() < 3) {
            edtLastName.setError(Constants.ERROR_LAST_NAME);
            flag = false;
        } else {
            edtLastName.setError(null);
        }
        if (strEmail.length() < 7 || !Patterns.EMAIL_ADDRESS.matcher(strEmail).matches()) {
            edtEmail.setError(Constants.ERROR_EMAIL);
            flag = false;
        } else {
            edtEmail.setError(null);
        }
        return flag;
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
