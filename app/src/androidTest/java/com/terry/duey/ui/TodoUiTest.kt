package com.terry.duey.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.terry.duey.MainActivity
import org.junit.Rule
import org.junit.Test

class TodoUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_startsOnHomeworkTab() {
        composeTestRule.onNodeWithTag("tab_homework").assertIsDisplayed()
    }

    @Test
    fun navigation_worksBetweenTabs() {
        composeTestRule.onNodeWithTag("tab_schedule").performClick()
        composeTestRule.onNodeWithTag("tab_schedule").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_new").performClick()
        composeTestRule.onNodeWithTag("new_schedule_title").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_more").performClick()
        composeTestRule.onNodeWithTag("tab_more").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_homework").performClick()
        composeTestRule.onNodeWithTag("tab_homework").assertIsDisplayed()
    }

    @Test
    fun newSchedule_voiceButtonIsDisabledWhenLoggedOut() {
        composeTestRule.onNodeWithTag("tab_new").performClick()

        composeTestRule.onNodeWithTag("btn_voice_add_schedule").assertIsNotEnabled()
    }

    @Test
    fun addNewTodo_showsInHomeworkTab() {
        val testTitle = "UI Test Todo ${System.currentTimeMillis()}"

        addTodo(testTitle)

        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }

    @Test
    fun toggleTodo_updatesCompletionState() {
        val testTitle = "UI Toggle Todo ${System.currentTimeMillis()}"

        addTodo(testTitle)
        composeTestRule.onNodeWithTag("todo_checkbox_$testTitle").performClick()

        composeTestRule.onNodeWithText("완료").assertIsDisplayed()
    }

    private fun addTodo(title: String) {
        composeTestRule.onNodeWithTag("tab_new").performClick()
        composeTestRule.onNodeWithTag("new_schedule_title").performTextInput(title)
        composeTestRule.onNodeWithTag("new_schedule_save").performClick()
    }
}
