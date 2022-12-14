package ui.manager

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.minus
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

@Composable
fun ManagerLayout(
    modifier: Modifier = Modifier,
    searchBlock: @Composable (modifier: Modifier) -> Unit,
    editBlock: @Composable (modifier: Modifier) -> Unit,
) {
    Row(modifier.role("main").overflowX(Overflow.Auto).gap(32.px).flexWrap(FlexWrap.Nowrap)) {
        Box(Modifier.fillMaxHeight().minWidth(50.percent), contentAlignment = Alignment.CenterEnd) {

            searchBlock(
                Modifier.minWidth(400.px)
                    .width(70.percent)
                    .margin(topBottom = 32.px)
                    .height(100.percent - 64.px)
                    .borderRadius(8.px)
                    .overflow(Overflow.Hidden)
                    .boxShadow(
                        offsetX = 0.px,
                        offsetY = 5.px,
                        blurRadius = 10.px,
                        spreadRadius = 4.px,
                        Colors.DarkGray
                    )
            )
        }

        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxHeight()) {

            editBlock(
                Modifier
                    .borderRadius(8.px)
                    .padding(16.px)
                    .boxShadow(
                        offsetX = 0.px,
                        offsetY = 5.px,
                        blurRadius = 8.px,
                        spreadRadius = 2.px,
                        Colors.Gray
                    )
            )
        }
    }
}