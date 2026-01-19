package com.example.messmanagement;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MessDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "mess_management.db";
    private static final int DB_VERSION = 8; // ✅ v8: add meal last_changed_type/val + price history method support

    // -------------------------------------------------------------------------
    // MEMBERS table
    // -------------------------------------------------------------------------
    public static final String TABLE_MEMBERS = "members";
    public static final String COL_MEMBER_ID = "id";
    public static final String COL_MEMBER_NAME = "name";              // email/username
    public static final String COL_MEMBER_ROLE = "role";              // "admin" / "member"
    public static final String COL_MEMBER_CONTACT = "contact";
    public static final String COL_MEMBER_ADDRESS = "address";
    public static final String COL_MEMBER_PARENT_CONTACT = "parent_contact";

    // -------------------------------------------------------------------------
    // USERS table (login)
    // -------------------------------------------------------------------------
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "user_id";               // Firebase UID
    public static final String COL_USER_FULL_NAME = "full_name";
    public static final String COL_USER_USERNAME = "username";        // email
    public static final String COL_USER_PASSWORD = "password";        // local offline only
    public static final String COL_USER_CONTACT = "contact";
    public static final String COL_USER_ADDRESS = "address";
    public static final String COL_USER_PARENT_CONTACT = "parent_contact";
    public static final String COL_USER_IS_ADMIN = "is_admin";        // 1 admin, 0 member
    public static final String COL_USER_STATUS = "status";            // "active"/"pending"/"deleted"

    // -------------------------------------------------------------------------
    // EXPENSES table
    // -------------------------------------------------------------------------
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COL_EXPENSE_ID = "id";                 // local autoinc id
    public static final String COL_EXPENSE_REMOTE_ID = "remote_id";   // Firestore doc id
    public static final String COL_EXPENSE_SYNC_STATE = "sync_state"; // 0=pending, 1=synced
    public static final String COL_EXPENSE_TITLE = "title";
    public static final String COL_EXPENSE_AMOUNT = "amount";
    public static final String COL_EXPENSE_CATEGORY = "category";
    public static final String COL_EXPENSE_PAIDBY = "paid_by";
    public static final String COL_EXPENSE_DATE = "expense_date";     // yyyy-MM-dd

    public static final String CATEGORY_PAYMENT = "PAYMENT";

    // -------------------------------------------------------------------------
    // MEALS_DAILY table
    // -------------------------------------------------------------------------
    public static final String TABLE_MEALS_DAILY = "meals_daily";
    public static final String COL_MEAL_MEMBER = "member_name";       // email
    public static final String COL_MEAL_DATE = "meal_date";           // yyyy-MM-dd
    public static final String COL_MEAL_BREAKFAST = "breakfast";
    public static final String COL_MEAL_LUNCH = "lunch";
    public static final String COL_MEAL_DINNER = "dinner";
    public static final String COL_MEAL_SYNC_STATE = "sync_state";    // 0=pending, 1=synced

    // ✅ for notification trigger (FirestoreSyncWorker)
    public static final String COL_MEAL_LAST_CHANGED_TYPE = "last_changed_type"; // Breakfast/Lunch/Dinner
    public static final String COL_MEAL_LAST_CHANGED_VAL  = "last_changed_val";  // 1 or 0

    // -------------------------------------------------------------------------
    // MEAL_PRICES table
    // -------------------------------------------------------------------------
    public static final String TABLE_MEAL_PRICES = "meal_prices";
    public static final String COL_PRICE_ID = "id";
    public static final String COL_PRICE_BREAKFAST = "breakfast_price";
    public static final String COL_PRICE_LUNCH = "lunch_price";
    public static final String COL_PRICE_DINNER = "dinner_price";
    public static final String COL_PRICE_DATE = "price_date";

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    public MessDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // -------------------------------------------------------------------------
    // onCreate / onUpgrade
    // -------------------------------------------------------------------------
    @Override
    public void onCreate(SQLiteDatabase db) {

        // MEMBERS
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MEMBERS + " (" +
                COL_MEMBER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MEMBER_NAME + " TEXT UNIQUE, " +
                COL_MEMBER_ROLE + " TEXT, " +
                COL_MEMBER_CONTACT + " TEXT, " +
                COL_MEMBER_ADDRESS + " TEXT, " +
                COL_MEMBER_PARENT_CONTACT + " TEXT)");

        // USERS
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " +
                COL_USER_FULL_NAME + " TEXT, " +
                COL_USER_USERNAME + " TEXT UNIQUE, " +
                COL_USER_PASSWORD + " TEXT, " +
                COL_USER_CONTACT + " TEXT, " +
                COL_USER_ADDRESS + " TEXT, " +
                COL_USER_PARENT_CONTACT + " TEXT, " +
                COL_USER_IS_ADMIN + " INTEGER, " +
                COL_USER_STATUS + " TEXT)");

        // EXPENSES
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXPENSES + " (" +
                COL_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EXPENSE_REMOTE_ID + " TEXT, " +
                COL_EXPENSE_SYNC_STATE + " INTEGER DEFAULT 1, " +
                COL_EXPENSE_TITLE + " TEXT, " +
                COL_EXPENSE_AMOUNT + " REAL, " +
                COL_EXPENSE_CATEGORY + " TEXT, " +
                COL_EXPENSE_PAIDBY + " TEXT, " +
                COL_EXPENSE_DATE + " TEXT)");

        // MEALS_DAILY (✅ includes sync_state + last_changed fields)
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MEALS_DAILY + " (" +
                COL_MEAL_MEMBER + " TEXT NOT NULL, " +
                COL_MEAL_DATE + " TEXT NOT NULL, " +
                COL_MEAL_BREAKFAST + " INTEGER DEFAULT 0, " +
                COL_MEAL_LUNCH + " INTEGER DEFAULT 0, " +
                COL_MEAL_DINNER + " INTEGER DEFAULT 0, " +
                COL_MEAL_SYNC_STATE + " INTEGER DEFAULT 1, " +
                COL_MEAL_LAST_CHANGED_TYPE + " TEXT, " +
                COL_MEAL_LAST_CHANGED_VAL + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY(" + COL_MEAL_MEMBER + ", " + COL_MEAL_DATE + "))");

        // MEAL_PRICES
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MEAL_PRICES + " (" +
                COL_PRICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PRICE_BREAKFAST + " REAL DEFAULT 50.0, " +
                COL_PRICE_LUNCH + " REAL DEFAULT 150.0, " +
                COL_PRICE_DINNER + " REAL DEFAULT 150.0, " +
                COL_PRICE_DATE + " TEXT)");

        // default prices
        ContentValues defaultPrices = new ContentValues();
        defaultPrices.put(COL_PRICE_BREAKFAST, 50.0);
        defaultPrices.put(COL_PRICE_LUNCH, 150.0);
        defaultPrices.put(COL_PRICE_DINNER, 150.0);
        defaultPrices.put(COL_PRICE_DATE, "2024-01-01");
        db.insert(TABLE_MEAL_PRICES, null, defaultPrices);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MEAL_PRICES + " (" +
                    COL_PRICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_PRICE_BREAKFAST + " REAL DEFAULT 50.0, " +
                    COL_PRICE_LUNCH + " REAL DEFAULT 150.0, " +
                    COL_PRICE_DINNER + " REAL DEFAULT 150.0, " +
                    COL_PRICE_DATE + " TEXT)");

            ContentValues defaultPrices = new ContentValues();
            defaultPrices.put(COL_PRICE_BREAKFAST, 50.0);
            defaultPrices.put(COL_PRICE_LUNCH, 150.0);
            defaultPrices.put(COL_PRICE_DINNER, 150.0);
            defaultPrices.put(COL_PRICE_DATE, "2024-01-01");
            db.insert(TABLE_MEAL_PRICES, null, defaultPrices);
        }

        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    COL_USER_ID + " TEXT PRIMARY KEY, " +
                    COL_USER_FULL_NAME + " TEXT, " +
                    COL_USER_USERNAME + " TEXT UNIQUE, " +
                    COL_USER_PASSWORD + " TEXT, " +
                    COL_USER_CONTACT + " TEXT, " +
                    COL_USER_ADDRESS + " TEXT, " +
                    COL_USER_PARENT_CONTACT + " TEXT, " +
                    COL_USER_IS_ADMIN + " INTEGER, " +
                    COL_USER_STATUS + " TEXT)");
        }

        // v5: add expense remote_id + sync_state
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN " + COL_EXPENSE_REMOTE_ID + " TEXT"); }
            catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN " + COL_EXPENSE_SYNC_STATE + " INTEGER DEFAULT 1"); }
            catch (Exception ignored) {}

            ContentValues cv = new ContentValues();
            cv.put(COL_EXPENSE_SYNC_STATE, 1);
            db.update(TABLE_EXPENSES, cv, null, null);
        }

        // v6: add meals sync_state
        if (oldVersion < 6) {
            try { db.execSQL("ALTER TABLE " + TABLE_MEALS_DAILY + " ADD COLUMN " + COL_MEAL_SYNC_STATE + " INTEGER DEFAULT 1"); }
            catch (Exception ignored) {}

            ContentValues cv = new ContentValues();
            cv.put(COL_MEAL_SYNC_STATE, 1);
            db.update(TABLE_MEALS_DAILY, cv, null, null);
        }

        // ✅ v8: add last_changed_type + last_changed_val
        if (oldVersion < 8) {
            try { db.execSQL("ALTER TABLE " + TABLE_MEALS_DAILY + " ADD COLUMN " + COL_MEAL_LAST_CHANGED_TYPE + " TEXT"); }
            catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_MEALS_DAILY + " ADD COLUMN " + COL_MEAL_LAST_CHANGED_VAL + " INTEGER DEFAULT 0"); }
            catch (Exception ignored) {}

            ContentValues cv = new ContentValues();
            cv.put(COL_MEAL_LAST_CHANGED_TYPE, (String) null);
            cv.put(COL_MEAL_LAST_CHANGED_VAL, 0);
            db.update(TABLE_MEALS_DAILY, cv, null, null);
        }
    }

    // =========================================================================
    // ✅ OFFLINE LOGIN HELPERS
    // =========================================================================

    public boolean checkLocalLoginByUsernamePassword(String username, String password) {
        if (username == null || password == null) return false;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? AND " + COL_USER_PASSWORD + "=? LIMIT 1",
                new String[]{username, password}
        );
        boolean ok = false;
        if (c != null) {
            ok = c.moveToFirst();
            c.close();
        }
        return ok;
    }

    public void updateLocalPasswordByUsername(String username, String password) {
        if (username == null || password == null) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_PASSWORD, password);
        db.update(TABLE_USERS, cv, COL_USER_USERNAME + "=?", new String[]{username});
    }

    // =========================================================================
    // ✅ USERS + MEMBERS SYNC
    // =========================================================================

    public void upsertUserFromRemote(String userId,
                                     String fullName,
                                     String email,
                                     String contact,
                                     String address,
                                     String parentContact,
                                     boolean isAdmin,
                                     String status) {

        if (email == null) return;

        SQLiteDatabase db = this.getWritableDatabase();

        String existingPass = null;
        Cursor cp = db.rawQuery(
                "SELECT " + COL_USER_PASSWORD + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{email}
        );
        if (cp != null) {
            if (cp.moveToFirst() && !cp.isNull(0)) existingPass = cp.getString(0);
            cp.close();
        }

        ContentValues cvUser = new ContentValues();
        cvUser.put(COL_USER_ID, userId);
        cvUser.put(COL_USER_FULL_NAME, fullName);
        cvUser.put(COL_USER_USERNAME, email);
        cvUser.put(COL_USER_PASSWORD, existingPass != null ? existingPass : "");
        cvUser.put(COL_USER_CONTACT, contact);
        cvUser.put(COL_USER_ADDRESS, address);
        cvUser.put(COL_USER_PARENT_CONTACT, parentContact);
        cvUser.put(COL_USER_IS_ADMIN, isAdmin ? 1 : 0);
        cvUser.put(COL_USER_STATUS, status);

        Cursor c = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USER_USERNAME + " = ?",
                new String[]{email},
                null, null, null
        );

        try {
            if (c != null && c.moveToFirst()) {
                db.update(TABLE_USERS, cvUser, COL_USER_USERNAME + " = ?", new String[]{email});
            } else {
                db.insert(TABLE_USERS, null, cvUser);
            }
        } finally {
            if (c != null) c.close();
        }

        if ("active".equalsIgnoreCase(status)) {
            addOrUpdateMember(email, isAdmin ? "admin" : "member", contact, address, parentContact);
        } else {
            db.delete(TABLE_MEMBERS, COL_MEMBER_NAME + " = ?", new String[]{email});
        }
    }

    public void upsertUserLocalKeepPassword(User user) {
        if (user == null || user.getUsername() == null) return;

        SQLiteDatabase db = getWritableDatabase();

        String existingPass = null;
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_PASSWORD + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{user.getUsername()}
        );
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) existingPass = c.getString(0);
            c.close();
        }

        ContentValues cv = new ContentValues();
        cv.put(COL_USER_ID, user.getUserId());
        cv.put(COL_USER_FULL_NAME, user.getFullName());
        cv.put(COL_USER_USERNAME, user.getUsername());

        String newPass = user.getPassword();
        if (newPass != null && !newPass.isEmpty()) {
            cv.put(COL_USER_PASSWORD, newPass);
        } else if (existingPass != null) {
            cv.put(COL_USER_PASSWORD, existingPass);
        } else {
            cv.put(COL_USER_PASSWORD, "");
        }

        cv.put(COL_USER_CONTACT, user.getContact());
        cv.put(COL_USER_ADDRESS, user.getAddress());
        cv.put(COL_USER_PARENT_CONTACT, user.getParentContact());
        cv.put(COL_USER_IS_ADMIN, user.isAdmin() ? 1 : 0);
        cv.put(COL_USER_STATUS, user.getStatus());

        Cursor cx = db.rawQuery(
                "SELECT 1 FROM " + TABLE_USERS + " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{user.getUsername()}
        );
        boolean exists = cx != null && cx.moveToFirst();
        if (cx != null) cx.close();

        if (exists) {
            db.update(TABLE_USERS, cv, COL_USER_USERNAME + "=?", new String[]{user.getUsername()});
        } else {
            db.insert(TABLE_USERS, null, cv);
        }
    }

    // -------------------------------------------------------------------------
    // USERS (local operations)
    // -------------------------------------------------------------------------

    public boolean checkUsernameExists(String username) {
        if (username == null || username.isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_USERS + " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );
        boolean exists = c != null && c.moveToFirst();
        if (c != null) c.close();
        return exists;
    }

    public boolean addUser(User user) {
        if (user == null || user.getUsername() == null) return false;
        upsertUserLocalKeepPassword(user);
        return true;
    }

    public boolean hasAdminAccount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_USERS + " WHERE " + COL_USER_IS_ADMIN + " = 1 LIMIT 1",
                null
        );
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return exists;
    }

    public boolean isUserAdmin(String userId) {
        if (userId == null || userId.isEmpty()) return false;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_USER_IS_ADMIN + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_ID + "=? LIMIT 1",
                new String[]{userId}
        );

        boolean isAdmin = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) isAdmin = (cursor.getInt(0) == 1);
            cursor.close();
        }
        return isAdmin;
    }

    public String getUserStatusByUsername(String username) {
        if (username == null || username.isEmpty()) return null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_STATUS + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );

        String status = null;
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) status = c.getString(0);
            c.close();
        }
        return status;
    }

    public String getUserRoleByUsername(String username) {
        if (username == null || username.isEmpty()) return null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_IS_ADMIN + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );

        String role = null;
        if (c != null) {
            if (c.moveToFirst()) role = (c.getInt(0) == 1) ? "admin" : "member";
            c.close();
        }
        return role;
    }

    public boolean deactivateUserByUsername(String username) {
        if (username == null || username.isEmpty()) return false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_STATUS, "deleted");
        int rows = db.update(TABLE_USERS, cv, COL_USER_USERNAME + "=?", new String[]{username});
        return rows > 0;
    }

    public boolean deactivateUserByFullName(String fullNameOrEmail) {
        if (fullNameOrEmail == null || fullNameOrEmail.isEmpty()) return false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_STATUS, "deleted");

        int rows = db.update(TABLE_USERS, cv,
                COL_USER_FULL_NAME + "=? OR " + COL_USER_USERNAME + "=?",
                new String[]{fullNameOrEmail, fullNameOrEmail});
        return rows > 0;
    }

    // -------------------------------------------------------------------------
    // MEMBERS
    // -------------------------------------------------------------------------

    public void addOrUpdateMember(String name, String role,
                                  String contact, String address, String parentContact) {
        if (name == null) return;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MEMBER_NAME, name);
        cv.put(COL_MEMBER_ROLE, role);
        cv.put(COL_MEMBER_CONTACT, contact);
        cv.put(COL_MEMBER_ADDRESS, address);
        cv.put(COL_MEMBER_PARENT_CONTACT, parentContact);

        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_MEMBERS + " WHERE " + COL_MEMBER_NAME + "=? LIMIT 1",
                new String[]{name}
        );
        boolean exists = c != null && c.moveToFirst();
        if (c != null) c.close();

        if (exists) {
            db.update(TABLE_MEMBERS, cv, COL_MEMBER_NAME + "=?", new String[]{name});
        } else {
            db.insert(TABLE_MEMBERS, null, cv);
        }
    }

    public long addMember(String name, String role,
                          String contact, String address, String parentContact) {
        addOrUpdateMember(name, role, contact, address, parentContact);
        return 1;
    }

    public List<Member> getAllMembers() {
        List<Member> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_MEMBERS + " ORDER BY " + COL_MEMBER_NAME, null);
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow(COL_MEMBER_ID));
            String name = c.getString(c.getColumnIndexOrThrow(COL_MEMBER_NAME));
            String role = c.getString(c.getColumnIndexOrThrow(COL_MEMBER_ROLE));
            list.add(new Member(id, name, role, 0, 0.0));
        }
        c.close();
        return list;
    }

    public int getMemberCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEMBERS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public Member getMemberByName(String name) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_MEMBERS + " WHERE " + COL_MEMBER_NAME + "=?",
                new String[]{name}
        );
        Member m = null;
        if (c.moveToFirst()) {
            int id = c.getInt(c.getColumnIndexOrThrow(COL_MEMBER_ID));
            String role = c.getString(c.getColumnIndexOrThrow(COL_MEMBER_ROLE));
            m = new Member(id, name, role, 0, 0.0);
        }
        c.close();
        return m;
    }

    public Member addOrGetMember(String name, String role) {
        Member existing = getMemberByName(name);
        if (existing != null) return existing;
        addOrUpdateMember(name, role, "", "", "");
        Member m = getMemberByName(name);
        return m != null ? m : new Member(0, name, role, 0, 0.0);
    }

    public void deleteMemberByName(String name) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MEMBERS, COL_MEMBER_NAME + " = ?", new String[]{name});
    }

    public void approveMemberByName(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MEMBER_ROLE, "member");
        db.update(TABLE_MEMBERS, cv, COL_MEMBER_NAME + " = ?", new String[]{name});
    }

    // -------------------------------------------------------------------------
    // EXPENSES
    // -------------------------------------------------------------------------

    public long addExpense(String title, double amount,
                           String category, String paidBy, String date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_TITLE, title);
        cv.put(COL_EXPENSE_AMOUNT, amount);
        cv.put(COL_EXPENSE_CATEGORY, category);
        cv.put(COL_EXPENSE_PAIDBY, paidBy);
        cv.put(COL_EXPENSE_DATE, date);

        cv.put(COL_EXPENSE_SYNC_STATE, 1);

        return db.insert(TABLE_EXPENSES, null, cv);
    }

    public long addExpensePending(String remoteId, String title, double amount,
                                  String category, String paidBy, String date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_REMOTE_ID, remoteId);
        cv.put(COL_EXPENSE_SYNC_STATE, 0); // pending
        cv.put(COL_EXPENSE_TITLE, title);
        cv.put(COL_EXPENSE_AMOUNT, amount);
        cv.put(COL_EXPENSE_CATEGORY, category);
        cv.put(COL_EXPENSE_PAIDBY, paidBy);
        cv.put(COL_EXPENSE_DATE, date);
        return db.insert(TABLE_EXPENSES, null, cv);
    }

    public void markExpenseSynced(String remoteId) {
        if (remoteId == null) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_SYNC_STATE, 1);
        db.update(TABLE_EXPENSES, cv, COL_EXPENSE_REMOTE_ID + "=?", new String[]{remoteId});
    }

    public void clearSyncedExpensesForMonth(String monthPrefix) {
        if (monthPrefix == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXPENSES,
                COL_EXPENSE_SYNC_STATE + "=1 AND " + COL_EXPENSE_DATE + " LIKE ?",
                new String[]{monthPrefix + "%"});
    }

    public List<PendingExpense> getPendingExpenses() {
        List<PendingExpense> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COL_EXPENSE_REMOTE_ID + "," +
                        COL_EXPENSE_TITLE + "," +
                        COL_EXPENSE_AMOUNT + "," +
                        COL_EXPENSE_CATEGORY + "," +
                        COL_EXPENSE_PAIDBY + "," +
                        COL_EXPENSE_DATE +
                        " FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_SYNC_STATE + "=0" +
                        " AND " + COL_EXPENSE_REMOTE_ID + " IS NOT NULL",
                null
        );

        while (c.moveToNext()) {
            PendingExpense p = new PendingExpense();
            p.remoteId = c.getString(0);
            p.title = c.getString(1);
            p.amount = c.getDouble(2);
            p.category = c.getString(3);
            p.paidBy = c.getString(4);
            p.date = c.getString(5);
            list.add(p);
        }
        c.close();
        return list;
    }

    public List<Expense> getExpensesByMember(String memberName) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_PAIDBY + "=? ORDER BY " + COL_EXPENSE_DATE + " DESC",
                new String[]{memberName}
        );

        while (c.moveToNext()) {
            Expense e = new Expense();
            e.id = c.getLong(c.getColumnIndexOrThrow(COL_EXPENSE_ID));
            e.title = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_TITLE));
            e.amount = c.getDouble(c.getColumnIndexOrThrow(COL_EXPENSE_AMOUNT));
            e.category = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_CATEGORY));
            e.paidBy = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_PAIDBY));
            e.date = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_DATE));
            list.add(e);
        }
        c.close();
        return list;
    }

    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_EXPENSES + " ORDER BY " + COL_EXPENSE_DATE + " DESC",
                null
        );

        while (c.moveToNext()) {
            Expense e = new Expense();
            e.id = c.getLong(c.getColumnIndexOrThrow(COL_EXPENSE_ID));
            e.title = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_TITLE));
            e.amount = c.getDouble(c.getColumnIndexOrThrow(COL_EXPENSE_AMOUNT));
            e.category = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_CATEGORY));
            e.paidBy = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_PAIDBY));
            e.date = c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_DATE));
            list.add(e);
        }
        c.close();
        return list;
    }

    public double getMonthlyPaidByMember(String memberName, String monthPrefix) {
        double sum = 0;
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_PAIDBY + "=? AND " +
                        COL_EXPENSE_CATEGORY + "=? AND " +
                        COL_EXPENSE_DATE + " LIKE ?",
                new String[]{memberName, CATEGORY_PAYMENT, monthPrefix + "%"}
        );

        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) sum = c.getDouble(0);
            c.close();
        }
        return sum;
    }

    public double getMonthlyTotalPaidAllMembers(String monthPrefix) {
        double sum = 0;
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_CATEGORY + "=? AND " + COL_EXPENSE_DATE + " LIKE ?",
                new String[]{CATEGORY_PAYMENT, monthPrefix + "%"}
        );

        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) sum = c.getDouble(0);
            c.close();
        }
        return sum;
    }

    public long addPayment(String memberName, double amount, String date) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_TITLE, "Payment");
        cv.put(COL_EXPENSE_AMOUNT, amount);
        cv.put(COL_EXPENSE_CATEGORY, CATEGORY_PAYMENT);
        cv.put(COL_EXPENSE_PAIDBY, memberName);
        cv.put(COL_EXPENSE_DATE, date);
        cv.put(COL_EXPENSE_SYNC_STATE, 1);

        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE_EXPENSES, null, cv);
    }

    // -------------------------------------------------------------------------
    // MEALS
    // -------------------------------------------------------------------------

    public void ensureMealsDailyRow(String member, String date) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + "=?",
                new String[]{member, date}
        );
        boolean exists = c != null && c.moveToFirst();
        if (c != null) c.close();

        if (!exists) {
            ContentValues cv = new ContentValues();
            cv.put(COL_MEAL_MEMBER, member);
            cv.put(COL_MEAL_DATE, date);
            cv.put(COL_MEAL_BREAKFAST, 0);
            cv.put(COL_MEAL_LUNCH, 0);
            cv.put(COL_MEAL_DINNER, 0);
            cv.put(COL_MEAL_SYNC_STATE, 0); // new row = pending

            // ✅ new row: no trigger by default
            cv.put(COL_MEAL_LAST_CHANGED_TYPE, (String) null);
            cv.put(COL_MEAL_LAST_CHANGED_VAL, 0);

            db.insert(TABLE_MEALS_DAILY, null, cv);
        }
    }

    public void setMealForDate(String member, String date, String meal, int value) {
        ensureMealsDailyRow(member, date);

        SQLiteDatabase db = getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COL_MEAL_BREAKFAST + ", " + COL_MEAL_LUNCH + ", " + COL_MEAL_DINNER +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + "=?",
                new String[]{member, date}
        );

        int b = 0, l = 0, d = 0;
        if (c != null && c.moveToFirst()) {
            b = c.getInt(0);
            l = c.getInt(1);
            d = c.getInt(2);
        }
        if (c != null) c.close();

        // keep your original "do nothing" logic
        if ("Breakfast".equalsIgnoreCase(meal)) {
            if (b == 1 && value == 0) return;
            b = value;
        } else if ("Lunch".equalsIgnoreCase(meal)) {
            if (l == 1 && value == 0) return;
            l = value;
        } else {
            if (d == 1 && value == 0) return;
            d = value;
        }

        ContentValues cv = new ContentValues();
        cv.put(COL_MEAL_BREAKFAST, b);
        cv.put(COL_MEAL_LUNCH, l);
        cv.put(COL_MEAL_DINNER, d);
        cv.put(COL_MEAL_SYNC_STATE, 0); // pending on change

        // ✅ store last change so worker can notify once
        cv.put(COL_MEAL_LAST_CHANGED_TYPE, meal);
        cv.put(COL_MEAL_LAST_CHANGED_VAL, value);

        db.update(TABLE_MEALS_DAILY, cv,
                COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + "=?",
                new String[]{member, date});
    }

    private double[] getMealPricesForDate(String date) {
        double[] prices = new double[]{50.0, 150.0, 150.0};
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COL_PRICE_BREAKFAST + ", " + COL_PRICE_LUNCH + ", " + COL_PRICE_DINNER +
                        " FROM " + TABLE_MEAL_PRICES +
                        " WHERE " + COL_PRICE_DATE + " <= ?" +
                        " ORDER BY " + COL_PRICE_DATE + " DESC LIMIT 1",
                new String[]{date}
        );

        if (c != null) {
            if (c.moveToFirst()) {
                prices[0] = c.getDouble(0);
                prices[1] = c.getDouble(1);
                prices[2] = c.getDouble(2);
            }
            c.close();
        }
        return prices;
    }


    public int[] getMealsForDate(String member, String date) {
        int[] result = new int[]{0, 0, 0};
        if (member == null || member.trim().isEmpty() || date == null) return result;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_MEAL_BREAKFAST + ", " + COL_MEAL_LUNCH + ", " + COL_MEAL_DINNER +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + "=?",
                new String[]{member, date}
        );

        if (c != null) {
            if (c.moveToFirst()) {
                result[0] = c.getInt(0);
                result[1] = c.getInt(1);
                result[2] = c.getInt(2);
            }
            c.close();
        }
        return result;
    }

    public int[] getMonthlyMealCountsForMember(String member, String monthPrefix) {
        int[] counts = new int[3];
        if (member == null || member.trim().isEmpty()) return counts;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_MEAL_BREAKFAST + "), SUM(" + COL_MEAL_LUNCH + "), SUM(" + COL_MEAL_DINNER + ")" +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + " LIKE ?",
                new String[]{member, monthPrefix + "%"}
        );

        if (c != null) {
            if (c.moveToFirst()) {
                counts[0] = c.isNull(0) ? 0 : c.getInt(0);
                counts[1] = c.isNull(1) ? 0 : c.getInt(1);
                counts[2] = c.isNull(2) ? 0 : c.getInt(2);
            }
            c.close();
        }
        return counts;
    }

    public int getMonthlyTotalMealsAllMembers(String monthPrefix) {
        int total = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_MEAL_BREAKFAST + " + " + COL_MEAL_LUNCH + " + " + COL_MEAL_DINNER + ")" +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_DATE + " LIKE ?",
                new String[]{monthPrefix + "%"}
        );

        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) total = c.getInt(0);
            c.close();
        }
        return total;
    }

    public void upsertMealsFromRemote(String member, String date, int breakfast, int lunch, int dinner) {
        if (member == null || member.trim().isEmpty() || date == null) return;

        SQLiteDatabase db = getWritableDatabase();
        ensureMealsDailyRow(member, date);

        ContentValues cv = new ContentValues();
        cv.put(COL_MEAL_BREAKFAST, breakfast);
        cv.put(COL_MEAL_LUNCH, lunch);
        cv.put(COL_MEAL_DINNER, dinner);
        cv.put(COL_MEAL_SYNC_STATE, 1);

        // ✅ remote updates should not trigger local notification
        cv.put(COL_MEAL_LAST_CHANGED_TYPE, (String) null);
        cv.put(COL_MEAL_LAST_CHANGED_VAL, 0);

        db.update(TABLE_MEALS_DAILY, cv,
                COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + "=?",
                new String[]{member, date});
    }

    // ✅ REQUIRED BY FirestoreSyncWorker
    public List<PendingMeal> getPendingMeals() {
        List<PendingMeal> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COL_MEAL_MEMBER + "," +
                        COL_MEAL_DATE + "," +
                        COL_MEAL_BREAKFAST + "," +
                        COL_MEAL_LUNCH + "," +
                        COL_MEAL_DINNER + "," +
                        COL_MEAL_LAST_CHANGED_TYPE + "," +
                        COL_MEAL_LAST_CHANGED_VAL +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_SYNC_STATE + "=0",
                null
        );

        while (c.moveToNext()) {
            PendingMeal pm = new PendingMeal();
            pm.memberName = c.getString(0);
            pm.date = c.getString(1);
            pm.breakfast = c.getInt(2);
            pm.lunch = c.getInt(3);
            pm.dinner = c.getInt(4);
            pm.changedMealType = c.isNull(5) ? null : c.getString(5);
            pm.changedValue = c.getInt(6);
            list.add(pm);
        }
        c.close();
        return list;
    }

    // ✅ REQUIRED BY FirestoreSyncWorker
    public void markMealSynced(String member, String date) {
        if (member == null || member.trim().isEmpty() || date == null) return;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MEAL_SYNC_STATE, 1);

        // ✅ clear trigger so notification does not repeat
        cv.put(COL_MEAL_LAST_CHANGED_TYPE, (String) null);
        cv.put(COL_MEAL_LAST_CHANGED_VAL, 0);

        db.update(TABLE_MEALS_DAILY, cv,
                COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + "=?",
                new String[]{member, date});
    }

    // -------------------------------------------------------------------------
    // MEAL PRICES
    // -------------------------------------------------------------------------

    public boolean setMealPrices(double breakfastPrice, double lunchPrice, double dinnerPrice, String date) {
        boolean ok = insertMealPriceHistory(breakfastPrice, lunchPrice, dinnerPrice, date);
        return ok;
    }


    // ✅ FIX: your app calls this but it was missing before
    public boolean insertMealPriceHistory(double breakfastPrice, double lunchPrice, double dinnerPrice, String date) {
        if (date == null || date.trim().isEmpty()) return false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PRICE_BREAKFAST, breakfastPrice);
        cv.put(COL_PRICE_LUNCH, lunchPrice);
        cv.put(COL_PRICE_DINNER, dinnerPrice);
        cv.put(COL_PRICE_DATE, date);

        Cursor c = db.rawQuery(
                "SELECT " + COL_PRICE_ID + " FROM " + TABLE_MEAL_PRICES + " WHERE " + COL_PRICE_DATE + "=?",
                new String[]{date}
        );
        boolean exists = c != null && c.moveToFirst();
        if (c != null) c.close();

        if (exists) {
            return db.update(TABLE_MEAL_PRICES, cv, COL_PRICE_DATE + "=?", new String[]{date}) > 0;
        } else {
            return db.insert(TABLE_MEAL_PRICES, null, cv) > 0;
        }
    }

    public double[] getCurrentMealPrices() {
        double[] prices = new double[]{50.0, 150.0, 150.0};
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COL_PRICE_BREAKFAST + ", " + COL_PRICE_LUNCH + ", " + COL_PRICE_DINNER +
                        " FROM " + TABLE_MEAL_PRICES +
                        " ORDER BY " + COL_PRICE_DATE + " DESC LIMIT 1",
                null
        );

        if (c != null && c.moveToFirst()) {
            prices[0] = c.getDouble(c.getColumnIndexOrThrow(COL_PRICE_BREAKFAST));
            prices[1] = c.getDouble(c.getColumnIndexOrThrow(COL_PRICE_LUNCH));
            prices[2] = c.getDouble(c.getColumnIndexOrThrow(COL_PRICE_DINNER));
            c.close();
        }
        return prices;
    }

    // -------------------------------------------------------------------------
    // CALCULATIONS
    // -------------------------------------------------------------------------

    public double getMonthlyOtherExpensesForMember(String memberName, String monthPrefix) {
        double sum = 0;
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_PAIDBY + "=? AND " +
                        COL_EXPENSE_CATEGORY + " <> ? AND " +
                        COL_EXPENSE_DATE + " LIKE ?",
                new String[]{memberName, CATEGORY_PAYMENT, monthPrefix + "%"}
        );

        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) sum = c.getDouble(0);
            c.close();
        }
        return sum;
    }

    public double getMonthlyTotalExpenses(String monthPrefix) {
        double totalMealCost = getMonthlyTotalMealCostAllMembers(monthPrefix);
        double totalOtherExpenses = getMonthlyOtherExpenses(monthPrefix);
        return totalMealCost + totalOtherExpenses;
    }

    public double getMonthlyMealCostForMember(String member, String monthPrefix) {
        if (member == null || monthPrefix == null) return 0.0;

        SQLiteDatabase db = getReadableDatabase();
        double total = 0.0;

        Cursor c = db.rawQuery(
                "SELECT " + COL_MEAL_DATE + ", " +
                        COL_MEAL_BREAKFAST + ", " + COL_MEAL_LUNCH + ", " + COL_MEAL_DINNER +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + " LIKE ?",
                new String[]{member, monthPrefix + "%"}
        );

        if (c != null) {
            while (c.moveToNext()) {
                String date = c.getString(0);
                int b = c.getInt(1);
                int l = c.getInt(2);
                int d = c.getInt(3);

                double[] p = getMealPricesForDate(date); // ✅ price effective on that date
                total += (b * p[0]) + (l * p[1]) + (d * p[2]);
            }
            c.close();
        }

        return total;
    }


    public double getMonthlyTotalMealCostAllMembers(String monthPrefix) {
        if (monthPrefix == null) return 0.0;

        SQLiteDatabase db = getReadableDatabase();
        double total = 0.0;

        Cursor c = db.rawQuery(
                "SELECT " + COL_MEAL_DATE + ", " +
                        COL_MEAL_BREAKFAST + ", " + COL_MEAL_LUNCH + ", " + COL_MEAL_DINNER +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_DATE + " LIKE ?",
                new String[]{monthPrefix + "%"}
        );

        if (c != null) {
            while (c.moveToNext()) {
                String date = c.getString(0);
                int b = c.getInt(1);
                int l = c.getInt(2);
                int d = c.getInt(3);

                double[] p = getMealPricesForDate(date); // ✅ price effective on that date
                total += (b * p[0]) + (l * p[1]) + (d * p[2]);
            }
            c.close();
        }

        return total;
    }


    public double getMonthlyOtherExpenses(String monthPrefix) {
        double sum = 0;
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_DATE + " LIKE ? AND " +
                        COL_EXPENSE_CATEGORY + " <> ?",
                new String[]{monthPrefix + "%", CATEGORY_PAYMENT}
        );

        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) sum = c.getDouble(0);
            c.close();
        }
        return sum;
    }

    public ExpenseBreakdown getMemberExpenseBreakdown(String member, String monthPrefix) {
        ExpenseBreakdown breakdown = new ExpenseBreakdown();
        if (member == null || monthPrefix == null) return breakdown;

        SQLiteDatabase db = getReadableDatabase();

        int breakfastCount = 0, lunchCount = 0, dinnerCount = 0;
        double mealCost = 0.0;

        Cursor c = db.rawQuery(
                "SELECT " + COL_MEAL_DATE + ", " +
                        COL_MEAL_BREAKFAST + ", " + COL_MEAL_LUNCH + ", " + COL_MEAL_DINNER +
                        " FROM " + TABLE_MEALS_DAILY +
                        " WHERE " + COL_MEAL_MEMBER + "=? AND " + COL_MEAL_DATE + " LIKE ?",
                new String[]{member, monthPrefix + "%"}
        );

        if (c != null) {
            while (c.moveToNext()) {
                String date = c.getString(0);
                int b = c.getInt(1);
                int l = c.getInt(2);
                int d = c.getInt(3);

                breakfastCount += b;
                lunchCount += l;
                dinnerCount += d;

                double[] p = getMealPricesForDate(date); // ✅ historical effective price
                mealCost += (b * p[0]) + (l * p[1]) + (d * p[2]);
            }
            c.close();
        }

        breakdown.breakfastCount = breakfastCount;
        breakdown.lunchCount = lunchCount;
        breakdown.dinnerCount = dinnerCount;
        breakdown.mealCost = mealCost;

        double memberOther = getMonthlyOtherExpensesForMember(member, monthPrefix);
        breakdown.otherExpensesShare = memberOther;

        breakdown.paidAmount = getMonthlyPaidByMember(member, monthPrefix);

        breakdown.totalCost = breakdown.mealCost + breakdown.otherExpensesShare;
        breakdown.balance = breakdown.totalCost - breakdown.paidAmount;

        return breakdown;
    }



    // -------------------------------------------------------------------------
    // USER APPROVAL HELPERS (local pending)
    // -------------------------------------------------------------------------

    public List<User> getPendingMembers() {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_ID + ", " +
                        COL_USER_FULL_NAME + ", " +
                        COL_USER_USERNAME + ", " +
                        COL_USER_PASSWORD + ", " +
                        COL_USER_CONTACT + ", " +
                        COL_USER_ADDRESS + ", " +
                        COL_USER_PARENT_CONTACT + ", " +
                        COL_USER_IS_ADMIN + ", " +
                        COL_USER_STATUS +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_STATUS + "=?",
                new String[]{"pending"}
        );

        while (c.moveToNext()) {
            String userId = c.getString(0);
            String fullName = c.getString(1);
            String username = c.getString(2);
            String password = c.getString(3);
            String contact = c.getString(4);
            String address = c.getString(5);
            String parentContact = c.getString(6);
            boolean isAdmin = c.getInt(7) == 1;
            String status = c.getString(8);

            list.add(new User(userId, fullName, username, password, contact, address, parentContact, isAdmin, status));
        }
        c.close();
        return list;
    }

    public boolean approvePendingMember(String userId) {
        if (userId == null || userId.isEmpty()) return false;

        SQLiteDatabase db = getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_FULL_NAME + ", " +
                        COL_USER_USERNAME + ", " +
                        COL_USER_CONTACT + ", " +
                        COL_USER_ADDRESS + ", " +
                        COL_USER_PARENT_CONTACT +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_ID + "=? LIMIT 1",
                new String[]{userId}
        );

        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            return false;
        }

        String username = c.getString(1);
        String contact = c.getString(2);
        String address = c.getString(3);
        String parentContact = c.getString(4);
        c.close();

        ContentValues cvUser = new ContentValues();
        cvUser.put(COL_USER_STATUS, "active");

        int updated = db.update(TABLE_USERS, cvUser, COL_USER_ID + "=?", new String[]{userId});
        if (updated <= 0) return false;

        addOrUpdateMember(username, "member", contact, address, parentContact);
        return true;
    }

    public boolean deleteUserById(String userId) {
        if (userId == null || userId.isEmpty()) return false;
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_USERS, COL_USER_ID + "=?", new String[]{userId});
        return rows > 0;
    }

    // -------------------------------------------------------------------------
    // FIRESTORE -> SQLITE UPSERT (remote-id based)
    // -------------------------------------------------------------------------

    public void upsertExpenseFromRemote(String remoteId,
                                        String date,
                                        String title,
                                        String category,
                                        String paidBy,
                                        double amount) {

        if (remoteId == null || remoteId.trim().isEmpty()) return;
        if (date == null || paidBy == null) return;

        SQLiteDatabase db = this.getWritableDatabase();

        String safeTitle = (title == null) ? "" : title;
        String safeCategory = (category == null) ? "" : category;

        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_REMOTE_ID, remoteId);
        cv.put(COL_EXPENSE_SYNC_STATE, 1);
        cv.put(COL_EXPENSE_DATE, date);
        cv.put(COL_EXPENSE_TITLE, safeTitle);
        cv.put(COL_EXPENSE_CATEGORY, safeCategory);
        cv.put(COL_EXPENSE_PAIDBY, paidBy);
        cv.put(COL_EXPENSE_AMOUNT, amount);

        Cursor c = db.query(
                TABLE_EXPENSES,
                new String[]{COL_EXPENSE_ID},
                COL_EXPENSE_REMOTE_ID + "=?",
                new String[]{remoteId},
                null, null, null
        );

        try {
            if (c != null && c.moveToFirst()) {
                db.update(TABLE_EXPENSES, cv, COL_EXPENSE_REMOTE_ID + "=?", new String[]{remoteId});
            } else {
                db.insert(TABLE_EXPENSES, null, cv);
            }
        } finally {
            if (c != null) c.close();
        }
    }

    // -------------------------------------------------------------------------
    // Existing helper methods kept
    // -------------------------------------------------------------------------

    public double[] getMemberLocalExpenseAndPaymentForMonth(String memberName, String monthPrefix) {
        double otherExp = 0.0;
        double paid = 0.0;

        if (memberName == null || monthPrefix == null) return new double[]{0.0, 0.0};

        SQLiteDatabase db = this.getReadableDatabase();
        String likePattern = monthPrefix + "%";

        Cursor c1 = db.rawQuery(
                "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_PAIDBY + "=? AND " +
                        COL_EXPENSE_DATE + " LIKE ? AND " +
                        COL_EXPENSE_CATEGORY + " <> ?",
                new String[]{memberName, likePattern, CATEGORY_PAYMENT}
        );
        try {
            if (c1 != null && c1.moveToFirst()) otherExp = c1.isNull(0) ? 0.0 : c1.getDouble(0);
        } finally {
            if (c1 != null) c1.close();
        }

        Cursor c2 = db.rawQuery(
                "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_PAIDBY + "=? AND " +
                        COL_EXPENSE_DATE + " LIKE ? AND " +
                        COL_EXPENSE_CATEGORY + "=?",
                new String[]{memberName, likePattern, CATEGORY_PAYMENT}
        );
        try {
            if (c2 != null && c2.moveToFirst()) paid = c2.isNull(0) ? 0.0 : c2.getDouble(0);
        } finally {
            if (c2 != null) c2.close();
        }

        return new double[]{otherExp, paid};
    }

    public double getTotalLocalOtherExpensesForMonth(String monthPrefix) {
        if (monthPrefix == null) return 0.0;

        SQLiteDatabase db = this.getReadableDatabase();
        String likePattern = monthPrefix + "%";
        double total = 0.0;

        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_EXPENSE_DATE + " LIKE ? AND " +
                        COL_EXPENSE_CATEGORY + " <> ?",
                new String[]{likePattern, CATEGORY_PAYMENT}
        );
        try {
            if (c != null && c.moveToFirst()) total = c.isNull(0) ? 0.0 : c.getDouble(0);
        } finally {
            if (c != null) c.close();
        }
        return total;
    }

    public String getDisplayNameByUsername(String username) {
        if (username == null) return "Unknown";

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_FULL_NAME +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );

        String name = username;
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) {
                name = c.getString(0);
            }
            c.close();
        }
        return name;
    }

    // ===================== USER PROFILE GETTERS (for MemberDetails) =====================

    public String getFullNameByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_FULL_NAME +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );

        String val = null;
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) val = c.getString(0);
            c.close();
        }
        return val;
    }

    public String getContactByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_CONTACT +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );

        String val = null;
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) val = c.getString(0);
            c.close();
        }
        return val;
    }

    public String getParentContactByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_PARENT_CONTACT +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );

        String val = null;
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) val = c.getString(0);
            c.close();
        }
        return val;
    }

    public String getAddressByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_USER_ADDRESS +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );

        String val = null;
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) val = c.getString(0);
            c.close();
        }
        return val;
    }
}
