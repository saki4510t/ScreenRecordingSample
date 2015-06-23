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

import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.FullFrameRect;
import com.serenegiant.glutils.Texture2dProgram;
import com.serenegiant.glutils.WindowSurface;

public class MediaScreenEncoder extends MediaVideoEncoderBase {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "MediaScreenEncoder";

	private static final String MIME_TYPE = "video/avc";
	// parameters for recording
    private static final int FRAME_RATE = 25;

	private MediaProjection mMediaProjection;
    private final int mDensity;
    private Surface mSurface;
    private final Handler mHandler;

	public MediaScreenEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener,
		final MediaProjection projection, final int width, final int height, final int density) {

		super(muxer, listener, width, height);
		mMediaProjection = projection;
		mDensity = density;
		final HandlerThread thread = new HandlerThread(TAG);
		thread.start();
		mHandler = new Handler(thread.getLooper());
	}

	@Override
	protected void release() {
		mHandler.getLooper().quit();
		super.release();
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
		synchronized (mSync) {
			mIsCapturing = false;
			mSync.notifyAll();
		}
		super.stopRecording();
	}

	private boolean requestDraw;
	private final DrawTask mScreenCaptureTask = new DrawTask(null, 0);

	private final class DrawTask extends EglTask {
		private VirtualDisplay display;
		private long intervals;
		private int mTexId;
		private SurfaceTexture mSourceTexture;
		private Surface mSourceSurface;
    	private WindowSurface mEncoderSurface;
    	private FullFrameRect mDrawer;
    	private final float[] mTexMatrix = new float[16];

    	public DrawTask(final EGLContext shared_context, final int flags) {
    		super(shared_context, flags);
    	}

		@Override
		protected void onStart() {
		    if (DEBUG) Log.d(TAG,"mScreenCaptureTask#onStart:");
	    	mDrawer = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
			mTexId = mDrawer.createTextureObject();
			mSourceTexture = new SurfaceTexture(mTexId);
			mSourceTexture.setDefaultBufferSize(mWidth, mHeight);
			mSourceSurface = new Surface(mSourceTexture);
			mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
			mEncoderSurface = new WindowSurface(getEglCore(), mSurface);

	    	if (DEBUG) Log.d(TAG,"setup VirtualDisplay");
			intervals = (long)(1000f / FRAME_RATE);
		    display = mMediaProjection.createVirtualDisplay(
		    	"Capturing Display",
		    	mWidth, mHeight, mDensity,
		    	DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
		    	mSourceSurface, null, null);
			if (DEBUG) Log.v(TAG,  "screen capture loop:display=" + display);
			queueEvent(mDrawTask);
		}

		@Override
		protected void onStop() {
			if (mDrawer != null) {
				mDrawer.release();
				mDrawer = null;
			}
			if (mSourceSurface != null) {
				mSourceSurface.release();
				mSourceSurface = null;
			}
			if (mSourceTexture != null) {
				mSourceTexture.release();
				mSourceTexture = null;
			}
			if (mEncoderSurface != null) {
				mEncoderSurface.release();
				mEncoderSurface = null;
			}
			makeCurrent();
			if (DEBUG) Log.v(TAG, "mScreenCaptureTask#onStop:");
			if (display != null) {
				if (DEBUG) Log.v(TAG,  "release VirtualDisplay");
				display.release();
			}
			if (DEBUG) Log.v(TAG,  "tear down MediaProjection");
		    if (mMediaProjection != null) {
	            mMediaProjection.stop();
	            mMediaProjection = null;
	        }
		}

		@Override
		protected boolean onError(final Exception e) {
			if (DEBUG) Log.w(TAG, "mScreenCaptureTask:", e);
			return false;
		}

		@Override
		protected boolean processRequest(final int request, final int arg1, final Object arg2) {
			return false;
		}

		private final OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
				if (mIsCapturing) {
					synchronized (mSync) {
						requestDraw = true;
						mSync.notifyAll();
					}
				}
			}
		};

		private final Runnable mDrawTask = new Runnable() {
			@Override
			public void run() {
				synchronized (mSync) {
					if (!requestDraw) {
						try {
							mSync.wait(intervals);
						} catch (final InterruptedException e) {
							return;
						}
					}
					if (mIsCapturing) {
						if (requestDraw) {
							requestDraw = false;
							mSourceTexture.updateTexImage();
							mSourceTexture.getTransformMatrix(mTexMatrix);
						}
						if (!mRequestPause) {
							mEncoderSurface.makeCurrent();
					    	mDrawer.drawFrame(mTexId, mTexMatrix);
					    	mEncoderSurface.swapBuffers();
						}
						makeCurrent();
						GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
						GLES20.glFlush();
						frameAvailableSoon();
						queueEvent(this);
					} else {
						releaseSelf();
					}
				}
			}
		};

	};
}
