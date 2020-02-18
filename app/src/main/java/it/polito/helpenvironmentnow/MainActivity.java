package it.polito.helpenvironmentnow;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.List;

import it.polito.helpenvironmentnow.Helper.BtDevice;
import it.polito.helpenvironmentnow.Helper.DynamicModeStatus;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity"; // This string is used as tag for debug
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private RelativeLayout dynamicLayout;
    private ProgressBar pbTop;
    private Switch switchDynamicMode;
    private TextView tvDynamicModeContent;

    private CompoundButton.OnCheckedChangeListener switchListener;
    private boolean scanning = false;
    private List<BtDevice> scanningResult = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private SharedPreferences sharedPref;
    private LocationRequest locationRequest; // This field is used for the DYNAMIC mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(savedInstanceState!= null)
        Log.d(TAG, "omcreate: " + savedInstanceState.getString("keyyy"));

        dynamicLayout = findViewById(R.id.movementRelativeLayout);
        pbTop = findViewById(R.id.progressBarMovement);
        tvDynamicModeContent = findViewById(R.id.tvMovementModeContent);
        sharedPref = getSharedPreferences(getString(R.string.config_file), Context.MODE_PRIVATE);
        int dynamicModeStatus = sharedPref.getInt(getString(R.string.MODE), DynamicModeStatus.OFF);
        switchDynamicMode = findViewById(R.id.switchMovementMode);
        String dev_name = sharedPref.getString(getString(R.string.DEVICE_NM), "");
        String dev_addr = sharedPref.getString(getString(R.string.DEVICE_ADDR), "");
        BtDevice btDevice = new BtDevice(dev_name, dev_addr);
        initDynamicModeLayout(dynamicModeStatus, btDevice);
        switchListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            changeButtonState(false);
                            showPermissionSnackbar();
                        } else {
                            // Check if the device has enabled the needed geolocation settings and start
                            // DynamicService if the user has the necessary settings
                            checkDeviceSettings();
                        }
                    } else {
                        // Check if the device has enabled the needed geolocation settings and start
                        // DynamicService if the user has the necessary settings
                        checkDeviceSettings();
                    }


                } else {
                    if (scanning)
                        stopScanning();
                    else
                        stopDynamicService();
                }
            }
        };
        switchDynamicMode.setOnCheckedChangeListener(switchListener);

        /* TODO remove this part(START) and the corresponding button from activity_main.xml */
        Button btnConfig = findViewById(R.id.buttonConfig);
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                startActivity(intent);
            }
        });
        /* TODO remove this part(END) */

        /* TODO remove this part(START) and the corresponding button from activity_main.xml */
        Button btnConnect = findViewById(R.id.buttonConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), StaticService.class);
                intent.putExtra("remoteMacAddress", "B8:27:EB:47:CF:BE");
                ContextCompat.startForegroundService(getApplicationContext(), intent);
            }
        });
        /* TODO remove this part(END) */

        // Check permissions
        requestFineLocationPermission();
    }

    private void changeButtonState(boolean state) {
        switchDynamicMode.setOnCheckedChangeListener(null);
        switchDynamicMode.setChecked(state);
        switchDynamicMode.setOnCheckedChangeListener(switchListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(scanning)
            stopScanning();
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
                changeButtonState(false);
                showSettingsSnackbar();
            } else if (resultCode == RESULT_OK) {
                checkBluetoothEnabled();
            }
        }
        if(requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startScanning();
            } else if(resultCode == RESULT_CANCELED) {
                changeButtonState(false);
                showBluetoothSnackbar();
            }
        }
    }

    private void checkBluetoothEnabled() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                startScanning();
            }
        }
    }

    private void startScanning() {
        setLayoutScanning();
        scanning = true;
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        // start scanning Bluetooth devices
        boolean started = bluetoothAdapter.startDiscovery();
        if(!started) { // Bluetooth is not enabled
            changeButtonState(false);
            showBluetoothSnackbar();
        }
    }

    private void stopScanning() {
        try {
            unregisterReceiver(receiver);
            receiver =  null;
        } catch (IllegalArgumentException e) {
            // do nothing - it has already been unregistered
        }
        if(bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        setLayoutOff();
        scanning = false;
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevic object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BtDevice btDevice = new BtDevice(deviceName, deviceHardwareAddress);
                if(!scanningResult.contains(btDevice))
                    scanningResult.add(btDevice);
                else
                    Log.d(TAG, "DUPLICATE");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopScanning();
                for (BtDevice b : scanningResult)
                    Log.d(TAG, "name: " + b.getName() + " address: " + b.getAddress());
                /*for(int i = 0; i<20; i++) {
                    scanningResult.add(new BtDevice("dv", "a"+i));
                }*/
                createDialog(scanningResult);
            }
        }
    };

    private void createDialog(final List<BtDevice> devices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;

        if(devices.size() > 0) {
            final CharSequence[] items = new CharSequence[devices.size()];
            int index = 0;
            for (BtDevice btDevice : devices) {
                items[index] = btDevice.getName() + " (" + btDevice.getAddress() + ")";
                index++;
            }
            builder.setTitle("Select Raspberry Pi device")
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position of the selected item
                        startDynamicService(devices.get(which));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stopScanning();

                    }
                });
            dialog = builder.setCancelable(false).create();
            ListView listView = dialog.getListView();
            listView.setDivider(new ColorDrawable(Color.GRAY));
            listView.setDividerHeight(1);
        } else {
            builder.setTitle("No device found!").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    stopScanning();
                }
            });
            dialog = builder.setCancelable(false).create();
        }
        dialog.show();
    }

    private void startDynamicService(BtDevice btDevice) {
        Intent intent = new Intent(getApplicationContext(), DynamicService.class);
        intent.putExtra("request", locationRequest);
        intent.putExtra("address", btDevice.getAddress());
        ContextCompat.startForegroundService(getApplicationContext(), intent);
        saveDynamicModeStatus(DynamicModeStatus.CONNECTING, btDevice);
        setLayoutConnecting(btDevice);
        // TODO register broadcast receiver
    }

    private void stopDynamicService() {
        Intent intent = new Intent(getApplicationContext(), DynamicService.class);
        stopService(intent);
        saveDynamicModeStatus(DynamicModeStatus.OFF, null);
        setLayoutOff();
    }

    private void saveDynamicModeStatus(int dynamicModeStatus, BtDevice btDevice) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.MODE), dynamicModeStatus);
        if(btDevice != null) {
            editor.putString(getString(R.string.DEVICE_NM), btDevice.getName());
            editor.putString(getString(R.string.DEVICE_ADDR), btDevice.getAddress());
        }
        editor.commit();
    }

    // Initialize the background colour and the switch button of the Dynamic mode layout - called only in onCreate(...)
    private void initDynamicModeLayout(int dynamicModeStatus, BtDevice btDevice) {
        switch (dynamicModeStatus) {
            case DynamicModeStatus.OFF:
                dynamicLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
                switchDynamicMode.setChecked(false);
                tvDynamicModeContent.setText(R.string.movement_mode_body);
                break;
            case DynamicModeStatus.CONNECTING:
                dynamicLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
                switchDynamicMode.setChecked(true);
                String s2 = R.string.connect_dev + btDevice.getName() + " (" + btDevice.getAddress() + ")";
                tvDynamicModeContent.setText(s2);
                pbTop.setVisibility(View.VISIBLE);
                break;
            case DynamicModeStatus.CONNECTED:
                dynamicLayout.setBackgroundColor(Color.parseColor("#068DE5"));
                switchDynamicMode.setChecked(true);
                String s3 = R.string.connected_dev + btDevice.getName() + " (" + btDevice.getAddress() + ")";
                tvDynamicModeContent.setText(s3);
                break;
        }
    }

    private void setLayoutOff() {
        pbTop.setVisibility(View.GONE);
        dynamicLayout.setBackgroundColor(Color.parseColor("#9E9E9E"));
        tvDynamicModeContent.setText(R.string.movement_mode_body);
    }

    private void setLayoutScanning() {
        tvDynamicModeContent.setText(R.string.search_dev);
        pbTop.setVisibility(View.VISIBLE);
    }

    private void setLayoutConnecting(BtDevice btDevice) {
        String s = R.string.connect_dev + btDevice.getName() + " (" + btDevice.getAddress() + ")";
        tvDynamicModeContent.setText(s);
    }

    private void setLayoutConnected(BtDevice btDevice) {
        pbTop.setVisibility(View.GONE);
        dynamicLayout.setBackgroundColor(Color.parseColor("#068DE5"));
        String s = R.string.connected_dev + btDevice.getName() + " (" + btDevice.getAddress() + ")";
        tvDynamicModeContent.setText(s);
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

    // Show a snackbar that inform the user that the settings for continuous tracking are not set
    private void showBluetoothSnackbar() {
        Snackbar permissionSnackbar = Snackbar.make(findViewById(R.id.mainConstraintLayout),
                "Bluetooth is not enabled!", Snackbar.LENGTH_LONG);
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
                checkBluetoothEnabled();
            }
        });
    }
}
