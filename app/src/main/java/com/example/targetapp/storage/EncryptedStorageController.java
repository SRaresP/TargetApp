package com.example.targetapp.storage;

import android.content.Context;
import android.util.Log;

import com.example.targetapp.cryptography.CryptoHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EncryptedStorageController extends StorageController {
	private static final String TAG = "EncryptedStorageController";
	private static final String EncryptedStorageFolderPath = "/EncryptedStorage";

	private static EncryptedStorageController instance;

	private final File encryptedStorageRoot;
	private File masterPasswordFile;
	private final CryptoHandler cryptoHandler;
	private final MessageDigest messageDigest;

	private EncryptedStorageController(final Context context) throws NoSuchAlgorithmException {
		super();
		encryptedStorageRoot = new File(context.getFilesDir().getAbsolutePath() + EncryptedStorageFolderPath);
		if (!encryptedStorageRoot.mkdirs())
			Log.i(TAG, "Failed to make EncryptedStorage folder when initialising, this may be because it already exists");
		cryptoHandler = CryptoHandler.getInstance(context);

		messageDigest = MessageDigest.getInstance("SHA-256");

		masterPasswordFile = findFile(context.getFilesDir(), "mPass", false);
	}

	public static EncryptedStorageController getInstance(final Context context) {
		if (instance == null) {
			try {
				return instance = new EncryptedStorageController(context);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	//the file name should be the name of the text within the app
	//eg if we store an account, we would have "Steam account" as the name

	public void setMasterPassword(final Context context, final String masterPassword) throws IOException {
		masterPasswordFile = new File(context.getFilesDir().getAbsolutePath(), "mPass");

		messageDigest.update(masterPassword.getBytes(StandardCharsets.UTF_8));
		byte[] hashedPassword = messageDigest.digest();

		FileOutputStream fileOutputStream = new FileOutputStream(masterPasswordFile);
		fileOutputStream.write(hashedPassword);
		fileOutputStream.flush();
		fileOutputStream.close();
	}

	public boolean checkMasterPassword(final String masterPassword) throws IOException {
		if (masterPasswordFile == null) {
			throw new FileNotFoundException("Hash file was not found.");
		}

		FileInputStream fileInputStream = new FileInputStream(masterPasswordFile);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		byte[] input = new byte[32];
		while (fileInputStream.read(input) != -1) {
			byteArrayOutputStream.write(input);
		}
		byte[] hashedFileContents = byteArrayOutputStream.toByteArray();

		messageDigest.update(masterPassword.getBytes(StandardCharsets.UTF_8));
		byte[] hashedPassword = messageDigest.digest();

		fileInputStream.close();
		byteArrayOutputStream.close();

		return Arrays.equals(hashedFileContents, hashedPassword);
	}

	public void wipeMasterPassword() {
		if(!masterPasswordFile.delete()) {
			Log.i(TAG, "Failed to delete master password");
		}
	}

	public boolean masterPasswordFileExists() {
		return masterPasswordFile != null;
	}

	public void add(final String fileName, final String fileContents, final String internalAppFolder)
			throws IOException {
		File folder = new File(encryptedStorageRoot.getAbsolutePath() + internalAppFolder);
		folder.mkdirs();
		File file = findFile(folder, fileName, false);
		if (file != null) {
			throw new FileAlreadyExistsException("Duplicate file name provided");
		}
		file = new File(folder, fileName);
		FileOutputStream fileOutputStream = new FileOutputStream(file);

		byte[] encFileContents = cryptoHandler.encrypt(fileContents);

		fileOutputStream.write(encFileContents);
		fileOutputStream.flush();
		fileOutputStream.close();
	}

	public void add(final String fileName, final String fileContents) throws IOException {
		add(fileName, fileContents, "");
	}

	public File createDirectory(final String fileName, final String internalAppFolder) throws IOException {
		File folder = new File(encryptedStorageRoot.getAbsolutePath() + internalAppFolder, fileName);
		if (!folder.mkdirs()) {
			throw new IOException("Failed to create directory " + fileName + " inside " + internalAppFolder);
		}
		return folder;
	}

	public String get(final String fileName, final String folderToGetFrom) throws NullPointerException, IOException {
		File folder = new File(encryptedStorageRoot.getAbsolutePath() + folderToGetFrom);
		folder.mkdirs();
		File targetFile = findFile(folder, fileName, false);
		if (targetFile == null) {
			throw new FileNotFoundException("Encrypted file was not found.");
		}

		FileInputStream fileInputStream = new FileInputStream(targetFile);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		//HAVE TO READ THE ENCRYPTED DATA IN CHUNKS OF THE EXACT BLOCK SIZE IT WAS WRITTEN IN
		//well, I think
		byte[] input = new byte[cryptoHandler.getCurrentBlockSize()];
		while (fileInputStream.read(input) != -1) {
			byteArrayOutputStream.write(input);
		}
		byte[] fileContents = byteArrayOutputStream.toByteArray();

		fileInputStream.close();
		byteArrayOutputStream.close();
		return cryptoHandler.decrypt(fileContents);
	}

	public String get(final String fileName) throws NullPointerException, IOException {
		return get(fileName, "");
	}

	public void wipeEncryptedData() {
		deleteDirContents(encryptedStorageRoot);
	}

	public List<String> getFilenamesFrom(final String internalDirPath) {
		File directory = new File(encryptedStorageRoot.getAbsolutePath() + internalDirPath);
		if (!directory.mkdirs()) {
			Log.e(TAG, "File passed to getFilenamesFrom() was not a directory");
		}
		File[] files = directory.listFiles();
		if (files == null) {
			return null;
		}
		ArrayList<String> filenames = new ArrayList<>(files.length);
		for (File file : files) {
			filenames.add(file.getName());
		}
		return filenames;
	}

	public ArrayList<String> getFilePathsFrom(final String internalDirPath) {
		File directory = new File(encryptedStorageRoot.getAbsolutePath() + internalDirPath);
		File[] files = directory.listFiles();
		if (files == null) {
			return null;
		}
		ArrayList<String> filePaths = new ArrayList<>(files.length);
		for (File file : files) {
			filePaths.add(file.getAbsolutePath());
		}
		return filePaths;
	}

	public String getEncryptedStorageRootPath() {
		return encryptedStorageRoot.getAbsolutePath();
	}
}
