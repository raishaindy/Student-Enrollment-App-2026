
package com.example.studentenrollmentapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MainActivity extends Activity {

    private EditText nameInput;
    private EditText idInput;
    private CheckBox enrolledCheck;
    private LinearLayout studentList;
    private DatabaseHelper dbHelper;

    private int selectedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);

        createInterface();
        displayStudents();
    }

    private void createInterface() {
        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(Color.rgb(248, 245, 250));

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Student Enrollment");
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(80, 45, 100));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Manage Student Information");
        subtitle.setTextSize(15);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 24);
        root.addView(subtitle);

        nameInput = new EditText(this);
        nameInput.setHint("Enter Student Name");
        nameInput.setSingleLine(true);
        root.addView(nameInput);

        idInput = new EditText(this);
        idInput.setHint("Enter Student ID");
        idInput.setSingleLine(true);
        root.addView(idInput);

        enrolledCheck = new CheckBox(this);
        enrolledCheck.setText("Has Enrolled");
        enrolledCheck.setTextSize(16);
        enrolledCheck.setPadding(0, 12, 0, 12);
        root.addView(enrolledCheck);

        addButton(root, "ADD", v -> addStudent());
        addButton(root, "VIEW / REFRESH", v -> displayStudents());
        addButton(root, "EDIT SELECTED", v -> editStudent());
        addButton(root, "DELETE SELECTED", v -> deleteStudent());

        TextView listTitle = new TextView(this);
        listTitle.setText("Student Records");
        listTitle.setTextSize(20);
        listTitle.setTypeface(null, Typeface.BOLD);
        listTitle.setTextColor(Color.rgb(80, 45, 100));
        listTitle.setPadding(0, 24, 0, 12);
        root.addView(listTitle);

        TextView instruction = new TextView(this);
        instruction.setText(
                "Tap a student record to select it for editing or deleting."
        );
        instruction.setTextSize(13);
        root.addView(instruction);

        studentList = new LinearLayout(this);
        studentList.setOrientation(LinearLayout.VERTICAL);
        root.addView(studentList);

        setContentView(scrollView);
    }

    private void addButton(
            LinearLayout root,
            String label,
            View.OnClickListener listener) {

        Button button = new Button(this);
        button.setText(label);
        root.addView(button);
        button.setOnClickListener(listener);
    }

    private void addStudent() {
        String name = nameInput.getText().toString().trim();
        String studentId = idInput.getText().toString().trim();

        if (name.isEmpty() || studentId.isEmpty()) {
            showMessage("Please enter Student Name and Student ID.");
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("student_name", name);
        values.put("student_id", studentId);
        values.put("has_enrolled", enrolledCheck.isChecked() ? 1 : 0);

        long result = db.insert("students", null, values);

        if (result == -1) {
            showMessage("Could not add student. Student ID may already exist.");
        } else {
            showMessage("Student added successfully.");
            clearForm();
            displayStudents();
        }
    }

    private void displayStudents() {
        studentList.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                "students",
                null,
                null,
                null,
                null,
                null,
                "id DESC"
        );

        boolean selectedExists = false;

        if (cursor.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No student records yet.");
            empty.setTextSize(15);
            empty.setPadding(8, 16, 8, 16);
            studentList.addView(empty);
        }

        while (cursor.moveToNext()) {
            int rowId = cursor.getInt(
                    cursor.getColumnIndexOrThrow("id")
            );

            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow("student_name")
            );

            String studentId = cursor.getString(
                    cursor.getColumnIndexOrThrow("student_id")
            );

            int enrolled = cursor.getInt(
                    cursor.getColumnIndexOrThrow("has_enrolled")
            );

            if (rowId == selectedId) {
                selectedExists = true;
            }

            TextView item = new TextView(this);
            item.setText(
                    "Name: " + name +
                            "\nStudent ID: " + studentId +
                            "\nHas Enrolled: " + (enrolled == 1 ? "Yes" : "No") +
                            "\nTap to select"
            );

            item.setTextSize(16);
            item.setTextColor(Color.rgb(45, 45, 45));
            item.setPadding(16, 16, 16, 16);
            item.setBackgroundColor(
                    rowId == selectedId
                            ? Color.rgb(225, 210, 240)
                            : Color.WHITE
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(0, 8, 0, 8);
            studentList.addView(item, params);

            item.setOnClickListener(v -> {
                selectedId = rowId;

                nameInput.setText(name);
                idInput.setText(studentId);
                enrolledCheck.setChecked(enrolled == 1);

                displayStudents();
                showMessage("Selected student: " + name);
            });
        }

        cursor.close();

        if (!selectedExists) {
            selectedId = -1;
        }
    }

    private void editStudent() {
        if (selectedId == -1) {
            showMessage("Please select a student record first.");
            return;
        }

        String name = nameInput.getText().toString().trim();
        String studentId = idInput.getText().toString().trim();

        if (name.isEmpty() || studentId.isEmpty()) {
            showMessage("Name and Student ID cannot be empty.");
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("student_name", name);
        values.put("student_id", studentId);
        values.put("has_enrolled", enrolledCheck.isChecked() ? 1 : 0);

        int result = db.update(
                "students",
                values,
                "id = ?",
                new String[]{String.valueOf(selectedId)}
        );

        if (result > 0) {
            showMessage("Student updated successfully.");
            clearForm();
            displayStudents();
        } else {
            showMessage("Update failed. Student ID may already exist.");
        }
    }

    private void deleteStudent() {
        if (selectedId == -1) {
            showMessage("Please tap a student record first.");
            return;
        }

        // Save the selected ID before showing the confirmation dialog.
        final int idToDelete = selectedId;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to delete this student?")
                .setNegativeButton("CANCEL", (d, which) -> d.dismiss())
                .setPositiveButton("DELETE", (d, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    int deletedRows = db.delete(
                            "students",
                            "id = ?",
                            new String[]{String.valueOf(idToDelete)}
                    );

                    if (deletedRows > 0) {
                        clearForm();
                        displayStudents();
                        showMessage("Student deleted successfully.");
                    } else {
                        showMessage("Delete failed: record not found.");
                        displayStudents();
                    }
                })
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.RED);
        });

        dialog.show();
    }

    private void clearForm() {
        nameInput.setText("");
        idInput.setText("");
        enrolledCheck.setChecked(false);
        selectedId = -1;
    }

    private void showMessage(String message) {
        Toast.makeText(
                MainActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "students.db";
        private static final int DATABASE_VERSION = 1;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE students (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "student_name TEXT NOT NULL, " +
                            "student_id TEXT NOT NULL UNIQUE, " +
                            "has_enrolled INTEGER NOT NULL DEFAULT 0" +
                            ")"
            );
        }

        @Override
        public void onUpgrade(
                SQLiteDatabase db,
                int oldVersion,
                int newVersion) {
            // No database migration is needed for version 1.
        }
    }
}
