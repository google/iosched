package com.larvalabs.svgandroid;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;

/**
 * Based on work by nimingtao:
 * https://code.google.com/p/adet/source/browse/trunk/adet/src/cn/mobileww/adet/graphics/SvgDrawable.java
 * 
 * @since 19 Aug 2013
 * @author nimingtao, mstevens83
 */
public class SVGDrawable extends PictureDrawable {

	private final String TAG = "SVGDrawable";

	private SVGState mSvgState;

	/**
	 * @param picture
	 */
	public SVGDrawable(SVG svg) {
		super(svg.getPicture());
		this.mSvgState = new SVGState(svg);
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
	}

	/**
	 * Original author nimingtao wrote that this method may not work on devices with Ice Cream Sandwich (Android v4.0).<br/>
	 * See: http://stackoverflow.com/q/10384613/1084488<br/>
	 * Apparently this is because canvas.drawPicture is not supported with hardware acceleration. If the problem occurs
	 * and solved by programmatically turning off hardware acceleration only on the view that will draw the Picture:
	 * view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);<br/>
	 * <br/>
	 * However, I (mstevens83) was unable to reproduce this problem on an emulator running Ice Cream Sandwich, nor on
	 * physical devices running Jelly Bean (v4.1.2 and v4.3 tested).
	 */
	@Override
	public void draw(Canvas canvas) {
		if (getPicture() != null) {
			Rect bounds = getBounds();
			canvas.save();
			// draw picture to fit bounds!
			canvas.drawPicture(getPicture(), bounds);
			canvas.restore();
		}
	}

	// @Override
	// public int getIntrinsicWidth() {
	// Rect bounds = getBounds();
	// RectF limits = mSvgState.mSvg.getLimits();
	// if (bounds != null) {
	// return (int) bounds.width();
	// } else if (limits != null) {
	// return (int) limits.width();
	// } else {
	// return -1;
	// }
	// }
	//
	// @Override
	// public int getIntrinsicHeight() {
	// Rect bounds = getBounds();
	// RectF limits = mSvgState.mSvg.getLimits();
	// if (bounds != null) {
	// return (int) bounds.height();
	// } else if (limits != null) {
	// return (int) limits.height();
	// } else {
	// return -1;
	// }
	// }

	@Override
	public int getChangingConfigurations() {
		int c = super.getChangingConfigurations() | mSvgState.mChangingConfigurations;
		Log.e(TAG, "CC = " + c);
		return c;
	}

	@Override
	public ConstantState getConstantState() {
		mSvgState.mChangingConfigurations = super.getChangingConfigurations();
		return this.mSvgState;
	}

	final static class SVGState extends ConstantState {
		int mChangingConfigurations;
		private SVG mSvg;

		private SVGState(SVG svg) {
			this.mSvg = svg;
		}

		/*
		 * (non-Javadoc)
		 * @see android.graphics.drawable.Drawable.ConstantState#newDrawable()
		 */
		@Override
		public Drawable newDrawable() {
			return new SVGDrawable(mSvg);
		}

		/*
		 * (non-Javadoc)
		 * @see android.graphics.drawable.Drawable.ConstantState#getChangingConfigurations()
		 */
		@Override
		public int getChangingConfigurations() {
			return mChangingConfigurations;
		}

	}

}