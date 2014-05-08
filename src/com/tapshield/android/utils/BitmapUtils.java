package com.tapshield.android.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;

public class BitmapUtils {

	public static final int CLIP_RADIUS_DEFAULT = -1;
	
	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}
	
	public static Bitmap clipCircle(Context context, int drawableResource, int clipRadiusPercent) {
		Bitmap b = BitmapFactory.decodeResource(context.getResources(), drawableResource);
		return clipCircle(b, clipRadiusPercent);
	}
	
	public static Bitmap clipCircle(Bitmap bitmap, int clipRadiusPercent) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		float cx = (float) w/2;
		float cy = (float) h/2;
		int percent = clipRadiusPercent > 0 ? clipRadiusPercent : 100;
		float r = (((float) Math.min(w, h))/2f) * ((float) percent) / 100;
		
		Path path = new Path();
		path.addCircle(cx, cy, r, Path.Direction.CCW);
		
		Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		c.clipPath(path);
		c.drawBitmap(
				bitmap,
				new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
				new Rect(0, 0, b.getWidth(), b.getHeight()),
				null);
		return b;
	}
	public static Bitmap resizeBitmap(Bitmap bitmap, int maximumDimension, boolean scaleUpIfSmaller) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float newScale;
		
		if (Math.max(width, height) <= maximumDimension && !scaleUpIfSmaller) {
			return bitmap;
		}
		
		if (width > height) {
			newScale = (float)maximumDimension / (float)width;
		} else {
			newScale = (float)maximumDimension / (float)height;
		}
		
		Matrix matrix = new Matrix();
		matrix.postScale(newScale, newScale);
		
		return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	}
}
