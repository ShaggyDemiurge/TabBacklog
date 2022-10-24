package repository.bookmark

import browser.tabs.Tab
import com.juul.indexeddb.Key
import com.juul.indexeddb.Transaction
import com.juul.indexeddb.WriteTransaction
import common.DateUtils
import core.TestDatabaseHolder
import core.TestScope
import data.BookmarkRepository
import data.BrowserInteractor
import data.database.core.DatabaseHolder
import data.database.core.DbSchema
import data.database.core.generate
import data.database.schema.BookmarkSchema
import data.database.schema.TagSchema
import data.database.schema.extractObject
import entity.Bookmark
import entity.BookmarkType
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.js.Date

abstract class BookmarkRepositoryBaseTest {

    val bookmarkSchema = DbSchema<BookmarkSchema>()
    val tagSchema = DbSchema<TagSchema>()

    fun Date.toLocalDate() = LocalDate(getFullYear(), getMonth(), getDate())
    fun Date.toLocalDateTime() =
        LocalDateTime(getFullYear(), getMonth(), this.getDate(), getHours(), getMinutes(), getSeconds())

    val tagArb = arbitrary { Arb.string(minSize = 3, maxSize = 20).bind() }
    val tags = tagArb.take(20).toList()

    val bookmarkArb = arbitrary {
        Bookmark(
            url = "http://" + Arb.domain().bind(),
            title = Arb.string().bind(),
            favicon = Arb.domain().orNull(0.1).bind()?.let {
                "http://$it"
            },
            type = Arb.enum<BookmarkType>().bind(),
            creationDate = Arb.datetime().bind().toLocalDateTime(),
            deadline = Arb.date().orNull(0.8).bind()?.toLocalDate(),
            remindDate = Arb.date().orNull(0.5).bind()?.toLocalDate(),
            expirationDate = Arb.int(min = 0, max = 365 * 10).orNull(0.9).bind()
                ?.let { DateUtils.today + DatePeriod(days = it) },
            tags = Arb.subsequence(tags).bind(),
            favorite = Arb.boolean().bind(),
            comment = Arb.string().bind()
        )
    }

    fun bookmarkTagInvariantMatcher(target: Bookmark?) = Matcher<Bookmark?> { source ->
        MatcherResult(
            source?.let { BookmarkTagInvariant(it) } == target?.let { BookmarkTagInvariant(it) },
            { "$source should be the same as $target" },
            { "$source should not be the same as $target" },
        )
    }

    infix fun Bookmark?.shouldBeSame(target: Bookmark) = this shouldBe bookmarkTagInvariantMatcher(target)

    protected data class BookmarkTagInvariant(
        val url: String,
        val title: String,
        val favicon: String?,
        val type: BookmarkType,
        val creationDate: LocalDateTime?,
        val deadline: LocalDate? = null,
        val remindDate: LocalDate? = null,
        val expirationDate: LocalDate? = null,
        val tags: Set<String> = emptySet(),
        val favorite: Boolean = false,
        val comment: String = "",
    ) {
        constructor(bookmark: Bookmark) : this(
            bookmark.url,
            bookmark.title,
            bookmark.favicon,
            bookmark.type,
            bookmark.creationDate,
            bookmark.deadline,
            bookmark.remindDate,
            bookmark.expirationDate,
            bookmark.tags.toSet(),
            bookmark.favorite,
            bookmark.comment
        )
    }

    fun repository(holder: DatabaseHolder): BookmarkRepository =
        BookmarkRepository(
            holder,
            // TODO mocking
            object : BrowserInteractor {
                override suspend fun getCurrentTab(): Tab {
                    throw IllegalStateException("Can't test this with unit tests")
                }

                override fun openManager() {
                    throw IllegalStateException("Can't test this with unit tests")
                }
            }
        )


    protected suspend fun WriteTransaction.saveBookmark(bookmark: Bookmark, withTags: Boolean) {
        objectStore(bookmarkSchema.storeName).put(bookmarkSchema.generate(bookmark))
        if (withTags) {
            val tagsStore = objectStore(tagSchema.storeName)
            bookmark.tags.forEach { tag ->
                tagsStore.put(
                    tagSchema.generate(
                        mapOf(
                            TagSchema.Url to bookmark.url,
                            TagSchema.Tag to tag,
                        )
                    )
                )
            }
        }
    }

    protected suspend fun Transaction.loadBookmark(url: String, withTags: Boolean): Bookmark? {

        val bookmarkEntity = objectStore(bookmarkSchema.storeName).get(Key(url)) ?: return null
        val bookmark = bookmarkSchema.extractObject(bookmarkEntity)
        return if (withTags) {
            bookmark.copy(tags = getTags(url))
        } else bookmark
    }

    protected suspend fun Transaction.getTags(url: String): List<String> {
        return objectStore(tagSchema.storeName).index(TagSchema.Url.name).getAll(Key(url)).map { entity ->
            tagSchema.extract<String>(entity, TagSchema.Tag)
        }
    }

    internal suspend fun TestScope.openDatabase(): TestDatabaseHolder {
        val holder = TestDatabaseHolder(
            "test_database",
            listOf(bookmarkSchema, tagSchema)
        )
        onCleanup {
            holder.deleteDatabase()
        }
        holder.database().writeTransaction(bookmarkSchema.storeName, tagSchema.storeName) {
            bookmarkArb.take(40).forEach { bookmark ->
                saveBookmark(bookmark, withTags = true)
            }
        }
        return holder
    }

}