package com.serenegiant.screenrecordingsample;
/*
 * ScreenRecordingSample
 * Sample project to cature and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: MainActivity.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.service.ScreenRecorderService;

public final class MainActivity extends Activity {
	private static final boolean DEBUG = false;
	private static final String TAG = "MainActivity";

	private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

	private ToggleButton mRecordButton;
	private ToggleButton mPauseButton;
	private MyBroadcastReceiver mReceiver;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		setContentView(R.layout.activity_main);
		mRecordButton = (ToggleButton)findViewById(R.id.record_button);
		mPauseButton = (ToggleButton)findViewById(R.id.pause_button);
		updateRecording(false, false);
		if (mReceiver == null)
			mReceiver = new MyBroadcastReceiver(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG) Log.v(TAG, "onResume:");
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT);
		registerReceiver(mReceiver, intentFilter);
		queryRecordingStatus();
	}

	@Override
	protected void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		unregisterReceiver(mReceiver);
		super.onPause();
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (DEBUG) Log.v(TAG, "onActivityResult:resultCode=" + resultCode + ",data=" + data);
		super.onActivityResult(requestCode, resultCode, data);
		if (REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
            if (resultCode != Activity.RESULT_OK) {
                // when no permission
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                return;
            }
            startScreenRecorder(resultCode, data);
        }
	}

	private final OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
			switch (buttonView.getId()) {
			case R.id.record_button:
				if (isChecked) {
					final MediaProjectionManager manager
						= (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
						final Intent permissionIntent = manager.createScreenCaptureIntent();
				       startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
				} else {
					final Intent intent = new Intent(MainActivity.this, ScreenRecorderService.class);
					intent.setAction(ScreenRecorderService.ACTION_STOP);
					startService(intent);
				}
				break;
			case R.id.pause_button:
				if (isChecked) {
					final Intent intent = new Intent(MainActivity.this, ScreenRecorderService.class);
					intent.setAction(ScreenRecorderService.ACTION_PAUSE);
					startService(intent);
				} else {
					final Intent intent = new Intent(MainActivity.this, ScreenRecorderService.class);
					intent.setAction(ScreenRecorderService.ACTION_RESUME);
					startService(intent);
				}
				break;
			}
		}
	};

	private void queryRecordingStatus() {
		if (DEBUG) Log.v(TAG, "queryRecording:");
		final Intent intent = new Intent(this, ScreenRecorderService.class);
		intent.setAction(ScreenRecorderService.ACTION_QUERY_STATUS);
		startService(intent);
	}

	private void startScreenRecorder(final int resultCode, final Intent data) {
		final Intent intent = new Intent(this, ScreenRecorderService.class);
		intent.setAction(ScreenRecorderService.ACTION_START);
		intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
		intent.putExtras(data);
		startService(intent);
	}

	private void updateRecording(final boolean isRecording, final boolean isPausing) {
		if (DEBUG) Log.v(TAG, "updateRecording:isRecording=" + isRecording + ",isPausing=" + isPausing);
		mRecordButton.setOnCheckedChangeListener(null);
		mPauseButton.setOnCheckedChangeListener(null);
		try {
			mRecordButton.setChecked(isRecording);
			mPauseButton.setEnabled(isRecording);
			mPauseButton.setChecked(isPausing);
		} finally {
			mRecordButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
			mPauseButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
		}
	}

	private static final class MyBroadcastReceiver extends BroadcastReceiver {
		private final WeakReference<MainActivity> mWeakParent;
		public MyBroadcastReceiver(final MainActivity parent) {
			mWeakParent = new WeakReference<MainActivity>(parent);
		}

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (DEBUG) Log.v(TAG, "onReceive:" + intent);
			final String action = intent.getAction();
			if (ScreenRecorderService.ACTION_QUERY_STATUS_RESULT.equals(action)) {
				final boolean isRecording = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_RECORDING, false);
				final boolean isPausing = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_PAUSING, false);
				final MainActivity parent = mWeakParent.get();
				if (parent != null) {
					parent.updateRecording(isRecording, isPausing);
				}
			}
		}
	}
}
