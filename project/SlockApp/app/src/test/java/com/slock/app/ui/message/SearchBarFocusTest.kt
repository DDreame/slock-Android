package com.slock.app.ui.message

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SearchBarFocusTest {

    private val source: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `SearchBar TextField has focusRequester modifier`() {
        val searchBarBody = source.substringAfter("private fun SearchBar(")
            .substringBefore("private fun NeoMessage(")
        assertTrue(
            "TextField must have .focusRequester(focusRequester) modifier",
            searchBarBody.contains(".focusRequester(focusRequester)")
        )
    }

    @Test
    fun `SearchBar focusRequester is attached before focusable`() {
        val searchBarBody = source.substringAfter("private fun SearchBar(")
            .substringBefore("private fun NeoMessage(")
        val focusRequesterIndex = searchBarBody.indexOf(".focusRequester(focusRequester)")
        val focusableIndex = searchBarBody.indexOf(".focusable()")
        assertTrue(
            ".focusRequester must appear before .focusable in modifier chain",
            focusRequesterIndex in 1 until focusableIndex
        )
    }

    @Test
    fun `SearchBar requestFocus is guarded with try-catch`() {
        val searchBarBody = source.substringAfter("private fun SearchBar(")
            .substringBefore("private fun NeoMessage(")
        assertTrue(
            "requestFocus must be wrapped in try-catch for IllegalStateException",
            searchBarBody.contains("try") &&
                searchBarBody.contains("requestFocus()") &&
                searchBarBody.contains("IllegalStateException")
        )
    }

    @Test
    fun `SearchBar uses LaunchedEffect for auto-focus`() {
        val searchBarBody = source.substringAfter("private fun SearchBar(")
            .substringBefore("private fun NeoMessage(")
        assertTrue(
            "SearchBar must use LaunchedEffect for auto-focus",
            searchBarBody.contains("LaunchedEffect") &&
                searchBarBody.contains("requestFocus()")
        )
    }
}
