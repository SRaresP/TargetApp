package com.example.targetapp.server_comm;

import com.example.targetapp.TargetApp;

public class CurrentUser {
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

	public static String toText() {
		return email + TargetApp.USER_SEPARATOR + name + TargetApp.USER_SEPARATOR + password + TargetApp.USER_SEPARATOR + locationHistory;
	}

	public static Void fromText(String userString) {
		String[] parameterUser = userString.split(String.valueOf(TargetApp.USER_SEPARATOR));
		if (parameterUser.length != 4) return null;
		email = parameterUser[0];
		name = parameterUser[1];
		password = parameterUser[2];
		locationHistory = parameterUser[3];
		return null;
	}
}
