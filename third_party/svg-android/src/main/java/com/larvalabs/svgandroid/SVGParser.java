package com.larvalabs.svgandroid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.FloatMath;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

/**
 * @author Larva Labs, LLC
 */
public class SVGParser {

	static final String TAG = "SVGAndroid";

	private static boolean DISALLOW_DOCTYPE_DECL = true;

	/**
	 * Parses a single SVG path and returns it as a <code>android.graphics.Path</code> object. An example path is
	 * <code>M250,150L150,350L350,350Z</code>, which draws a triangle.
	 * 
	 * @param pathString the SVG path, see the specification <a href="http://www.w3.org/TR/SVG/paths.html">here</a>.
	 */
	public static Path parsePath(String pathString) {
		return doPath(pathString);
	}

	static SVG parse(InputSource data, SVGHandler handler) throws SVGParseException {
		try {
			final Picture picture = new Picture();
			handler.setPicture(picture);

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(handler);
			xr.setFeature("http://xml.org/sax/features/validation", false);
			if (DISALLOW_DOCTYPE_DECL) {
				try {
					xr.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				} catch (SAXNotRecognizedException e) {
					DISALLOW_DOCTYPE_DECL = false;
				}
			}
			xr.parse(data);

			SVG result = new SVG(picture, handler.bounds);
			// Skip bounds if it was an empty pic
			if (!Float.isInfinite(handler.limits.top)) {
				result.setLimits(handler.limits);
			}
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Failed to parse SVG.", e);
			throw new SVGParseException(e);
		}
	}

	private static NumberParse parseNumbers(String s) {
		// Util.debug("Parsing numbers from: '" + s + "'");
		int n = s.length();
		int p = 0;
		ArrayList<Float> numbers = new ArrayList<Float>();
		boolean skipChar = false;
		boolean prevWasE = false;
		for (int i = 1; i < n; i++) {
			if (skipChar) {
				skipChar = false;
				continue;
			}
			char c = s.charAt(i);
			switch (c) {
			// This ends the parsing, as we are on the next element
			case 'M':
			case 'm':
			case 'Z':
			case 'z':
			case 'L':
			case 'l':
			case 'H':
			case 'h':
			case 'V':
			case 'v':
			case 'C':
			case 'c':
			case 'S':
			case 's':
			case 'Q':
			case 'q':
			case 'T':
			case 't':
			case 'a':
			case 'A':
			case ')': {
				String str = s.substring(p, i);
				if (str.trim().length() > 0) {
					// Util.debug("  Last: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
				}
				p = i;
				return new NumberParse(numbers, p);
			}
			case '-':
				// Allow numbers with negative exp such as 7.23e-4
				if (prevWasE) {
					prevWasE = false;
					break;
				}
				// fall-through
			case '\n':
			case '\t':
			case ' ':
			case ',': {
				String str = s.substring(p, i);
				// Just keep moving if multiple whitespace
				if (str.trim().length() > 0) {
					// Util.debug("  Next: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
					if (c == '-') {
						p = i;
					} else {
						p = i + 1;
						skipChar = true;
					}
				} else {
					p++;
				}
				prevWasE = false;
				break;
			}
			case 'e':
				prevWasE = true;
				break;
			default:
				prevWasE = false;
			}
		}

		String last = s.substring(p);
		if (last.length() > 0) {
			// Util.debug("  Last: " + last);
			try {
				numbers.add(Float.parseFloat(last));
			} catch (NumberFormatException nfe) {
				// Just white-space, forget it
			}
			p = s.length();
		}
		return new NumberParse(numbers, p);
	}

	private static final Pattern TRANSFORM_SEP = Pattern.compile("[\\s,]*");

	/**
	 * Parse a list of transforms such as: foo(n,n,n...) bar(n,n,n..._ ...) Delimiters are whitespaces or commas
	 */
	private static Matrix parseTransform(String s) {
		Matrix matrix = new Matrix();
		while (true) {
			parseTransformItem(s, matrix);
			// Log.i(TAG, "Transformed: (" + s + ") " + matrix);
			final int rparen = s.indexOf(")");
			if (rparen > 0 && s.length() > rparen + 1) {
				s = TRANSFORM_SEP.matcher(s.substring(rparen + 1)).replaceFirst("");
			} else {
				break;
			}
		}
		return matrix;
	}

	private static Matrix parseTransformItem(String s, Matrix matrix) {
		if (s.startsWith("matrix(")) {
			NumberParse np = parseNumbers(s.substring("matrix(".length()));
			if (np.numbers.size() == 6) {
				Matrix mat = new Matrix();
				mat.setValues(new float[] {
						// Row 1
						np.numbers.get(0), np.numbers.get(2), np.numbers.get(4),
						// Row 2
						np.numbers.get(1), np.numbers.get(3), np.numbers.get(5),
						// Row 3
						0, 0, 1, });
				matrix.preConcat(mat);
			}
		} else if (s.startsWith("translate(")) {
			NumberParse np = parseNumbers(s.substring("translate(".length()));
			if (np.numbers.size() > 0) {
				float tx = np.numbers.get(0);
				float ty = 0;
				if (np.numbers.size() > 1) {
					ty = np.numbers.get(1);
				}
				matrix.preTranslate(tx, ty);
			}
		} else if (s.startsWith("scale(")) {
			NumberParse np = parseNumbers(s.substring("scale(".length()));
			if (np.numbers.size() > 0) {
				float sx = np.numbers.get(0);
				float sy = sx;
				if (np.numbers.size() > 1) {
					sy = np.numbers.get(1);
				}
				matrix.preScale(sx, sy);
			}
		} else if (s.startsWith("skewX(")) {
			NumberParse np = parseNumbers(s.substring("skewX(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew((float) Math.tan(angle), 0);
			}
		} else if (s.startsWith("skewY(")) {
			NumberParse np = parseNumbers(s.substring("skewY(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew(0, (float) Math.tan(angle));
			}
		} else if (s.startsWith("rotate(")) {
			NumberParse np = parseNumbers(s.substring("rotate(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				float cx = 0;
				float cy = 0;
				if (np.numbers.size() > 2) {
					cx = np.numbers.get(1);
					cy = np.numbers.get(2);
				}
				matrix.preTranslate(-cx, -cy);
				matrix.preRotate(angle);
				matrix.preTranslate(cx, cy);
			}
		} else {
			Log.w(TAG, "Invalid transform (" + s + ")");
		}
		return matrix;
	}

	/**
	 * This is where the hard-to-parse paths are handled. Uppercase rules are absolute positions, lowercase are
	 * relative. Types of path rules:
	 * <p/>
	 * <ol>
	 * <li>M/m - (x y)+ - Move to (without drawing)
	 * <li>Z/z - (no params) - Close path (back to starting point)
	 * <li>L/l - (x y)+ - Line to
	 * <li>H/h - x+ - Horizontal ine to
	 * <li>V/v - y+ - Vertical line to
	 * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
	 * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1,
	 * y1 of this bezier)
	 * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
	 * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t.
	 * to current point)
	 * </ol>
	 * <p/>
	 * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a -
	 * sign)
	 * 
	 * @param s the path string from the XML
	 */
	private static Path doPath(String s) {
		int n = s.length();
		ParserHelper ph = new ParserHelper(s, 0);
		ph.skipWhitespace();
		Path p = new Path();
		float lastX = 0;
		float lastY = 0;
		float lastX1 = 0;
		float lastY1 = 0;
		float subPathStartX = 0;
		float subPathStartY = 0;
		char prevCmd = 0;
		while (ph.pos < n) {
			char cmd = s.charAt(ph.pos);
			switch (cmd) {
			case '-':
			case '+':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				if (prevCmd == 'm' || prevCmd == 'M') {
					cmd = (char) ((prevCmd) - 1);
					break;
				} else if (("lhvcsqta").indexOf(Character.toLowerCase(prevCmd)) >= 0) {
					cmd = prevCmd;
					break;
				}
			default: {
				ph.advance();
				prevCmd = cmd;
			}
			}

			boolean wasCurve = false;
			switch (cmd) {
			case 'M':
			case 'm': {
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'm') {
					subPathStartX += x;
					subPathStartY += y;
					p.rMoveTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					subPathStartX = x;
					subPathStartY = y;
					p.moveTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'Z':
			case 'z': {
				p.close();
				p.moveTo(subPathStartX, subPathStartY);
				lastX = subPathStartX;
				lastY = subPathStartY;
				lastX1 = subPathStartX;
				lastY1 = subPathStartY;
				wasCurve = true;
				break;
			}
			case 'T':
			case 't':
				// todo - smooth quadratic Bezier (two parameters)
			case 'L':
			case 'l': {
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'l') {
					p.rLineTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					p.lineTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'H':
			case 'h': {
				float x = ph.nextFloat();
				if (cmd == 'h') {
					p.rLineTo(x, 0);
					lastX += x;
				} else {
					p.lineTo(x, lastY);
					lastX = x;
				}
				break;
			}
			case 'V':
			case 'v': {
				float y = ph.nextFloat();
				if (cmd == 'v') {
					p.rLineTo(0, y);
					lastY += y;
				} else {
					p.lineTo(lastX, y);
					lastY = y;
				}
				break;
			}
			case 'C':
			case 'c': {
				wasCurve = true;
				float x1 = ph.nextFloat();
				float y1 = ph.nextFloat();
				float x2 = ph.nextFloat();
				float y2 = ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'c') {
					x1 += lastX;
					x2 += lastX;
					x += lastX;
					y1 += lastY;
					y2 += lastY;
					y += lastY;
				}
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'Q':
			case 'q':
				// todo - quadratic Bezier (four parameters)
			case 'S':
			case 's': {
				wasCurve = true;
				float x2 = ph.nextFloat();
				float y2 = ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (Character.isLowerCase(cmd)) {
					x2 += lastX;
					x += lastX;
					y2 += lastY;
					y += lastY;
				}
				float x1 = 2 * lastX - lastX1;
				float y1 = 2 * lastY - lastY1;
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'A':
			case 'a': {
				float rx = ph.nextFloat();
				float ry = ph.nextFloat();
				float theta = ph.nextFloat();
				int largeArc = ph.nextFlag();
				int sweepArc = ph.nextFlag();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'a') {
					x += lastX;
					y += lastY;
				}
				drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc, sweepArc);
				lastX = x;
				lastY = y;
				break;
			}
			default:
				Log.w(TAG, "Invalid path command: " + cmd);
				ph.advance();
			}
			if (!wasCurve) {
				lastX1 = lastX;
				lastY1 = lastY;
			}
			ph.skipWhitespace();
		}
		return p;
	}

	private static float angle(float x1, float y1, float x2, float y2) {

		return (float) Math.toDegrees(Math.atan2(x1, y1) - Math.atan2(x2, y2)) % 360;
	}

	private static final RectF arcRectf = new RectF();
	private static final Matrix arcMatrix = new Matrix();
	private static final Matrix arcMatrix2 = new Matrix();

	private static void drawArc(Path p, float lastX, float lastY, float x, float y, float rx, float ry, float theta,
			int largeArc, int sweepArc) {
		// Log.d("drawArc", "from (" + lastX + "," + lastY + ") to (" + x + ","+ y + ") r=(" + rx + "," + ry +
		// ") theta=" + theta + " flags="+ largeArc + "," + sweepArc);

		// http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes

		if (rx == 0 || ry == 0) {
			p.lineTo(x, y);
			return;
		}

		if (x == lastX && y == lastY) {
			return; // nothing to draw
		}

		rx = Math.abs(rx);
		ry = Math.abs(ry);

		final float thrad = theta * (float) Math.PI / 180;
		final float st = FloatMath.sin(thrad);
		final float ct = FloatMath.cos(thrad);

		final float xc = (lastX - x) / 2;
		final float yc = (lastY - y) / 2;
		final float x1t = ct * xc + st * yc;
		final float y1t = -st * xc + ct * yc;

		final float x1ts = x1t * x1t;
		final float y1ts = y1t * y1t;
		float rxs = rx * rx;
		float rys = ry * ry;

		float lambda = (x1ts / rxs + y1ts / rys) * 1.001f; // add 0.1% to be sure that no out of range occurs due to
															// limited precision
		if (lambda > 1) {
			float lambdasr = FloatMath.sqrt(lambda);
			rx *= lambdasr;
			ry *= lambdasr;
			rxs = rx * rx;
			rys = ry * ry;
		}

		final float R =
				FloatMath.sqrt((rxs * rys - rxs * y1ts - rys * x1ts) / (rxs * y1ts + rys * x1ts))
						* ((largeArc == sweepArc) ? -1 : 1);
		final float cxt = R * rx * y1t / ry;
		final float cyt = -R * ry * x1t / rx;
		final float cx = ct * cxt - st * cyt + (lastX + x) / 2;
		final float cy = st * cxt + ct * cyt + (lastY + y) / 2;

		final float th1 = angle(1, 0, (x1t - cxt) / rx, (y1t - cyt) / ry);
		float dth = angle((x1t - cxt) / rx, (y1t - cyt) / ry, (-x1t - cxt) / rx, (-y1t - cyt) / ry);

		if (sweepArc == 0 && dth > 0) {
			dth -= 360;
		} else if (sweepArc != 0 && dth < 0) {
			dth += 360;
		}

		// draw
		if ((theta % 360) == 0) {
			// no rotate and translate need
			arcRectf.set(cx - rx, cy - ry, cx + rx, cy + ry);
			p.arcTo(arcRectf, th1, dth);
		} else {
			// this is the hard and slow part :-)
			arcRectf.set(-rx, -ry, rx, ry);

			arcMatrix.reset();
			arcMatrix.postRotate(theta);
			arcMatrix.postTranslate(cx, cy);
			arcMatrix.invert(arcMatrix2);

			p.transform(arcMatrix2);
			p.arcTo(arcRectf, th1, dth);
			p.transform(arcMatrix);
		}
	}

	private static NumberParse getNumberParseAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return parseNumbers(attributes.getValue(i));
			}
		}
		return null;
	}

	private static String getStringAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return attributes.getValue(i);
			}
		}
		return null;
	}

	private static Float getFloatAttr(String name, Attributes attributes) {
		return getFloatAttr(name, attributes, null);
	}

	private static Float getFloatAttr(String name, Attributes attributes, Float defaultValue) {
		String v = getStringAttr(name, attributes);
		return parseFloatValue(v, defaultValue);
	}

	private static float getFloatAttr(String name, Attributes attributes, float defaultValue) {
		String v = getStringAttr(name, attributes);
		return parseFloatValue(v, defaultValue);
	}

	private static Float parseFloatValue(String str, Float defaultValue) {
		if (str == null) {
			return defaultValue;
		} else if (str.endsWith("px")) {
			str = str.substring(0, str.length() - 2);
		} else if (str.endsWith("%")) {
			str = str.substring(0, str.length() - 1);
			return Float.parseFloat(str) / 100;
		}
		// Log.d(TAG, "Float parsing '" + name + "=" + v + "'");
		return Float.parseFloat(str);
	}

	private static class NumberParse {
		private ArrayList<Float> numbers;
		private int nextCmd;

		public NumberParse(ArrayList<Float> numbers, int nextCmd) {
			this.numbers = numbers;
			this.nextCmd = nextCmd;
		}

		public int getNextCmd() {
			return nextCmd;
		}

		public float getNumber(int index) {
			return numbers.get(index);
		}

	}

	private static class Gradient {
		String id;
		String xlink;
		boolean isLinear;
		float x1, y1, x2, y2;
		float x, y, radius;
		ArrayList<Float> positions = new ArrayList<Float>();
		ArrayList<Integer> colors = new ArrayList<Integer>();
		Matrix matrix = null;
		public Shader shader = null;
		public boolean boundingBox = false;
		public TileMode tilemode;

/*
		public Gradient createChild(Gradient g) {
			Gradient child = new Gradient();
			child.id = g.id;
			child.xlink = id;
			child.isLinear = g.isLinear;
			child.x1 = g.x1;
			child.x2 = g.x2;
			child.y1 = g.y1;
			child.y2 = g.y2;
			child.x = g.x;
			child.y = g.y;
			child.radius = g.radius;
			child.positions = positions;
			child.colors = colors;
			child.matrix = matrix;
			if (g.matrix != null) {
				if (matrix == null) {
					child.matrix = g.matrix;
				} else {
					Matrix m = new Matrix(matrix);
					m.preConcat(g.matrix);
					child.matrix = m;
				}
			}
			child.boundingBox = g.boundingBox;
			child.shader = g.shader;
			child.tilemode = g.tilemode;
			return child;
		}
*/
        public void inherit(Gradient parent) {
            Gradient child = this;
            child.xlink = parent.id;
            child.positions = parent.positions;
            child.colors = parent.colors;
            if (child.matrix == null) {
                child.matrix = parent.matrix;
            } else if (parent.matrix != null) {
            	Matrix m = new Matrix(parent.matrix);
                m.preConcat(child.matrix);
                child.matrix = m;
            }
        }
	}

	private static class StyleSet {
		HashMap<String, String> styleMap = new HashMap<String, String>();

		private StyleSet(String string) {
			String[] styles = string.split(";");
			for (String s : styles) {
				String[] style = s.split(":");
				if (style.length == 2) {
					styleMap.put(style[0], style[1]);
				}
			}
		}

		public String getStyle(String name) {
			return styleMap.get(name);
		}
	}

	private static class Properties {
		StyleSet styles = null;
		Attributes atts;

		private Properties(Attributes atts) {
			this.atts = atts;
			String styleAttr = getStringAttr("style", atts);
			if (styleAttr != null) {
				styles = new StyleSet(styleAttr);
			}
		}

		public String getAttr(String name) {
			String v = null;
			if (styles != null) {
				v = styles.getStyle(name);
			}
			if (v == null) {
				v = getStringAttr(name, atts);
			}
			return v;
		}

		public String getString(String name) {
			return getAttr(name);
		}

		private Integer rgb(int r, int g, int b) {
			return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
		}

		private int parseNum(String v) throws NumberFormatException {
			if (v.endsWith("%")) {
				v = v.substring(0, v.length() - 1);
				return Math.round(Float.parseFloat(v) / 100 * 255);
			}
			return Integer.parseInt(v);
		}

		public Integer getColor(String name) {
            String v = name;
			if (v == null) {
				return null;
			} else if (v.startsWith("#")) {
				try { // #RRGGBB or #AARRGGBB
					return Color.parseColor(v);
				} catch (IllegalArgumentException iae) {
					return null;
				}
			} else if (v.startsWith("rgb(") && v.endsWith(")")) {
				String values[] = v.substring(4, v.length() - 1).split(",");
				try {
					return rgb(parseNum(values[0]), parseNum(values[1]), parseNum(values[2]));
				} catch (NumberFormatException nfe) {
					return null;
				} catch (ArrayIndexOutOfBoundsException e) {
					return null;
				}
			} else {
				return SVGColors.mapColour(v);
			}
		}

		// convert 0xRGB into 0xRRGGBB
		private int hex3Tohex6(int x) {
			return (x & 0xF00) << 8 | (x & 0xF00) << 12 | (x & 0xF0) << 4 | (x & 0xF0) << 8 | (x & 0xF) << 4
					| (x & 0xF);
		}

		public float getFloat(String name, float defaultValue) {
			String v = getAttr(name);
			if (v == null) {
				return defaultValue;
			} else {
				try {
					return Float.parseFloat(v);
				} catch (NumberFormatException nfe) {
					return defaultValue;
				}
			}
		}

		public Float getFloat(String name, Float defaultValue) {
			String v = getAttr(name);
			if (v == null) {
				return defaultValue;
			} else {
				try {
					return Float.parseFloat(v);
				} catch (NumberFormatException nfe) {
					return defaultValue;
				}
			}
		}

		public Float getFloat(String name) {
			return getFloat(name, null);
		}
	}

	private static class LayerAttributes {
		public final float opacity;

		public LayerAttributes(float opacity) {
			this.opacity = opacity;
		}
	}

	static class SVGHandler extends DefaultHandler {

		private Picture picture;
		private Canvas canvas;
		private Float limitsAdjustmentX, limitsAdjustmentY;

		final LinkedList<LayerAttributes> layerAttributeStack = new LinkedList<LayerAttributes>();

		Paint strokePaint;
		boolean strokeSet = false;
		final LinkedList<Paint> strokePaintStack = new LinkedList<Paint>();
		final LinkedList<Boolean> strokeSetStack = new LinkedList<Boolean>();

		Paint fillPaint;
		boolean fillSet = false;
		final LinkedList<Paint> fillPaintStack = new LinkedList<Paint>();
		final LinkedList<Boolean> fillSetStack = new LinkedList<Boolean>();

		Paint textPaint;
		boolean drawCharacters;
		Float textX;
		Float textY;
		int newLineCount;
		Float textSize;
		Matrix font_matrix;

		// Scratch rect (so we aren't constantly making new ones)
		final RectF rect = new RectF();
		RectF bounds = null;
		final RectF limits = new RectF(
				Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

		Integer searchColor = null;
		Integer replaceColor = null;
		Float opacityMultiplier = null;

		boolean whiteMode = false;

		Integer canvasRestoreCount;

		final LinkedList<Boolean> transformStack = new LinkedList<Boolean>();
		final LinkedList<Matrix> matrixStack = new LinkedList<Matrix>();

		final HashMap<String, Gradient> gradientMap = new HashMap<String, Gradient>();
		Gradient gradient = null;

		public SVGHandler() {
			strokePaint = new Paint();
			strokePaint.setAntiAlias(true);
			strokePaint.setStyle(Paint.Style.STROKE);
			fillPaint = new Paint();
			fillPaint.setAntiAlias(true);
			fillPaint.setStyle(Paint.Style.FILL);
			textPaint = new Paint();
			textPaint.setAntiAlias(true);
			matrixStack.addFirst(new Matrix());
			layerAttributeStack.addFirst(new LayerAttributes(1f));
		}

		void setPicture(Picture picture) {
			this.picture = picture;
		}

		public void setColorSwap(Integer searchColor, Integer replaceColor, boolean overideOpacity) {
			this.searchColor = searchColor;
			this.replaceColor = replaceColor;
			if (replaceColor != null && overideOpacity) {
				opacityMultiplier = ((replaceColor >> 24) & 0x000000FF) / 255f;
			} else {
				opacityMultiplier = null;
			}
		}

		public void setWhiteMode(boolean whiteMode) {
			this.whiteMode = whiteMode;
		}

		@Override
		public void startDocument() throws SAXException {
			// Set up prior to parsing a doc
		}

		@Override
		public void endDocument() throws SAXException {
			// Clean up after parsing a doc
		}

		private final Matrix gradMatrix = new Matrix();

		private boolean doFill(Properties atts, RectF bounding_box) {
			if ("none".equals(atts.getString("display"))) {
				return false;
			}
			if (whiteMode) {
				fillPaint.setShader(null);
				fillPaint.setColor(Color.WHITE);
				return true;
			}
			String fillString = atts.getString("fill");
            if (fillString == null && SVG_FILL != null) {
                fillString = SVG_FILL;
            }
			if (fillString != null) {
				if (fillString.startsWith("url(#")) {

					// It's a gradient fill, look it up in our map
					String id = fillString.substring("url(#".length(), fillString.length() - 1);
					Gradient g = gradientMap.get(id);
					Shader shader = null;
					if (g != null) {
						shader = g.shader;
					}
					if (shader != null) {
						// Util.debug("Found shader!");
						fillPaint.setShader(shader);
						gradMatrix.set(g.matrix);
						if (g.boundingBox && bounding_box != null) {
							// Log.d("svg", "gradient is bounding box");
							gradMatrix.preTranslate(bounding_box.left, bounding_box.top);
							gradMatrix.preScale(bounding_box.width(), bounding_box.height());
						}
						shader.setLocalMatrix(gradMatrix);
						return true;
					} else {
						Log.w(TAG, "Didn't find shader, using black: " + id);
						fillPaint.setShader(null);
						doColor(atts, Color.BLACK, true, fillPaint);
						return true;
					}
				} else if (fillString.equalsIgnoreCase("none")) {
					fillPaint.setShader(null);
					fillPaint.setColor(Color.TRANSPARENT);
					return true;
				} else {
					fillPaint.setShader(null);
                    Integer color = atts.getColor(fillString);
					if (color != null) {
						doColor(atts, color, true, fillPaint);
						return true;
					} else {
						Log.w(TAG, "Unrecognized fill color, using black: " + fillString);
						doColor(atts, Color.BLACK, true, fillPaint);
						return true;
					}
				}
			} else {
				if (fillSet) {
					// If fill is set, inherit from parent
					return fillPaint.getColor() != Color.TRANSPARENT; // optimization
				} else {
					// Default is black fill
					fillPaint.setShader(null);
					fillPaint.setColor(Color.BLACK);
					return true;
				}
			}
		}

		private boolean doStroke(Properties atts) {
			if (whiteMode) {
				// Never stroke in white mode
				return false;
			}
			if ("none".equals(atts.getString("display"))) {
				return false;
			}

			// Check for other stroke attributes
			Float width = atts.getFloat("stroke-width");
			if (width != null) {
				strokePaint.setStrokeWidth(width);
			}

			String linecap = atts.getString("stroke-linecap");
			if ("round".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.ROUND);
			} else if ("square".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.SQUARE);
			} else if ("butt".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.BUTT);
			}

			String linejoin = atts.getString("stroke-linejoin");
			if ("miter".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.MITER);
			} else if ("round".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.ROUND);
			} else if ("bevel".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.BEVEL);
			}

			pathStyleHelper(atts.getString("stroke-dasharray"), atts.getString("stroke-dashoffset"));

			String strokeString = atts.getAttr("stroke");
			if (strokeString != null) {
				if (strokeString.equalsIgnoreCase("none")) {
					strokePaint.setColor(Color.TRANSPARENT);
					return false;
				} else {
                    Integer color = atts.getColor(strokeString);
					if (color != null) {
						doColor(atts, color, false, strokePaint);
						return true;
					} else {
						Log.w(TAG, "Unrecognized stroke color, using none: " + strokeString);
						strokePaint.setColor(Color.TRANSPARENT);
						return false;
					}
				}
			} else {
				if (strokeSet) {
					// Inherit from parent
					return strokePaint.getColor() != Color.TRANSPARENT; // optimization
				} else {
					// Default is none
					strokePaint.setColor(Color.TRANSPARENT);
					return false;
				}
			}
		}

		private Gradient doGradient(boolean isLinear, Attributes atts) {
			Gradient gradient = new Gradient();
			gradient.id = getStringAttr("id", atts);
			gradient.isLinear = isLinear;
			if (isLinear) {
				gradient.x1 = getFloatAttr("x1", atts, 0f);
				gradient.x2 = getFloatAttr("x2", atts, 1f);
				gradient.y1 = getFloatAttr("y1", atts, 0f);
				gradient.y2 = getFloatAttr("y2", atts, 0f);
			} else {
				gradient.x = getFloatAttr("cx", atts, 0f);
				gradient.y = getFloatAttr("cy", atts, 0f);
				gradient.radius = getFloatAttr("r", atts, 0f);
			}
			String transform = getStringAttr("gradientTransform", atts);
			if (transform != null) {
				gradient.matrix = parseTransform(transform);
			}
			String spreadMethod = getStringAttr("spreadMethod", atts);
			if (spreadMethod == null) {
				spreadMethod = "pad";
			}

			gradient.tilemode =
					(spreadMethod.equals("reflect")) ? Shader.TileMode.MIRROR
							: (spreadMethod.equals("repeat")) ? Shader.TileMode.REPEAT : Shader.TileMode.CLAMP;

			String unit = getStringAttr("gradientUnits", atts);
			if (unit == null) {
				unit = "objectBoundingBox";
			}
			gradient.boundingBox = !unit.equals("userSpaceOnUse");

			String xlink = getStringAttr("href", atts);
			if (xlink != null) {
				if (xlink.startsWith("#")) {
					xlink = xlink.substring(1);
				}
				gradient.xlink = xlink;
			}
			return gradient;
		}

        private void finishGradients() {
        	for(Gradient gradient : gradientMap.values()) {
                if (gradient.xlink != null) {
                    Gradient parent = gradientMap.get(gradient.xlink);
                    if (parent != null) {
                        gradient.inherit(parent);
                    }
                }
                int[] colors = new int[gradient.colors.size()];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = gradient.colors.get(i);
                }
                float[] positions = new float[gradient.positions.size()];
                for (int i = 0; i < positions.length; i++) {
                    positions[i] = gradient.positions.get(i);
                }
                if (colors.length == 0) {
               		Log.d("BAD", "BAD gradient, id="+gradient.id);
                }
                if (gradient.isLinear) {
                	gradient.shader= new LinearGradient(gradient.x1, gradient.y1, gradient.x2, gradient.y2, colors, positions, gradient.tilemode);
                } else {
                	gradient.shader= new RadialGradient(gradient.x, gradient.y, gradient.radius, colors, positions, gradient.tilemode);
                }
        	}
        }

        private void doColor(Properties atts, Integer color, boolean fillMode, Paint paint) {
			int c = (0xFFFFFF & color) | 0xFF000000;
			if (searchColor != null && searchColor.intValue() == c) {
				c = replaceColor;
			}
			paint.setShader(null);
			paint.setColor(c);
			Float opacityAttr = atts.getFloat("opacity");
			if (opacityAttr == null) {
				opacityAttr = atts.getFloat(fillMode ? "fill-opacity" : "stroke-opacity");
			}

			float opacity = opacityAttr != null ? opacityAttr : 1f;
			opacity *= currentLayerAttributes().opacity;
			if (opacityMultiplier != null) {
				opacity *= opacityMultiplier;
			}
			paint.setAlpha((int) (255f * opacity));
		}

		/**
		 * set the path style (if any) stroke-dasharray="n1,n2,..." stroke-dashoffset=n
		 */
		private void pathStyleHelper(String style, String offset) {
			if (style == null) {
				return;
			}

			if (style.equals("none")) {
				strokePaint.setPathEffect(null);
				return;
			}

			StringTokenizer st = new StringTokenizer(style, " ,");
			int count = st.countTokens();
			float[] intervals = new float[(count & 1) == 1 ? count * 2 : count];
			float max = 0;
			float current = 1f;
			int i = 0;
			while (st.hasMoreTokens()) {
				intervals[i++] = current = toFloat(st.nextToken(), current);
				max += current;
			}

			// in svg speak, we double the intervals on an odd count
			for (int start = 0; i < intervals.length; i++, start++) {
				max += intervals[i] = intervals[start];
			}

			float off = 0f;
			if (offset != null) {
				try {
					off = Float.parseFloat(offset) % max;
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			strokePaint.setPathEffect(new DashPathEffect(intervals, off));
		}

		private static float toFloat(String s, float dflt) {
			float result = dflt;
			try {
				result = Float.parseFloat(s);
			} catch (NumberFormatException e) {
				// ignore
			}
			return result;
		}

		private boolean hidden = false;
		private int hiddenLevel = 0;
		private boolean boundsMode = false;

		private void doLimits2(float x, float y) {
			if (x < limits.left) {
				limits.left = x;
			}
			if (x > limits.right) {
				limits.right = x;
			}
			if (y < limits.top) {
				limits.top = y;
			}
			if (y > limits.bottom) {
				limits.bottom = y;
			}
		}

		private final RectF tmpLimitRect = new RectF();

		private void doLimits(RectF box, Paint paint) {
			Matrix m = matrixStack.getLast();
			m.mapRect(tmpLimitRect, box);
			float width2 = (paint == null) ? 0 : paint.getStrokeWidth() / 2;
			doLimits2(tmpLimitRect.left - width2, tmpLimitRect.top - width2);
			doLimits2(tmpLimitRect.right + width2, tmpLimitRect.bottom + width2);
		}

		private void doLimits(RectF box) {
			doLimits(box, null);
		}

		private void pushTransform(Attributes atts) {
			final String transform = getStringAttr("transform", atts);
			boolean pushed = transform != null;
			transformStack.addLast(pushed);
			if (pushed) {
				final Matrix matrix = parseTransform(transform);
				canvas.save();
				canvas.concat(matrix);
				matrix.postConcat(matrixStack.getLast());
				matrixStack.addLast(matrix);
			}

		}

		private void popTransform() {
			if (transformStack.removeLast()) {
				canvas.restore();
				matrixStack.removeLast();
			}
		}

        private String SVG_FILL = null;

		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
				throws SAXException {
			// Reset paint opacity
			strokePaint.setAlpha(255);
			fillPaint.setAlpha(255);
			textPaint.setAlpha(255);

			this.drawCharacters = false;

			// Ignore everything but rectangles in bounds mode
			if (boundsMode) {
				if (localName.equals("rect")) {
					Float x = getFloatAttr("x", atts);
					if (x == null) {
						x = 0f;
					}
					Float y = getFloatAttr("y", atts);
					if (y == null) {
						y = 0f;
					}
					Float width = getFloatAttr("width", atts);
					Float height = getFloatAttr("height", atts);
					bounds = new RectF(x, y, x + width, y + height);
				}
				return;
			}
			if (localName.equals("svg")) {
				canvas = null;
                SVG_FILL = getStringAttr("fill", atts);
				String viewboxStr = getStringAttr("viewBox", atts);
				if (viewboxStr != null) {
					String[] dims = viewboxStr.replace(',', ' ').split("\\s+");
					if (dims.length == 4) {
						Float x1 = parseFloatValue(dims[0], null);
						Float y1 = parseFloatValue(dims[1], null);
						Float x2 = parseFloatValue(dims[2], null);
						Float y2 = parseFloatValue(dims[3], null);
						if (x1 != null && x2 != null && y1 != null && y2 != null) {
							x2 += x1;
							y2 += y1;

							float width = FloatMath.ceil(x2 - x1);
							float height = FloatMath.ceil(y2 - y1);
							canvas = picture.beginRecording((int) width, (int) height);
							canvasRestoreCount = canvas.save();
							canvas.clipRect(0f, 0f, width, height);
							limitsAdjustmentX = -x1;
							limitsAdjustmentY = -y1;
							canvas.translate(limitsAdjustmentX, limitsAdjustmentY);
						}
					}
				}
				// No viewbox
				if (canvas == null) {
					int width = (int) FloatMath.ceil(getFloatAttr("width", atts));
					int height = (int) FloatMath.ceil(getFloatAttr("height", atts));
					canvas = picture.beginRecording(width, height);
					canvasRestoreCount = null;
				}

			} else if (localName.equals("defs")) {
				// Ignore
			} else if (localName.equals("linearGradient")) {
				gradient = doGradient(true, atts);
			} else if (localName.equals("radialGradient")) {
				gradient = doGradient(false, atts);
			} else if (localName.equals("stop")) {
				if (gradient != null) {
					final Properties props = new Properties(atts);

					final int colour;
                    final Integer stopColour = props.getColor(props.getAttr("stop-color"));
					if (stopColour == null) {
						colour = 0;
					} else {
						float alpha = props.getFloat("stop-opacity", 1) * currentLayerAttributes().opacity;
						int alphaInt = Math.round(255 * alpha);
						colour = stopColour.intValue() | (alphaInt << 24);
					}
					gradient.colors.add(colour);

					float offset = props.getFloat("offset", 0);
					gradient.positions.add(offset);
				}
			} else if (localName.equals("g")) {
				final Properties props = new Properties(atts);

				// Check to see if this is the "bounds" layer
				if ("bounds".equalsIgnoreCase(getStringAttr("id", atts))) {
					boundsMode = true;
				}
				if (hidden) {
					hiddenLevel++;
					// Util.debug("Hidden up: " + hiddenLevel);
				}
				// Go in to hidden mode if display is "none"
				if ("none".equals(getStringAttr("display", atts)) || "none".equals(props.getString("display"))) {
					if (!hidden) {
						hidden = true;
						hiddenLevel = 1;
						// Util.debug("Hidden up: " + hiddenLevel);
					}
				}

				// Create layer attributes
				final float opacity = props.getFloat("opacity", 1f);
				LayerAttributes curLayerAttr = currentLayerAttributes();
				LayerAttributes newLayerAttr = new LayerAttributes(curLayerAttr.opacity * opacity);
				layerAttributeStack.addLast(newLayerAttr);

				pushTransform(atts);
				fillPaintStack.addLast(new Paint(fillPaint));
				strokePaintStack.addLast(new Paint(strokePaint));
				fillSetStack.addLast(fillSet);
				strokeSetStack.addLast(strokeSet);

				doFill(props, null); // Added by mrn but a boundingBox is now required by josef.
				doStroke(props);

				fillSet |= (props.getString("fill") != null);
				strokeSet |= (props.getString("stroke") != null);

			} else if (!hidden && localName.equals("rect")) {
				Float x = getFloatAttr("x", atts);
				if (x == null) {
					x = 0f;
				}
				Float y = getFloatAttr("y", atts);
				if (y == null) {
					y = 0f;
				}
				Float width = getFloatAttr("width", atts);
				Float height = getFloatAttr("height", atts);
				Float rx = getFloatAttr("rx", atts, 0f);
				Float ry = getFloatAttr("ry", atts, 0f);
				pushTransform(atts);
				Properties props = new Properties(atts);
				rect.set(x, y, x + width, y + height);
				if (doFill(props, rect)) {
					rect.set(x, y, x + width, y + height);
					if (rx <= 0f && ry <= 0f) {
						canvas.drawRect(rect, fillPaint);
					} else {
						canvas.drawRoundRect(rect, rx, ry, fillPaint);
					}
					doLimits(rect);
				}
				if (doStroke(props)) {
					rect.set(x, y, x + width, y + height);
					if (rx <= 0f && ry <= 0f) {
						canvas.drawRect(rect, strokePaint);
					} else {
						canvas.drawRoundRect(rect, rx, ry, strokePaint);
					}
					doLimits(rect, strokePaint);
				}
				popTransform();
			} else if (!hidden && localName.equals("line")) {
				Float x1 = getFloatAttr("x1", atts);
				Float x2 = getFloatAttr("x2", atts);
				Float y1 = getFloatAttr("y1", atts);
				Float y2 = getFloatAttr("y2", atts);
				Properties props = new Properties(atts);
				if (doStroke(props)) {
					pushTransform(atts);
					rect.set(x1, y1, x2, y2);
					canvas.drawLine(x1, y1, x2, y2, strokePaint);
					doLimits(rect, strokePaint);
					popTransform();
				}
			} else if (!hidden && localName.equals("text")) {
				Float textX = getFloatAttr("x", atts);
				Float textY = getFloatAttr("y", atts);
				Float fontSize = getFloatAttr("font-size", atts);
				Matrix font_matrix = parseTransform(getStringAttr("transform",
						atts));
				drawCharacters = true;
				if (fontSize != null) {
					textSize = fontSize;
					pushTransform(atts);
					if (textX != null && textY != null) {
						this.textX = textX;
						this.textY = textY;
					} else if (font_matrix != null) {
						this.font_matrix = font_matrix;
					}
					Properties props = new Properties(atts);
					Integer color = props.getColor("fill");
					if (color != null) {
						doColor(props, color, true, textPaint);
					} else {
						textPaint.setColor(Color.BLACK);
					}
					this.newLineCount = 0;
					textPaint.setTextSize(textSize);
					canvas.save();
					popTransform();
				}
			} else if (!hidden && (localName.equals("circle") || localName.equals("ellipse"))) {
				Float centerX, centerY, radiusX, radiusY;

				centerX = getFloatAttr("cx", atts);
				centerY = getFloatAttr("cy", atts);
				if (localName.equals("ellipse")) {
					radiusX = getFloatAttr("rx", atts);
					radiusY = getFloatAttr("ry", atts);

				} else {
					radiusX = radiusY = getFloatAttr("r", atts);
				}
				if (centerX != null && centerY != null && radiusX != null && radiusY != null) {
					pushTransform(atts);
					Properties props = new Properties(atts);
					rect.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);
					if (doFill(props, rect)) {
						canvas.drawOval(rect, fillPaint);
						doLimits(rect);
					}
					if (doStroke(props)) {
						canvas.drawOval(rect, strokePaint);
						doLimits(rect, strokePaint);
					}
					popTransform();
				}
			} else if (!hidden && (localName.equals("polygon") || localName.equals("polyline"))) {
				NumberParse numbers = getNumberParseAttr("points", atts);
				if (numbers != null) {
					Path p = new Path();
					ArrayList<Float> points = numbers.numbers;
					if (points.size() > 1) {
						pushTransform(atts);
						Properties props = new Properties(atts);
						p.moveTo(points.get(0), points.get(1));
						for (int i = 2; i < points.size(); i += 2) {
							float x = points.get(i);
							float y = points.get(i + 1);
							p.lineTo(x, y);
						}
						// Don't close a polyline
						if (localName.equals("polygon")) {
							p.close();
						}
						p.computeBounds(rect, false);
						if (doFill(props, rect)) {
							canvas.drawPath(p, fillPaint);
							doLimits(rect);
						}
						if (doStroke(props)) {
							canvas.drawPath(p, strokePaint);
							doLimits(rect, strokePaint);
						}
						popTransform();
					}
				}
			} else if (!hidden && localName.equals("path")) {
				Path p = doPath(getStringAttr("d", atts));
				pushTransform(atts);
				Properties props = new Properties(atts);
				p.computeBounds(rect, false);
				if (doFill(props, rect)) {
					canvas.drawPath(p, fillPaint);
					doLimits(rect);
				}
				if (doStroke(props)) {
					canvas.drawPath(p, strokePaint);
					doLimits(rect, strokePaint);
				}
				popTransform();
			} else if (!hidden) {
				Log.w(TAG, "UNRECOGNIZED SVG COMMAND: " + localName);
			}
		}

		public LayerAttributes currentLayerAttributes() {
			return layerAttributeStack.getLast();
		}

		@Override
		public void characters(char ch[], int start, int length) {
			if (this.drawCharacters) {
				if (length == 1 && ch[0] == '\n') {
					canvas.restore();
					canvas.save();

					newLineCount += 1;
					canvas.translate(0, newLineCount * textSize);
				} else {
					String text = new String(ch, start, length);
					if (this.textX != null && this.textY != null) {
						canvas.drawText(text, this.textX, this.textY, textPaint);
					} else {
						canvas.setMatrix(font_matrix);
						canvas.drawText(text, 0, 0, textPaint);
					}
					Float delta = textPaint.measureText(text);

					canvas.translate(delta, 0);
				}
			}
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
			if (localName.equals("svg")) {
				if (canvasRestoreCount != null) {
					canvas.restoreToCount(canvasRestoreCount);
				}
				if (limitsAdjustmentX != null) {
					limits.left += limitsAdjustmentX;
					limits.right += limitsAdjustmentX;
				}
				if (limitsAdjustmentY != null) {
					limits.top += limitsAdjustmentY;
					limits.bottom += limitsAdjustmentY;
				}
				picture.endRecording();

			} else if (localName.equals("linearGradient") || localName.equals("radialGradient")) {
				if (gradient.id != null) {
					gradientMap.put(gradient.id, gradient);
				}
			} else if (localName.equals("defs")) {
				finishGradients();
			} else if (localName.equals("g")) {
				if (boundsMode) {
					boundsMode = false;
				}
				// Break out of hidden mode
				if (hidden) {
					hiddenLevel--;
					// Util.debug("Hidden down: " + hiddenLevel);
					if (hiddenLevel == 0) {
						hidden = false;
					}
				}
				// // Clear gradient map
				// gradientRefMap.clear();
				popTransform();
				fillPaint = fillPaintStack.removeLast();
				fillSet = fillSetStack.removeLast();
				strokePaint = strokePaintStack.removeLast();
				strokeSet = strokeSetStack.removeLast();
				if (!layerAttributeStack.isEmpty()) {
					layerAttributeStack.removeLast();
				}
			} else if (localName.equals("text")) {
				if (this.drawCharacters) {
					this.drawCharacters = false;
					canvas.restore();
				}
			}
		}
	}
}
