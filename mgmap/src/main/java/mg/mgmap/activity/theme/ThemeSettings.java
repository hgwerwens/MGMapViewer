/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.theme;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.HomeObserver;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.util.basic.TopExceptionHandler;

public class ThemeSettings extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    MGMapApplication application = null;
    ListPreference baseLayerPreference;
    SharedPreferences prefs;
    XmlRenderThemeStyleMenu renderthemeOptions;
    PreferenceCategory renderthemeMenu;
    ThemeSettingsFragment themeSettingsFragment;

    ArrayList<String> themePreferenceKeys = new ArrayList<>(); // store in this list all preference keys used by the renderThemeMenu

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key) {
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " key="+key+" value="+ preferences.getAll().get(key).toString());
        if (this.renderthemeOptions != null && this.renderthemeOptions.getId().equals(key)) {
            createRenderthemeMenu();
        }
        if (themePreferenceKeys.contains(key)){
            preferences.edit().putString(getResources().getString(R.string.preference_theme_changed), UUID.randomUUID().toString()).apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w(MGMapApplication.LABEL, NameUtil.context());
        application = (MGMapApplication) getApplication();
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(application.getPersistenceManager()));
        super.onCreate(savedInstanceState);
//        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.theme_settings_activity);
        themeSettingsFragment = new ThemeSettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.themesettings, themeSettingsFragment)
                .commit();

        // if the render theme has a style menu, its data is delivered via the intent
        renderthemeOptions = (XmlRenderThemeStyleMenu) getIntent().getSerializableExtra(getResources().getString(R.string.my_rendertheme_menu_key));

        ViewGroup qcs = findViewById(R.id.tsa_qc);
        ControlView.createQuickControlETV(qcs)
                .setData(R.drawable.back)
                .setOnClickListener(createBackOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(R.drawable.home)
                .setOnClickListener(createHomeOCL());
    }
    private View.OnClickListener createBackOCL(){
        return v -> ThemeSettings.this.onBackPressed();
    }
    private View.OnClickListener createHomeOCL() {
        return v -> HomeObserver.launchHomeScreen(this);
    }

    public static class ThemeSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.mypreferences, rootKey);
        }
        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), RecyclerView.VERTICAL);
            recyclerView.addItemDecoration(itemDecoration);
            return recyclerView;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.prefs.registerOnSharedPreferenceChangeListener(this);
        boolean fullscreen = this.prefs.getBoolean(getResources().getString(R.string.FSControl_qcFullscreenOn), true);
        FullscreenUtil.enforceState(this, fullscreen);
        if (renderthemeOptions != null) {
            // the preference category is hard-wired into this app and serves as
            // the hook to add a list preference to allow users to select a style
            renderthemeMenu = (PreferenceCategory) themeSettingsFragment.findPreference(getResources().getString(R.string.my_rendertheme_menu_key));
            createRenderthemeMenu();
        }
    }


    private void createRenderthemeMenu() {
        this.renderthemeMenu.removeAll();

        this.baseLayerPreference = new ListPreference(this);

        // the id of the setting is the id of the stylemenu, that allows this
        // app to store different settings for different render themes.
        baseLayerPreference.setKey(this.renderthemeOptions.getId());

        // this is the user language for the app, in 'en', 'de' etc format
        // no dialects are supported at the moment
        String language = Locale.getDefault().getLanguage();
        if (prefs != null){
            String l1 = prefs.getString(getResources().getString(R.string.preferences_language_key), null);
            if (l1 != null){
                language = l1;
            }
        }


        // build data structure for the ListPreference
        Map<String, XmlRenderThemeStyleLayer> baseLayers = renderthemeOptions.getLayers();

        int visibleStyles = 0;
        for (XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
            if (baseLayer.isVisible()) {
                ++visibleStyles;
            }
        }

        CharSequence[] entries = new CharSequence[visibleStyles];
        CharSequence[] values = new CharSequence[visibleStyles];
        int i = 0;
        for (XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
            if (baseLayer.isVisible()) {
                // build up the entries in the list
                entries[i] = baseLayer.getTitle(language);
                values[i] = baseLayer.getId();
                ++i;
            }
        }

        baseLayerPreference.setEntries(entries);
        baseLayerPreference.setEntryValues(values);
        baseLayerPreference.setEnabled(true);
        baseLayerPreference.setPersistent(true);
        baseLayerPreference.setDefaultValue(renderthemeOptions.getDefaultValue());
        baseLayerPreference.setIconSpaceReserved(false);

        renderthemeMenu.addPreference(baseLayerPreference);
        themePreferenceKeys.add(renderthemeOptions.getId());

        String selection = baseLayerPreference.getValue();
        // need to check that the selection stored is actually a valid getLayer in the current
        // rendertheme.
        if (selection == null || !renderthemeOptions.getLayers().containsKey(selection)) {
            selection = renderthemeOptions.getLayer(renderthemeOptions.getDefaultValue()).getId();
        }
        // the new Android style is to display information here, not instruction
        baseLayerPreference.setSummary(renderthemeOptions.getLayer(selection).getTitle(language));

        for (XmlRenderThemeStyleLayer overlay : this.renderthemeOptions.getLayer(selection).getOverlays()) {
            CheckBoxPreference checkbox = new CheckBoxPreference(this);
            checkbox.setKey(overlay.getId());
            checkbox.setPersistent(true);
            checkbox.setTitle(overlay.getTitle(language));
            checkbox.setIconSpaceReserved(false);

            if (themeSettingsFragment.findPreference(overlay.getId()) == null) {
                // value has never been set, so set from default
                checkbox.setChecked(overlay.isEnabled());
            }
            this.renderthemeMenu.addPreference(checkbox);
            themePreferenceKeys.add(overlay.getId());
        }
        this.renderthemeMenu.addPreference(new Preference(this));
    }
}
