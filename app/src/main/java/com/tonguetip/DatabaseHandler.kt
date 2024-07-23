package com.tonguetip

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// A suggestion context (i.e. the sentence that caused the LLM to give the suggestion)
data class SuggestionContext(
    val contextString: String,
    val dateSuggested: LocalDate
)

// Defines all table and column names for sqlite database
object SuggestionHistoryContract {
    // Table contents are grouped together in an anonymous object.
    object SuggestionEntry : BaseColumns {
        const val TABLE_NAME = "suggestion"
        const val COLUMN_SUGGESTION_NAME = "suggestion_name"
        const val COLUMN_SUGGESTION_OCCURRENCES = "occurrences"
    }

    object SuggestionContextsEntry : BaseColumns {
        const val TABLE_NAME = "suggestion_context"
        const val COLUMN_SUG_KEY = "suggestion_key"
        const val COLUMN_SUGGESTION_SENTENCE = "sentence"
        const val COLUMN_SUGGESTION_DATE = "date"
    }
}

// Manages the sqlite database, create on app start, call close after the activity is destroyed.
// Opens automatically on calling a fetch or push request
class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Companion object/singleton
    companion object {
        // Constant values for database handler
        const val DATABASE_VERSION = 4
        const val DATABASE_NAME = "SuggestionHistory.db"

        // SQLITE queries for creating and deleting tables
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} TEXT UNIQUE," +
                "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES} INTEGER DEFAULT 0)"

        private const val SQL_CREATE_CONTEXTS =
            "CREATE TABLE IF NOT EXISTS ${SuggestionHistoryContract.SuggestionContextsEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY} INTEGER," +
                    "${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_SENTENCE} TEXT," +
                    "${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_DATE} TEXT," +
                    "FOREIGN KEY(${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY}) " +
                    "REFERENCES ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME}(${BaseColumns._ID}))"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME}"

        private const val SQL_DELETE_CONTEXTS = "DROP TABLE IF EXISTS ${SuggestionHistoryContract.SuggestionContextsEntry.TABLE_NAME}"

        // Static functions for accessing database
        private lateinit var DBINSTANCE: DatabaseHandler
        private lateinit var DATABASE: SQLiteDatabase

        // Call on every activity's OnDestroy if the activity used the database
        fun closeDatabase(){
            if(::DATABASE.isInitialized && DATABASE.isOpen){
                DATABASE.close()
            }
        }
        // Implicitly called by all functions that fetch/push info to the db
        fun initDataBaseInst(ctx: Context): SQLiteOpenHelper{
            DBINSTANCE = DatabaseHandler(ctx)
            return DBINSTANCE
        }

        // Private function for translating suggestion to the primary key
        private fun getIDofWord(word: String): Long{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }
            val cursor = DATABASE.query(
                SuggestionHistoryContract.SuggestionEntry.TABLE_NAME,
                arrayOf(BaseColumns._ID),
                "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} = ?",
                arrayOf(word),
                null,
                null,
                null
            )

            var itemID: Long
            with(cursor) {
                moveToNext()
                itemID = getLong(getColumnIndexOrThrow(BaseColumns._ID))
            }
            cursor.close()

            return itemID
        }

        // Get how many suggestions total have been given to the user (that they accepted)
        fun getTotalSuggestionsUsed(): Int{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            val QUERY = "SELECT SUM(${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES}) AS totSum " +
                    "FROM ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME}"

            val cursor = DATABASE.rawQuery(QUERY, null)

            var dbSum = 0
            with(cursor) {
                if (moveToNext()) {
                    dbSum = getInt(getColumnIndexOrThrow("totSum"))
                }
            }
            cursor.close()

            return dbSum
        }

        // Get the n least recently used suggestions by the user (if no value passed for n, gives all)
        fun getNLeastRecentSuggestions(numSuggestions: Long? = null): List<Pair<String,LocalDate>>{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            var QUERY = "SELECT ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME}," +
                    "MIN(${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_DATE}) AS leastRecent FROM " +
                    "${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} JOIN ${SuggestionHistoryContract.SuggestionContextsEntry.TABLE_NAME} " +
                    "ON ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME}.${BaseColumns._ID} = ${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY} " +
                    "GROUP BY ${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY} " +
                    "ORDER BY leastRecent DESC"

            if(numSuggestions != null)
            {
                QUERY += " LIMIT $numSuggestions"
            }

            val cursor = DATABASE.rawQuery(QUERY, null)

            val suggestionList = mutableListOf<Pair<String,LocalDate>>()
            with(cursor) {
                while (moveToNext()) {
                    val pair = Pair<String,LocalDate>(
                        getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME)),
                        LocalDate.parse(
                            getString(getColumnIndexOrThrow("leastRecent")),
                            DateTimeFormatter.BASIC_ISO_DATE))
                    suggestionList.add(pair)
                }
            }
            cursor.close()
            return suggestionList
        }

        // Get the n most recently used suggestions by the user (if no value passed for n, gives all)
        fun getNMostRecentSuggestions(numSuggestions: Long? = null): List<Pair<String,LocalDate>>{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            var QUERY = "SELECT ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME}," +
                    "MAX(${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_DATE}) AS mostRecent FROM " +
                    "${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} JOIN ${SuggestionHistoryContract.SuggestionContextsEntry.TABLE_NAME} " +
                    "ON ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME}.${BaseColumns._ID} = ${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY} " +
                    "GROUP BY ${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY} " +
                    "ORDER BY mostRecent DESC"

            if(numSuggestions != null)
            {
                QUERY += " LIMIT $numSuggestions"
            }

            val cursor = DATABASE.rawQuery(QUERY, null)

            val suggestionList = mutableListOf<Pair<String,LocalDate>>()
            with(cursor) {
                while (moveToNext()) {
                    val pair = Pair<String,LocalDate>(
                        getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME)),
                        LocalDate.parse(
                            getString(getColumnIndexOrThrow("mostRecent")),
                            DateTimeFormatter.BASIC_ISO_DATE))
                    suggestionList.add(pair)
                }
            }
            cursor.close()
            return suggestionList
        }

        // Get the n most reoccurring/used suggestions by the user (if no value passed for n, gives all)
        fun getNMostUsedSuggestions(numSuggestions: Long? = null): List<String>{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            var QUERY = "SELECT ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} FROM " +
                    "${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} " +
                    "ORDER BY ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES} DESC"

            if(numSuggestions != null)
            {
                QUERY += " LIMIT $numSuggestions"
            }

            val cursor = DATABASE.rawQuery(QUERY, null)

            val suggestionList = mutableListOf<String>()
            with(cursor) {
                while (moveToNext()) {
                    suggestionList.add(getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME)))
                }
            }
            cursor.close()
            return suggestionList
        }

        // Get n past suggestions by the user in alphabetical order (if no value passed for n, gives all)
        fun getNPastSuggestionsAlphabetical(numSuggestions: Long? = null): List<String>{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            // Get all if no limit given
            var QUERY = "SELECT ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} FROM " +
                    "${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} " +
                    "ORDER BY ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} COLLATE NOCASE ASC"

            // Get only n highest alphabetically words
            if(numSuggestions != null)
            {
                QUERY += " LIMIT $numSuggestions"
            }

            val cursor = DATABASE.rawQuery(QUERY, null)

            val suggestionList = mutableListOf<String>()
            with(cursor) {
                while (moveToNext()) {
                    suggestionList.add(getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME)))
                }
            }
            cursor.close()
            return suggestionList
        }

        // Get n random past suggestions (if no value passed for n, gives all)
        fun getNPastSuggestions(numSuggestions: Long? = null): List<String>{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            // Get all if no limit given
            var QUERY = "SELECT ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} FROM " +
                    SuggestionHistoryContract.SuggestionEntry.TABLE_NAME

            // Get n random if limit given
            if(numSuggestions != null)
            {
                QUERY = "SELECT ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} FROM " +
                        "${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} " +
                        "ORDER BY RANDOM() LIMIT $numSuggestions"
            }

            val cursor = DATABASE.rawQuery(QUERY, null)

            val suggestionList = mutableListOf<String>()
            with(cursor) {
                while (moveToNext()) {
                    suggestionList.add(getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME)))
                }
            }
            cursor.close()
            return suggestionList
        }

        // Get n contexts (sentence that lead to the suggestion + date the suggestion was given) for a given suggestion
        // (if n not provided, gives all contexts for that suggestion)
        fun getContextForSuggestion(suggestion: String, numContexts: Long? = null): List<SuggestionContext>{
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            val numContextsString = if(numContexts == null) "" else "LIMIT $numContexts"

            val QUERY = "SELECT ${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_SENTENCE}, " +
                    "${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_DATE} FROM " +
                    "${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} JOIN ${SuggestionHistoryContract.SuggestionContextsEntry.TABLE_NAME} " +
                    "ON ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME}.${BaseColumns._ID} = ${SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY} " +
                    "WHERE ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} = ? " + numContextsString

            val cursor = DATABASE.rawQuery(QUERY, arrayOf(suggestion))

            val suggestionContexts = mutableListOf<SuggestionContext>()
            with(cursor) {
                while (moveToNext()) {
                    val contextString = getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_SENTENCE))

                    val contextDate = LocalDate.parse(
                        getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_DATE)),
                        DateTimeFormatter.BASIC_ISO_DATE)

                    suggestionContexts.add(SuggestionContext(contextString,contextDate))
                }
            }
            cursor.close()

            return suggestionContexts
        }

        // Add a suggestion + it's context (sentence that lead to it + date of conversation) to the database
        fun addSuggestion(suggestion:String, suggestionContext: SuggestionContext){
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }

            val trimmedSuggestion = suggestion.trim()
            val cleanSuggestion = Regex("\\s+").replace( trimmedSuggestion, " ")

            val trimmedSuggestionContext = suggestionContext.contextString.trim()
            val cleanSuggestionContext = Regex("\\s+").replace( trimmedSuggestionContext, " ")

            try {
                DATABASE.beginTransaction()
                val ADD_OR_UPDATE_SUGGESTION = "INSERT INTO ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} (" +
                        "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME}, " +
                        "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES}) " +
                        "VALUES ('${cleanSuggestion}', 1) " +
                        "ON CONFLICT(${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME}) " +
                        "DO UPDATE SET ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES} = " +
                        "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES} + 1"

                DATABASE.execSQL(ADD_OR_UPDATE_SUGGESTION)

                val itemID = getIDofWord(cleanSuggestion)

                val values = ContentValues().apply {
                    put(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY, itemID)
                    put(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_SENTENCE, cleanSuggestionContext)
                    put(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_DATE, suggestionContext.dateSuggested.format(
                        DateTimeFormatter.BASIC_ISO_DATE))
                }

                // Insert the new row, returning the primary key value of the new row
                val newRowId = DATABASE.insert(SuggestionHistoryContract.SuggestionContextsEntry.TABLE_NAME, null, values)

                DATABASE.setTransactionSuccessful()
            } catch(e: SQLiteException){
                e.printStackTrace()
            } finally {
                DATABASE.endTransaction()
            }

        }

        fun deleteAllEntries(){
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }
            try {
                DATABASE.beginTransaction()
                val DELETE_ALL_CONTEXTS = "DELETE FROM ${SuggestionHistoryContract.SuggestionContextsEntry.TABLE_NAME}"
                DATABASE.execSQL(DELETE_ALL_CONTEXTS)

                val DELETE_ALL_SUGGESTIONS = "DELETE FROM ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME}"
                DATABASE.execSQL(DELETE_ALL_SUGGESTIONS)
                DATABASE.setTransactionSuccessful()

            } catch(e: SQLiteException){
                e.printStackTrace()
            } finally {
                DATABASE.endTransaction()
            }
        }

        fun loadDemoSuggestions(){
            deleteAllEntries()
            addSuggestion("weekend",SuggestionContext("How was your", LocalDate.now().minusDays(1)))
            addSuggestion("book",SuggestionContext("I just got a new", LocalDate.now().minusDays(1)))
            addSuggestion("tonight",SuggestionContext("What are your plans for", LocalDate.now().minusDays(2)))
            addSuggestion("weather",SuggestionContext("I love this", LocalDate.now().minusDays(2)))
            addSuggestion("pets",SuggestionContext("Do you have any", LocalDate.now().minusDays(3)))
            addSuggestion("movie",SuggestionContext("Have you seen the latest", LocalDate.now().minusDays(4)))
            addSuggestion("recipe",SuggestionContext("I tried a new", LocalDate.now().minusDays(5)))
            addSuggestion("going",SuggestionContext("How's work", LocalDate.now().minusDays(5)))
            addSuggestion("guitar",SuggestionContext("I'm learning to play the", LocalDate.now().minusDays(7)))
            addSuggestion("restaurant",SuggestionContext("Can you recommend a good", LocalDate.now().minusDays(8)))
            addSuggestion("soon",SuggestionContext("I need a vacation", LocalDate.now().minusDays(10)))
            addSuggestion("hobby",SuggestionContext("What's your favorite", LocalDate.now().minusDays(10)))
            addSuggestion("dog",SuggestionContext("I'm thinking of adopting a", LocalDate.now().minusDays(10)))
            addSuggestion("abroad",SuggestionContext("Have you ever traveled", LocalDate.now().minusDays(15)))
            addSuggestion("dinner",SuggestionContext("What did you eat for", LocalDate.now().minusDays(14)))
            addSuggestion("baseball",SuggestionContext("My favourite sport is", LocalDate.now().minusDays(7)))
            addSuggestion("weekend",SuggestionContext("What would you like to do this", LocalDate.now().minusDays(13)))
        }
    }

    // Create the tables
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_CONTEXTS)
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    // Called on upgrading the database, we just delete and recreate
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_CONTEXTS)
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    // Called on downgrading database, basically just an update but other way
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}
