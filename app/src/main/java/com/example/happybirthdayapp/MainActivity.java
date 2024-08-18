package com.example.happybirthdayapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int READ_CONTACTS_PERMISSION_REQUEST = 1;
    private static final String DEBUG = "MainActivity";
    private static final int CONTACT_LOADER_ID = 90;
    private static final int CONTACT_ID_INDEX = 0;
    private static final int LOOKUP_KEY_INDEX = 1;

    private SimpleCursorAdapter simpleCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        setupCursorAdapter();
        ListView contactsListView = findViewById(R.id.list_view_contacts);
        contactsListView.setAdapter(simpleCursorAdapter);
        contactsListView.setOnItemClickListener(this);

        getPermissionToReadUserContacts();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupCursorAdapter() {

        String[] uiBindFrom = {
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI
        };

        int[] uiBindTo = { R.id.tvName, R.id.vImageView };

        simpleCursorAdapter = new SimpleCursorAdapter(this,
                R.layout.content_list_item, null, uiBindFrom, uiBindTo, 0);

    }

    private void getPermissionToReadUserContacts() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.READ_CONTACTS}, READ_CONTACTS_PERMISSION_REQUEST);
                return;
            } else {
                loadingContacts();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case READ_CONTACTS_PERMISSION_REQUEST: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadingContacts();

                } else {
                    Log.d(DEBUG, "Permission Denied");
                }
            }

        }
    }

    private void loadingContacts() {
        Log.d(DEBUG, "We have permission to load the contacts!");
        getSupportLoaderManager().initLoader(CONTACT_LOADER_ID, new Bundle(), contactsLoader);
    }

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // TODO: Inflate the menu this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {

        // TODO: Handle action bar item clicks here. the action bar will
        // TODO: automatically handle clicks on the Home/Up button so long
        // TODO: as you specify a parent activity in AndroidManifest.xml

        int id = menuItem.getItemId();

        // TODO: Notification simplifiable if statement
        if (id == R.id.action_about_me) {
            return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    private LoaderManager.LoaderCallbacks<Cursor> contactsLoader = new LoaderManager.LoaderCallbacks<Cursor>() {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

            String[] projectionFields = new String[] {
                  ContactsContract.Contacts._ID,
                  ContactsContract.Contacts.DISPLAY_NAME,
                  ContactsContract.Contacts.PHOTO_URI

            };

            CursorLoader cursorLoader = new CursorLoader(MainActivity.this,
                    ContactsContract.Contacts.CONTENT_URI,
                    projectionFields,
                    null,
                    null,
                    null);

            return cursorLoader;
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            simpleCursorAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            simpleCursorAdapter.swapCursor(null);
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        Cursor cursor = ((SimpleCursorAdapter) adapterView.getAdapter()).getCursor();

        cursor.moveToPosition(position);

        String contactName = cursor.getString(LOOKUP_KEY_INDEX);

        Uri contactUri = ContactsContract.Contacts.getLookupUri(
                cursor.getLong(CONTACT_ID_INDEX), contactName);

        String email = getEmail(contactUri);

        if (!email.equals("")) {
            sendEmail(email, contactName);
        }

    }

    private void sendEmail(String email, String contactName) {

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
            "mailto", email,null));

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.main_email_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.main_email_body, contactName));

        startActivity(Intent.createChooser(emailIntent, getString(R.string.main_email_chooser)));

    }

    private String getEmail(Uri contactUri) {

        String email = "";
        String id = contactUri.getLastPathSegment();

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
                new String[] { id },
                null);

        int emailIdX = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

        if (cursor.moveToFirst()) {
            email = cursor.getString(emailIdX);
        }

        return email;

    }

    private void loadAboutMe() {

        Intent loadAboutMeIntent = new Intent(MainActivity.this, AboutMeActivity.class);
        startActivity(loadAboutMeIntent);
    }

}