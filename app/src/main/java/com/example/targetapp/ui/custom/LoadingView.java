package com.example.targetapp.ui.custom;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.LinearLayoutCompat;

public class LoadingView {
	private final String TAG = "LoadingView";
	private final ViewGroup layoutToAddTo;
	private final LinearLayoutCompat containerLayout;
	private final View[] children;
	private final View toReplace;
	private final AppCompatButton[] toDisable;

	public LoadingView(final ViewGroup layoutToAddTo, final Context context, final String message, final @Nullable View toReplace, final @Nullable AppCompatButton[] toDisable, final boolean vertical) {
		this.layoutToAddTo = layoutToAddTo;
		this.toReplace = toReplace;
		this.toDisable = toDisable;
		if (toDisable != null) {
			for (AppCompatButton button : toDisable) {
				button.setEnabled(false);
			}
		}
		ProgressBar progressBar = new ProgressBar(context);
		TextView textView = new TextView(context);

		int childCount = layoutToAddTo.getChildCount();
		children = new View[childCount];
		for (int i = 0; i < childCount; ++i) {
			children[i] = layoutToAddTo.getChildAt(i);
		}

		containerLayout = new LinearLayoutCompat(context);
		if (vertical) containerLayout.setOrientation(LinearLayoutCompat.VERTICAL);
		if (toReplace != null) {
			progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, toReplace.getHeight()));
			containerLayout.setLayoutParams(toReplace.getLayoutParams());
		} else {
			progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			containerLayout.setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		containerLayout.setGravity(Gravity.CENTER);
		textView.setTextSize(20);
		textView.setText(message);
		containerLayout.addView(progressBar);
		containerLayout.addView(textView);
	}

	public LoadingView show() {
		layoutToAddTo.removeAllViewsInLayout();
		if (children.length > 0) {
			for (View c : children) {
				if (c.equals(toReplace)) layoutToAddTo.addView(containerLayout);
				else layoutToAddTo.addView(c);
			}
		} else layoutToAddTo.addView(containerLayout);
		return this;
	}

	public void terminate() {
		layoutToAddTo.removeAllViewsInLayout();
		for (View c : children) {
			layoutToAddTo.addView(c);
		}
		if (toDisable == null) {
			return;
		}
		for (AppCompatButton button : toDisable) {
			button.setEnabled(true);
		}
	}
}
