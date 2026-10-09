package app.nowus.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.nowus.android.data.StateRepository
import app.nowus.android.domain.AppState
import app.nowus.android.domain.Note
import app.nowus.android.domain.Profile
import app.nowus.android.ui.NowUsApp
import app.nowus.android.ui.NowUsTheme
import app.nowus.android.ui.defaultSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test

class DaylightNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun primaryNavigationUsesMomentDayAndNoteDestinations() {
        val repository = MemoryRepository(AppState(Profile("阿青", "beijing"), setupComplete = true))
        val viewModel = AppViewModel(repository)
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithText("此刻").assertIsDisplayed()
        compose.onNodeWithText("一天").assertIsDisplayed().performClick()
        compose.onNodeWithText("留话").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("此刻").assertCountEquals(1)
    }

    @Test
    fun dayPageUsesTheV41HeadlineAndSupportingCopy() {
        val viewModel = AppViewModel(MemoryRepository(pairedState()))
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithText("一天").performClick()
        compose.onNodeWithText("我们的一天").assertIsDisplayed()
        compose.onNodeWithText("同一刻，各自的生活。").assertExists()
    }

    @Test
    fun pairedHomeUsesTheWideDesignClockAndCompactPaperPreview() {
        val viewModel = AppViewModel(MemoryRepository(pairedState()))
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithTag("clock-self").assertHeightIsEqualTo(170.dp)
        compose.onNodeWithTag("clock-partner").assertHeightIsEqualTo(170.dp)
        compose.onNodeWithText("小满 与 阿远 · 各自生活，也彼此惦记").assertExists()
        compose.onNodeWithTag("partner-note-preview").performScrollTo().assertExists()
        compose.onNodeWithText("今天的第一杯咖啡，替你也喝了一口。")
            .performScrollTo().assertExists()
    }

    @Test
    fun notePageUsesTheV41Introduction() {
        val viewModel = AppViewModel(MemoryRepository(pairedState()))
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithText("留话").performClick()
        compose.onNodeWithText("把想说的话，轻轻放在这里。").assertExists()
    }

    @Test
    fun profileDestinationIsSettingsAndExposesSharingSwitch() {
        val repository = MemoryRepository(pairedState())
        val viewModel = AppViewModel(repository)
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithContentDescription("打开我的设置").performClick()
        compose.onNodeWithText("我的", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("向对方分享日常").assertIsDisplayed().performClick()
        compose.onNodeWithText("暂停分享日常？").assertIsDisplayed()
        compose.onNodeWithText("暂停分享").performClick()
        compose.waitUntil(5_000) { !repository.data.value.sharingEnabled }
    }

    @Test
    fun rhythmLinkOpensTheDedicatedDesignScreen() {
        val viewModel = AppViewModel(MemoryRepository(pairedState()))
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithContentDescription("打开我的设置").performClick()
        compose.onNodeWithText("我的节奏").performScrollTo().performClick()
        compose.onNodeWithText("安排好平常的一天，就不用每天填写。").assertExists()
    }

    @Test
    fun invitePageUsesTheV41HeroCopy() {
        val viewModel = AppViewModel(MemoryRepository(AppState(Profile("阿青", "beijing"), setupComplete = true)))
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithText("给彼此留一个位置").performScrollTo().performClick()
        compose.onNodeWithText("从你的此刻，\n到你们的日常。").assertExists()
    }

    @Test
    fun noteDeletionRequiresConfirmationAndUndoRestoresTheSameNote() {
        val original = Note("留给你的旧留言", 1_790_000_000_000)
        val repository = MemoryRepository(AppState(Profile("阿青", "beijing"), setupComplete = true, note = original))
        val viewModel = AppViewModel(repository)
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithText("留话").performClick()
        compose.onNodeWithTag("note-delete").performScrollTo().performClick()
        compose.onNodeWithText("删除这张便签？").assertIsDisplayed()
        compose.onNodeWithText("再想想").performClick()
        compose.onNodeWithText("留给你的旧留言").assertExists()
        compose.onNodeWithTag("note-delete").performScrollTo().performClick()
        compose.onNodeWithText("删除这张便签？").assertIsDisplayed()
        compose.onNodeWithText("删除留言").performClick()
        compose.onNodeWithText("撤销").performClick()

        compose.waitUntil(5_000) { repository.data.value.note?.text == original.text }
        compose.runOnIdle { assert(repository.data.value.note?.text == original.text) }
    }

    @Test
    fun noteDraftSurvivesLeavingAndReopeningTheEditor() {
        val repository = MemoryRepository(AppState(Profile("阿青", "beijing"), setupComplete = true))
        val viewModel = AppViewModel(repository)
        compose.setContent { NowUsTheme { NowUsApp(viewModel) } }

        compose.onNodeWithText("留话").performClick()
        compose.onNodeWithTag("note-open-editor").performClick()
        compose.onNodeWithTag("noteDraft").performTextInput("路过时想起你")
        compose.onNodeWithTag("note-editor-close").performClick()
        compose.onNodeWithText("一天").performClick()
        compose.onNodeWithText("留话").performClick()
        compose.onNodeWithTag("note-open-editor").performClick()

        compose.onNodeWithTag("noteDraft").assertTextContains("路过时想起你")
        compose.runOnIdle { assert(repository.data.value.noteDraft == null) }
    }

    private class MemoryRepository(initial: AppState) : StateRepository {
        val data = MutableStateFlow(initial)
        override val states: Flow<AppState> = flow { emitAll(data) }
        override suspend fun update(transform: (AppState) -> AppState) {
            data.value = transform(data.value)
        }
    }

    private fun pairedState() = AppState(
        me = Profile("小满", "beijing"),
        schedule = defaultSchedule(),
        setupComplete = true,
        partner = Profile("阿远", "new-york"),
        partnerSchedule = defaultSchedule("new-york").let { schedule ->
            schedule.copy(
                weekday = schedule.weekday.copy(contactStart = "08:00", contactEnd = "10:30"),
                rest = schedule.rest.copy(contactStart = "08:00", contactEnd = "10:30"),
            )
        },
        partnerNote = Note("今天的第一杯咖啡，替你也喝了一口。", 1_790_000_000_000),
        paired = true,
    )
}
