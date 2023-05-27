package com.example.targetapp.Location;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.targetapp.R;
import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.CurrentUser;
import com.example.targetapp.server_comm.ServerHandler;
import com.example.targetapp.server_comm.exceptions.EmptyMessageException;
import com.example.targetapp.ui.DebugActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class LocationService extends Service {
	private static final String TAG = "LocationService";
	private static final int STOP_REQUEST_CODE = 1;

	private static final String ACTION_STOP_TRACKING = "targetapp.intent.action.stop_tracking";

	private static LocationRequest locationRequest;
	private static LocationCallback locationCallBack;

	private FusedLocationProviderClient fusedLocationProviderClient;

	@SuppressLint("MissingPermission")
	private static void startUpdates(
			@NonNull Context context,
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
					//add location to current user
					String lastLocation = String.valueOf((new Date()).getTime()) +
							TargetApp.DATE_LAT_LONG_SEPARATOR +
							(location.getLatitude() + Math.random() / 10) +
							TargetApp.DATE_LAT_LONG_SEPARATOR +
							(location.getLongitude() + Math.random() / 10);
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
										Toast.makeText(context, "Server reports successful location update!", Toast.LENGTH_SHORT).show();
									} catch (Exception e) {
										Log.e(TAG, e.getMessage());
										Toast.makeText(context, "Exception on location update!", Toast.LENGTH_SHORT).show();
									}
								} else if (response.contains(ServerHandler.NOT_FOUND)) {
									Toast.makeText(context, "Server reports user was not found on location update", Toast.LENGTH_LONG).show();
								} else if (response.contains(ServerHandler.WRONG_PASSWORD)) {
									Toast.makeText(context, "Server reports password was wrong on location update", Toast.LENGTH_LONG).show();
								} else if (response.contains(ServerHandler.UNDEFINED_CASE)) {
									Toast.makeText(context, "Server reports undefined case / request on location update", Toast.LENGTH_LONG).show();
								} else {
									Toast.makeText(context, "Server reports something unknown on location update", Toast.LENGTH_LONG).show();
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
	private static void stopUpdates(@NonNull FusedLocationProviderClient fusedLocationProviderClient) {
		fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		// Intentional as binding isn't needed
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction() == ACTION_STOP_TRACKING) {
			stopUpdates(fusedLocationProviderClient);
			stopForeground(true);
			stopSelf();
		}

		Intent toAppIntent = new Intent(this, DebugActivity.class);
		toAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent notificationPendingIntent = PendingIntent.getActivity(this, 0, toAppIntent, PendingIntent.FLAG_IMMUTABLE);

		Intent stopTrackingIntent = new Intent(this, LocationService.class)
				.setAction(ACTION_STOP_TRACKING);

		PendingIntent pStopTrackingIntent = PendingIntent.getService(
				this,
				STOP_REQUEST_CODE,
				stopTrackingIntent,
				PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TargetApp.STATUS_NOTIFICATION_CHANNEL)
				.setSmallIcon(R.drawable.ic_baseline_location_on_24)
				.setContentTitle("Your location is being tracked")
				.setContentText("Tap to open the app.")
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setAutoCancel(false)
				.setContentIntent(notificationPendingIntent)
				.setOngoing(true)
				.setCategory(Notification.CATEGORY_STATUS)
				.addAction(R.drawable.ic_baseline_location_off_24, "Stop", pStopTrackingIntent);
		Notification statusNotification = builder.build();

		startForeground(TargetApp.STATUS_NOTIFICATION_ID, statusNotification);

		startUpdates(this, fusedLocationProviderClient);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		stopUpdates(fusedLocationProviderClient);
	}
}
