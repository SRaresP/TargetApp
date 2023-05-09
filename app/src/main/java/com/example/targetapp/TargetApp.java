package com.example.targetapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TargetApp extends Application {
	public static final int PERM_REQ_CODE = 101;
	public static final int INTERVAL_USUAL = 3000;
	public static final int INTERVAL_FASTEST = 3000;

	//Server communication constants
	// used to separate pieces of a package to send (email, name, location etc)
	public static final char COMM_SEPARATOR = '■';
	// used to separate a location's date, latitude and longitude
	public static final char DATE_LAT_LONG_SEPARATOR = '²';
	// used to separate locations in a location history string
	public static final char LOC_HISTORY_SEPARATOR = 'ⁿ';
	// used to separate user info like email, name, password etc
	public static final char USER_SEPARATOR = '√';

	public static final String STATUS_NOTIFICATION_CHANNEL = "status_notification_channel";

	public static final int STATUS_NOTIFICATION_ID = 1;

	private static TargetApp instance;

	private final ExecutorService executorService = Executors.newFixedThreadPool(4);
	private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

	private NotificationChannel statusNotificationChannel;

	public void onCreate() {
		super.onCreate();
		instance = this;

		statusNotificationChannel = new NotificationChannel(
				STATUS_NOTIFICATION_CHANNEL,
				"Status notification channel",
				NotificationManager.IMPORTANCE_NONE);
		NotificationManager notificationManager = getSystemService(NotificationManager.class);
		notificationManager.createNotificationChannel(statusNotificationChannel);
	}

	public static TargetApp getInstance() {
		return instance;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public Handler getMainThreadHandler() {
		return mainThreadHandler;
	}

	public NotificationChannel getStatusNotificationChannel() { return statusNotificationChannel; }


}
