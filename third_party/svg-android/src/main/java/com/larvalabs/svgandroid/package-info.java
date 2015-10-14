/**
 * Provides a mechanism to parse a limited version of SVG Basic 1.1 files in to <code>android.graphics.Picture</code>
 * objects. This allows vector graphics files to be saved out of illustration software (such as Adobe Illustrator) as
 * SVG Basic and then used directly in an Android app. The <code>android.graphics.Picture</code> is a very optimized and
 * convenient vector graphics class. Performance is very good on a wide array of Android devices.
 * <p/>
 * Note that only SVG features that can be directly converted in to Android graphics calls are supported. The following
 * SVG Basic 1.1 features are not supported and will be ignored by the parser:
 * <ul>
 * <li>All text and font features.
 * <li>Styles.
 * <li>Symbols, conditional processing.
 * <li>Patterns.
 * <li>Masks, filters and views.
 * <li>Interactivity, linking, scripting and animation.
 * </ul>
 * Even with the above features missing, users will find that most Illustrator drawings will render perfectly well on
 * Android with this library. See the {@link com.larvalabs.svgandroid.SVGParser SVGParser} class for instructions on how
 * to use the parser.
 * 
 * @see com.larvalabs.svgandroid.SVGParser
 */
package com.larvalabs.svgandroid;

