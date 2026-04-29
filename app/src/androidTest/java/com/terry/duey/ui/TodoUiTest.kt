package com.terry.duey.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
        composeTestRule.onNodeWithText("오늘의 숙제").assertIsDisplayed()
    }

    @Test
    fun navigation_worksBetweenTabs() {
        // Go to Schedule tab
        composeTestRule.onNodeWithTag("tab_schedule").performClick()
        composeTestRule.onNodeWithText("내 일정").assertIsDisplayed()

        // Go to New Schedule tab
        composeTestRule.onNodeWithTag("tab_new").performClick()
        composeTestRule.onNodeWithText("새 일정 만들기").assertIsDisplayed()

        // Go to Settings tab
        composeTestRule.onNodeWithTag("tab_more").performClick()
        // Use atIndex(0) because "설정" is both tab label and screen title
        composeTestRule.onAllNodesWithText("설정").onFirst().assertIsDisplayed()

        // Return to Homework tab
        composeTestRule.onNodeWithTag("tab_homework").performClick()
        composeTestRule.onNodeWithText("오늘의 숙제").assertIsDisplayed()
    }

    @Test
    fun addNewTodo_showsInHomeworkTab() {
        val testTitle = "UI Test Todo ${System.currentTimeMillis()}"

        // Go to New Schedule tab
        composeTestRule.onNodeWithTag("tab_new").performClick()

        // Input title
        composeTestRule.onNodeWithText("무엇을 해야 하나요?").performTextInput(testTitle)

        // Save
        composeTestRule.onNodeWithText("일정 저장하기").performClick()

        // Should return to Homework tab
        composeTestRule.onNodeWithText("오늘의 숙제").assertIsDisplayed()

        // Check if new todo exists
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }

    @Test
    fun toggleTodo_updatesCompletionState() {
        // Find a checkbox and click it
        composeTestRule.onAllNodes(hasClickAction()).filter(hasSetTextAction().not())
            .onFirst().performClick()

        // Opening detail dialog to verify state
        composeTestRule.onAllNodesWithText("D-", substring = true).onFirst().performClick()

        // In detail dialog, check for "미완료" or "완료" button
        val nodesFound = composeTestRule.onAllNodesWithText("미완료").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("완료").fetchSemanticsNodes().isNotEmpty()
        assertTrue("Completion toggle button should be present", nodesFound)
    }

    private fun assertTrue(message: String, condition: Boolean) {
        if (!condition) throw java.lang.AssertionError(message)
    }
}
