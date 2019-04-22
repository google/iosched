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
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPointStyle
import com.google.maps.android.ui.IconGenerator
import com.google.samples.apps.iosched.R
import java.util.Locale

/** Process a [GeoJsonLayer] for display on a Map. */
fun processGeoJsonLayer(layer: GeoJsonLayer, context: Context) {
    val iconGenerator = getLabelIconGenerator(context)
    layer.features.forEach { feature ->
        val icon = feature.getProperty("icon")
        val label = feature.getProperty("label") ?: feature.getProperty("title")

        val drawableRes = getDrawableResourceForIcon(context, icon)
        feature.pointStyle = when {
            drawableRes != 0 -> createIconMarker(context, drawableRes, label)
            label != null -> createLabelMarker(iconGenerator, label) // Fall back to label
            else -> GeoJsonPointStyle() // no styling
        }
    }
}

/** Creates a new IconGenerator for labels on the map. */
private fun getLabelIconGenerator(context: Context): IconGenerator {
    val labelBg = context.getDrawable(R.drawable.map_marker_label_background)
    return IconGenerator(context).apply {
        setTextAppearance(context, R.style.TextAppearance_IOSched_Map_MarkerLabel)
        setBackground(labelBg)
    }
}

/**
 * Returns the drawable resource id for an icon marker, or 0 if no resource with this name exists.
 */
@DrawableRes
fun getDrawableResourceForIcon(context: Context, iconType: String?): Int {
    if (iconType == null) {
        return 0
    }
    return context.resources.getIdentifier(
        iconType.toLowerCase(Locale.US),
        "drawable",
        context.packageName
    )
}

/** Creates a GeoJsonPointStyle for a label. */
private fun createLabelMarker(
    iconGenerator: IconGenerator,
    title: String
): GeoJsonPointStyle {
    val icon = BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(title))
    return GeoJsonPointStyle().apply {
        setAnchor(.5f, .5f)
        setIcon(icon)
        // Don't set the title because we don't want to show an InfoWindow, but set the snippet for
        // accessibility services (TalkBack).
        snippet = title
    }
}

/**
 * Creates a GeoJsonPointStyle for a map icon. The icon is chosen based on the marker type and is
 * anchored at the bottom center of the marker's location.
 */
private fun createIconMarker(
    context: Context,
    drawableRes: Int,
    title: String
): GeoJsonPointStyle {
    val bitmap = drawableToBitmap(context, drawableRes)
    val icon = BitmapDescriptorFactory.fromBitmap(bitmap)
    return GeoJsonPointStyle().apply {
        setAnchor(0.5f, 1f)
        setTitle(title)
        setIcon(icon)
    }
}

/** Convert a drawable resource to a Bitmap. */
private fun drawableToBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
    return requireNotNull(AppCompatResources.getDrawable(context, resId)).toBitmap()
}
