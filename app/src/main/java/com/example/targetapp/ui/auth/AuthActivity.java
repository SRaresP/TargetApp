package com.example.targetapp.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.targetapp.R;
import com.example.targetapp.TargetApp;
import com.example.targetapp.server_comm.CurrentUser;
import com.example.targetapp.server_comm.ServerHandler;
import com.example.targetapp.ui.MapActivity;
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

	private static boolean arePermissionsGranted(@NonNull Context context) {
		boolean hasCoarsePermission =
				ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
						== PackageManager.PERMISSION_GRANTED;
		boolean hasFinePermission =
				ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
						== PackageManager.PERMISSION_GRANTED;
		boolean hasInternetPermission =
				ActivityCompat.checkSelfPermission(context, Manifest.permission.INTERNET)
						== PackageManager.PERMISSION_GRANTED;
		return (hasCoarsePermission && hasFinePermission && hasInternetPermission);
	}

	private static void requestPermissions(@NonNull Activity activity) {
		ActivityCompat.requestPermissions(
				activity,
				new String[]{
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.INTERNET},
				TargetApp.PERM_REQ_CODE);
	}

	@SuppressLint("MissingPermission")
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == TargetApp.PERM_REQ_CODE) {
			if ((grantResults.length < 1) || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
				Toast.makeText(this, "App needs location permissions to run", Toast.LENGTH_LONG).show();
				finish();
			} else {
				continueCreating();
				Toast.makeText(this, "Thank you!", Toast.LENGTH_LONG).show();
			}
		}
	}

	private void setUpForManualLogin(
			final @Nullable AlertDialog alertDialog,
			final @Nullable String toToast) {
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
						Intent intent = new Intent(this, MapActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
						alertDialog.dismiss();
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

	private void continueCreating() {
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
				setUpForManualLogin(alertDialogAutoLogin, null);
			}
		}
		else {
			setUpForManualLogin(null, null);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_auth);

		if (!arePermissionsGranted(this)) {
			requestPermissions(this);
		} else {
			continueCreating();
		}
	}
}