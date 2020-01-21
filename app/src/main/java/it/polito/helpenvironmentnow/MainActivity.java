package it.polito.helpenvironmentnow;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity"; // This string is used as tag for debug
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private final static int REQUEST_CHECK_SETTINGS = 2;

    private RelativeLayout movementRelativeLayout;
    private Switch switchMovementMode;
    private Button btnConfig;
    private SharedPreferences sharedPref;

    private LocationRequest locationRequest; // This field is used for the MOVEMENT mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        movementRelativeLayout = findViewById(R.id.movementRelativeLayout);
        sharedPref = getSharedPreferences(getString(R.string.config_file), Context.MODE_PRIVATE);
        boolean movementMode = sharedPref.getBoolean(getString(R.string.MODE), false);
        switchMovementMode = findViewById(R.id.switchMovementMode);
        initMovementLayout(movementMode);
        switchMovementMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            switchMovementMode.setChecked(false);
                            showPermissionSnackbar();
                        } else {
                            // Check if the device has enabled the needed geolocation settings and start
                            // LocationService if the user has the necessary settings
                            checkDeviceSettings();
                        }
                    } else {
                        // Check if the device has enabled the needed geolocation settings and start
                        // LocationService if the user has the necessary settings
                        checkDeviceSettings();
                    }


                } else {
                    stopMovementService();
                }
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
                if(sharedPref.getBoolean(getString(R.string.MODE), false))
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
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {

                            showPermissionSnackbar();

                        }

                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // This is invoked as a consequence of the call to "checkDeviceSettings()" if the necessary
        // geolocation settings are not enabled and onFailure is called
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_CANCELED) {
                switchMovementMode.setChecked(false);
                showSettingsSnackbar();
            } else if (resultCode == RESULT_OK) {
                startMovementService();
            }
        }
    }

    private void startMovementService() {
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        intent.putExtra("request", locationRequest);
        ContextCompat.startForegroundService(getApplicationContext(), intent);
        saveMovementStatus(true);
        setMovementRelativeLayoutBackground(true);
    }

    private void stopMovementService() {
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        stopService(intent);
        saveMovementStatus(false);
        setMovementRelativeLayoutBackground(false);
    }

    private void saveMovementStatus(boolean movementMode) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.MODE), movementMode);
        editor.commit();
    }

    private void initMovementLayout(boolean movementMode) {
        setMovementRelativeLayoutBackground(movementMode);
        switchMovementMode.setChecked(movementMode);
    }

    // Sets the background colour of the Movement mode layout
    private void setMovementRelativeLayoutBackground(boolean state) {
        if(state)
            movementRelativeLayout.setBackgroundColor(Color.parseColor("#068DE5"));
        else
            movementRelativeLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
    }

    // Show a snackbar that inform the user that the location permission has not been granted
    private void showPermissionSnackbar() {
        Snackbar permissionSnackbar = Snackbar.make(findViewById(R.id.mainConstraintLayout),
                "Location permission NOT granted!", Snackbar.LENGTH_INDEFINITE)
                .setAction("GRANT IT", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestFineLocationPermission();
                    }
                });
        permissionSnackbar.show();
    }

    // Show a snackbar that inform the user that the settings for continuous tracking are not set
    private void showSettingsSnackbar() {
        Snackbar permissionSnackbar = Snackbar.make(findViewById(R.id.mainConstraintLayout),
                "Location settings NOT set!", Snackbar.LENGTH_LONG);
        permissionSnackbar.show();
    }

    // Check if the device has enabled the needed geolocation settings otherwise a dialog is
    // started asking the user to accept the necessary geolocation settings for continuous tracking
    private void checkDeviceSettings() {
        // Set the location request in order to receive continuous location updates
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(5000);

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();

        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> task = client.checkLocationSettings(settingsRequest);

        // onFailure will be invoked if the device settings do not match the LocationRequest and
        // a dialog is presented to the user to accept the necessary settings
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    // Location settings are not satisfied, but this can
                    // be fixed by showing the user a dialog
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the
                        // result in onActivityResult()
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error
                    }
                }
            }
        });

        // onSuccess is invoked if the device settings are ok
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startMovementService();
            }
        });
    }
}
