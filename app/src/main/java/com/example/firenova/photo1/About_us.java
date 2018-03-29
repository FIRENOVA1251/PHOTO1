package com.example.firenova.photo1;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class About_us extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_us);

    }


    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
            super.onBackPressed();
            return;
    }

}

