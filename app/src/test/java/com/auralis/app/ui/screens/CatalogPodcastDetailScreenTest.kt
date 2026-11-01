package com.auralis.app.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auralis.app.data.catalog.CatalogLoadState
import com.auralis.app.data.catalog.CatalogRepository
import com.auralis.app.data.catalog.CatalogPodcast
import com.auralis.app.data.download.DownloadIdentity
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogPodcastDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playDownloadAndLibraryMembershipAreSeparateAccessibleActions() {
        val repository = CatalogRepository()
        val podcast = repository.getPodcast("ai-philosophy")!!
        val episode = repository.getEpisode("ep-philo-1")!!
        val identity = DownloadIdentity("catalog:auralis", episode.id)
        val libraryMembership = mutableStateOf(emptySet<DownloadIdentity>())
        val playCalls = AtomicInteger()
        val downloadCalls = AtomicInteger()
        val addCalls = AtomicInteger()

        composeRule.setContent {
            CatalogPodcastDetailScreen(
                podcast = podcast,
                catalogRepository = repository,
                libraryIdentities = libraryMembership.value,
                isDownloaded = { false },
                loadState = CatalogLoadState.Idle,
                onAddToLibrary = {
                    addCalls.incrementAndGet()
                    libraryMembership.value = libraryMembership.value + identity
                },
                onRemoveFromLibrary = { libraryMembership.value = libraryMembership.value - identity },
                onPlayEpisode = { playCalls.incrementAndGet() },
                onDownloadEpisode = { downloadCalls.incrementAndGet() },
                onBack = {},
            )
        }

        composeRule.onNodeWithContentDescription("پخش آنلاین " + episode.title)
            .assertExists()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, playCalls.get())
            assertTrue("Play must not create Library membership", libraryMembership.value.isEmpty())
        }

        composeRule.onNodeWithContentDescription("دانلود خصوصی " + episode.title)
            .assertExists()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, downloadCalls.get())
            assertEquals("Download must not dispatch the Add action handler", 0, addCalls.get())
            assertTrue(libraryMembership.value.isEmpty())
        }

        composeRule.onNodeWithContentDescription("افزودن " + episode.title + " به کتابخانه")
            .assertExists()
            .performClick()
        composeRule.waitUntil(5_000) { libraryMembership.value.contains(identity) }
        composeRule.runOnIdle { assertEquals(1, addCalls.get()) }
        composeRule.onNodeWithContentDescription("حذف عضویت " + episode.title + " از کتابخانه")
            .assertExists()
            .performClick()
        composeRule.waitUntil(5_000) { libraryMembership.value.isEmpty() }
    }

    @Test
    fun loadingAndOfflineStatesAreVisibleWithoutHidingCachedContent() {
        val repository = CatalogRepository()
        val podcast = repository.getPodcast("ai-philosophy")!!
        val loadState = mutableStateOf<CatalogLoadState>(CatalogLoadState.Loading)

        composeRule.setContent {
            CatalogPodcastDetailScreen(
                podcast = podcast,
                catalogRepository = repository,
                loadState = loadState.value,
                onBack = {},
            )
        }

        composeRule.onNodeWithText("در حال بررسی به‌روزرسانی کاتالوگ…").assertExists()
        composeRule.onNodeWithText(podcast.title).assertExists()

        composeRule.runOnIdle {
            loadState.value = CatalogLoadState.Skipped("offline")
        }
        composeRule.onNodeWithText("دریافت آنلاین غیرفعال است؛ محتوای موجود نمایش داده می‌شود.")
            .assertExists()
        composeRule.onNodeWithText(podcast.title).assertExists()
    }

    @Test
    fun podcastWithoutEpisodesShowsExplicitEmptyState() {
        val repository = CatalogRepository()
        val emptyPodcast = CatalogPodcast(
            id = "empty-podcast",
            title = "Empty podcast",
            description = "No episodes yet",
        )

        composeRule.setContent {
            CatalogPodcastDetailScreen(
                podcast = emptyPodcast,
                catalogRepository = repository,
                onBack = {},
            )
        }

        composeRule.onNodeWithText("برای این پادکست هنوز اپیزودی ثبت نشده است.").assertExists()
    }

    @Test
    fun missingArtworkUsesSquareAccessiblePlaceholder() {
        composeRule.setContent {
            PodcastArtwork(
                coverUrl = null,
                title = "Auralis",
                modifier = Modifier.size(56.dp),
            )
        }

        composeRule.onNodeWithContentDescription("جلد پادکست Auralis")
            .assertExists()
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithText("A").assertExists()
    }

    @Test
    fun catalogErrorShowsRetryAction() {
        val repository = CatalogRepository()
        val podcast = repository.getPodcast("ai-philosophy")!!
        val retryCalls = AtomicInteger()

        composeRule.setContent {
            CatalogPodcastDetailScreen(
                podcast = podcast,
                catalogRepository = repository,
                loadState = CatalogLoadState.Error("network unavailable"),
                onRetryCatalog = { retryCalls.incrementAndGet() },
                onBack = {},
            )
        }

        composeRule.onNodeWithText("دریافت کاتالوگ آنلاین ناموفق بود؛ محتوای قبلی حفظ شده است.")
            .assertExists()
        composeRule.onNodeWithText("تلاش دوباره")
            .assertExists()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, retryCalls.get()) }
    }
}
