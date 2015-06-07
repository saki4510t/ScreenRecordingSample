package com.serenegiant.media;
/*
 * ScreenRecordingSample
 * Sample project to cature and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: MediaScreenEncoder.java
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

import java.io.IOException;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;


public class MediaScreenEncoder extends MediaVideoEncoderBase {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "MediaScreenEncoder";

	private static final String MIME_TYPE = "video/avc";
	// parameters for recording
    private static final int FRAME_RATE = 25;

	private MediaProjection mMediaProjection;
    private final int mDensity;
    private Surface mSurface;

	public MediaScreenEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener,
		final MediaProjection projection, final int width, final int height, final int density) {

		super(muxer, listener, width, height);
		mMediaProjection = projection;
		mDensity = density;
	}

	@Override
	void prepare() throws IOException {
		if (DEBUG) Log.i(TAG, "prepare: ");
		mSurface = prepare_surface_encoder(MIME_TYPE, FRAME_RATE);
        mMediaCodec.start();
        mIsCapturing = true;
        new Thread(mScreenCaptureTask, "ScreenCaptureThread").start();
        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
        	try {
        		mListener.onPrepared(this);
        	} catch (final Exception e) {
        		Log.e(TAG, "prepare:", e);
        	}
        }
	}


	@Override
	void stopRecording() {
		if (DEBUG) Log.v(TAG,  "stopRecording:");
		mIsCapturing = false;
		super.stopRecording();
	}

	private final Runnable mScreenCaptureTask = new Runnable() {
		@Override
		public void run() {
		    if (DEBUG) Log.d(TAG,"setup VirtualDisplay");
		    for ( ; mIsCapturing ; ) {
				synchronized (mSync) {
					if (mIsCapturing && !mRequestStop && mRequestPause) {
						try {
							mSync.wait();
						} catch (final InterruptedException e) {
							break;
						}
						continue;
					}
				}
				if (mIsCapturing && !mRequestStop && !mRequestPause) {
				    final VirtualDisplay display = mMediaProjection.createVirtualDisplay(
					    "Capturing Display",
						mWidth, mHeight, mDensity,
						DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
						mSurface, null, null);
					if (DEBUG) Log.v(TAG,  "screen capture loop:display=" + display);
				    if (display != null) {
						for ( ; mIsCapturing && !mRequestStop && !mRequestPause && !mIsEOS; ) {
							synchronized (mSync) {
								try {
									mSync.wait(40);
									frameAvailableSoon();
								} catch (final InterruptedException e) {
									break;
								}
							}
						}
						frameAvailableSoon();
						if (DEBUG) Log.v(TAG,  "release VirtualDisplay");
						display.release();
				    }
				}
		    }
			if (DEBUG) Log.v(TAG,  "tear down MediaProjection");
		    if (mMediaProjection != null) {
	            mMediaProjection.stop();
	            mMediaProjection = null;
	        }
			if (DEBUG) Log.v(TAG,  "finished:");
		}
	};
}
