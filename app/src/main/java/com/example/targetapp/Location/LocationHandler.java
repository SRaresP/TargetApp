package com.example.targetapp.Location;

import com.example.targetapp.TargetApp;

//This class is used to process locations,
//which in the context of this entire server,
//are stored exclusively as Strings for ease of storage.
//Think of locationHistory as an array of Strings which is accessed
//by using this utility class.
public class LocationHandler {
	private static final int CAPACITY = 4;

	private LocationHandler() { }

	public static String AddLocation(String locationHistory, String locationToAdd) throws IllegalArgumentException {
		if (locationHistory == null)
		{
			throw new IllegalArgumentException("locationHistory was null");
		}

		if (locationToAdd == null)
		{
			throw new IllegalArgumentException("locationToAdd was null");
		}
		//get the amount of locations in this location history String
		//and the index of the oldest known location
		int locationsCount = 0;
		int oldestLocationStartIndex = locationHistory.length();

		for(int i = 0; i < locationHistory.length(); ++i)
		{
			if (locationHistory.charAt(i) == TargetApp.LOC_HISTORY_SEPARATOR)
			{
				++locationsCount;
				if (locationsCount == CAPACITY - 1)
				{
					oldestLocationStartIndex = i + 1;
				}
			}
		}

		//insert according to whether the history is full or not
		locationToAdd += TargetApp.LOC_HISTORY_SEPARATOR;
		if (locationsCount >= CAPACITY) {
			locationHistory = locationHistory.substring(0, oldestLocationStartIndex);
		}
		return locationToAdd + locationHistory;
	}

	public static String getLocationByIndex(String locationHistory, int locationIndex) {
		return locationHistory.split(String.valueOf(TargetApp.LOC_HISTORY_SEPARATOR))[locationIndex];
	}

	public static String getLastLocation(String locationHistory) {
		return locationHistory.split(String.valueOf(TargetApp.LOC_HISTORY_SEPARATOR))[0];
	}
}
