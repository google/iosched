package com.larvalabs.svgandroid;

/**
 * Runtime exception thrown when there is a problem parsing an SVG.
 * 
 * @author Larva Labs, LLC
 */
public class SVGParseException extends RuntimeException {

	public SVGParseException(String s) {
		super(s);
	}

	public SVGParseException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public SVGParseException(Throwable throwable) {
		super(throwable);
	}
}
