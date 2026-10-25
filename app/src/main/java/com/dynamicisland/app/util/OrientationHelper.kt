package com.dynamicisland.app.util

import android.view.Gravity
import com.dynamicisland.app.model.CutoutConfig

data class WindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val gravity: Int
)

object OrientationHelper {

    /**
     * Calculates the physical center coordinates (camX, camY) of the camera punch-hole
     * in the current display coordinate system based on display rotation.
     */
    fun calculateCameraCenter(
        rotation: Int, // 0 = ROTATION_0, 1 = ROTATION_90, 2 = ROTATION_180, 3 = ROTATION_270
        config: CutoutConfig,
        density: Float,
        screenW: Int,
        screenH: Int
    ): Pair<Float, Float> {
        val portraitW = minOf(screenW, screenH).toFloat()
        val portraitH = maxOf(screenW, screenH).toFloat()

        val camX_p = (portraitW / 2f) + (config.offsetX * density)
        val camY_p = (config.offsetY * density) + ((config.pillHeight * density) / 2f)

        return when (rotation) {
            1 -> { // Surface.ROTATION_90: Physical top edge is on the LEFT
                Pair(camY_p, portraitW - camX_p)
            }
            3 -> { // Surface.ROTATION_270: Physical top edge is on the RIGHT
                Pair(portraitH - camY_p, camX_p)
            }
            2 -> { // Surface.ROTATION_180: Upside down
                Pair(portraitW - camX_p, portraitH - camY_p)
            }
            else -> { // Surface.ROTATION_0: Standard portrait
                Pair(camX_p, camY_p)
            }
        }
    }

    /**
     * Calculates window layout parameters for the Dynamic Island overlay,
     * ensuring it stays strictly anchored to the camera in both portrait and landscape.
     */
    fun calculateWindowBounds(
        rotation: Int,
        isExpanded: Boolean,
        config: CutoutConfig,
        density: Float,
        screenW: Int,
        screenH: Int
    ): WindowBounds {
        val isLandscape = rotation == 1 || rotation == 3 || screenW > screenH
        val (camX, camY) = calculateCameraCenter(rotation, config, density, screenW, screenH)

        if (!isLandscape) {
            // PORTRAIT
            return if (isExpanded) {
                WindowBounds(
                    x = 0,
                    y = (config.offsetY * density).toInt(),
                    width = (config.expandedWidth * density).toInt() + (8 * density).toInt(),
                    height = (config.expandedHeight * density).toInt() + (8 * density).toInt(),
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                )
            } else {
                val pillW = (config.pillWidth * density).toInt()
                val pillH = (config.pillHeight * density).toInt()
                WindowBounds(
                    x = (config.offsetX * density).toInt(),
                    y = ((config.offsetY - 4f) * density).toInt().coerceAtLeast(0),
                    width = maxOf(pillW + (48 * density).toInt(), (200 * density).toInt()),
                    height = maxOf(pillH + (28 * density).toInt(), (64 * density).toInt()),
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                )
            }
        }

        // LANDSCAPE MODE:
        // Anchors strictly to the physical camera!
        if (isExpanded) {
            val expW = minOf((config.expandedWidth * density).toInt(), screenW - (32 * density).toInt())
            val expH = minOf((config.expandedHeight * density).toInt(), screenH - (24 * density).toInt())

            val targetX = if (rotation == 1) {
                // Camera on Left: extend from left edge with padding
                16
            } else if (rotation == 3) {
                // Camera on Right: extend to right edge with padding
                screenW - expW - 16
            } else {
                (screenW - expW) / 2
            }
            val targetY = maxOf(16, (camY - expH / 2f).toInt())

            return WindowBounds(
                x = targetX,
                y = targetY,
                width = expW,
                height = expH,
                gravity = Gravity.TOP or Gravity.START
            )
        } else {
            val pillW = (config.pillWidth * density).toInt()
            val pillH = (config.pillHeight * density).toInt()
            val winW = maxOf(pillW + (48 * density).toInt(), (200 * density).toInt())
            val winH = maxOf(pillH + (28 * density).toInt(), (64 * density).toInt())

            val targetX = if (rotation == 1) {
                // Camera on Left: anchored directly at the left edge covering punch hole
                0
            } else if (rotation == 3) {
                // Camera on Right: anchored at the right edge
                screenW - winW
            } else {
                (camX - winW / 2f).toInt()
            }
            val targetY = (camY - winH / 2f).toInt().coerceAtLeast(0)

            return WindowBounds(
                x = targetX,
                y = targetY,
                width = winW,
                height = winH,
                gravity = Gravity.TOP or Gravity.START
            )
        }
    }
}
