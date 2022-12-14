package di

import data.BookmarkRepository
import data.BrowserInteractor
import data.PolyfillBrowserInteractor
import data.TabsRepository
import data.TagRepository
import data.database.core.AppDatabaseHolder
import data.database.core.DatabaseHolder
import data.database.core.DbSchema
import data.database.schema.AppMigrationManager
import data.database.schema.BookmarkSchema
import data.database.schema.TagCountSchema
import data.database.schema.TagSchema
import dev.shustoff.dikt.Create


@Suppress("unused", "UNUSED_PARAMETER", "KotlinRedundantDiagnosticSuppress")
class RepositoryModule {

    val databaseHolder: DatabaseHolder by lazy {
        AppDatabaseHolder(
            "TabBacklog",
            1,
            listOf(
                DbSchema<BookmarkSchema>(),
                DbSchema<TagSchema>(),
                DbSchema<TagCountSchema>(),
            ),
            AppMigrationManager(),
        )
    }

    val browserInteractor: BrowserInteractor by lazy {
        PolyfillBrowserInteractor()
    }

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