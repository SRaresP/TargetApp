package com.example.targetapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.targetapp.Location.LocationHandler;
import com.example.targetapp.Location.LocationService;
import com.example.targetapp.R;
import com.example.targetapp.TargetApp;
import com.example.targetapp.databinding.ActivityMapBinding;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
	private static final String TAG = "MainActivity";

	private GoogleMap map;
	private ActivityMapBinding binding;

	private AppCompatButton mainGetCodeBTN;
	private AppCompatButton mainLogOutB;
	private Dialog codeDialog;
	private SwitchCompat moveSwitch;

	private static LocationRequest locationRequest;
	private static LocationCallback locationCallBack;

	private FusedLocationProviderClient fusedLocationProviderClient;

	private Marker userMarker;

	private boolean shouldStartLocationServiceOnPause = true;
	private boolean camShouldTrackPosition = true;

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
	public void startUpdates(
			@NonNull MapActivity activity,
			@NonNull FusedLocationProviderClient fusedLocationProviderClient) {
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
					// TODO: remove randomness
					LatLng newPosition = new LatLng(
							location.getLatitude() + Math.random() / 100,
							location.getLongitude() + Math.random() / 100
					);
					userMarker.remove();
					userMarker = map.addMarker(new MarkerOptions()
							.position(newPosition)
					);
					if (camShouldTrackPosition) {
						map.animateCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 15));
					}

					//add location to current user
					String lastLocation = String.valueOf((new Date()).getTime()) +
							TargetApp.DATE_LAT_LONG_SEPARATOR +
							(location.getLatitude() + Math.random() / 100) +
							TargetApp.DATE_LAT_LONG_SEPARATOR +
							(location.getLongitude() + Math.random() / 100);
					try {
						CurrentUser.locationHistory = LocationHandler.AddLocation(CurrentUser.locationHistory, lastLocation);
					} catch (IllegalArgumentException e) {
						Log.e(TAG, e.getMessage(), e);
					}

					//sync to server
					TargetApp.getInstance().getExecutorService().execute(() -> {
						try {
							Socket socket = ServerHandler.updateLocation();
							String response = ServerHandler.receive(socket).trim();
							TargetApp.getInstance().getMainThreadHandler().post(() -> {
								if (response.contains(ServerHandler.LOCATION_UPDATED)) {
									try {
										int newInterval = Integer.parseInt(response.split(String.valueOf(TargetApp.COMM_SEPARATOR))[1]);
										locationRequest.setInterval(newInterval);
										fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
										Toast.makeText(MapActivity.this, "Server reports successful location update!", Toast.LENGTH_SHORT).show();
									} catch (Exception e) {
										Log.e(TAG, e.getMessage());
										Toast.makeText(MapActivity.this, "Exception on location update!", Toast.LENGTH_SHORT).show();
									}
								} else if (response.contains(ServerHandler.NOT_FOUND)) {
									Toast.makeText(MapActivity.this, "Server reports user was not found on location update", Toast.LENGTH_LONG).show();
								} else if (response.contains(ServerHandler.WRONG_PASSWORD)) {
									Toast.makeText(MapActivity.this, "Server reports password was wrong on location update", Toast.LENGTH_LONG).show();
								} else if (response.contains(ServerHandler.UNDEFINED_CASE)) {
									Toast.makeText(MapActivity.this, "Server reports undefined case / request on location update", Toast.LENGTH_LONG).show();
								} else {
									Toast.makeText(MapActivity.this, "Server reports something unknown on location update", Toast.LENGTH_LONG).show();
								}
							});
						} catch (IOException | EmptyMessageException e) {
							Log.e(TAG, e.getMessage());
						}
					});
				});
			}
		};
		try {
			fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
		} catch (SecurityException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	@SuppressLint("MissingPermission")
	public static void stopUpdates(@NonNull FusedLocationProviderClient fusedLocationProviderClient) {
		fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
	}

	@SuppressLint("MissingPermission")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityMapBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
		AlertDialog noNotifPerdialog;
		if (!notificationManager.areNotificationsEnabled()) {
			noNotifPerdialog = new AlertDialog.Builder(this)
					.setCancelable(false)
					.setTitle("No notification permission.")
					.setMessage("Please enable app notifications for OverseerApp and try restarting the application. The app will quit in 10 seconds.")
					.create();
			noNotifPerdialog.show();
			Timer timer = new Timer(true);
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					noNotifPerdialog.dismiss();
					MapActivity.this.finish();
				}
			}, 10000);
		}

		codeDialog = new Dialog(MapActivity.this);
		codeDialog.setCancelable(false);
		codeDialog.setContentView(R.layout.dialog_code);
		codeDialog.findViewById(R.id.codeBackB).setOnClickListener((viewDialog) -> {
			codeDialog.cancel();
		});

		moveSwitch = findViewById(R.id.mainMoveCamS);
		moveSwitch.setOnClickListener((view) -> {
			camShouldTrackPosition = moveSwitch.isChecked();
		});
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
		if (userMarker != null) {
			userMarker.remove();
		}
		userMarker = map.addMarker(new MarkerOptions()
				.position(new LatLng(0, 0)));
		startUpdates(this, fusedLocationProviderClient);
	}

	@SuppressLint("MissingPermission")
	@Override
	protected void onResume() {
		super.onResume();
		shouldStartLocationServiceOnPause = true;
		Intent intent = new Intent(this, LocationService.class);
		stopService(intent);

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		TargetApp targetApp = TargetApp.getInstance();

		mainGetCodeBTN = findViewById(R.id.mainGetCodeB);
		mainLogOutB = findViewById(R.id.mainLogOutB);

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

		mainGetCodeBTN.setOnClickListener(view -> {
			targetApp.getExecutorService().execute(() -> {
				try {
					Socket socket = ServerHandler.getUniqueCode();
					String response = ServerHandler.receive(socket).trim();

					if (response.contains(ServerHandler.DELIVERED_CODE)) {
						String code = response.split(String.valueOf(TargetApp.COMM_SEPARATOR))[1];
						targetApp.getMainThreadHandler().post(() -> {
							AppCompatTextView codeTV = codeDialog.findViewById(R.id.codeCodeTV);
							codeTV.setText(code);
							codeDialog.show();
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
			Intent logOutIntent = new Intent(this, AuthActivity.class);
			logOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			logOutIntent.putExtra("useSavedCredentials", false);
			stopUpdates(fusedLocationProviderClient);
			shouldStartLocationServiceOnPause = false;
			startActivity(logOutIntent);
			finish();
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopUpdates(fusedLocationProviderClient);
		if (shouldStartLocationServiceOnPause) {
			Intent intent = new Intent(this, LocationService.class);
			startForegroundService(intent);
		}
	}
}