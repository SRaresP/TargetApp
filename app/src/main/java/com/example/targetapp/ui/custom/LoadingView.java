package com.example.targetapp.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

public class LoadingView extends LinearLayoutCompat {
	public LoadingView(@NonNull Context context) {
		super(context);
	}

	public LoadingView(@NonNull Context context, String message, boolean vertical) {
		super(context);

		setPadding(10, 10, 10, 10);

		ProgressBar progressBar = new ProgressBar(context);
		TextView textView = new TextView(context);

		if (vertical) setOrientation(LinearLayoutCompat.VERTICAL);

		progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		setGravity(Gravity.CENTER);
		textView.setTextSize(20);
		textView.setText(message);
		addView(progressBar);
		addView(textView);
	}
}
