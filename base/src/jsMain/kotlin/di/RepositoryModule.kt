package di

import data.BookmarkRepository
import data.TabsRepository
import data.TagRepository
import dev.shustoff.dikt.Create


@Suppress("unused", "UNUSED_PARAMETER")
class RepositoryModule {

    // Doing it like this because @CreateSingle causes compilation issues
    val bookmarkRepository by lazy {
        provideBookmarkRepository()
    }

    @Create
    private fun provideBookmarkRepository(): BookmarkRepository

    val tabRepository by lazy {
        provideTabsRepository()
    }

    @Create
    private fun provideTabsRepository(): TabsRepository

    val tagRepository by lazy {
        provideTagRepository()
    }

    @Create
    private fun provideTagRepository(): TagRepository
}