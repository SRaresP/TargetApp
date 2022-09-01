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

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

//TODO: send provider thru intent
public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	TextView mainLatValueTV;
	TextView mainLonValueTV;
	AppCompatButton mainSendBTN;
	TextView mainHistoryValueTV;
	TextView mainDateValueTV;

	LocationRequest locationRequest;
	LocationCallback locationCallBack;
	FusedLocationProviderClient fusedLocationProviderClient;

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

	@SuppressLint("MissingPermission")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!arePermissionsGranted(this)) {
			requestPermissions(this);
		}

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
					Date currentDate = new Date();
					mainDateValueTV.setText(currentDate.toString());
					mainLatValueTV.setText(String.valueOf(location.getLatitude()));
					mainLonValueTV.setText(String.valueOf(location.getLongitude()));

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
					String[] historyArray = CurrentUser.locationHistory.split(String.valueOf(TargetApp.LOC_HISTORY_SEPARATOR));
					StringBuilder historyString = new StringBuilder();
					for (String s : historyArray) {
						historyString.append(s).append("\n");
					}
					mainHistoryValueTV.setText(historyString.toString());
				});
			}
		};

		try {
			fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
		} catch (SecurityException e) {
			Log.e(TAG, e.getMessage(), e);
		}

		mainSendBTN.setOnClickListener(view -> {
			TargetApp.getInstance().getExecutorService().execute(() -> {
				String response = "";
				try {
					Socket socket = ServerHandler.updateLocation();
					response = ServerHandler.receive(socket);
				} catch (IOException | EmptyMessageException e) {
					TargetApp.getInstance().getMainThreadHandler().post(() -> {
						Toast.makeText(this, "Server communication threw exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
					});
					Log.e(TAG, e.getMessage(), e);
				}

				String finalResponse = response;
				TargetApp.getInstance().getMainThreadHandler().post(() -> {
					switch (finalResponse) {
						case ServerHandler.LOGGED_IN:
							Toast.makeText(this, "Server reports jibberish", Toast.LENGTH_LONG).show();
							break;
						case ServerHandler.REGISTERED:
							Toast.makeText(this, "Server reports jibberish", Toast.LENGTH_LONG).show();
							break;
						case ServerHandler.EDITED:
							Toast.makeText(this, "Server reports jibberish", Toast.LENGTH_LONG).show();
							break;
						case ServerHandler.LOCATION_UPDATED:
							Toast.makeText(this, "Server reports succesful location update!", Toast.LENGTH_LONG).show();
							break;
						case ServerHandler.NOT_FOUND:
							Toast.makeText(this, "Server reports jibberish", Toast.LENGTH_LONG).show();
							break;
						case ServerHandler.WRONG_PASSWORD:
							Toast.makeText(this, "Server reports jibberish", Toast.LENGTH_LONG).show();
							break;
						case ServerHandler.EMAIL_ALREADY_TAKEN:
							Toast.makeText(this, "Server reports jibberish", Toast.LENGTH_LONG).show();
							break;
						case ServerHandler.UNDEFINED_CASE:
							Toast.makeText(this, "Server reports jibberish", Toast.LENGTH_LONG).show();
							break;
						default:
							Toast.makeText(this, "Server reports something undefined", Toast.LENGTH_LONG).show();
							break;
					}
				});
			});
		});

//		if (!initialised) {
//			Intent intent = new Intent(this, MapsActivity.class);
//			startActivity(intent);
//			initialised = true;
//		}


	}
}