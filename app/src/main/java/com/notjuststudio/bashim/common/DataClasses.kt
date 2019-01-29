package com.notjuststudio.bashim.common

import com.notjuststudio.bashim.comics.ComicsBodyLoader

enum class Rating(val id: Int, val code: String) {
    RULEZ(0, "rulez"),
    SUX(1, "sux"),
    BAYAN(2, "bayan"),
}

data class Quote(
        val id: String?,
        var value: String?,
        val date: String,
        val text: String,
        var favorite: Boolean = false
) {
    var isVoted = false
    var isVoting = false
    var countOfClicking = 0
    var currentSmiley = -1
    var lastRating: Rating? = null
}

enum class QuoteType(val trueId: Boolean = true, val canLink: Boolean = true, val canFavorite: Boolean = true, val canVote: Boolean = true, val needTop: Boolean = false) {
    REGULAR(),
    COMICS(),
    SINGLE(canLink = false),
    RANDOM_OFFLINE(canVote = false),
    ABYSS_NEW(trueId = false, canLink = false, canFavorite = false),
    ABYSS_TOP(trueId = false, canLink = false, canFavorite = false, canVote = false, needTop = true),
    ABYSS_BEST(trueId = false, canLink = false, canFavorite = false, canVote = false);
}

enum class TitleType {
    NONE,
    BEST_MONTH,
    BEST_YEAR,
    ABYSS_BEST,
    COMICS
}

enum class Link(val id: Int, val type: QuoteType = QuoteType.REGULAR, val title: TitleType = TitleType.NONE, val loadOnScroll: Boolean = true){
    NONE(-1),
    NEW(0),

    RANDOM_ONLINE(1),
    RANDOM_OFFLINE(2, type = QuoteType.RANDOM_OFFLINE),

    BEST_TODAY(3, loadOnScroll = false),
    BEST_MONTH(4, title = TitleType.BEST_MONTH, loadOnScroll = false),
    BEST_YEAR(5, title = TitleType.BEST_YEAR, loadOnScroll = false),
    BEST_ALL(6),

    ABYSS_NEW(7, type = QuoteType.ABYSS_NEW),
    ABYSS_TOP(8, type = QuoteType.ABYSS_TOP, loadOnScroll = false),
    ABYSS_BEST(9, type = QuoteType.ABYSS_BEST, title = TitleType.ABYSS_BEST, loadOnScroll = false),

    FAVORITE(10),
    COMICS(11, type = QuoteType.COMICS, title = TitleType.COMICS, loadOnScroll = false),
    SEARCH(12, loadOnScroll = false);

    companion object {
        private val map = Link.values().associateBy(Link::id)

        fun fromInt(type: Int) = map[type] ?: NONE
    }
}

data class ComicsHeader(
        val comicsId: String,
        val url: String)

data class ComicsUnit(
        val header: ComicsHeader
) {

    fun loadBody(setup: (String, String?, String, String?) -> Unit, onFail: () -> Unit) {
        ComicsBodyLoader.loadComics(header.comicsId, {
            comicsUrl, quoteId, authorName, authorUrl ->
            setup(comicsUrl, quoteId, authorName, authorUrl)
        }, onFail)
        header.comicsId
    }

}

data class ComicsMonth(
        val name: String,
        val comics: List<ComicsUnit>
)

data class ComicsYear(
        val name: String,
        val months: List<ComicsMonth>
) {

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        if (!(other is ComicsYear))
            return false
        if (this === other)
            return true

        return this.name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + months.hashCode()
        return result
    }

}