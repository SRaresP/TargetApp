package com.example.targetapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.targetapp.Location.LocationHandler;
import com.example.targetapp.R;
import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.CurrentUser;
import com.example.targetapp.server_comm.ServerHandler;
import com.example.targetapp.server_comm.exceptions.EmptyMessageException;
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

//TODO: send provider thru intent
public class DebugActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private TextView mainLatValueTV;
	private TextView mainLonValueTV;
	private TextView mainHistoryValueTV;
	private TextView mainDateValueTV;
	private AppCompatButton mainSendBTN;

	private LocationRequest locationRequest;
	private LocationCallback locationCallBack;
	private FusedLocationProviderClient fusedLocationProviderClient;

	private static boolean arePermissionsGranted(Context context) {
		boolean hasCoarsePermission =
				ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
						== PackageManager.PERMISSION_GRANTED;
		boolean hasFinePermission =
				ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
						== PackageManager.PERMISSION_GRANTED;
		boolean hasInternetPermission =
				ActivityCompat.checkSelfPermission(context, Manifest.permission.INTERNET)
						== PackageManager.PERMISSION_GRANTED;
		return (hasCoarsePermission && hasFinePermission && hasInternetPermission);
	}

	private static void requestPermissions(Activity activity) {
		if (ActivityCompat.shouldShowRequestPermissionRationale(
				activity,
				Manifest.permission.ACCESS_COARSE_LOCATION)) {
			//TODO: display a dialogue to explain why the app needs the permission
		}

		ActivityCompat.requestPermissions(
				activity,
				new String[]{
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.INTERNET},
				TargetApp.PERM_REQ_CODE);


	}

	@SuppressLint("MissingPermission")
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == TargetApp.PERM_REQ_CODE) {
			if ((grantResults.length < 1) || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
				Toast.makeText(this, "App needs all of these permissions to run", Toast.LENGTH_LONG).show();
				finish();
			} else {
				Toast.makeText(this, "Thank you!", Toast.LENGTH_LONG).show();
			}
		}
	}

	private void displayLatLon(LatLng latLng) {
		mainLatValueTV.setText(String.valueOf(latLng.latitude));
		mainLonValueTV.setText(String.valueOf(latLng.longitude));
	}

	private void sendLocationUpdate() {
		TargetApp.getInstance().getExecutorService().execute(() -> {
			String response = "";
			//send and receive from server
			try {
				Socket socket = ServerHandler.updateLocation();
				response = ServerHandler.receive(socket);
			} catch (IOException | EmptyMessageException e) {
				TargetApp.getInstance().getMainThreadHandler().post(() -> {
					Toast.makeText(this, "Server communication threw exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
				});
				Log.e(TAG, e.getMessage(), e);
			}
			//process response
			String finalResponse = response;
			TargetApp.getInstance().getMainThreadHandler().post(() -> {
				if (finalResponse.contains(ServerHandler.LOCATION_UPDATED)) {
					Toast.makeText(this, "Server reports successful location update!", Toast.LENGTH_LONG).show();
				} else if (finalResponse.contains(ServerHandler.NOT_FOUND)) {
					Toast.makeText(this, "Server reports user was not found on location update", Toast.LENGTH_LONG).show();
				} else if (finalResponse.contains(ServerHandler.WRONG_PASSWORD)) {
					Toast.makeText(this, "Server reports password was wrong on location update", Toast.LENGTH_LONG).show();
				} else if (finalResponse.contains(ServerHandler.UNDEFINED_CASE)) {
					Toast.makeText(this, "Server reports undefined case / request on location update", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, "Server reports something unknown on location update", Toast.LENGTH_LONG).show();
				}
			});
		});
	}

	@SuppressLint("MissingPermission")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);

		if (!arePermissionsGranted(this)) {
			requestPermissions(this);
		}

		TargetApp targetApp = TargetApp.getInstance();

		mainLatValueTV = findViewById(R.id.mainLatValueTV);
		mainLonValueTV = findViewById(R.id.mainLonValueTV);
		mainSendBTN = findViewById(R.id.mainSendBTN);
		mainHistoryValueTV = findViewById(R.id.mainHistoryValueTV);
		mainDateValueTV = findViewById(R.id.mainDateValueTV);

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

		locationRequest = LocationRequest.create();
		locationRequest.setInterval(TargetApp.INTERVAL_USUAL);
		locationRequest.setFastestInterval(TargetApp.INTERVAL_FASTEST);
		locationRequest.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY);

		locationCallBack = new LocationCallback() {
			@SuppressLint("MissingPermission")
			@Override
			public void onLocationResult(@NonNull LocationResult locationResult) {
				super.onLocationResult(locationResult);
				fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
					//update current location ui
					Date currentDate = new Date();
					mainDateValueTV.setText(currentDate.toString());
					mainLatValueTV.setText(String.valueOf(location.getLatitude()));
					mainLonValueTV.setText(String.valueOf(location.getLongitude()));

					//add location to current user
					String lastLocation = String.valueOf(currentDate.getTime()) +
							TargetApp.DATE_LAT_LONG_SEPARATOR +
							location.getLatitude() +
							TargetApp.DATE_LAT_LONG_SEPARATOR +
							location.getLongitude();
					try {
						CurrentUser.locationHistory = LocationHandler.AddLocation(CurrentUser.locationHistory, lastLocation);
					} catch (IllegalArgumentException e) {
						Log.e(TAG, e.getMessage(), e);
					}

					//update history ui
					String[] historyArray = CurrentUser.locationHistory.split(String.valueOf(TargetApp.LOC_HISTORY_SEPARATOR));
					StringBuilder historyString = new StringBuilder();
					for (String s : historyArray) {
						historyString.append(s).append("\n");
					}
					mainHistoryValueTV.setText(historyString.toString());

					//sync to server
					sendLocationUpdate();
				});
			}
		};

		try {
			fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
		} catch (SecurityException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
}