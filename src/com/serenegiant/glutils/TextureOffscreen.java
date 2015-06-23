package com.serenegiant.glutils;
/*
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: TextureOffscreen.java
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
*/

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Offscreen class with backing texture using FBO to draw using OpenGL|ES into the texture
 */
public class TextureOffscreen {
	private static final boolean DEBUG = false;
	private static final String TAG = "TextureOffscreen";

	private final int TEX_TARGET = GLES20.GL_TEXTURE_2D;
	private final boolean mHasDepthBuffer;
	private int mWidth, mHeight;							// dimension of drawing area of this offscreen
	private int mTexWidth, mTexHeight;						// actual texture size
	private int mFBOTextureId = -1;							// backing texture id
	private int mDepthBufferObj = -1, mFrameBufferObj = -1;	// buffer object ids for offscreen
	private final float[] mTexMatrix = new float[16];		// texture matrix

	/**
	 * Constructor
	 * @param width dimension of offscreen(width)
	 * @param height dimension of offscreen(height)
	 * @param use_depth_buffer set true if you use depth buffer. the depth is fixed as 16bits
	 */
	public TextureOffscreen(final int width, final int height, final boolean use_depth_buffer) {
		this(width, height, use_depth_buffer, false);
	}

	public TextureOffscreen(final int width, final int height, final boolean use_depth_buffer, final boolean adjust_power2) {
		if (DEBUG) Log.v(TAG, "Constructor");
		mWidth = width;
		mHeight = height;
		mHasDepthBuffer = use_depth_buffer;
		prepareFramebuffer(width, height, adjust_power2);
	}

	/**
	 * wrap a existing texture as TextureOffscreen
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	public TextureOffscreen(final int tex_id, final int width, final int height, final boolean use_depth_buffer) {
		this(tex_id, width, height, use_depth_buffer, false);
	}

	/**
	 * wrap a existing texture as TextureOffscreen
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	public TextureOffscreen(final int tex_id, final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {
		if (DEBUG) Log.v(TAG, "Constructor");
		mWidth = width;
		mHeight = height;
		mHasDepthBuffer = use_depth_buffer;

		createFrameBuffer(width, height, adjust_power2);
		assignTexture(tex_id, width, height);
	}

	/**
	 * release related resources
	 */
	public void release() {
		if (DEBUG) Log.v(TAG, "release");
		releaseFrameBuffer();
	}

	/**
	 * switch to rendering buffer to draw
	 * viewport of OpenGL|ES is automatically changed
	 * and you will apply your own viewport after calling #unbind.
	 */
	public void bind() {
//		if (DEBUG) Log.v(TAG, "bind:");
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GLES20.glViewport(0, 0, mWidth, mHeight);
	}

	/**
	 * return to default frame buffer
	 */
	public void unbind() {
//		if (DEBUG) Log.v(TAG, "unbind:");
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}

	private final float[] mResultMatrix = new float[16];
	/**
	 * get copy of texture matrix
	 * @return
	 */
	public float[] getTexMatrix() {
		System.arraycopy(mTexMatrix, 0, mResultMatrix, 0, 16);
		return mResultMatrix;
	}

	/**
	 * get internal texture matrix
	 * @return
	 */
	public float[] getRawTexMatrix() {
		return mTexMatrix;
	}

	/**
	 * get copy of texture matrix
	 * you should allocate array at least 16 of float
	 * @param matrix
	 */
	public void getTexMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}

	/**
	 * get backing texture id for this offscreen
	 * you can use this texture id to draw other render buffer with OpenGL|ES
	 * @return
	 */
	public int getTexture() {
		return mFBOTextureId;
	}

	public void assignTexture(final int texture_id, final int width, final int height) {
		if ((width > mTexWidth) || (height > mTexHeight)) {
			final boolean adjust_power2 = (mTexWidth == mWidth) && (mTexHeight == mHeight);
			mWidth = width;
			mHeight = height;
			releaseFrameBuffer();
			createFrameBuffer(width, height, adjust_power2);
		}
		mFBOTextureId = texture_id;
		// bind frame buffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GlUtil.checkGlError("glBindFramebuffer " + mFrameBufferObj);
		// connect color buffer(backing texture) to frame buffer object as a color buffer
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
			TEX_TARGET, mFBOTextureId, 0);
		GlUtil.checkGlError("glFramebufferTexture2D");

		if (mHasDepthBuffer) {
			// connect depth buffer to frame buffer object
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBufferObj);
			GlUtil.checkGlError("glFramebufferRenderbuffer");
		}

		// confirm whether all process successfully completed.
		final int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer not complete, status=" + status);
		}

		// reset to default frame buffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		// initialize texture matrix
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
	}

	public void loadBitmap(final Bitmap bitmap) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		if ((width > mTexWidth) || (height > mTexHeight)) {
			final boolean adjust_power2 = (mTexWidth == mWidth) && (mTexHeight == mHeight);
			mWidth = width;
			mHeight = height;
			releaseFrameBuffer();
			createFrameBuffer(width, height, adjust_power2);
		}
		GLES20.glBindTexture(TEX_TARGET, mFBOTextureId);
		GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0);
		GLES20.glBindTexture(TEX_TARGET, 0);
		// initialize texture matrix
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
	}

    /**
     * prepare frame buffer etc. for this instance
     */
    private final void prepareFramebuffer(final int width, final int height, final boolean adjust_power2) {
		GlUtil.checkGlError("prepareFramebuffer start");

		createFrameBuffer(width, height, adjust_power2);
		// make a texture id as a color buffer
		final int[] ids = new int[1];
		GLES20.glGenTextures(1, ids, 0);
		GlUtil.checkGlError("glGenTextures");

		GLES20.glBindTexture(TEX_TARGET, ids[0]);
		GlUtil.checkGlError("glBindTexture " + ids[0]);

		// set parameters for backing texture
		GLES20.glTexParameterf(TEX_TARGET, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(TEX_TARGET, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(TEX_TARGET, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(TEX_TARGET, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GlUtil.checkGlError("glTexParameter");

		// allocate memory for texture
		GLES20.glTexImage2D(TEX_TARGET, 0, GLES20.GL_RGBA, mTexWidth, mTexHeight, 0,
				GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		GlUtil.checkGlError("glTexImage2D");

		assignTexture(ids[0], width, height);
    }

	private final void createFrameBuffer(final int width, final int height, final boolean adjust_power2) {
		final int[] ids = new int[1];

		if (adjust_power2) {
			// dimension of texture should be a power of 2
			int w = 1;
			for (; w < width; w <<= 1) ;
			int h = 1;
			for (; h < height; h <<= 1) ;
			if (mTexWidth != w || mTexHeight != h) {
				mTexWidth = w;
				mTexHeight = h;
			}
		} else {
			mTexWidth = width;
			mTexHeight = height;
		}

		if (mHasDepthBuffer) {
			// if depth buffer is required, create and initialize render buffer object
			GLES20.glGenRenderbuffers(1, ids, 0);
			mDepthBufferObj = ids[0];
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBufferObj);
			// the depth is always 16 bits
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mTexWidth, mTexHeight);
		}
		// create and bind frame buffer object
		GLES20.glGenFramebuffers(1, ids, 0);
		GlUtil.checkGlError("glGenFramebuffers");
		mFrameBufferObj = ids[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GlUtil.checkGlError("glBindFramebuffer " + mFrameBufferObj);

		// reset to default frame buffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

	}

    /**
     * release all related resources
     */
    private final void releaseFrameBuffer() {
        final int[] ids = new int[1];
		// release frame buffer object
		if (mFrameBufferObj >= 0) {
			ids[0] = mFrameBufferObj;
			GLES20.glDeleteFramebuffers(1, ids, 0);
			mFrameBufferObj = -1;
		}
		// release depth buffer is exists
		if (mDepthBufferObj >= 0) {
			ids[0] = mDepthBufferObj;
			GLES20.glDeleteRenderbuffers(1, ids, 0);
			mDepthBufferObj = 0;
		}
		// release backing texture
		if (mFBOTextureId >= 0) {
			ids[0] = mFBOTextureId;
			GLES20.glDeleteTextures(1, ids, 0);
			mFBOTextureId = -1;
		}
    }

	/**
	 * get dimension(width) of this offscreen
	 * @return
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * get dimension(height) of this offscreen
	 * @return
	 */
	public int getHeight() {
		return mHeight;
	}

	/**
	 * get backing texture dimension(width) of this offscreen
	 * @return
	 */
	public int getTexWidth() {
		return mTexWidth;
	}

	/**
	 * get backing texture dimension(height) of this offscreen
	 * @return
	 */
	public int getTexHeight() {
		return mTexHeight;
	}
}
