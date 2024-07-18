package com.tonguetip

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SuggestionContext(
    val contextString: String,
    val dateSuggested: LocalDate
)
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


class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        // Constant values for database handler
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "SuggestionHistory.db"

        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER ," +
                "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME} TEXT PRIMARY KEY," +
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

        fun closeDatabase(){
            if(::DATABASE.isInitialized && DATABASE.isOpen){
                DATABASE.close()
            }
        }
        fun initDataBaseInst(ctx: Context): SQLiteOpenHelper{
            DBINSTANCE = DatabaseHandler(ctx)
            return DBINSTANCE
        }

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
                            getString(getColumnIndexOrThrow(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_DATE)),
                            DateTimeFormatter.BASIC_ISO_DATE))
                    suggestionList.add(pair)
                }
            }
            cursor.close()
            return suggestionList
        }
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

        fun addSuggestion(suggestion:String, suggestionContext: SuggestionContext){
            if(!::DATABASE.isInitialized || !DATABASE.isOpen){
                DATABASE = DBINSTANCE.writableDatabase
            }


            try {
                DATABASE.beginTransaction()
                val ADD_OR_UPDATE_SUGGESTION = "INSERT INTO ${SuggestionHistoryContract.SuggestionEntry.TABLE_NAME} (" +
                        "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME}, " +
                        "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES}) " +
                        "VALUES ('${suggestion}', 1) " +
                        "ON CONFLICT(${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_NAME}) " +
                        "DO UPDATE SET ${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES} = " +
                        "${SuggestionHistoryContract.SuggestionEntry.COLUMN_SUGGESTION_OCCURRENCES} + 1"

                DATABASE.execSQL(ADD_OR_UPDATE_SUGGESTION)

                val itemID = getIDofWord(suggestion)

                val values = ContentValues().apply {
                    put(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUG_KEY, itemID)
                    put(SuggestionHistoryContract.SuggestionContextsEntry.COLUMN_SUGGESTION_SENTENCE, suggestionContext.contextString)
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
    }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_CONTEXTS)
        db.execSQL(SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_CONTEXTS)
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}
