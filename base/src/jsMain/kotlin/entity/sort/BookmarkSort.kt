package entity.sort

import common.CombinableComparator
import entity.Bookmark
import entity.retrieve.RetrieveRequest
import entity.retrieve.RetrieveScope

abstract class BookmarkSort : CombinableComparator<Bookmark> {

    override val next: BookmarkSort? get() = null

    final override fun compare(a: Bookmark, b: Bookmark): Int {
        return super.compare(a, b)
    }

    fun sort(list: List<Bookmark>) = list.sortedWith(this)

    open val retrieve: RetrieveRequest<Bookmark, BookmarkRetrieveQuery> = RetrieveRequest {
        next?.retrieve ?: empty()
    }

    protected fun retrieveRequest(builder: RetrieveScope<Bookmark, BookmarkRetrieveQuery>.() -> RetrieveRequest<Bookmark, BookmarkRetrieveQuery>) =
        RetrieveRequest(builder)

    companion object {
        fun combine(
            first: (parent: BookmarkSort?) -> BookmarkSort,
            vararg rest: (parent: BookmarkSort?) -> BookmarkSort
        ) =
            first(
                rest.foldRight<_, BookmarkSort?>(null) { op, acc ->
                    op(acc)
                }
            )
    }
}

val SmartSort = BookmarkSort.combine(
    SortType::UnreachedReminderLast,
    SortType::DeadlineFirst,
    SortType::ReminderFirst,
    SortType::ExpiringSoonFirst,
    { SortType.CreationDate(next = it) },
)