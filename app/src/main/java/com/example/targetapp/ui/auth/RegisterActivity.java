package com.example.targetapp.ui.auth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.example.targetapp.R;
import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.CurrentUser;
import com.example.targetapp.server_comm.ServerHandler;
import com.example.targetapp.ui.DebugActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.net.Socket;

public class RegisterActivity extends AppCompatActivity {
	private static final String TAG = "RegisterActivity";

	private TextInputEditText emailTIET;
	private TextInputEditText nameTIET;
	private TextInputEditText passwordTIET;
	private AppCompatButton registerB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		TargetApp targetApp = TargetApp.getInstance();

		emailTIET = findViewById(R.id.regEmailRegisterTIET);
		nameTIET = findViewById(R.id.regNameRegisterTIET);
		passwordTIET = findViewById(R.id.regPasswordRegisterTIET);
		registerB = findViewById(R.id.regRegisterB);

		registerB.setOnClickListener(view -> {
			String email = emailTIET.getText().toString();
			if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
				Toast.makeText(this, "Email address entered appears to be invalid", Toast.LENGTH_SHORT).show();
				return;
			}
			String name = nameTIET.getText().toString();
			String password = passwordTIET.getText().toString();
			CurrentUser.setCurrentUser(email, name, password, "");

			Toast.makeText(this, "Connecting to server", Toast.LENGTH_SHORT).show();
			targetApp.getExecutorService().execute(() -> {
				try {
					Socket socket = ServerHandler.register();
					String response = ServerHandler.receive(socket);
					targetApp.getMainThreadHandler().post(() -> {
						//THIS IF ONLY WORKS IF I USE "CONTAINS" INSTEAD OF "EQUALS"
						//AND I HAVE NO IDEA WHY GOD HELP US ALL
						if (response.contains(ServerHandler.REGISTERED)) {
							Intent intent = new Intent(this, DebugActivity.class);
							startActivity(intent);
						} else if (response.contains(ServerHandler.EMAIL_ALREADY_TAKEN)) {
							Toast.makeText(this, "That email is taken", Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(this, "Server sent an unexpected reply", Toast.LENGTH_SHORT).show();
						}
					});
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
					targetApp.getMainThreadHandler().post(() -> {
						Toast.makeText(this, "Error communicating with server", Toast.LENGTH_LONG).show();
					});
				}
			});
		});
	}
}