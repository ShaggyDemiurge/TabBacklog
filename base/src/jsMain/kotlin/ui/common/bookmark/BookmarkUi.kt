package ui.common.bookmark

import androidx.compose.runtime.Composable
import entity.Bookmark
import entity.Loadable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import ui.common.basecomponent.LoadingSpinner
import ui.common.bookmark.editor.BookmarkEditor

@Composable
fun BookmarkContent(
    bookmark: Loadable<Bookmark>,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
) {
    when (bookmark) {
        is Loadable.Error -> BookmarkError(bookmark.error, attrs)
        is Loadable.Loading -> BookmarkLoading(attrs)
        is Loadable.Success -> BookmarkEditor(bookmark.value, attrs)
    }
}

@Composable
fun BookmarkError(
    error: Throwable,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
) {
    Div(attrs = {
        attrs?.invoke(this)
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            flexDirection(FlexDirection.Column)
            alignContent(AlignContent.Center)
            justifyContent(JustifyContent.Center)
        }
    }) {
        P { Text("Extension has encountered an error") }

        error.message?.let {
            P(attrs = { style { color(Color.red) } }) {
                Text(it)
            }
        }
    }
}

@Composable
fun BookmarkLoading(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
) {
    Div(attrs = {
        attrs?.invoke(this)
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
        }
    }) {
        LoadingSpinner()
    }
}