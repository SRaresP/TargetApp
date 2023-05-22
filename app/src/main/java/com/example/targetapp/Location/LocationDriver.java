package com.example.targetapp.Location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

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

public class LocationDriver {
	public static final String TAG = "LocationDriver";

	public static void sendLocationUpdate(@NonNull Context context) {
		TargetApp.getInstance().getExecutorService().execute(() -> {
			String response = "";
			//send and receive from server
			try {
				Socket socket = ServerHandler.updateLocation();
				response = ServerHandler.receive(socket).trim();
			} catch (IOException | EmptyMessageException e) {
				TargetApp.getInstance().getMainThreadHandler().post(() -> {
					Toast.makeText(context, "Server communication threw exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
				});
				Log.e(TAG, e.getMessage(), e);
			}
			//process response
			String finalResponse = response;
			TargetApp.getInstance().getMainThreadHandler().post(() -> {
				if (finalResponse.contains(ServerHandler.LOCATION_UPDATED)) {
					Toast.makeText(context, "Server reports successful location update!", Toast.LENGTH_LONG).show();
				} else if (finalResponse.contains(ServerHandler.NOT_FOUND)) {
					Toast.makeText(context, "Server reports user was not found on location update", Toast.LENGTH_LONG).show();
				} else if (finalResponse.contains(ServerHandler.WRONG_PASSWORD)) {
					Toast.makeText(context, "Server reports password was wrong on location update", Toast.LENGTH_LONG).show();
				} else if (finalResponse.contains(ServerHandler.UNDEFINED_CASE)) {
					Toast.makeText(context, "Server reports undefined case / request on location update", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context, "Server reports something unknown on location update", Toast.LENGTH_LONG).show();
				}
			});
		});
	}
}
