package ui.page.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.foundation.layout.Spacer
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.icons.fa.FaPencil
import com.varabyte.kobweb.silk.components.icons.fa.FaStar
import com.varabyte.kobweb.silk.components.icons.fa.FaTrash
import com.varabyte.kobweb.silk.components.icons.fa.IconStyle
import com.varabyte.kobweb.silk.components.text.SpanText
import di.AppModule
import entity.BookmarkSource
import entity.BookmarkType
import org.jetbrains.compose.web.css.minus
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Text
import ui.common.basecomponent.DivText
import ui.common.basecomponent.LoadableView
import ui.common.basecomponent.RowButton
import ui.common.basecomponent.TagListView
import ui.common.bookmark.BookmarkSummaryTimerView
import ui.common.bookmark.BookmarkTitleView
import ui.common.bookmark.BookmarkTypeBacklogButton
import ui.common.bookmark.BookmarkTypeLibraryButton

@Composable
fun BookmarkSummary(
    target: BookmarkSource,
    onEditRequest: () -> Unit,
    firstButton: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appModule = AppModule.Local.current
    val scope = rememberCoroutineScope()

    val model: BookmarkSummaryModel =
        remember(target) { appModule.createBookmarkSummaryModel(scope, target) }

    LoadableView(model.bookmark, modifier.minHeight(100.px)) { bookmark, m ->
        Column(m.gap(8.px).margin(bottom = 8.px)) {
            Row(Modifier.fillMaxWidth().gap(8.px)) {

                firstButton()

                Spacer()
                if (bookmark.isSaved) {
                    RowButton(
                        onClick = { model.updateFavorite(!bookmark.favorite) },
                    ) {
                        FaStar(style = if (bookmark.favorite) IconStyle.FILLED else IconStyle.OUTLINE)
                        Text("Favorite")
                    }
                    RowButton(onClick = { model.deleteBookmark() }) {
                        FaTrash()
                        Text("Delete")
                    }
                }
            }
            BookmarkTitleView(
                bookmark.title,
                bookmark.favicon,
                bookmark.url,
                Modifier.margin(leftRight = 8.px).width(100.percent - 16.px).height(64.px)
            )
            Row(Modifier.fillMaxWidth().gap(8.px)) {
                val currentType = bookmark.takeIf { it.isSaved }?.type
                BookmarkTypeLibraryButton(currentType == BookmarkType.LIBRARY, Modifier.width(30.percent)) {
                    model.updateType(BookmarkType.LIBRARY)
                }
                BookmarkTypeBacklogButton(currentType == BookmarkType.BACKLOG, Modifier.width(30.percent)) {
                    model.updateType(BookmarkType.BACKLOG)
                }

                Spacer()

                RowButton(onClick = {
                    onEditRequest()
                }) {
                    FaPencil()
                    Text("Edit")
                }
            }

            if (bookmark.comment.isNotBlank()) {
                SpanText("Comment:")
                DivText(
                    bookmark.comment, Modifier.fontWeight(FontWeight.Lighter)
                        .margin(left = 8.px)
                        .width(100.percent - 8.px)
                )
            }

            if (bookmark.tags.isNotEmpty()) {
                SpanText("Tags:")
                TagListView(
                    bookmark.tags, Modifier.margin(leftRight = 8.px).width(100.percent - 16.px),
                )
            }

            BookmarkSummaryTimerView(
                "Timers:",
                bookmark.remindDate, bookmark.deadline, bookmark.expirationDate,
                onReminderDelete = { model.deleteReminder() },
                onDeadlineDelete = { model.deleteDeadline() },
                onExpirationDelete = { model.deleteExpiration() },
                modifier = Modifier.margin(left = 8.px).gap(8.px).width(100.percent - 8.px)
            )
        }
    }
}