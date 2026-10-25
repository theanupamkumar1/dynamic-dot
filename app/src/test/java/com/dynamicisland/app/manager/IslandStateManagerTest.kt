package com.dynamicisland.app.manager

import com.dynamicisland.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandStateManagerTest {

    private lateinit var manager: IslandStateManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        manager = IslandStateManager.instance
        manager.clearAllTasks()
        manager.collapseIsland()
        manager.setForegroundPackage(null)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPriorityArbitration_CallOverMedia() = runTest {
        val media = MediaTask(title = "Sparks", artist = "Coldplay", isPlaying = true)
        val call = CallTask(callerName = "Anupam", isIncoming = true)

        manager.postTask(media)
        assertEquals(media.id, manager.primaryTask.value?.id)
        assertEquals(IslandMode.COLLAPSED_SINGLE, manager.islandMode.value)

        // Incoming Call has higher priority than Media
        manager.postTask(call)
        assertEquals(call.id, manager.primaryTask.value?.id)
        // Secondary task should be media (split / dual-island)
        assertEquals(media.id, manager.secondaryTask.value?.id)
        assertEquals(IslandMode.COLLAPSED_SPLIT, manager.islandMode.value)
    }

    @Test
    fun testPriorityArbitration_AlertOverCall() = runTest {
        val call = CallTask(callerName = "Anupam", isIncoming = false, durationSeconds = 30)
        val alert = ChargingAlert(batteryPercent = 62, isCharging = true, wattage = "67W Fast")

        manager.postTask(call)
        manager.postTask(alert)

        // Alert has higher priority than Call
        assertEquals(alert.id, manager.primaryTask.value?.id)
        assertEquals(call.id, manager.secondaryTask.value?.id)
    }

    @Test
    fun testExpandAndCollapseTransitions() = runTest {
        val media = MediaTask(title = "Sparks", artist = "Coldplay", isPlaying = true)
        manager.postTask(media)

        assertEquals(IslandMode.COLLAPSED_SINGLE, manager.islandMode.value)

        manager.expandIsland()
        assertEquals(IslandMode.EXPANDED, manager.islandMode.value)

        manager.collapseIsland()
        assertEquals(IslandMode.COLLAPSED_SINGLE, manager.islandMode.value)
    }

    @Test
    fun testRemoveTask_RestoresLowerPriority() = runTest {
        val media = MediaTask(title = "Sparks", artist = "Coldplay", isPlaying = true)
        val timer = TimerTask(label = "Timer", remainingSeconds = 207)

        manager.postTask(media)
        manager.postTask(timer)

        // Timer has higher priority than media
        assertEquals(timer.id, manager.primaryTask.value?.id)

        // Removing timer restores media as primary
        manager.removeTask(timer.id)
        assertEquals(media.id, manager.primaryTask.value?.id)
        assertNull(manager.secondaryTask.value)
        assertEquals(IslandMode.COLLAPSED_SINGLE, manager.islandMode.value)
    }

    @Test
    fun testAmbientState_WhenNoTasks() = runTest {
        manager.clearAllTasks()
        manager.alwaysShowAmbient = true

        val primary = manager.primaryTask.value
        assertNotNull(primary)
        assertTrue(primary is AmbientTask)
        assertEquals(IslandMode.AMBIENT, manager.islandMode.value)
    }

    @Test
    fun testForegroundAppAutoHiding() = runTest {
        val spotifyMedia = MediaTask(
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        manager.postTask(spotifyMedia)

        // Initially no foreground app set
        assertFalse(manager.isForegroundAppMatching.value)

        // When user switches to Spotify app on screen:
        manager.setForegroundPackage("com.spotify.music")
        assertTrue(manager.isForegroundAppMatching.value)

        // When user switches to WhatsApp:
        manager.setForegroundPackage("com.whatsapp")
        assertFalse(manager.isForegroundAppMatching.value)

        // When returning to home screen / launcher (no package or launcher):
        manager.setForegroundPackage(null)
        assertFalse(manager.isForegroundAppMatching.value)
    }

    @Test
    fun testArtworkColorTintingPreserved() = runTest {
        val media = MediaTask(
            title = "Blinding Lights",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music",
            accentColor = 0xFF1DB954L // Spotify green
        )
        manager.postTask(media)

        val active = manager.primaryTask.value as? MediaTask
        assertNotNull(active)
        assertEquals(0xFF1DB954L, active?.accentColor)
    }

    @Test
    fun testCycleStackedTasks() = runTest {
        val media = MediaTask(title = "Track", artist = "Artist", isPlaying = true)
        val timer = TimerTask(label = "Oven", remainingSeconds = 120)

        manager.postTask(media)
        manager.postTask(timer)

        // Timer has higher priority (80 vs 50)
        assertEquals(timer.id, manager.primaryTask.value?.id)
        assertEquals(media.id, manager.secondaryTask.value?.id)

        // Swipe / cycle forward
        manager.cycleStackedTasks(forward = true)
        assertEquals(media.id, manager.primaryTask.value?.id)
        assertEquals(timer.id, manager.secondaryTask.value?.id)

        // Cycle backward
        manager.cycleStackedTasks(forward = false)
        assertEquals(timer.id, manager.primaryTask.value?.id)
        assertEquals(media.id, manager.secondaryTask.value?.id)
    }

    @Test
    fun testStopAndDismissActiveTask() = runTest {
        var mediaStopCalled = false
        manager.onMediaStop = { mediaStopCalled = true }

        val media = MediaTask(title = "Song", artist = "Band", isPlaying = true)
        manager.postTask(media)
        assertEquals(media.id, manager.primaryTask.value?.id)

        // Stop and dismiss (e.g. left swipe on expanded card)
        manager.stopAndDismissActiveTask()
        assertTrue(mediaStopCalled)
        assertNotEquals(media.id, manager.primaryTask.value?.id)
    }

    @Test
    fun testNotificationPanelAutoCollapse() = runTest {
        val media = MediaTask(title = "Song", artist = "Band", isPlaying = true)
        manager.postTask(media)
        manager.expandIsland()
        assertEquals(IslandMode.EXPANDED, manager.islandMode.value)

        // Pulling down notification shade automatically collapses expanded card
        manager.setNotificationPanelOpen(true)
        assertEquals(IslandMode.COLLAPSED_SINGLE, manager.islandMode.value)
    }
}
