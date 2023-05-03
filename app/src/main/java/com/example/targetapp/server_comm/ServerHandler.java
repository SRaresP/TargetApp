package com.example.targetapp.server_comm;

import androidx.annotation.NonNull;

import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.exceptions.EmptyMessageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerHandler {
	private static final String IP = "192.168.100.2";
	private static final int PORT = 8000;

	//server request types
	private static final String LOGIN_TARGET = "LOGIN_TARGET";
	private static final String REGISTER_TARGET = "REGISTER_TARGET";
	private static final String EDIT_TARGET = "EDIT_TARGET";
	private static final String LOCATION_UPDATE_TARGET = "LOCATION_UPDATE_TARGET";
	private static final String GET_UNIQUE_CODE_TARGET = "GET_UNIQUE_CODE_TARGET";

	//replies from server
	//positive
	public static final String LOGGED_IN = "LOGGED_IN";
	public static final String REGISTERED = "REGISTERED";
	public static final String EDITED = "EDITED";
	public static final String LOCATION_UPDATED = "LOCATION_UPDATED";
	public static final String DELIVERED_CODE = "DELIVERED_CODE";
	//negative
	public static final String NOT_FOUND = "NOT_FOUND";
	public static final String WRONG_PASSWORD = "WRONG_PASSWORD";
	public static final String EMAIL_ALREADY_TAKEN = "EMAIL_ALREADY_TAKEN";
	//code problem
	public static final String UNDEFINED_CASE = "UNDEFINED_CASE";

	//Do not call constructor, use static methods
	private ServerHandler() {}

	public static Socket login() throws IOException {
		Socket socket = new Socket(IP, PORT);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

		StringBuilder stringBuilder = new StringBuilder()
				.append(LOGIN_TARGET)
				.append(TargetApp.COMM_SEPARATOR)
				.append(CurrentUser.toText(true));
		printWriter.write(stringBuilder.toString());
		printWriter.flush();
		return socket;
	}

	public static Socket register() throws IOException {
		Socket socket = new Socket(IP, PORT);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

		StringBuilder stringBuilder = new StringBuilder()
				.append(REGISTER_TARGET)
				.append(TargetApp.COMM_SEPARATOR)
				.append(CurrentUser.toText(true));
		printWriter.write(stringBuilder.toString());
		printWriter.flush();
		return socket;
	}

	public static Socket updateLocation() throws IOException, NullPointerException, EmptyMessageException {
		Socket socket = new Socket(IP, PORT);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

		if (CurrentUser.locationHistory == null) {
			throw new NullPointerException("History is null");
		}
		if (CurrentUser.locationHistory.equals("")) {
			throw new EmptyMessageException("History is empty");
		}

		String lastLocation = CurrentUser.locationHistory.split(String.valueOf(TargetApp.LOC_HISTORY_SEPARATOR))[0];
		StringBuilder stringBuilder = new StringBuilder()
				.append(LOCATION_UPDATE_TARGET)
				.append(TargetApp.COMM_SEPARATOR)
				.append(CurrentUser.toText(true))
				.append(TargetApp.COMM_SEPARATOR)
				.append(lastLocation);
		printWriter.write(stringBuilder.toString());
		printWriter.flush();
		return socket;
	}

	public static Socket getUniqueCode() throws IOException {
		Socket socket = new Socket(IP, PORT);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

		StringBuilder stringBuilder = new StringBuilder()
				.append(GET_UNIQUE_CODE_TARGET)
				.append(TargetApp.COMM_SEPARATOR)
				.append(CurrentUser.toText(true));
		printWriter.write(stringBuilder.toString());
		printWriter.flush();
		return socket;
	}

	public static String receive(final @NonNull Socket socket) throws IOException {

		char[] response = new char[500];

		InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		bufferedReader.read(response);
		socket.close();

		return String.valueOf(response);
	}
}
