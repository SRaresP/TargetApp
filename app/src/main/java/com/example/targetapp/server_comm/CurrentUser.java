package com.example.targetapp.server_comm;

import com.example.targetapp.TargetApp;
import com.example.targetapp.storage.EncryptedStorageController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CurrentUser {
	private static final String TAG = "CurrentUser";
	private static final String SAVED_USER_FILENAME = "CurrentUser";
	public static String email = "";
	public static String name = "";
	public static String password = "";
	//manipulate using Location/LocationHandler
	public static String locationHistory = "";

	//do not call constructor, use static methods
	private CurrentUser() { }

	public static void setCurrentUser(String Email, String Name, String password, String locationHistory) {
		CurrentUser.email = Email;
		CurrentUser.name = Name;
		CurrentUser.password = password;
		CurrentUser.locationHistory = locationHistory;
	}

	public static void reset() {
		email = "";
		name = "";
		password = "";
		locationHistory = "";
	}

	public static String toText(boolean omitLocation) {
		if (omitLocation) {
			return email + TargetApp.USER_SEPARATOR + name + TargetApp.USER_SEPARATOR + password;
		} else {
			return email + TargetApp.USER_SEPARATOR + name + TargetApp.USER_SEPARATOR + password + TargetApp.USER_SEPARATOR + locationHistory;
		}
	}

	public static void fromText(String userString) throws IllegalArgumentException {
		String[] parameterUser = userString.split(String.valueOf(TargetApp.USER_SEPARATOR));
		if (parameterUser.length != 4) throw new IllegalArgumentException("userString was not properly formatted");
		email = parameterUser[0];
		name = parameterUser[1];
		password = parameterUser[2];
		locationHistory = parameterUser[3];
	}

	public static void saveToDisk() throws IOException {
		EncryptedStorageController encryptedStorageController = EncryptedStorageController.getInstance(TargetApp.getInstance().getApplicationContext());
		encryptedStorageController.add(SAVED_USER_FILENAME, toText(true));
	}

	public static void setCurrentUserFromDisk() throws IOException {
		EncryptedStorageController encryptedStorageController = EncryptedStorageController.getInstance(TargetApp.getInstance().getApplicationContext());
		fromText(encryptedStorageController.get(SAVED_USER_FILENAME));
	}
}
