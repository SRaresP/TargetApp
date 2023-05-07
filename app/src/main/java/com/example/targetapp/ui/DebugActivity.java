package com.example.targetapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.targetapp.Location.LocationDriver;
import com.example.targetapp.Location.LocationHandler;
import com.example.targetapp.R;
import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.CurrentUser;
import com.example.targetapp.server_comm.ServerHandler;
import com.example.targetapp.server_comm.exceptions.EmptyMessageException;
import com.example.targetapp.ui.auth.AuthActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class DebugActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private TextView mainLatValueTV;
	private TextView mainLonValueTV;
	private TextView mainHistoryValueTV;
	private TextView mainDateValueTV;
	private AppCompatButton mainSendBTN;
	private AppCompatButton mainGetCodeBTN;
	private TextView mainCodeValueTV;
	private AppCompatButton mainLogOutB;

	FusedLocationProviderClient fusedLocationProviderClient;

	@SuppressLint("MissingPermission")
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == TargetApp.PERM_REQ_CODE) {
			if ((grantResults.length < 1) || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
				Toast.makeText(this, "App needs location permissions to run", Toast.LENGTH_LONG).show();
				finish();
			} else {
				Toast.makeText(this, "Thank you!", Toast.LENGTH_LONG).show();
			}
		}
	}

	@SuppressLint("MissingPermission")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);

		TargetApp targetApp = TargetApp.getInstance();

		mainLatValueTV = findViewById(R.id.mainLatValueTV);
		mainLonValueTV = findViewById(R.id.mainLonValueTV);
		mainSendBTN = findViewById(R.id.mainSendBTN);
		mainHistoryValueTV = findViewById(R.id.mainHistoryValueTV);
		mainDateValueTV = findViewById(R.id.mainDateValueTV);
		mainGetCodeBTN = findViewById(R.id.mainGetCodeB);
		mainCodeValueTV = findViewById(R.id.mainCodeValueTV);
		mainLogOutB = findViewById(R.id.mainLogOutB);
		mainSendBTN = findViewById(R.id.mainSendBTN);

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

		mainGetCodeBTN.setOnClickListener(view -> {
			targetApp.getExecutorService().execute(() -> {
				try {
					Socket socket = ServerHandler.getUniqueCode();
					String response = ServerHandler.receive(socket).trim();

					if (response.contains(ServerHandler.DELIVERED_CODE)) {
						String code = response.split(String.valueOf(TargetApp.COMM_SEPARATOR))[1];
						targetApp.getMainThreadHandler().post(() -> {
							mainCodeValueTV.setText(code);
						});
					} else {
						targetApp.getMainThreadHandler().post(() -> {
							if (response.contains(ServerHandler.NOT_FOUND)) {
								Toast.makeText(this, "Server could not find an account with that email address", Toast.LENGTH_LONG).show();
							} else if (response.contains(ServerHandler.WRONG_PASSWORD)) {
								Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(this, "Server sent an unexpected reply", Toast.LENGTH_SHORT).show();
							}
						});
					}
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			});
		});

		mainLogOutB.setOnClickListener(view -> {
			Intent intent = new Intent(this, AuthActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("useSavedCredentials", false);
			LocationDriver.stop(fusedLocationProviderClient);
			startActivity(intent);
			finish();
		});

		mainSendBTN.setOnClickListener(view -> {
			LocationDriver.sendLocationUpdate(this);
		});
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		LocationDriver.start(this, fusedLocationProviderClient);
	}

	public void showLocation(double latitude, double longitude) {
		Date currentDate = new Date();
		mainDateValueTV.setText(currentDate.toString());
		mainLatValueTV.setText(String.valueOf(latitude));
		mainLonValueTV.setText(String.valueOf(longitude));
		//update history ui
		StringBuilder historyString = new StringBuilder();
		String[] historyArray = CurrentUser.locationHistory.split(String.valueOf(TargetApp.LOC_HISTORY_SEPARATOR));
		for (String s : historyArray) {
			historyString.append(s).append("\n");
		}
		mainHistoryValueTV.setText(historyString.toString());
	}
}