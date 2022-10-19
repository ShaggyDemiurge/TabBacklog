package ui.page.bookmarklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.style.toModifier
import entity.Bookmark
import org.jetbrains.compose.web.css.px
import org.w3c.dom.HTMLElement
import ui.common.basecomponent.DivText
import ui.common.basecomponent.LoadingSpinner
import ui.styles.components.TableContentComponent

@Composable
fun BookmarkTable(
    values: List<Int>,
    isLoading: Boolean,
    modifier: Modifier,
    onSelect: (Bookmark) -> Unit,
    onLoadMore: (() -> Unit)?
) {

    fun HTMLElement.checkLoadMore() {
        if (isLoading || onLoadMore == null) return
        if (offsetHeight + scrollTop >= scrollHeight * 0.95f) {
            onLoadMore()
        }
    }

    Column(
        elementScope = {
            DisposableEffect(this, values) {
                scopeElement.checkLoadMore()
                onDispose { }
            }
        },
        modifier = TableContentComponent.Style.toModifier()
            .then(modifier)
            .overflowY(Overflow.Auto)
            .attrsModifier {
                if (onLoadMore != null) {
                    onScroll { event ->
                        val element = event.target as? HTMLElement ?: return@onScroll
                        element.checkLoadMore()
                    }
                }
            }
    ) {
        for (i in values) {
            DivText(i.toString(), Modifier.fillMaxWidth())
        }

        if (isLoading) {
            Box(
                modifier = Modifier.height(40.px).padding(topBottom = 8.px).margin(bottom = 8.px).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner()
            }
        }
    }
}