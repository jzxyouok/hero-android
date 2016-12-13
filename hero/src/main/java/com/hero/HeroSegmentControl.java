package com.hero;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liuguoping on 15/9/24.
 */
public class HeroSegmentControl extends RadioGroup implements IHero {
    HeroSegmentControl self = this;
    JSONArray actions;
    private int currentSelectedId;
    private RadioGroup.LayoutParams layoutParamsChild;
    private RadioGroup.OnCheckedChangeListener checkListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            currentSelectedId = checkedId;
            sendData(currentSelectedId);
        }
    };

    public HeroSegmentControl(Context context) {
        super(context);
        this.setOrientation(HORIZONTAL);
        currentSelectedId = 0;
        //        this.setBackgroundResource(R.drawable.segmentcontrol_bg);

        //        this.setShowDividers(SHOW_DIVIDER_BEGINNING | SHOW_DIVIDER_MIDDLE);
        //        this.setDividerDrawable(new ColorDrawable(Color.DKGRAY));
        layoutParamsChild = new RadioGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1.0f);
    }

    @Override
    public void on(JSONObject jsonObject) throws JSONException {
        HeroView.on(this, jsonObject);
        int color = 0;
        if (jsonObject.has("selectedSegmentIndex")) {
            currentSelectedId = jsonObject.getInt("selectedSegmentIndex");
        }
        if (jsonObject.has("tinyColor")) {
            color = HeroView.parseColor("#" + jsonObject.getString("tinyColor"));
        }
        if (jsonObject.has("dataSource")) {
            JSONArray items = jsonObject.getJSONArray("dataSource");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                RadioButton button = new RadioButton(this.getContext());
                button.setButtonDrawable(android.R.color.transparent);
                button.setBackgroundResource(R.drawable.segmentcontrol_button);
                button.setLayoutParams(layoutParamsChild);
                button.setGravity(Gravity.CENTER);
                if (color != 0) {
                    button.setTextColor(color);
                }
                button.setText(item.getString("title"));
                this.addView(button);

                if (currentSelectedId == i) {
                    button.setChecked(true);
                }
            }

            if (jsonObject.has("action")) {
                actions = jsonObject.getJSONArray("action");
                sendData(currentSelectedId);
            }
        }

        this.setOnCheckedChangeListener(checkListener);
    }

    public int getSelectedIndex() {
        return currentSelectedId;
    }

    public void setSelectedIndex(int index) {
        if (this.getChildAt(index) != null) {
            RadioButton button = (RadioButton) this.getChildAt(index);
            button.setChecked(true);
        }
    }

    private void sendData(int index) {
        if (actions != null && index < actions.length()) {
            JSONObject action = null;
            try {
                action = (JSONObject) (actions.get(index));
                if (action != null) {
                    ((IHeroContext) getContext()).on(action);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
