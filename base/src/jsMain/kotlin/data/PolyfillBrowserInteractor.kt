package data

import browser.downloads.DownloadOptions
import browser.tabs.AttachInfoProperty
import browser.tabs.ChangeInfoProperty
import browser.tabs.CreateCreateProperties
import browser.tabs.MoveInfoProperty
import browser.tabs.QueryQueryInfo
import browser.tabs.RemoveInfoProperty
import browser.tabs.Tab
import browser.windows.QueryOptions
import browser.windows.Window
import browser.windows.WindowType
import common.DateUtils
import data.entity.BookmarkJson
import data.entity.toBookmark
import data.entity.toJsonEntity
import data.event.TabUpdate
import data.event.WindowUpdate
import entity.Bookmark
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.BroadcastChannel
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PolyfillBrowserInteractor : BrowserInteractor {

    override suspend fun getCurrentTab() = browser.tabs.query(QueryQueryInfo {
        active = true
        currentWindow = true
    }).await().first()

    override suspend fun getTabById(id: Int): Tab = browser.tabs.get(id).await()

    override fun openManager() {
        browser.tabs.create(CreateCreateProperties {
            this.url = "manager.html"
        })
    }

    override fun openPage(url: String, active: Boolean) {
        browser.tabs.create(CreateCreateProperties {
            this.url = url
            this.active = active
        })
    }

    override fun openPages(urls: List<String>) {
        val confirm = window.confirm("You're going to open ${urls.size} tabs")
        if (confirm) {
            urls.forEach { openPage(it, false) }
        }
    }

    override fun closeTabs(tabIds: Collection<Int>) {
        val confirm = tabIds.size < 2 || window.confirm("You're going to close ${tabIds.size} tabs")
        if (confirm) {
            browser.tabs.remove(tabIds.toTypedArray())
        }
    }

    private val updateChannel = BroadcastChannel("bookmark_db_update")
    private val localUpdateFlow = MutableSharedFlow<String>()

    override suspend fun sendBookmarkUpdateMessage(url: String) {
        updateChannel.postMessage(url)
        localUpdateFlow.emit(url)
    }

    override fun subscribeToBookmarkUpdates(): Flow<String> =
        merge(
            callbackFlow {
                updateChannel.onmessage = {
                    trySend(it.data as String)
                }
                awaitClose {
                    updateChannel.onmessage = null
                }
            },
            localUpdateFlow
        )

    override fun subscribeToTabUpdates(): Flow<TabUpdate> =
        callbackFlow {
            val onCreatedListener = fun(tab: Tab) {
                trySend(TabUpdate.Open(tab.id ?: return, tab.index, tab.windowId))
            }
            val onRemovedListener = fun(tabId: Int, removeInfo: RemoveInfoProperty) {
                // Check to not handle it extra time
                if (!removeInfo.isWindowClosing) {
                    trySend(TabUpdate.Close(tabId))
                }
            }
            val onAttachedListener = fun(tabId: Int, attachInfo: AttachInfoProperty) {
                trySend(TabUpdate.Open(tabId, attachInfo.newPosition, attachInfo.newWindowId))
            }
            val onDetachedListener = fun(tabId: Int) {
                trySend(TabUpdate.Close(tabId))
            }
            val onMovedListener = fun(tabId: Int, moveInfo: MoveInfoProperty) {
                trySend(TabUpdate.Move(tabId, moveInfo.toIndex))
            }
            val onUpdatedListener = fun(tabId: Int, changeInfo: ChangeInfoProperty, tab: Tab) {
                if (changeInfo.favIconUrl != null || changeInfo.title != null || changeInfo.url != null) {
                    trySend(TabUpdate.Update(tabId, tab.title, tab.favIconUrl, tab.url))
                }
            }
            browser.tabs.onCreated.addDynamicListener(onCreatedListener)
            browser.tabs.onRemoved.addDynamicListener(onRemovedListener)
            browser.tabs.onAttached.addDynamicListener(onAttachedListener)
            browser.tabs.onDetached.addDynamicListener(onDetachedListener)
            browser.tabs.onMoved.addDynamicListener(onMovedListener)
            browser.tabs.onUpdated.addDynamicListener(onUpdatedListener)
            awaitClose {
                browser.tabs.onCreated.removeDynamicListener(onCreatedListener)
                browser.tabs.onRemoved.removeDynamicListener(onRemovedListener)
                browser.tabs.onAttached.removeDynamicListener(onAttachedListener)
                browser.tabs.onDetached.removeDynamicListener(onDetachedListener)
                browser.tabs.onMoved.removeDynamicListener(onMovedListener)
                browser.tabs.onUpdated.removeDynamicListener(onUpdatedListener)
            }
        }

    override fun subscribeToWindowUpdates(): Flow<WindowUpdate> = callbackFlow {
        val onCreatedListener = fun(window: Window) {
            if (window.isNormal()) {
                trySend(WindowUpdate.Open(window.id ?: return))
            }
        }
        val onRemovedListener = fun(windowId: Int) {
            trySend(WindowUpdate.Close(windowId))
        }
        browser.windows.onCreated.addDynamicListener(onCreatedListener)
        browser.windows.onRemoved.addDynamicListener(onRemovedListener)
        awaitClose {
            browser.windows.onCreated.removeDynamicListener(onCreatedListener)
            browser.windows.onRemoved.removeDynamicListener(onRemovedListener)
        }
    }

    override suspend fun getWindowIds(): List<Int> =
        browser.windows.getAll(QueryOptions {
            populate = false
        }).await()
            // Filtering instead of using it in query because WindowType mapping is broken
            .filter { it.isNormal() }
            .mapNotNull { it.id }

    private fun Window.isNormal(): Boolean {
        return WindowType.valueOf(this.type?.toString() ?: return false) == WindowType.normal
    }

    override suspend fun getWindowTabs(windowId: Int): List<Tab> =
        browser.tabs.query(
            QueryQueryInfo {
                this.windowId = windowId
            }
        ).await().toList()

    override suspend fun getCurrentWindowId(): Int? =
        browser.windows.getCurrent().await().id

    override suspend fun exportBookmarks(bookmarks: List<Bookmark>) {
        val json = if (bookmarks.isNotEmpty()) JSON.stringify(bookmarks.map { it.toJsonEntity() }) else "[]"
        val url = URL.createObjectURL(Blob(arrayOf(json), BlobPropertyBag(type = "application/json")))
        browser.downloads.download(
            DownloadOptions {
                this.url = url
                filename = "bl_backup_" + DateUtils.Formatter.YmdhsDash(DateUtils.now) + ".json"
            }
        ).await()
    }

    override suspend fun importBookmarks(file: Blob): List<Bookmark> {
        val fr = FileReader()
        val data = suspendCancellableCoroutine<String> { cont ->
            fr.onloadend = {
                cont.resume(fr.result as? String? ?: "[]")
            }
            fr.onerror = {
                console.log(fr.error)
                cont.resumeWithException(RuntimeException(fr.error.toString()))
            }
            fr.readAsText(file)
            cont.invokeOnCancellation {
                fr.onloadend = null
                fr.onerror = null
                fr.abort()
            }
        }
        return try {
            JSON.parse<Array<BookmarkJson>>(data).mapNotNull {
                try {
                    it.toBookmark()
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            console.error("Error while parsing input json", e)
            emptyList()
        }
    }
}