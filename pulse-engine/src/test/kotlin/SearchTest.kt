import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.models.bodies.SearchSuggestionsBody
import com.synap.pulse.requests.searchPage
import com.synap.pulse.requests.searchSuggestions
import com.synap.pulse.utils.from
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class SearchTest {
    @Test
    fun testSearchSuggestions() = runBlocking {
        val result = SynapService.searchSuggestions(SearchSuggestionsBody(input = "test"))
        if (result != null) {
            assertTrue("Result should be success: ${result.exceptionOrNull()}", result.isSuccess)
            val suggestions = result.getOrNull()
            assertNotNull("Suggestions should not be null", suggestions)
        }
    }

    @Test
    fun testSearchSongs() = runBlocking {
        val result = SynapService.searchPage(
            body = SearchBody(query = "test", params = SynapService.SearchFilter.Song.value),
            fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
        )
        if (result != null) {
            assertTrue("Result should be success: ${result.exceptionOrNull()}", result.isSuccess)
            val itemsPage = result.getOrNull()
            assertNotNull("ItemsPage should not be null", itemsPage)
        }
    }

    @Test
    fun testSearchVideos() = runBlocking {
        val result = SynapService.searchPage(
            body = SearchBody(query = "Michael Jackson", params = SynapService.SearchFilter.Video.value),
            fromMusicShelfRendererContent = SynapService.VideoItem.Companion::from,
            fromMusicTwoRowItemRenderer = SynapService.VideoItem.Companion::from
        )
        if (result != null) {
            assertTrue("Result should be success: ${result.exceptionOrNull()}", result.isSuccess)
            val itemsPage = result.getOrNull()
            assertNotNull("ItemsPage should not be null", itemsPage)
            println("Found ${itemsPage?.list?.size} videos")
            itemsPage?.list?.forEach { video ->
                println("Video: ${video.info?.name} (ID: ${video.info?.endpoint?.videoId})")
            }
        }
    }
}
