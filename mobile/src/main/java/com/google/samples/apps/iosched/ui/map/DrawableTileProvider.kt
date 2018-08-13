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

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Picture
import android.graphics.drawable.Drawable
import androidx.core.util.Pools.Pool
import androidx.core.util.Pools.SynchronizedPool
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.samples.apps.iosched.BuildConfig
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

class DrawableTileProvider(
    dpi: Float,
    drawable: Drawable
) : TileProvider {

    companion object {
        private const val BASE_TILE_SIZE = 256
    }

    private val scale = Math.round(dpi + .3f) // Make it look nice for tvdpi (1.3x)
    private val tileSize = BASE_TILE_SIZE * scale

    private val picture: Picture
    private val baseMatrix: Matrix

    private val pool: Pool<TileGenerator> = SynchronizedPool(5)

    init {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight

        picture = Picture()
        val canvas = picture.beginRecording(width, height)
        drawable.draw(canvas)
        picture.endRecording()

        val points = floatArrayOf(
            0f, 0f, // North-west
            width.toFloat(), 0f, // North-east
            width.toFloat(), height.toFloat() // Sourth-east
        )

        baseMatrix = Matrix().apply {
            // Map the drawable's corners to global coordinates
            setPolyToPoly(points, 0, BuildConfig.MAP_FLOORPLAN_MAPPING, 0, 3)
        }
    }

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val tileGenerator = pool.acquire()
            ?: TileGenerator(tileSize, scale, baseMatrix, picture)
        val data = tileGenerator.getTileImageData(x, y, zoom)
        if (!pool.release(tileGenerator)) {
            // Not returned to pool
            tileGenerator.cleanup()
        }
        return Tile(tileSize, tileSize, data)
    }

    /** Generates tiles from a picture. */
    internal class TileGenerator(
        private val tileSize: Int,
        private val scale: Int,
        private val baseMatrix: Matrix,
        private val picture: Picture
    ) {
        // Tiles are square, and each pixel is represented by 4 bytes.
        private val outputStream = ByteArrayOutputStream(tileSize * tileSize * 4)
        private val bitmap = Bitmap.createBitmap(tileSize, tileSize, ARGB_8888)

        internal fun getTileImageData(x: Int, y: Int, zoom: Int): ByteArray {
            outputStream.reset()

            val scaleFactor = (Math.pow(2.0, zoom.toDouble()) * scale).toFloat()
            val matrix = Matrix(baseMatrix).apply {
                postScale(scaleFactor, scaleFactor)
                postTranslate((-x * tileSize).toFloat(), (-y * tileSize).toFloat())
            }

            bitmap.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(bitmap).apply {
                setMatrix(matrix)
            }

            // TODO maybe synchronize?
            picture.draw(canvas)

            val stream = BufferedOutputStream(outputStream)
            stream.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
            }
            return outputStream.toByteArray()
        }

        internal fun cleanup() {
            bitmap.recycle()
            outputStream.close()
        }
    }
}
