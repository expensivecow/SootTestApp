package com.helloworld.mike.helloworld;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    public static final String NAMEMSG = "com.helloworld.mike.NAMEMSG";
    private static final int ACCESS_FINE_LOCATION = 0;
    private static final int ACCESS_COARSE_LOCATION = 1;

    private class PermissionPair {
        private String _permission;
        private int _permissionCode;

        PermissionPair(String permission, int permissionCode) {
            _permission = permission;
            _permissionCode = permissionCode;
        }
        public void setPermission(String permission) {
            this._permission = permission;
        }
        public void setPermissionCode(int permissionCode) {
            this._permissionCode = permissionCode;
        }
        public String getPermission() { return this._permission; }
        public int getPermissionCode() {
            return this._permissionCode;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    public void onGo(View view) {
        EditText name = (EditText) findViewById(R.id.name);

        try {
            validateName(name.getText().toString());

            // Create Location Permissions
            ArrayList<PermissionPair> locationPermissions = new ArrayList<PermissionPair>();
            locationPermissions.add(new PermissionPair(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_LOCATION));
            locationPermissions.add(new PermissionPair(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestLocationPermissions(locationPermissions);

                // Ensure location mode is set to low/high and not device only/off
                if(getLocationMode(this) < 2 )
                {
                    throw new Exception("Please set the mode of your location to non-device only within the settings.");
                }
            }

            // Switch Activities
            Intent intent = new Intent(this, ReplyActivity.class);
            intent.putExtra(NAMEMSG, name.getText().toString());
            startActivity(intent);
        }
        catch (Exception e) {
            // Display Error Message
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void validateName(String text) throws Exception {
        if (text.equals(null) || text.isEmpty() || text.trim().length() == 0) {
            throw new Exception("Please fill in a name.");
        }

        return;
    }

    private void requestLocationPermissions(ArrayList<PermissionPair> permissions) throws Exception {
        boolean hasPermissions = true;
        int j = 0;

        for (Iterator<PermissionPair> i = permissions.iterator(); i.hasNext();) {
            PermissionPair permission = i.next();

            if (ContextCompat.checkSelfPermission(this, permission.getPermission()) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{permission.getPermission()}, permission.getPermissionCode());
                }
                hasPermissions = false;
            }
        }

        if(!hasPermissions) {
            throw new Exception("Need Location Permissions from Android Phone. Please Add The Permissions and Try Again.");
        }
    }

    private int getLocationMode(Context context) throws Settings.SettingNotFoundException {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
    }
}
