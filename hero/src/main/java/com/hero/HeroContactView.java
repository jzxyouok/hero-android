package com.hero;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.widget.TextView;

import com.hero.depandency.MPermissionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HeroContactView extends TextView implements IHero {
    private static final String TAG = "HeroContactView";
    private JSONObject contactObject;
    private JSONObject callsObject;

    public HeroContactView(Context context) {
        super(context);
        this.setVisibility(INVISIBLE);
    }

    @Override
    public void on(JSONObject jsonObject) throws JSONException {
        HeroView.on(this, jsonObject);
        if (jsonObject.has("getContact")) {
            if (jsonObject.get("getContact") instanceof JSONObject) {
                contactObject = jsonObject.getJSONObject("getContact");
            }
            //            pickContact();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JSONArray jsonArray = getAllContacts();
                    if (jsonArray.length() > 0) {
                        JSONObject value = new JSONObject();
                        try {
                            value.put("contacts", jsonArray);
                            contactObject.put("value", value);
                            ((IHeroContext) getContext()).on(contactObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        JSONObject value = new JSONObject();
                        try {
                            value.put("error", false);
                            contactObject.put("value", value);
                            ((IHeroContext) getContext()).on(contactObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        if (jsonObject.has("getRecent")) {
            if (jsonObject.get("getRecent") instanceof JSONObject) {
                callsObject = jsonObject.getJSONObject("getRecent");
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JSONArray jsonArray = getAllCallLogs();
                    if (jsonArray.length() > 0) {
                        JSONObject value = new JSONObject();
                        try {
                            value.put("callHistories", jsonArray);
                            callsObject.put("value", value);
                            ((IHeroContext) getContext()).on(callsObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        JSONObject value = new JSONObject();
                        try {
                            value.put("error", "fail");
                            callsObject.put("value", value);
                            ((IHeroContext) getContext()).on(callsObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        if (jsonObject.has("contactName") || jsonObject.has("contactNumber")) {
            if (contactObject != null) {
                if (jsonObject.has("error")) { // got fail
                    JSONObject value = new JSONObject();
                    try {
                        value.put("error", jsonObject.getString("error"));
                        contactObject.put("value", value);
                        ((IHeroContext) getContext()).on(contactObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    contactObject.put("name", jsonObject.getString("contactName"));
                    contactObject.put("phone", jsonObject.getString("contactNumber"));
                    ((IHeroContext) this.getContext()).on(contactObject);
                }
            }
        }
    }

    public JSONArray getAllContacts() {
        JSONArray array = new JSONArray();

        if (!requestContactPermission()) {
            return array;
        }
        ContentResolver cr = getContext().getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String phoneNumber = "";

                Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                while (phones.moveToNext()) {
                    phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
                phones.close();
                JSONObject item = new JSONObject();
                try {
                    item.put("phone", phoneNumber);
                    item.put("name", name);
                    array.put(item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());
        }
        cursor.close();
        return array;
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        if (getContext() instanceof HeroActivity) {
            ((HeroActivity) (getContext())).startActivityByView(intent, HeroActivity.REQUEST_CODE_PICK_CONTACT, this);
        }
    }

    private JSONArray getAllCallLogs() {
        JSONArray array = new JSONArray();

        if (!requestCalllogPermission()) {
            return array;
        }
        final ContentResolver resolver = getContext().getContentResolver();
        Uri uri;
        uri = CallLog.Calls.CONTENT_URI;
        //        uri = uri.buildUpon().appendQueryParameter("address_book_index_extras", "true").build();

        //        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.OUTGOING_TYPE, CallLog.Calls.MISSED_TYPE;
        String[] projection = new String[] {CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.TYPE};
        String[] selectionArgs = null;
        String sortOrder = CallLog.Calls.DATE + " desc";

        Cursor callCursor = resolver.query(uri, projection, null, selectionArgs, sortOrder);
        if (callCursor.moveToFirst()) {
            do {
                if (callCursor.getInt(4) != CallLog.Calls.MISSED_TYPE) {
                    JSONObject item = new JSONObject();
                    try {
                        item.put("phone", callCursor.getString(0));
                        item.put("name", callCursor.getString(1));
                        item.put("callTime", callCursor.getString(2));
                        item.put("duration", callCursor.getString(3));
                        item.put("callType", callCursor.getInt(4) == CallLog.Calls.INCOMING_TYPE ? "CALLIN" : "CALLOUT");
                        array.put(item);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } while (callCursor.moveToNext());
        }
        callCursor.close();
        return array;
    }

    private boolean requestContactPermission() {
        return MPermissionUtils.checkAndRequestPermission(getContext(), Manifest.permission.READ_CONTACTS, MPermissionUtils.HERO_PERMISSION_CONTACTS);
    }

    private boolean requestCalllogPermission() {
        return MPermissionUtils.checkAndRequestPermission(getContext(), Manifest.permission.READ_CALL_LOG, MPermissionUtils.HERO_PERMISSION_CALLLOG);
    }
}