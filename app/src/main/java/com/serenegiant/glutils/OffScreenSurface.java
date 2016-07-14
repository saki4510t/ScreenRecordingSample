package com.serenegiant.glutils;
/*
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: OffScreenSurface.java
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

/**
 * Create offscreen using EglSurface.
 */
public class OffScreenSurface extends EglSurfaceBase {

	public OffScreenSurface(final EglCore eglBase, final int width, final int height) {
		super(eglBase);
		createOffscreenSurface(width, height);
		makeCurrent();
	}

	public void release() {
		releaseEglSurface();
	}

}
