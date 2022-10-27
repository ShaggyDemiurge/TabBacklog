package ui.page.bookmarklist

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.UserSelect
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.toCssColor
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.thenIf
import di.AppModule
import entity.Bookmark
import org.jetbrains.compose.web.css.*
import ui.common.basecomponent.DivText
import ui.common.basecomponent.LoadingTable
import ui.common.bookmark.BookmarkTableView
import ui.styles.Palette
import ui.styles.primaryColors

@Composable
fun BookmarkList(modifier: Modifier = Modifier, onBookmarkSelect: (Bookmark) -> Unit) {

    val appModule = AppModule.Local.current
    val scope = rememberCoroutineScope()
    val onBookmarkSelectState = rememberUpdatedState(BookmarkListModel.OnBookmarkSelect(onBookmarkSelect))
    val model: BookmarkListModel = remember() {
        appModule.createBookmarkListModel(scope, onBookmarkSelectState)
    }

    Column(modifier.overflow(Overflow.Hidden)) {
        // TODO search

        Box(Modifier.fillMaxWidth().height(200.px).backgroundColor(Color.gray), contentAlignment = Alignment.Center) {
            DivText("Search will be here")
        }

        LoadingTable(
            model.bookmarkListState.list,
            model.bookmarkListState.isLoading,
            Modifier.margin(leftRight = 16.px, topBottom = 16.px).width(100.percent - 32.px)
                .minHeight(20.percent).height(100.percent - 32.px)
                .border(1.px, LineStyle.Solid, Palette.Local.current.onBackground.toCssColor()),
            onLoadMore = {
                model.requestMoreBookmarks()
            }.takeIf { !model.bookmarkListState.reachedEnd }
        ) { bookmark ->
            key(bookmark.url) {
                BookmarkTableView(
                    bookmark,
                    modifier = Modifier.padding(topBottom = 4.px, leftRight = 8.px).width(100.percent - 16.px)
                        .thenIf(model.selectedBookmarkUrl == bookmark.url, Modifier.primaryColors())
                        .userSelect(UserSelect.None)
                        .onClick { model.selectBookmark(bookmark) }
                        .thenIf(bookmark.comment.isNotBlank(), Modifier.title(bookmark.comment))
                        .attrsModifier {
                            onDoubleClick { model.openBookmark(bookmark) }
                        },
                )
            }
        }
    }
}