/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.support.annotation.DrawableRes
import android.support.annotation.RawRes
import android.support.v7.content.res.AppCompatResources
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPointStyle
import com.google.maps.android.ui.IconGenerator
import com.google.samples.apps.iosched.R
import java.util.Locale

// Map marker types
private const val MARKER_TYPE_INACTIVE = 0
private const val MARKER_TYPE_SESSION = 1
private const val MARKER_TYPE_PLAIN = 2
private const val MARKER_TYPE_LABEL = 3
private const val MARKER_TYPE_CODELAB = 4
private const val MARKER_TYPE_SANDBOX = 5
private const val MARKER_TYPE_OFFICEHOURS = 6
private const val MARKER_TYPE_MISC = 7
private const val MARKER_TYPE_ICON = 8

// Marker icon resources in res/drawable all start with this prefix
private const val ICON_RESOURCE_PREFIX = "map_marker_"

/**
 * Convert a raw resource to a GeoJsonLayer for display on a Map. The content of the resource must
 * follow the [GeoJSON format][https://tools.ietf.org/html/rfc7946].
 */
fun applyGeoJsonLayer(
    map: GoogleMap,
    @RawRes resId: Int,
    context: Context
): GeoJsonLayer {
    val layer = GeoJsonLayer(map, resId, context)

    val iconGenerator = getLabelIconGenerator(context)
    layer.features.forEach { feature ->
        val type = feature.getProperty("type")
        val title = feature.getProperty("title")

        feature.pointStyle = when (detectMarkerType(type)) {
            MARKER_TYPE_INACTIVE -> GeoJsonPointStyle() // no styling
            MARKER_TYPE_LABEL -> createLabelMarker(iconGenerator, title)
            MARKER_TYPE_ICON -> createIconMarker(context, type, title)
            else -> createPinMarker(context, title)
        }
    }

    return layer
}

/** Creates a new IconGenerator for labels on the map. */
private fun getLabelIconGenerator(context: Context): IconGenerator {
    return IconGenerator(context).apply {
        setTextAppearance(context, R.style.TextApparance_IOSched_Map_Label)
        setBackground(null)
    }
}

/** Returns a marker type based on the type string. */
private fun detectMarkerType(typeString: String?): Int {
    if (typeString == null || typeString.isEmpty()) {
        return MARKER_TYPE_INACTIVE
    }

    // normalize
    val typeUppercase = typeString.toUpperCase(Locale.US)
    if (typeUppercase.startsWith("ICON")) {
        return MARKER_TYPE_ICON
    }
    return when (typeUppercase) {
        "SESSION" -> MARKER_TYPE_SESSION
        "PLAIN" -> MARKER_TYPE_PLAIN
        "LABEL" -> MARKER_TYPE_LABEL
        "CODELAB" -> MARKER_TYPE_CODELAB
        "SANDBOX" -> MARKER_TYPE_SANDBOX
        "OFFICEHOURS" -> MARKER_TYPE_OFFICEHOURS
        "MISC" -> MARKER_TYPE_MISC
        else -> MARKER_TYPE_INACTIVE
    }
}

/** Creates a GeoJsonPointStyle for a label. */
private fun createLabelMarker(
    iconGenerator: IconGenerator,
    title: String?
): GeoJsonPointStyle {
    val icon = BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(title))
    return GeoJsonPointStyle().apply {
        setAnchor(.5f, .5f)
        setTitle(title)
        setIcon(icon)
        isVisible = true
    }
}

/**
 * Creates a GeoJsonPointStyle for a map icon. The icon is chosen based on the marker type and is
 * anchored at the bottom center of the marker's location.
 */
private fun createIconMarker(
    context: Context,
    typeString: String?,
    title: String?
): GeoJsonPointStyle {
    val bitmap = getIconMarkerBitmap(context, typeString)
    val icon = BitmapDescriptorFactory.fromBitmap(bitmap)
    return GeoJsonPointStyle().apply {
        setAnchor(0.5f, 1f)
        setTitle(title)
        setIcon(icon)
        isVisible = true
    }
}

/** Creates a GeoJsonPointStyle with a regular map pin icon. */
private fun createPinMarker(context: Context, title: String): GeoJsonPointStyle {
    val icon = BitmapDescriptorFactory.fromBitmap(
        drawableToBitmap(context, R.drawable.map_marker_unselected)
    )
    return GeoJsonPointStyle().apply {
        setAnchor(.5f, .5f)
        setTitle(title)
        setIcon(icon)
        isVisible = true
    }
}

/** Creates a bitmap for icon markers. The type should start with "ICON". */
private fun getIconMarkerBitmap(context: Context, iconType: String?): Bitmap? {
    val resId = getDrawableResourceForIconType(context, iconType)
    if (resId == 0) {
        return null // invalid
    }
    return drawableToBitmap(context, resId)
}

/**
 * Returns the drawable resource id for an icon marker. The resource name is generated by
 * prefixing [ICON_RESOURCE_PREFIX] to the icon type in lower case. Returns 0 if no resource with
 * this name exists.
 */
@DrawableRes
private fun getDrawableResourceForIconType(context: Context, iconType: String?): Int {
    if (iconType == null) {
        return 0
    }
    return context.resources.getIdentifier(
        ICON_RESOURCE_PREFIX + iconType.toLowerCase(Locale.US),
        "drawable",
        context.packageName
    )
}

/** Convert a drawable resource to a Bitmap. */
private fun drawableToBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
    // TODO cache by id?
    val drawable = AppCompatResources.getDrawable(context, resId)!!
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
