package com.larvalabs.svgandroid;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.xml.sax.InputSource;

import com.larvalabs.svgandroid.SVGParser.SVGHandler;

/**
 * Builder for reading SVGs. Specify input, specify any parsing options (optional), then call {@link #build()} to parse
 * and return a {@link SVG}.
 * 
 * @since 24/12/2012
 */
public class SVGBuilder {
	private InputStream data;
	private Integer searchColor = null;
	private Integer replaceColor = null;
	private ColorFilter strokeColorFilter = null, fillColorFilter = null;
	private boolean whiteMode = false;
	private boolean overideOpacity = false;
	private boolean closeInputStream = true;

	/**
	 * Parse SVG data from an input stream.
	 * 
	 * @param svgData the input stream, with SVG XML data in UTF-8 character encoding.
	 * @return the parsed SVG.
	 */
	public SVGBuilder readFromInputStream(InputStream svgData) {
		this.data = svgData;
		return this;
	}

	/**
	 * Parse SVG data from a string.
	 * 
	 * @param svgData the string containing SVG XML data.
	 */
	public SVGBuilder readFromString(String svgData) {
		this.data = new ByteArrayInputStream(svgData.getBytes());
		return this;
	}

	/**
	 * Parse SVG data from an Android application resource.
	 * 
	 * @param resources the Android context resources.
	 * @param resId the ID of the raw resource SVG.
	 */
	public SVGBuilder readFromResource(Resources resources, int resId) {
		this.data = resources.openRawResource(resId);
		return this;
	}

	/**
	 * Parse SVG data from an Android application asset.
	 * 
	 * @param assetMngr the Android asset manager.
	 * @param svgPath the path to the SVG file in the application's assets.
	 * @throws IOException if there was a problem reading the file.
	 */
	public SVGBuilder readFromAsset(AssetManager assetMngr, String svgPath) throws IOException {
		this.data = assetMngr.open(svgPath);
		return this;
	}

	public SVGBuilder clearColorSwap() {
		searchColor = replaceColor = null;
		return this;
	}

	/**
	 * Replaces a single colour with another.
	 * 
	 * @param searchColor The colour in the SVG.
	 * @param replaceColor The desired colour.
	 */
	public SVGBuilder setColorSwap(int searchColor, int replaceColor) {
		return setColorSwap(searchColor, replaceColor, false);
	}

	/**
	 * Replaces a single colour with another, affecting the opacity.
	 * 
	 * @param searchColor The colour in the SVG.
	 * @param replaceColor The desired colour.
	 * @param overideOpacity If true, combines the opacity defined in the SVG resource with the alpha of replaceColor.
	 */
	public SVGBuilder setColorSwap(int searchColor, int replaceColor, boolean overideOpacity) {
		this.searchColor = searchColor;
		this.replaceColor = replaceColor;
		this.overideOpacity = overideOpacity;
		return this;
	}

	/**
	 * In white-mode, fills are drawn in white and strokes are not drawn at all.
	 */
	public SVGBuilder setWhiteMode(boolean whiteMode) {
		this.whiteMode = whiteMode;
		return this;
	}

	/**
	 * Applies a {@link ColorFilter} to the paint objects used to render the SVG.
	 */
	public SVGBuilder setColorFilter(ColorFilter colorFilter) {
		this.strokeColorFilter = this.fillColorFilter = colorFilter;
		return this;
	}

	/**
	 * Applies a {@link ColorFilter} to strokes in the SVG.
	 */
	public SVGBuilder setStrokeColorFilter(ColorFilter colorFilter) {
		this.strokeColorFilter = colorFilter;
		return this;
	}

	/**
	 * Applies a {@link ColorFilter} to fills in the SVG.
	 */
	public SVGBuilder setFillColorFilter(ColorFilter colorFilter) {
		this.fillColorFilter = colorFilter;
		return this;
	}

	/**
	 * Whether or not to close the input stream after reading (ie. after calling {@link #build()}.<br>
	 * <em>(default is true)</em>
	 */
	public SVGBuilder setCloseInputStreamWhenDone(boolean closeInputStream) {
		this.closeInputStream = closeInputStream;
		return this;
	}

	/**
	 * Loads, reads, parses the SVG (or SVGZ).
	 * 
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public SVG build() throws SVGParseException {
		if (data == null) {
			throw new IllegalStateException("SVG input not specified. Call one of the readFrom...() methods first.");
		}

		try {
			final SVGHandler handler = new SVGHandler();
			handler.setColorSwap(searchColor, replaceColor, overideOpacity);
			handler.setWhiteMode(whiteMode);
			if (strokeColorFilter != null) {
				handler.strokePaint.setColorFilter(strokeColorFilter);
			}
			if (fillColorFilter != null) {
				handler.fillPaint.setColorFilter(fillColorFilter);
			}

			// SVGZ support (based on https://github.com/josefpavlik/svg-android/commit/fc0522b2e1):
			if(!data.markSupported())
				data = new BufferedInputStream(data); // decorate stream so we can use mark/reset
			try {
				data.mark(4);
				byte[] magic = new byte[2];
				int r = data.read(magic, 0, 2);
				int magicInt = (magic[0] + ((magic[1]) << 8)) & 0xffff;
				data.reset();
				if (r == 2 && magicInt == GZIPInputStream.GZIP_MAGIC) {
					// Log.d(SVGParser.TAG, "SVG is gzipped");
					GZIPInputStream gin = new GZIPInputStream(data);
					data = gin;
				}
			} catch (IOException ioe) {
				throw new SVGParseException(ioe);
			}

			final SVG svg = SVGParser.parse(new InputSource(data), handler);
			return svg;

		} finally {
			if (closeInputStream) {
				try {
					data.close();
				} catch (IOException e) {
					Log.e(SVGParser.TAG, "Error closing SVG input stream.", e);
				}
			}
		}
	}
}
