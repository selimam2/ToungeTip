package com.tonguetip

import java.time.LocalDate

class MostForgottenWordMetricFactory(): MetricFactory{
    override fun makeMetric(): Metric {
        val dbList = DatabaseHandler.getNMostUsedSuggestions(1)
        val mostForgotten = if (dbList.isNotEmpty()) dbList[0] else "You haven't forgotten a word yet!"
        return MostForgottenWordMetric(mostForgotten)
    }
}

class NumberOfSuggestionsUsedMetricFactory(): MetricFactory{
    override fun makeMetric(): Metric {
        val numForgotten = DatabaseHandler.getTotalSuggestionsUsed()
        return NumberOfSuggestionsUsedMetric(numForgotten)
    }
}

class LeastRecentlyUsedMetricFactory(): MetricFactory{
    override fun makeMetric(): Metric {
        val dbList = DatabaseHandler.getNLeastRecentSuggestions(1)
        var word = "You haven't forgotten a word yet!"
        var date: LocalDate? = null
        if (dbList.isNotEmpty()){
            word = dbList[0].first
            date = dbList[0].second
        }
        return LeastRecentlyUsedWordMetric(word, date)
    }
}

class MostRecentlyUsedMetricFactory(): MetricFactory{
    override fun makeMetric(): Metric {
        val dbList = DatabaseHandler.getNMostRecentSuggestions(1)
        var word = "You haven't forgotten a word yet!"
        var date: LocalDate? = null
        if (dbList.isNotEmpty()){
            word = dbList[0].first
            date = dbList[0].second
        }
        return MostRecentlyUsedWordMetric(word, date)
    }
}

class ForgottenWordOfTheDayMetricFactory(): MetricFactory{
    override fun makeMetric(): Metric {
        val dbList = DatabaseHandler.getNPastSuggestions(1)
        val randForgotten = if (dbList.isNotEmpty()) dbList[0] else "You haven't forgotten a word yet!"
        return ForgottenWordOfTheDayMetric(randForgotten)
    }
}
class MostForgottenWordMetric(var dbFetch: String): Metric{
    override fun getTitle(): String{
        return "Your most forgotten word is:"
    }
    override fun getBody(): String{
        return "\"$dbFetch\""
    }

}

class NumberOfSuggestionsUsedMetric(var dbFetch: Int): Metric{
    override fun getTitle(): String{
        return "You have used this many suggestions:"
    }
    override fun getBody(): String{
        return dbFetch.toString()
    }
}

class LeastRecentlyUsedWordMetric(var dbFetchWord: String, var dbFetchDate: LocalDate?): Metric{
    override fun getTitle(): String{
        return "You haven't needed to be reminded of this word in a while:"
    }
    override fun getBody(): String{
        return "\"${dbFetchWord}\"" + if(dbFetchDate != null) "\r\n(" + dbFetchDate.toString() + ")" else ""
    }
}

class MostRecentlyUsedWordMetric(var dbFetchWord: String, var dbFetchDate: LocalDate?): Metric{
    override fun getTitle(): String{
        return "You just forgot this word:"
    }
    override fun getBody(): String{
        return "\"${dbFetchWord}\"" + if(dbFetchDate != null) "\r\n(" + dbFetchDate.toString() + ")" else ""
    }
}

class ForgottenWordOfTheDayMetric(var dbFetch: String): Metric{
    override fun getTitle(): String{
        return "Your previously used suggestion of the day is:"
    }
    override fun getBody(): String{
        return "\"$dbFetch\""
    }
}