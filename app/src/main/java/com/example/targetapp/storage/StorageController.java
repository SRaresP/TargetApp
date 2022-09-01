package com.example.targetapp.storage;

import android.util.Log;

import java.io.File;

public class StorageController {
    private static final String TAG = "StorageController";

    public StorageController() {}

    public File findOrCreateFile(final File directory, final String fileName) {
        File[] files = directory.listFiles();
        if (files == null) {
            return new File(directory, fileName);
        }
        for (File f : files) {
            if (f.getName().equals(fileName)) {
                return f;
            }
        }
        return new File(directory, fileName);
    }

    public File findFile(final File directory, final String fileName, final boolean checkSubdirectories) {
        if (!directory.isDirectory()) {
            return null;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (f.getName().equals(fileName)) {
                return f;
            }
            else if (checkSubdirectories && f.isDirectory()) {
                return findFile(f, fileName, true);
            }
        }
        return null;
    }

    public void deleteDirContents(final File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDir(f);
            } else if (!f.delete()) {
                Log.i(TAG, "Couldn't delete file " + f.getName());
            }
        }
    }

    public void deleteDir(final File directory) {
        deleteDirContents(directory);
        if (!directory.delete()) {
            Log.i(TAG, "Couldn't delete directory " + directory.getName());
        }
    }
}
