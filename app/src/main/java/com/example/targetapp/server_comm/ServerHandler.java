package com.example.targetapp.server_comm;

import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.exceptions.EmptyMessageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Date;

public class ServerHandler {
	private static final String IP = "192.168.100.2";
	private static final int PORT = 8000;

	//server request types
	private static final String LOGIN = "LOGIN";
	private static final String REGISTER = "REGISTER";
	private static final String EDIT = "EDIT";
	private static final String LOCATION_UPDATE = "LOCATION_UPDATE";

	//replies from server
	//positive
	public static final String LOGGED_IN = "LOGGED_IN";
	public static final String REGISTERED = "REGISTERED";
	public static final String EDITED = "EDITED";
	public static final String LOCATION_UPDATED = "LOCATION_UPDATED";
	//negative
	public static final String NOT_FOUND = "NOT_FOUND";
	public static final String WRONG_PASSWORD = "WRONG_PASSWORD";
	public static final String EMAIL_ALREADY_TAKEN = "EMAIL_ALREADY_TAKEN";
	//code problem
	public static final String UNDEFINED_CASE = "UNDEFINED_CASE";

	//Do not call constructor, use static methods
	private ServerHandler() {}

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
				.append(LOCATION_UPDATE)
				.append(TargetApp.COMM_SEPARATOR)
				.append(CurrentUser.toText(true))
				.append(TargetApp.COMM_SEPARATOR)
				.append(lastLocation);
		printWriter.write(stringBuilder.toString());
		printWriter.flush();
		return socket;
	}

	public static Socket login() throws IOException {
		Socket socket = new Socket(IP, PORT);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

		StringBuilder stringBuilder = new StringBuilder()
				.append(LOGIN)
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
				.append(REGISTER)
				.append(TargetApp.COMM_SEPARATOR)
				.append(CurrentUser.toText(true));
		printWriter.write(stringBuilder.toString());
		printWriter.flush();
		return socket;
	}

	public static String receive(Socket socket) throws IOException {

		char[] response = new char[500];

		InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		bufferedReader.read(response);
		socket.close();

		return String.valueOf(response);
	}
}
