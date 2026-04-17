package com.slock.app.ui.profile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileHeaderOverflowTest {

    private val profileSource: String = listOf(
        File("src/main/java/com/slock/app/ui/profile/ProfileScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/profile/ProfileScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `context label column uses weight modifier to constrain width`() {
        val headerBlock = profileSource
            .substringAfter("if (headerContextLabel != null)")
            .substringBefore("} else {")
        assertTrue(
            "Context label Column must use Modifier.weight(1f) to bound text width for ellipsis",
            headerBlock.contains("Modifier.weight(1f)")
        )
    }

    @Test
    fun `context label text has maxLines and ellipsis overflow`() {
        val headerBlock = profileSource
            .substringAfter("if (headerContextLabel != null)")
            .substringBefore("} else {")
        assertTrue(
            "Context label Text must have maxLines = 1",
            headerBlock.contains("maxLines = 1")
        )
        assertTrue(
            "Context label Text must have TextOverflow.Ellipsis",
            headerBlock.contains("TextOverflow.Ellipsis")
        )
    }
}
