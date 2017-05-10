package jp.yhonda;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.*;
import android.content.SharedPreferences;
import android.content.SharedPreferences.*;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.*;

/*
    Copyright 2012, 2013, 2014, 2015, 2016, 2017 Yasuaki Honda (yasuaki.honda@gmail.com)
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
 */

public final class MoAPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        List<String> list = Arrays.asList("auto_completion_check_box_pref", "manURL", "fontSize1", "fontSize2");
        for(String key : list){
            AppGlobals.getSingleton().set(key,"false");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary(key);
        AppGlobals.getSingleton().set(key,"true");
    }

    private void updatePreferenceSummary(String key) {
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);
        Preference pref = findPreference(key);

        if (pref.getClass().equals(CheckBoxPreference.class)) {
            CheckBoxPreference checkbox_preference = (CheckBoxPreference)pref;
            if (checkbox_preference.isChecked()) {
                checkbox_preference.setSummary("Yes");
            } else {
                checkbox_preference.setSummary("No");
            }
        } else if (pref.getClass().equals(ListPreference.class)) {
            ListPreference list_preference = (ListPreference)pref;
            list_preference.setSummary(list_preference.getEntry());
        }

    }
}
