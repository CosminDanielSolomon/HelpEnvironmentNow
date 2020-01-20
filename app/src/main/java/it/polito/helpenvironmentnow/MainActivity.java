package it.polito.helpenvironmentnow;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity"; // This string is used as tag for debug

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private RelativeLayout movementRelativeLayout;
    private Switch switchMovementMode;
    private Button btnConfig;
    private boolean movementMode;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        movementRelativeLayout = findViewById(R.id.movementRelativeLayout);
        sharedPref = getSharedPreferences(getString(R.string.config_file), Context.MODE_PRIVATE);
        movementMode = sharedPref.getBoolean(getString(R.string.MODE), false);
        switchMovementMode = findViewById(R.id.switchMovementMode);
        switchMovementMode.setChecked(movementMode);
        setMovementRelativeLayoutBackground(movementMode);
        switchMovementMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    Intent intent = new Intent(getApplicationContext(), LocationService.class);
                    ContextCompat.startForegroundService(getApplicationContext(), intent);
                    movementMode = true;
                } else {
                    Intent intent = new Intent(getApplicationContext(), LocationService.class);
                    stopService(intent);
                    movementMode = false;
                }
                setMovementRelativeLayoutBackground(movementMode);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.MODE), movementMode);
                editor.commit();
            }
        });

        btnConfig = findViewById(R.id.buttonConfig);
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                startActivity(intent);
            }
        });

        /* TODO remove this part */
        Button btnConnect = findViewById(R.id.buttonConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if(movementMode)
                    intent = new Intent(getApplicationContext(), MovementService.class);
                else
                    intent = new Intent(getApplicationContext(), ClassicService.class);
                intent.putExtra("remoteMacAddress", "B8:27:EB:C4:15:D6");
                ContextCompat.startForegroundService(getApplicationContext(), intent);
                Log.d(TAG, "startService(...) performed");
            }
        });
        /* TODO remove this part */

        requestFineLocationPermission();
    }

    private void requestFineLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons from Raspberry Pi around the city.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    // Sets the background colour of the Movement mode layout
    private void setMovementRelativeLayoutBackground(boolean state) {
        if(state)
            movementRelativeLayout.setBackgroundColor(Color.parseColor("#068DE5"));
        else
            movementRelativeLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
    }
}
