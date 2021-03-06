/**
 * BSD License
 * Copyright (c) Hero software.
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.

 * Neither the name Facebook nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific
 * prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.hero;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.hero.depandency.IImagePickHandler;
import com.hero.depandency.MPermissionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HeroActivity extends HeroFragmentActivity {
    public static final int RESULT_CODE_DISMISS = -1009;
    public static final int REQUEST_CODE_CAMERA = 8001;
    public static final int REQUEST_CODE_GALLERY = 8002;
    public static final int REQUEST_CODE_PHOTO_CROP = 8003;
    public static final int REQUEST_CODE_PICK_CONTACT = 8100;
    public static final int REQUEST_IMAGE = 2000;
    private static int autoGenerateRequestCode = 1000;

    public static final boolean SHOW_ACTIVITY_ANIM = true;

    HeroActivity self = this;
    JSONArray mRightItems;
    JSONObject mActionDatas;
    boolean shouldSendViewWillAppear;
    private HeroFragment mainFragment;
    private ProgressDialog progressDialog;
    private IHero resultHandlerView;

    protected ActivityResultCallback callback;

    public static int getAutoGenerateRequestCode() {
        return autoGenerateRequestCode++;
    }

    public void setCallback(ActivityResultCallback callback)
    {
        this.callback=callback;
    }

    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager connManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo.State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
            return true;
        }

        state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initContent();
    }

    protected void initContent() {
        mainFragment = new HeroFragment();
        mainFragment.setArguments(getIntent().getExtras());
        setContentView(R.layout.base_activity);
        if (mainFragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.layoutRoot, mainFragment).commit();
        }
        if (getActionBar() != null) {
            getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        }
    }

    @Override
    protected void onDestroy() {
        shouldSendViewWillAppear = true;
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        shouldSendViewWillAppear = true;
    }

    @Override
    protected void onPause() {
        shouldSendViewWillAppear = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        JSONObject leftItem = getCurrentFragment().getLeftItem();
        if (leftItem != null && leftItem.has("click")) {
            try {
                JSONObject click = leftItem.getJSONObject("click");
                self.on(click);
                return;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GALLERY || requestCode == REQUEST_CODE_CAMERA || requestCode == REQUEST_CODE_PHOTO_CROP) {
            if (resultCode == RESULT_OK) {
                JSONObject item = new JSONObject();
                try {
                    item.put("imagePicked", requestCode);
                    if (requestCode == REQUEST_CODE_GALLERY) {
                        if (data != null) {
                            Uri uri = data.getData();
                            item.put("imagePath", getPathFromURI(HeroActivity.this, uri));
                        } else {
                            resultHandlerView = null;
                        }
                    }
                    if (resultHandlerView != null) {
                        resultHandlerView.on(item);
                    }
                    // if back from camera, do not clear the for for cropping
//                    if (requestCode != REQUEST_CODE_CAMERA) {
                        resultHandlerView = null;
//                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return;
        } else if (REQUEST_CODE_PICK_CONTACT == requestCode) {
            if (resultCode == RESULT_OK) {
                handleContactPick(data);
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleContactPick(Intent data) {
        String phoneNumber = null, name = null;
        boolean isDenied = false;

        if (data != null) {
            Uri uri = data.getData();
            if (!MPermissionUtils.isPermissionGranted(this, Manifest.permission.READ_CONTACTS)) {
                isDenied = true;
            } else if (uri != null) {
                Cursor c = null;
                Cursor phone = null;
                try {
                    c = getContentResolver().query(uri, new String[] {BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME,}, null, null, null);
                    if (c != null && c.moveToFirst()) {
                        int id = c.getInt(0);
                        name = c.getString(1);
                        phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id, null, null);
                        if (phone != null) {
                            while (phone.moveToNext()) {
                                String num = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                phoneNumber = num;
                            }
                            phone.close();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    isDenied = true;
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
            if (phoneNumber != null) {
                JSONObject item = new JSONObject();
                try {
                    item.put("contactName", name == null ? "" : name);
                    item.put("contactNumber", phoneNumber);
                    if (resultHandlerView != null) {
                        resultHandlerView.on(item);
                        resultHandlerView = null;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                // permission denied or no phone number
                JSONObject item = new JSONObject();
                try {
                    item.put("contactName", name == null ? "" : name);
                    item.put("contactNumber", "");
                    if (isDenied) {
                        item.put("error", "denied");
                    }
                    if (resultHandlerView != null) {
                        resultHandlerView.on(item);
                        resultHandlerView = null;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getPathFromURI(Context context, Uri uri) {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    public HeroFragment getCurrentFragment() {
        return mainFragment;
    }

    @Override
    public boolean isActionBarShown() {
        return false;
    }

    @Override
    public void setRightItems(JSONArray array) {
        mRightItems = array;
    }

    public void startActivityByView(Intent intent, int requestCode, IHero view) {
        resultHandlerView = view;
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public interface ActivityResultCallback{
        void onResult(Object data);
    }

    public static void activitySwitchAnimation(Activity activity, int startAnim, int exitAnim) {
        if (SHOW_ACTIVITY_ANIM) {
            activity.overridePendingTransition(startAnim, exitAnim);
        }
    }

    @Override
    public void finish() {
        super.finish();
        activitySwitchAnimation(this, R.anim.activity_still, R.anim.activity_slide_out);
    }
}
