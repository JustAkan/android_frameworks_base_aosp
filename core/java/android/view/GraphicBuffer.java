/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simple wrapper for the native GraphicBuffer class.
 *
 * @hide
 */
@SuppressWarnings("UnusedDeclaration")
public class GraphicBuffer implements Parcelable {
    // Note: keep usage flags in sync with GraphicBuffer.h and gralloc.h
    public static final int USAGE_SW_READ_NEVER = 0x0;
    public static final int USAGE_SW_READ_RARELY = 0x2;
    public static final int USAGE_SW_READ_OFTEN = 0x3;
    public static final int USAGE_SW_READ_MASK = 0xF;

    public static final int USAGE_SW_WRITE_NEVER = 0x0;
    public static final int USAGE_SW_WRITE_RARELY = 0x20;
    public static final int USAGE_SW_WRITE_OFTEN = 0x30;
    public static final int USAGE_SW_WRITE_MASK = 0xF0;

    public static final int USAGE_SOFTWARE_MASK = USAGE_SW_READ_MASK | USAGE_SW_WRITE_MASK;

    public static final int USAGE_PROTECTED = 0x4000;

    public static final int USAGE_HW_TEXTURE = 0x100;
    public static final int USAGE_HW_RENDER = 0x200;
    public static final int USAGE_HW_2D = 0x400;
    public static final int USAGE_HW_COMPOSER = 0x800;
    public static final int USAGE_HW_VIDEO_ENCODER = 0x10000;
    public static final int USAGE_HW_MASK = 0x71F00;

    private final int mWidth;
    private final int mHeight;
    private final int mFormat;
    private final int mUsage;
    // Note: do not rename, this field is used by native code
    private final int mNativeObject;

    // These two fields are only used by lock/unlockCanvas()
    private Canvas mCanvas;
    private int mSaveCount;

    /**
     * Creates new <code>GraphicBuffer</code> instance. This method will return null
     * if the buffer cannot be created.
     *
     * @param width The width in pixels of the buffer
     * @param height The height in pixels of the buffer
     * @param format The format of each pixel as specified in {@link PixelFormat}
     * @param usage Hint indicating how the buffer will be used
     *
     * @return A <code>GraphicBuffer</code> instance or null
     */
    public static GraphicBuffer create(int width, int height, int format, int usage) {
        int nativeObject = nCreateGraphicBuffer(width, height, format, usage);
        if (nativeObject != 0) {
            return new GraphicBuffer(width, height, format, usage, nativeObject);
        }
        return null;
    }

    /**
     * Private use only. See {@link #create(int, int, int, int)}.
     */
    private GraphicBuffer(int width, int height, int format, int usage, int nativeObject) {
        mWidth = width;
        mHeight = height;
        mFormat = format;
        mUsage = usage;
        mNativeObject = nativeObject;
    }

    /**
     * Returns the width of this buffer in pixels.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the height of this buffer in pixels.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Returns the pixel format of this buffer. The pixel format must be one of
     * the formats defined in {@link PixelFormat}.
     */
    public int getFormat() {
        return mFormat;
    }

    /**
     * Returns the usage hint set on this buffer.
     */
    public int getUsage() {
        return mUsage;
    }

    /**
     * <p>Start editing the pixels in the buffer. A null is returned if the buffer
     * cannot be locked for editing.</p>
     *
     * <p>The content of the buffer is preserved between unlockCanvas()
     * and lockCanvas().</p>
     *
     * @return A Canvas used to draw into the buffer, or null.
     *
     * @see #lockCanvas(android.graphics.Rect)
     * @see #unlockCanvasAndPost(android.graphics.Canvas)
     */
    public Canvas lockCanvas() {
        return lockCanvas(null);
    }

    /**
     * Just like {@link #lockCanvas()} but allows specification of a dirty
     * rectangle.
     *
     * @param dirty Area of the buffer that may be modified.

     * @return A Canvas used to draw into the surface or null
     *
     * @see #lockCanvas()
     * @see #unlockCanvasAndPost(android.graphics.Canvas)
     */
    public Canvas lockCanvas(Rect dirty) {
        if (mCanvas == null) {
            mCanvas = new Canvas();
        }

        if (nLockCanvas(mNativeObject, mCanvas, dirty)) {
            mSaveCount = mCanvas.save();
            return mCanvas;
        }

        return null;
    }

    /**
     * Finish editing pixels in the buffer.
     *
     * @param canvas The Canvas previously returned by lockCanvas()
     *
     * @see #lockCanvas()
     * @see #lockCanvas(android.graphics.Rect)
     */
    public void unlockCanvasAndPost(Canvas canvas) {
        if (mCanvas != null && canvas == mCanvas) {
            canvas.restoreToCount(mSaveCount);
            mSaveCount = 0;

            nUnlockCanvasAndPost(mNativeObject, mCanvas);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            nDestroyGraphicBuffer(mNativeObject);
        } finally {
            super.finalize();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeInt(mFormat);
        dest.writeInt(mUsage);
        nWriteGraphicBufferToParcel(mNativeObject, dest);
    }

    public static final Parcelable.Creator<GraphicBuffer> CREATOR =
            new Parcelable.Creator<GraphicBuffer>() {
        public GraphicBuffer createFromParcel(Parcel in) {
            int width = in.readInt();
            int height = in.readInt();
            int format = in.readInt();
            int usage = in.readInt();
            int nativeObject = nReadGraphicBufferFromParcel(in);
            if (nativeObject != 0) {
                return new GraphicBuffer(width, height, format, usage, nativeObject);
            }
            return null;
        }

        public GraphicBuffer[] newArray(int size) {
            return new GraphicBuffer[size];
        }
    };

    private static native int nCreateGraphicBuffer(int width, int height, int format, int usage);
    private static native void nDestroyGraphicBuffer(int nativeObject);
    private static native void nWriteGraphicBufferToParcel(int nativeObject, Parcel dest);
    private static native int nReadGraphicBufferFromParcel(Parcel in);
    private static native boolean nLockCanvas(int nativeObject, Canvas canvas, Rect dirty);
    private static native boolean nUnlockCanvasAndPost(int nativeObject, Canvas canvas);
}
