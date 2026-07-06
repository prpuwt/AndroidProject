package com.example.mybill.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.mybill.model.Bill;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "mybill.db";
    private static final int DB_VERSION = 6;

    private static final String TABLE_BILLS = "bills";
    private static final String COL_ID = "_id";
    private static final String COL_DATE = "date";
    private static final String COL_PERSON_NAME = "person_name";
    private static final String COL_LOCATION = "location";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_DELETED = "deleted";
    private static final String COL_SETTLED = "settled";

    private static final String WHERE_ACTIVE = COL_DELETED + "=0";

    private static final String TABLE_PEOPLE = "people";
    private static final String COL_PEOPLE_ID = "_id";
    private static final String COL_PEOPLE_NAME = "name";

    private static final String TABLE_LOCATIONS = "locations";
    private static final String COL_LOC_ID = "_id";
    private static final String COL_LOC_NAME = "name";

    public BillDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_BILLS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DATE + " TEXT NOT NULL, " +
                COL_PERSON_NAME + " TEXT NOT NULL, " +
                COL_LOCATION + " TEXT NOT NULL, " +
                COL_DESCRIPTION + " TEXT NOT NULL, " +
                COL_AMOUNT + " REAL NOT NULL, " +
                COL_CREATED_AT + " TEXT, " +
                COL_DELETED + " INTEGER DEFAULT 0, " +
                COL_SETTLED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_PEOPLE + " (" +
                COL_PEOPLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PEOPLE_NAME + " TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_LOCATIONS + " (" +
                COL_LOC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LOC_NAME + " TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_date ON " + TABLE_BILLS + "(" + COL_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_person ON " + TABLE_BILLS + "(" + COL_PERSON_NAME + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_deleted ON " + TABLE_BILLS + "(" + COL_DELETED + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_date_deleted ON " + TABLE_BILLS + "(" + COL_DATE + ", " + COL_DELETED + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_settled ON " + TABLE_BILLS + "(" + COL_SETTLED + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 按版本号逐步迁移，永不自动删表
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN "
                    + COL_DELETED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            // 修复：上一版本升级到V2时可能未添加deleted列
            try {
                db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN "
                        + COL_DELETED + " INTEGER DEFAULT 0");
            } catch (Exception ignored) {
                // 列已存在，忽略
            }
        }
        if (oldVersion < 4) {
            // V4：schema 无变化，仅同步版本号
        }
        if (oldVersion < 5) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_date ON " + TABLE_BILLS + "(" + COL_DATE + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_person ON " + TABLE_BILLS + "(" + COL_PERSON_NAME + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_deleted ON " + TABLE_BILLS + "(" + COL_DELETED + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_date_deleted ON " + TABLE_BILLS + "(" + COL_DATE + ", " + COL_DELETED + ")");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN "
                    + COL_SETTLED + " INTEGER DEFAULT 0");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_settled ON " + TABLE_BILLS + "(" + COL_SETTLED + ")");
        }
    }

    // ==================== 人员管理 ====================

    public long insertPerson(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PEOPLE_NAME, name);
        long id = db.insertWithOnConflict(TABLE_PEOPLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return id;
    }

    public List<String> getAllPeople() {
        List<String> people = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PEOPLE, null, null, null, null, null, COL_PEOPLE_ID + " ASC");
        while (cursor.moveToNext()) {
            people.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_PEOPLE_NAME)));
        }
        cursor.close();
        return people;
    }

    public int deletePerson(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_PEOPLE, COL_PEOPLE_ID + "=?", new String[]{String.valueOf(id)});
        return rows;
    }

    public int deletePersonByName(String name) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_PEOPLE, COL_PEOPLE_NAME + "=?", new String[]{name});
        return rows;
    }

    // ==================== 地点管理 ====================

    public long insertLocation(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LOC_NAME, name);
        long id = db.insertWithOnConflict(TABLE_LOCATIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return id;
    }

    public List<String> getAllLocations() {
        List<String> locations = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCATIONS, null, null, null, null, null, COL_LOC_ID + " ASC");
        while (cursor.moveToNext()) {
            locations.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOC_NAME)));
        }
        cursor.close();
        return locations;
    }

    public int deleteLocation(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_LOCATIONS, COL_LOC_ID + "=?", new String[]{String.valueOf(id)});
        return rows;
    }

    public int deleteLocationByName(String name) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_LOCATIONS, COL_LOC_NAME + "=?", new String[]{name});
        return rows;
    }

    // ==================== 账单管理 ====================

    public long insertBill(Bill bill) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, bill.getDate());
        values.put(COL_PERSON_NAME, bill.getPersonName());
        values.put(COL_LOCATION, bill.getLocation());
        values.put(COL_DESCRIPTION, bill.getDescription());
        values.put(COL_AMOUNT, bill.getAmount());
        values.put(COL_CREATED_AT,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        long id = db.insert(TABLE_BILLS, null, values);
        return id;
    }

    public void insertBills(List<Bill> bills) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            for (Bill bill : bills) {
                ContentValues values = new ContentValues();
                values.put(COL_DATE, bill.getDate());
                values.put(COL_PERSON_NAME, bill.getPersonName());
                values.put(COL_LOCATION, bill.getLocation());
                values.put(COL_DESCRIPTION, bill.getDescription());
                values.put(COL_AMOUNT, bill.getAmount());
                values.put(COL_CREATED_AT, createdAt);
                db.insert(TABLE_BILLS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            }
    }

    public List<Bill> getBillsByMonth(int year, int month) {
        SQLiteDatabase db = getReadableDatabase();
        String monthStr = String.format(Locale.getDefault(), "%04d-%02d", year, month);
        Cursor cursor = db.query(TABLE_BILLS, null, COL_DATE + " LIKE ? AND " + WHERE_ACTIVE,
                new String[]{monthStr + "%"}, null, null, COL_DATE + " DESC, " + COL_CREATED_AT + " DESC");
        List<Bill> bills = cursorToBills(cursor);
        return bills;
    }

    public List<Bill> getBillsByDay(int year, int month, int day) {
        SQLiteDatabase db = getReadableDatabase();
        String dayStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
        Cursor cursor = db.query(TABLE_BILLS, null, COL_DATE + "=? AND " + WHERE_ACTIVE,
                new String[]{dayStr}, null, null, COL_CREATED_AT + " DESC");
        List<Bill> bills = cursorToBills(cursor);
        return bills;
    }

    public List<Bill> getBillsByDateRange(String startDate, String endDate, String personName) {
        SQLiteDatabase db = getReadableDatabase();
        String selection;
        String[] selectionArgs;
        if (personName == null) {
            selection = COL_DATE + ">=? AND " + COL_DATE + "<=? AND " + WHERE_ACTIVE;
            selectionArgs = new String[]{startDate, endDate};
        } else {
            selection = COL_DATE + ">=? AND " + COL_DATE + "<=? AND " + COL_PERSON_NAME + "=? AND " + WHERE_ACTIVE;
            selectionArgs = new String[]{startDate, endDate, personName};
        }
        Cursor cursor = db.query(TABLE_BILLS, null, selection, selectionArgs,
                null, null, COL_DATE + " ASC");
        List<Bill> bills = cursorToBills(cursor);
        return bills;
    }

    public int deleteBill(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_BILLS, COL_ID + "=?", new String[]{String.valueOf(id)});
        return rows;
    }

    public int updateBill(Bill bill) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, bill.getDate());
        values.put(COL_PERSON_NAME, bill.getPersonName());
        values.put(COL_LOCATION, bill.getLocation());
        values.put(COL_DESCRIPTION, bill.getDescription());
        values.put(COL_AMOUNT, bill.getAmount());
        int rows = db.update(TABLE_BILLS, values, COL_ID + "=?",
                new String[]{String.valueOf(bill.getId())});
        return rows;
    }

    public boolean billExists(String date, String personName, String location,
                               String description, double amount) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, null,
                COL_DATE + "=? AND " + COL_PERSON_NAME + "=? AND " + COL_LOCATION + "=? AND "
                        + COL_DESCRIPTION + "=? AND " + COL_AMOUNT + "=? AND " + WHERE_ACTIVE,
                new String[]{date, personName, location, description, String.format(Locale.US, "%.2f", amount)},
                null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public List<String> getDistinctPeopleWithBills() {
        List<String> names = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(true, TABLE_BILLS, new String[]{COL_PERSON_NAME},
                WHERE_ACTIVE, null, null, null, null, null);
        while (cursor.moveToNext()) {
            names.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)));
        }
        cursor.close();
        return names;
    }

    public int countBillsByPerson(String personName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, new String[]{"COUNT(*)"},
                COL_PERSON_NAME + "=? AND " + WHERE_ACTIVE, new String[]{personName},
                null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int countBillsByLocation(String location) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, new String[]{"COUNT(*)"},
                COL_LOCATION + "=? AND " + WHERE_ACTIVE, new String[]{location},
                null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public List<Bill> getAllBills() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, null, WHERE_ACTIVE, null, null, null, COL_DATE + " ASC");
        List<Bill> bills = cursorToBills(cursor);
        return bills;
    }

    public List<Bill> searchBills(String keyword) {
        List<Bill> bills = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String like = "%" + keyword + "%";
        Cursor cursor = db.query(TABLE_BILLS, null,
                "(" + COL_DESCRIPTION + " LIKE ? OR " + COL_PERSON_NAME + " LIKE ? OR "
                        + COL_LOCATION + " LIKE ? OR " + COL_DATE + " LIKE ?) AND " + WHERE_ACTIVE,
                new String[]{like, like, like, like},
                null, null, COL_DATE + " DESC, " + COL_CREATED_AT + " DESC");
        while (cursor.moveToNext()) {
            bills.add(cursorToBill(cursor));
        }
        cursor.close();
        return bills;
    }

    // ==================== 软删除系统 ====================

    public int softDeleteBill(long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DELETED, 1);
        int rows = db.update(TABLE_BILLS, values, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        return rows;
    }

    public int restoreBill(long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DELETED, 0);
        int rows = db.update(TABLE_BILLS, values, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        return rows;
    }

    public int permanentlyDeleteBill(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_BILLS, COL_ID + "=? AND " + COL_DELETED + "=1",
                new String[]{String.valueOf(id)});
        return rows;
    }

    public List<Bill> getDeletedBills() {
        List<Bill> bills = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, null, COL_DELETED + "=1",
                null, null, null, COL_DATE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                bills.add(cursorToBill(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bills;
    }

    public int softDeleteBillsByPerson(String personName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DELETED, 1);
        int rows = db.update(TABLE_BILLS, values,
                COL_PERSON_NAME + "=? AND " + WHERE_ACTIVE,
                new String[]{personName});
        return rows;
    }

    public int softDeleteBillsByMonth(int year, int month) {
        SQLiteDatabase db = getWritableDatabase();
        String monthStr = String.format(Locale.getDefault(), "%04d-%02d", year, month);
        ContentValues values = new ContentValues();
        values.put(COL_DELETED, 1);
        int rows = db.update(TABLE_BILLS, values,
                COL_DATE + " LIKE ? AND " + WHERE_ACTIVE,
                new String[]{monthStr + "%"});
        return rows;
    }

    public int permanentlyDeleteAllDeleted() {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_BILLS, COL_DELETED + "=1", null);
        return rows;
    }

    // ==================== 结算系统 ====================

    public int settleBillsByPersonAndDateRange(String personName, String startDate, String endDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SETTLED, 1);
        int rows = db.update(TABLE_BILLS, values,
                COL_PERSON_NAME + "=? AND " + COL_DATE + ">=? AND " + COL_DATE + "<=? AND " + WHERE_ACTIVE,
                new String[]{personName, startDate, endDate});
        return rows;
    }

    public int unsettleBill(long billId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SETTLED, 0);
        int rows = db.update(TABLE_BILLS, values, COL_ID + "=?",
                new String[]{String.valueOf(billId)});
        return rows;
    }

    public int unsettleBillsByPersonAndDateRange(String personName, String startDate, String endDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SETTLED, 0);
        int rows = db.update(TABLE_BILLS, values,
                COL_PERSON_NAME + "=? AND " + COL_DATE + ">=? AND " + COL_DATE + "<=? AND " + WHERE_ACTIVE,
                new String[]{personName, startDate, endDate});
        return rows;
    }

    public int countBillsByPersonAndDateRange(String personName, String startDate, String endDate) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, new String[]{"COUNT(*)"},
                COL_PERSON_NAME + "=? AND " + COL_DATE + ">=? AND " + COL_DATE + "<=? AND " + WHERE_ACTIVE,
                new String[]{personName, startDate, endDate}, null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public double sumBillsByPersonAndDateRange(String personName, String startDate, String endDate) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, new String[]{"SUM(" + COL_AMOUNT + ")"},
                COL_PERSON_NAME + "=? AND " + COL_DATE + ">=? AND " + COL_DATE + "<=? AND " + WHERE_ACTIVE,
                new String[]{personName, startDate, endDate}, null, null, null);
        double sum = 0;
        if (cursor.moveToFirst()) sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }

    private Bill cursorToBill(Cursor cursor) {
        Bill bill = new Bill();
        bill.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        bill.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
        bill.setPersonName(cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)));
        bill.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)));
        bill.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
        bill.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        bill.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
        bill.setSettled(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SETTLED)));
        return bill;
    }

    private List<Bill> cursorToBills(Cursor cursor) {
        List<Bill> bills = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Bill bill = new Bill();
                bill.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
                bill.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
                bill.setPersonName(cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)));
                bill.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)));
                bill.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
                bill.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
                bill.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
                bill.setSettled(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SETTLED)));
                bills.add(bill);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bills;
    }
}
