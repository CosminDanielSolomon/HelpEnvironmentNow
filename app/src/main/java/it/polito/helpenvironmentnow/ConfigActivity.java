package it.polito.helpenvironmentnow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

public class ConfigActivity extends AppCompatActivity {

    private EditText editTextIp, editTextPort;
    private Button btnSave;
    private ConstraintLayout constraintLayout;
    private String ip, port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
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

                }
            }
        });
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
