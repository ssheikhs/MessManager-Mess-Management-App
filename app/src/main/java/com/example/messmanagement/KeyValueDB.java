package com.example.messmanagement;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KeyValueDB extends SQLiteOpenHelper {

	// TABLE INFORMATION
	static final String DB_NAME = "KEY_VALUE.DB";
	public final String TABLE_KEY_VALUE = "key_value_pairs";
	public final String KEY = "keyname";
	public final String VALUE = "itemvalue";

	public KeyValueDB(Context context) {
		super(context, DB_NAME, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		System.out.println("DB@OnCreate");
		createKeyValueTable(db);
	}

	private void createKeyValueTable(SQLiteDatabase db) {
		try {
			db.execSQL("CREATE TABLE IF NOT EXISTS key_value_pairs (" +
					"keyname TEXT PRIMARY KEY, " +
					"itemvalue TEXT)");
		} catch (Exception e) {
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// schema changes if needed later
	}

	private void handleError(SQLiteDatabase db, Exception e) {
		String errorMsg = e.getMessage() != null ? e.getMessage() : "";
		if (errorMsg.contains("no such table")) {
			if (errorMsg.contains(TABLE_KEY_VALUE)) {
				createKeyValueTable(db);
			}
		}
	}

	public Cursor execute(String query) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor res = null;
		try {
			res = db.rawQuery(query, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	// 🔴 UPDATED: now does UPSERT (insert or replace), so UNIQUE constraint errors are gone
	public Boolean insertKeyValue(String key, String value) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(KEY, key);
		cv.put(VALUE, value);
		try {
			db.insertWithOnConflict(
					TABLE_KEY_VALUE,
					null,
					cv,
					SQLiteDatabase.CONFLICT_REPLACE   // <── key part
			);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			// in case table was missing, try to recover once
			handleError(db, e);
			try {
				db.insertWithOnConflict(
						TABLE_KEY_VALUE,
						null,
						cv,
						SQLiteDatabase.CONFLICT_REPLACE
				);
				return true;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return false;
	}

	public boolean updateValueByKey(String key, String value) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(KEY, key);
		cv.put(VALUE, value);
		try {
			db.update(TABLE_KEY_VALUE, cv, KEY + "=?", new String[]{key});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public Integer deleteDataByKey(String key) {
		SQLiteDatabase db = this.getWritableDatabase();
		int isDeleted = 0;
		try {
			isDeleted = db.delete(TABLE_KEY_VALUE, KEY + " = ?", new String[]{key});
		} catch (Exception e) {
			handleError(db, e);
			try {
				isDeleted = db.delete(TABLE_KEY_VALUE, KEY + " = ?", new String[]{key});
			} catch (Exception ex) {
			}
		}
		return isDeleted;
	}

	public String getValueByKey(String key) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor res;
		try {
			res = db.rawQuery(
					"SELECT * FROM " + TABLE_KEY_VALUE + " WHERE " + KEY + "=?",
					new String[]{key}
			);
		} catch (Exception e) {
			handleError(db, e);
			res = db.rawQuery(
					"SELECT * FROM " + TABLE_KEY_VALUE + " WHERE " + KEY + "=?",
					new String[]{key}
			);
		}
		if (res != null && res.getCount() > 0) {
			res.moveToNext();
			String v = res.getString(1);
			res.close();
			return v;
		}
		if (res != null) res.close();
		return null;
	}

	// Convenience: insert if not exists, otherwise update
	public void insertOrUpdate(String key, String value) {
		String existing = getValueByKey(key);
		if (existing == null) {
			insertKeyValue(key, value);   // now does upsert anyway
		} else {
			updateValueByKey(key, value);
		}
	}

	public void deleteQuery(String query) {
		SQLiteDatabase db = this.getWritableDatabase();
		try {
			db.execSQL(query);
		} catch (Exception e) {
			handleError(db, e);
			db.execSQL(query);
		}
	}
}