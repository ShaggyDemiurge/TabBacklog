package ui.page.collection

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.icons.fa.FaFileArrowUp
import data.BrowserInteractor
import entity.MultiBookmarkSource
import entity.SingleBookmarkSource
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.minus
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Text
import ui.common.basecomponent.RowButton
import ui.page.bookmarklist.BookmarkList
import ui.page.editor.BookmarkEditor
import ui.page.summary.BookmarkMultiSummary
import ui.page.summary.BookmarkSummary

@Composable
fun CollectionView(modifier: Modifier = Modifier) {

    var selectedBookmarkUrls by remember { mutableStateOf(emptySet<String>()) }
    var editMode by remember { mutableStateOf(false) }

    Row(modifier.role("main").overflowX(Overflow.Auto).gap(32.px).flexWrap(FlexWrap.Nowrap)) {
        Box(Modifier.fillMaxHeight().minWidth(50.percent), contentAlignment = Alignment.CenterEnd) {

            BookmarkList(
                Modifier.minWidth(400.px)
                    .width(70.percent)
                    .margin(topBottom = 32.px)
                    .height(100.percent - 64.px)
                    .borderRadius(8.px)
                    .boxShadow(
                        offsetX = 0.px,
                        offsetY = 5.px,
                        blurRadius = 10.px,
                        spreadRadius = 4.px,
                        Colors.DarkGray
                    ),
                onBookmarkSelect = {
                    editMode = false
                    selectedBookmarkUrls = it
                }
            )
        }

        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxHeight()) {

            val bookmarkViewModifier = Modifier
                .borderRadius(8.px)
                .padding(16.px)
                .width(400.px)
                .boxShadow(
                    offsetX = 0.px,
                    offsetY = 5.px,
                    blurRadius = 8.px,
                    spreadRadius = 2.px,
                    Colors.Gray
                )

            if (selectedBookmarkUrls.size == 1) {
                val bookmarkUrl = selectedBookmarkUrls.single()

                if (!editMode) {
                    BookmarkSummary(
                        target = SingleBookmarkSource.Url(bookmarkUrl),
                        modifier = bookmarkViewModifier,
                        firstButton = {
                            val browserInteractor = BrowserInteractor.Local.current
                            RowButton(onClick = { browserInteractor.openPage(bookmarkUrl) }) {
                                FaFileArrowUp()
                                Text("Open")
                            }
                        },
                        onEditRequest = { editMode = true })
                } else {
                    BookmarkEditor(
                        target = SingleBookmarkSource.Url(bookmarkUrl),
                        modifier = bookmarkViewModifier,
                        onNavigateBack = { editMode = false }
                    )
                }
            } else if (selectedBookmarkUrls.size > 1) {
                BookmarkMultiSummary(
                    target = MultiBookmarkSource.Url(selectedBookmarkUrls),
                    modifier = bookmarkViewModifier,
                    firstButton = {
                        val browserInteractor = BrowserInteractor.Local.current
                        RowButton(onClick = { browserInteractor.openPages(selectedBookmarkUrls.toList()) }) {
                            FaFileArrowUp()
                            Text("Open all")
                        }
                    },
                    onEditRequest = { editMode = true }
                )
            }
        }
    }
}