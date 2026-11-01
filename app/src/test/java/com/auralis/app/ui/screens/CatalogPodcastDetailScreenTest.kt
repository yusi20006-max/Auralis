package com.auralis.app.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.auralis.app.data.catalog.CatalogLoadState
import com.auralis.app.data.catalog.CatalogRepository
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

        composeRule.setContent {
            CatalogPodcastDetailScreen(
                podcast = podcast,
                catalogRepository = repository,
                libraryIdentities = libraryMembership.value,
                isDownloaded = { false },
                loadState = CatalogLoadState.Idle,
                onAddToLibrary = { libraryMembership.value = libraryMembership.value + identity },
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
            assertTrue("Download action is separately exposed from Add to Library", libraryMembership.value.isEmpty())
        }

        composeRule.onNodeWithContentDescription("افزودن " + episode.title + " به کتابخانه")
            .assertExists()
            .performClick()
        composeRule.waitUntil(5_000) { libraryMembership.value.contains(identity) }
        composeRule.onNodeWithContentDescription("حذف عضویت " + episode.title + " از کتابخانه")
            .assertExists()
            .performClick()
        composeRule.waitUntil(5_000) { libraryMembership.value.isEmpty() }
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
