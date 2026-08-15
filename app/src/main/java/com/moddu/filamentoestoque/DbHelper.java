package com.moddu.filamentoestoque;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB = "filamentos.db";
    private static final int VERSION = 1;

    public DbHelper(Context context) { super(context, DB, null, VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE filaments (id INTEGER PRIMARY KEY AUTOINCREMENT, material TEXT NOT NULL, color TEXT, brand TEXT, initial_weight INTEGER, remaining_weight INTEGER, notes TEXT, photo_path TEXT, qr_photo_path TEXT, qr_info TEXT)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public long save(Filament f) {
        ContentValues v = new ContentValues();
        v.put("material", f.material); v.put("color", f.color); v.put("brand", f.brand);
        v.put("initial_weight", f.initialWeight); v.put("remaining_weight", f.remainingWeight);
        v.put("notes", f.notes); v.put("photo_path", f.photoPath); v.put("qr_photo_path", f.qrPhotoPath); v.put("qr_info", f.qrInfo);
        if (f.id > 0) { getWritableDatabase().update("filaments", v, "id=?", new String[]{String.valueOf(f.id)}); return f.id; }
        return getWritableDatabase().insert("filaments", null, v);
    }

    public void delete(long id) { getWritableDatabase().delete("filaments", "id=?", new String[]{String.valueOf(id)}); }

    public List<Filament> list(String material, String search) {
        List<Filament> out = new ArrayList<>();
        String where = "1=1"; List<String> args = new ArrayList<>();
        if (material != null && !material.equals("TODOS")) { where += " AND material=?"; args.add(material); }
        if (search != null && !search.trim().isEmpty()) {
            where += " AND (color LIKE ? OR brand LIKE ? OR qr_info LIKE ? OR notes LIKE ?)";
            String s = "%" + search.trim() + "%"; args.add(s); args.add(s); args.add(s); args.add(s);
        }
        Cursor c = getReadableDatabase().query("filaments", null, where, args.toArray(new String[0]), null, null, "id DESC");
        while (c.moveToNext()) out.add(from(c)); c.close(); return out;
    }

    public int count(String material) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM filaments WHERE material=?", new String[]{material});
        int n = c.moveToFirst() ? c.getInt(0) : 0; c.close(); return n;
    }

    private Filament from(Cursor c) {
        Filament f = new Filament();
        f.id = c.getLong(c.getColumnIndexOrThrow("id")); f.material = c.getString(c.getColumnIndexOrThrow("material"));
        f.color = c.getString(c.getColumnIndexOrThrow("color")); f.brand = c.getString(c.getColumnIndexOrThrow("brand"));
        f.initialWeight = c.getInt(c.getColumnIndexOrThrow("initial_weight")); f.remainingWeight = c.getInt(c.getColumnIndexOrThrow("remaining_weight"));
        f.notes = c.getString(c.getColumnIndexOrThrow("notes")); f.photoPath = c.getString(c.getColumnIndexOrThrow("photo_path"));
        f.qrPhotoPath = c.getString(c.getColumnIndexOrThrow("qr_photo_path")); f.qrInfo = c.getString(c.getColumnIndexOrThrow("qr_info"));
        return f;
    }
}
