package it.polito.helpenvironmentnow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

public class ConfigActivity extends AppCompatActivity {

    private SharedPreferences sharedPref;
    private EditText editTextIp, editTextPort;
    private Button btnSave;
    private ConstraintLayout constraintLayout;
    private String ip, port;
    private String IP_KEY, IP_DEF, PORT_KEY, PORT_DEF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        IP_KEY = getString(R.string.IP_KEY);
        IP_DEF = getString(R.string.IP_DEF);
        PORT_KEY = getString(R.string.PORT_KEY);
        PORT_DEF = getString(R.string.PORT_DEF);
        sharedPref = getSharedPreferences(getString(R.string.config_file), Context.MODE_PRIVATE);
        constraintLayout = findViewById(R.id.configLayout);
        editTextIp = findViewById(R.id.editTextIp);
        editTextPort = findViewById(R.id.editTextPort);
        btnSave = findViewById(R.id.buttonSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip = editTextIp.getText().toString();
                port = editTextPort.getText().toString();
                if(validateIp(ip) && validatePort(port)) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(IP_KEY, ip);
                    editor.putString(PORT_KEY, port);
                    editor.commit();
                    Snackbar.make(constraintLayout, "IP and PORT saved",
                            Snackbar.LENGTH_LONG).show();
                }
            }
        });

        String ipSave = sharedPref.getString(IP_KEY, IP_DEF);
        String portSave = sharedPref.getString(PORT_KEY, PORT_DEF);
        if(!ipSave.equals(IP_DEF))
            editTextIp.setText(ipSave);
        if(!portSave.equals(PORT_DEF))
            editTextPort.setText(portSave);
    }

    private boolean validateIp(String ip) {
        boolean valid = Patterns.IP_ADDRESS.matcher(ip).matches();
        if(!valid) {
            Snackbar.make(constraintLayout, "No valid ip address",
                    Snackbar.LENGTH_LONG).show();
        }

        return valid;
    }

    private boolean validatePort(String port) {
        boolean valid = true;
        int convertedPort = -1;

        try {
            convertedPort = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            valid = false;
        }

        if(!(valid && convertedPort >= 0 && convertedPort <= 65535))
            valid = false;

        if(!valid) {
            Snackbar.make(constraintLayout, "No valid port",
                    Snackbar.LENGTH_LONG).show();
        }

        return valid;
    }
}
