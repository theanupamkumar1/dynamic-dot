package com.dynamicisland.app.util

import com.dynamicisland.app.model.CutoutConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationHelperTest {

    private val density = 2.625f // Pixel density for 1080p Nothing Phone (440dpi)
    private val config = CutoutConfig(
        offsetX = 0f,
        offsetY = 11f,
        pillWidth = 112f,
        pillHeight = 35f,
        cutoutGap = 24f
    )

    @Test
    fun testPortraitCoordinates_Rotation0() {
        val screenW = 1080
        val screenH = 2400

        val (camX, camY) = OrientationHelper.calculateCameraCenter(
            rotation = 0, // Surface.ROTATION_0
            config = config,
            density = density,
            screenW = screenW,
            screenH = screenH
        )

        // Portrait: centered horizontally (540f), Y = (11 * 2.625) + (35 * 2.625 / 2) = 28.875 + 45.9375 = 74.8125
        assertEquals(540f, camX, 1f)
        assertEquals(74.8f, camY, 1f)
    }

    @Test
    fun testLandscapeCoordinates_Rotation90_CameraOnLeft() {
        // Rotated counter-clockwise: Physical top is on the LEFT
        val screenW = 2400
        val screenH = 1080

        val (camX, camY) = OrientationHelper.calculateCameraCenter(
            rotation = 1, // Surface.ROTATION_90
            config = config,
            density = density,
            screenW = screenW,
            screenH = screenH
        )

        // CamX should be near left edge (~74.8f)
        assertEquals(74.8f, camX, 1f)
        // CamY should be vertically centered (1080 / 2 = 540f)
        assertEquals(540f, camY, 1f)
    }

    @Test
    fun testLandscapeCoordinates_Rotation270_CameraOnRight() {
        // Rotated clockwise: Physical top is on the RIGHT
        val screenW = 2400
        val screenH = 1080

        val (camX, camY) = OrientationHelper.calculateCameraCenter(
            rotation = 3, // Surface.ROTATION_270
            config = config,
            density = density,
            screenW = screenW,
            screenH = screenH
        )

        // CamX should be near right edge (2400 - 74.8f = 2325.2f)
        assertEquals(2325.2f, camX, 1f)
        // CamY should be vertically centered (540f)
        assertEquals(540f, camY, 1f)
    }

    @Test
    fun testLandscapeWindowBounds_AnchoredToCamera() {
        val screenW = 2400
        val screenH = 1080
        val pillW = (config.pillWidth * density).toInt()

        val bounds90 = OrientationHelper.calculateWindowBounds(
            rotation = 1, // ROTATION_90
            isExpanded = false,
            config = config,
            density = density,
            screenW = screenW,
            screenH = screenH
        )

        // Rotation 90: Anchored to left edge, centered vertically on camY (540)
        assertEquals(0, bounds90.x)
        assertEquals(540 - (bounds90.height / 2), bounds90.y)
        assertEquals(maxOf(pillW + (48 * density).toInt(), (200 * density).toInt()), bounds90.width)

        val bounds270 = OrientationHelper.calculateWindowBounds(
            rotation = 3, // ROTATION_270
            isExpanded = false,
            config = config,
            density = density,
            screenW = screenW,
            screenH = screenH
        )

        // Rotation 270: Anchored to right edge
        assertEquals(screenW - bounds270.width, bounds270.x)
        assertEquals(540 - (bounds270.height / 2), bounds270.y)
    }
}
