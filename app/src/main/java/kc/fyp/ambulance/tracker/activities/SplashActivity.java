package kc.fyp.ambulance.tracker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import kc.fyp.ambulance.tracker.R;
import kc.fyp.ambulance.tracker.director.Session;
import kc.fyp.ambulance.tracker.model.User;

public class SplashActivity extends Activity {
    private boolean isFirstAnimation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Animation hold = AnimationUtils.loadAnimation(this, R.anim.hold);

        final Animation translateScale = AnimationUtils.loadAnimation(this, R.anim.translate_scale);

        final ImageView imageView = findViewById(R.id.header_icon);

        translateScale.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!isFirstAnimation) {
                    imageView.clearAnimation();
                    Session session = new Session(SplashActivity.this);
                    User user = session.getUser();
                    if (user == null) {
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                    } else {
                        if (user.getType() == 0) {
                            Intent intent = new Intent(SplashActivity.this, Dashboard.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(SplashActivity.this, AmbulanceDashboard.class);
                            startActivity(intent);
                        }
                    }
                    finish();
                    isFirstAnimation = true;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        hold.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.clearAnimation();
                imageView.startAnimation(translateScale);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        imageView.startAnimation(hold);


    }
}
