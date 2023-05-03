package com.example.targetapp.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.targetapp.R;
import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.CurrentUser;
import com.example.targetapp.server_comm.ServerHandler;
import com.example.targetapp.ui.DebugActivity;
import com.example.targetapp.ui.custom.LoadingView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.net.Socket;

public class AuthActivity extends AppCompatActivity {
	private static final String TAG = "AuthActivity";

	private TextInputEditText emailTIET;
	private TextInputEditText passwordTIET;
	private AppCompatButton loginB;
	private AppCompatButton registerB;

	private void setUpForManualLogin(final @Nullable AlertDialog alertDialog, final @Nullable String toToast) {
		TargetApp.getInstance().getMainThreadHandler().post(() -> {
			emailTIET.setEnabled(true);
			passwordTIET.setEnabled(true);
			loginB.setEnabled(true);
			registerB.setEnabled(true);
			if (toToast != null) {
				Toast.makeText(this, toToast, Toast.LENGTH_SHORT).show();
			}
			if (alertDialog != null) {
				alertDialog.dismiss();
			}
		});
	}

	private void loginAndContinueAsync(@NonNull final AlertDialog alertDialog) {
		Toast.makeText(this, "Connecting to server", Toast.LENGTH_SHORT).show();
		TargetApp targetApp = TargetApp.getInstance();
		targetApp.getExecutorService().execute(() -> {
			try {
				Socket socket = ServerHandler.login();
				String response = ServerHandler.receive(socket);
				//THIS IF ONLY WORKS IF I USE "CONTAINS" INSTEAD OF "EQUALS"
				//AND I HAVE NO IDEA WHY GOD HELP US ALL
				if (response.contains(ServerHandler.LOGGED_IN)) {
					try {
						CurrentUser.saveToDisk();
					} catch (IOException e) {
						Log.e(TAG, e.getMessage(), e);
						targetApp.getMainThreadHandler().post(() -> {
							setUpForManualLogin(alertDialog, "Could not save user data to disk, you will have to log in again next time");
						});
					}
					targetApp.getMainThreadHandler().post(() -> {
						//alertDialog.dismiss();
						Intent intent = new Intent(this, DebugActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						finish();
					});
				} else {
					targetApp.getMainThreadHandler().post(() -> {
						if (response.contains(ServerHandler.NOT_FOUND)) {
							setUpForManualLogin(alertDialog, "Server could not find an account with that email address");
						} else if (response.contains(ServerHandler.WRONG_PASSWORD)) {
							setUpForManualLogin(alertDialog, "Entered password does not match");
						} else {
							setUpForManualLogin(alertDialog, "Server sent an unexpected reply");
						}
					});
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
				targetApp.getMainThreadHandler().post(() -> {
					setUpForManualLogin(alertDialog, "Error communicating with server");
				});
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_auth);

		TargetApp targetApp = TargetApp.getInstance();

		RelativeLayout innerRelLayout = findViewById(R.id.logInnerRelLayout);
		loginB = findViewById(R.id.logLoginB);
		registerB = findViewById(R.id.logRegisterB);

		emailTIET = findViewById(R.id.logEmailTIET);
		passwordTIET = findViewById(R.id.logPasswordTIET);

		loginB.setOnClickListener(view -> {
			String email = emailTIET.getText().toString();
			String password = passwordTIET.getText().toString();
			CurrentUser.setCurrentUser(email, "", password, "");

			//LoadingView loadingView = new LoadingView(innerRelLayout, this, "Logging in", null, new AppCompatButton[] {loginB, registerB}, false).show();
			AlertDialog alertDialogClassicLogin = new AlertDialog.Builder(this)
					.setView(new LoadingView(this, "Logging in", false))
					.setCancelable(false)
					.create();
			alertDialogClassicLogin.show();
			loginAndContinueAsync(alertDialogClassicLogin);
		});

		registerB.setOnClickListener(view -> {
			Intent intent = new Intent(this, RegisterActivity.class);
			startActivity(intent);
		});

		if (getIntent().getBooleanExtra("useSavedCredentials", true)) {
			AlertDialog alertDialogAutoLogin = new AlertDialog.Builder(this)
					.setView(new LoadingView(this, "Attempting to log in using stored credentials", true))
					.setCancelable(false)
					.create();
			alertDialogAutoLogin.show();
			try {
				CurrentUser.setFromDisk();
				loginAndContinueAsync(alertDialogAutoLogin);
			} catch (IOException e) {
				//only setCurrentUserFromDisk can make the thread reach this path by throwing an exception
				Log.i(TAG, e.getMessage() + "; user is not logged in", e);
				setUpForManualLogin(null, null);
			}
		}
		else {
			setUpForManualLogin(null, null);
		}
	}
}