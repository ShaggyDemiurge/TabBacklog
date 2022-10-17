package ui.page.tagedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.asAttributesBuilder
import com.varabyte.kobweb.compose.ui.graphics.toCssColor
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.icons.fa.FaCheck
import com.varabyte.kobweb.silk.components.icons.fa.FaEject
import com.varabyte.kobweb.silk.components.icons.fa.FaEraser
import com.varabyte.kobweb.silk.components.icons.fa.FaTrash
import di.ModuleLocal
import org.jetbrains.compose.web.attributes.list
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Datalist
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.TextInput
import ui.common.basecomponent.RowButton
import ui.common.basecomponent.TagListView
import ui.common.styles.Palette
import ui.common.styles.components.TagComponent

@Composable
fun TagEditView(tags: List<String>, modifier: Modifier, onTagEditEvent: (TagEditEvent) -> Unit) {

    val appModule = ModuleLocal.App.current
    val scope = rememberCoroutineScope()
    val model: TagEditModel =
        remember { appModule.createTagEditModel(scope) }

    Datalist(attrs = {
        id(SUGGESTION_DATA_LIST_ID)
    }) {
        model.suggestedTags.forEach {
            key(it) {
                Option(it)
            }
        }
    }

    Column(modifier = modifier.gap(8.px)) {
        TagListView(tags, modifier = Modifier.fillMaxWidth(), tagModifier = { tag ->
            if (model.selectedTag == tag) {
                TagComponent.Selected.toModifier()
            } else {
                TagComponent.Clickable.toModifier()
                    .onClick { model.selectTag(tag) }
            }
        })

        Row(
            Modifier.fillMaxWidth().flexWrap(FlexWrap.Nowrap).gap(4.px),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextInput(model.editedTag, Modifier.width(100.percent)
                .lineHeight(1.2.em).height(1.2.em)
                .border(0.px)
                .outline(0.px)
                .borderBottom(1.px, LineStyle.Dashed, Palette.primaryColor.toCssColor())
                .onKeyDown {
                    if (it.getNormalizedKey() == "Enter") {
                        model.confirmTag(onTagEditEvent)
                    }
                }
                .asAttributesBuilder {
                    list(SUGGESTION_DATA_LIST_ID)
                    var userInput = true
                    onKeyDown {
                        // When you select item from the list, key is undefined, we can use this to separate
                        // normal input and list selection
                        userInput = it.key != undefined
                    }
                    onInput {
                        model.onTagInput(it.value)
                        if (!userInput) {
                            model.confirmTag(onTagEditEvent)
                        }
                    }
                })
            RowButton(onClick = { model.confirmTag(onTagEditEvent) }, Modifier.size(1.2.em)) {
                FaCheck()
            }
            if (model.selectedTag == null) {
                RowButton(onClick = { model.onTagInput("") }, Modifier.size(1.2.em)) {
                    FaEraser()
                }
            } else {
                RowButton(onClick = { model.deselectTag() }, Modifier.size(1.2.em)) {
                    FaEject()
                }
                RowButton(onClick = { model.deleteSelectedTag(onTagEditEvent) }, Modifier.size(1.2.em)) {
                    FaTrash()
                }
            }
        }
    }
}

private const val SUGGESTION_DATA_LIST_ID = "suggestions"