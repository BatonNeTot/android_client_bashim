package com.notjuststudio.bashim.helper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import com.notjuststudio.bashim.common.Quote
import android.database.DatabaseUtils
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase.CONFLICT_NONE
import android.util.Log
import java.util.concurrent.locks.ReentrantLock


class DataBaseHelper(context: Context) {

    companion object {

        private val DB_VERSION = 1
        private val DB_NAME = "quote_db"
        private val DB_FAVORITE_TABLE = "favorite_tab"
        private val DB_QUOTE_TABLE = "quote_tab"

        val COLUMN_QUOTE_ID = "q_id"
        val COLUMN_QUOTE_FAV = "fav"
        val COLUMN_QUOTE_DATE = "date"
        val COLUMN_QUOTE_TEXT = "txt"

        private val DB_CREATE_FAVOTIRE = "create table `${DB_FAVORITE_TABLE}` (`${COLUMN_QUOTE_ID}` INTEGER NOT NULL, `${COLUMN_QUOTE_FAV}` BOOLEAN, PRIMARY KEY(`${COLUMN_QUOTE_ID}`));"
        private val DB_CREATE_QUOTE = "create table `${DB_QUOTE_TABLE}` (`${COLUMN_QUOTE_ID}` INTEGER NOT NULL, `${COLUMN_QUOTE_DATE}` TEXT NOT NULL, `${COLUMN_QUOTE_TEXT}` TEXT NOT NULL, PRIMARY KEY(`${COLUMN_QUOTE_ID}`));"

    }

    private var dbHelper: DBHelper = DBHelper(context, DB_NAME, null, DB_VERSION)
    private var lock: ReentrantLock = ReentrantLock()

    fun addQuotes(quotes: List<Quote>): Int {
        lock.lock()
        try {
            val db = dbHelper.writableDatabase

            var count = 0

            val cv = ContentValues()
            for (quote in quotes) {
                cv.put(COLUMN_QUOTE_ID, quote.id?.toInt())
                cv.put(COLUMN_QUOTE_DATE, quote.date)
                cv.put(COLUMN_QUOTE_TEXT, quote.text)

                try {
                    db.insertWithOnConflict(DB_QUOTE_TABLE, null, cv, CONFLICT_NONE)
                    count++
                } catch (e: SQLException) {}

            }

            dbHelper.close()

            return count
        } finally {
            lock.unlock()
        }
    }

    fun getQuotes(count: Int): List<Quote> {
        lock.lock()
        try {
            val db = dbHelper.readableDatabase

            val debugC = db.query(DB_QUOTE_TABLE, null, null, null, null, null, null)

            var counter = 0

            if (debugC.moveToFirst()) {
                do {
                    counter++
                } while (debugC.moveToNext())
            }

            debugC.close()

            val c = db.rawQuery(
                    "SELECT ${DB_QUOTE_TABLE}.${COLUMN_QUOTE_ID} as `id`, ${DB_QUOTE_TABLE}.${COLUMN_QUOTE_DATE} as `date`, ${DB_QUOTE_TABLE}.${COLUMN_QUOTE_TEXT} as `text`, ${DB_FAVORITE_TABLE}.${COLUMN_QUOTE_FAV} as `isFav` " +
                            "FROM ${DB_QUOTE_TABLE} LEFT JOIN ${DB_FAVORITE_TABLE} ON ${DB_QUOTE_TABLE}.${COLUMN_QUOTE_ID}=${DB_FAVORITE_TABLE}.${COLUMN_QUOTE_ID} " +
                            "ORDER BY RANDOM() LIMIT $count", emptyArray())

            val result = mutableListOf<Quote>()
            if (c != null && c.moveToFirst()) {
                do {
                    result.add(Quote(c.getString(0), null, c.getString(1), c.getString(2), !c.isNull(3)))
                } while (c.moveToNext())
            }

            c.close()
            return result
        } finally {
            lock.unlock()
        }
    }

    fun countQuotes(): Int {
        lock.lock()
        try {
            val db = dbHelper.readableDatabase

            return DatabaseUtils.queryNumEntries(db, DB_QUOTE_TABLE).toInt()
        } finally {
            lock.unlock()
        }
    }

    fun clearQuotes() {
        lock.lock()
        try {
            val db = dbHelper.writableDatabase

            db.delete(DB_QUOTE_TABLE, null, null)
        } finally {
            lock.unlock()
        }
    }

    fun addFavorite(id: Int) {
        lock.lock()
        try {
            val db = dbHelper.writableDatabase

            val cv = ContentValues()

            cv.put(COLUMN_QUOTE_ID, id)
            cv.put(COLUMN_QUOTE_FAV, true)

            db.insert(DB_FAVORITE_TABLE, null, cv)
        } finally {
            lock.unlock()
        }
    }

    fun removeFavorite(id: Int) {
        lock.lock()
        try {
            val db = dbHelper.writableDatabase

            db.delete(DB_FAVORITE_TABLE, "${COLUMN_QUOTE_ID} = $id", null)
        } finally {
            lock.unlock()
        }
    }

    fun isFavorite(id: Int): Boolean {
        lock.lock()
        try {
            val db = dbHelper.readableDatabase

            val c = db.query(DB_FAVORITE_TABLE, null, "${COLUMN_QUOTE_ID} = $id", null, null, null, null)
            if (c.moveToFirst()) {
                c.close()
                return true
            } else {
                c.close()
                return false
            }
        } finally {
            lock.unlock()
        }
    }

    fun getFavorites(count: Int, offset: Int = 0): List<String> {
        lock.lock()
        try {
            val db = dbHelper.readableDatabase

            val c = db.query(DB_FAVORITE_TABLE, null, null, null, null, null, COLUMN_QUOTE_ID, "$offset,$count")
//            val c = db.rawQuery(
//                    "SELECT * FROM $DB_FAVORITE_TABLE ORDER BY $COLUMN_QUOTE_ID LIMIT $count OFFSET $offset;", emptyArray())

            val result = mutableListOf<String>()
            if (c != null && c.moveToFirst()) {
                do {
                    result.add(c.getString(0))
                } while (c.moveToNext())
            }

            c.close()
            return result
        } finally {
            lock.unlock()
        }
    }

    fun countFavorites() : Int {
        lock.lock()
        try {
            val db = dbHelper.readableDatabase

            return DatabaseUtils.queryNumEntries(db, DB_FAVORITE_TABLE).toInt()
        } finally {
            lock.unlock()
        }
    }

    fun clearFavorites() {
        lock.lock()
        try {
            val db = dbHelper.writableDatabase

            db.delete(DB_FAVORITE_TABLE, null, null)
        } finally {
            lock.unlock()
        }
    }

    private class DBHelper(context: Context, name: String, factory: CursorFactory?,
                           version: Int) : SQLiteOpenHelper(context, name, factory, version) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DB_CREATE_FAVOTIRE)
            db.execSQL(DB_CREATE_QUOTE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

}