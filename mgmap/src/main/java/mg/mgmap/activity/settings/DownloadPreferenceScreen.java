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
package mg.mgmap.activity.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.preference.Preference;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import mg.mgmap.BuildConfig;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Zipper;

public class DownloadPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.download_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        setBrowseIntent(R.string.preferences_dl_maps_wd_key, R.string.url_oam_dl);
        setBrowseIntent(R.string.preferences_dl_maps_eu_key, R.string.url_oam_dl_eu);
        setBrowseIntent(R.string.preferences_dl_maps_de_key, R.string.url_oam_dl_de);

        setBrowseIntent(R.string.preferences_dl_theme_el_key, R.string.url_oam_th_el);

        setBrowseIntent(R.string.preferences_dl_sw_other_key, R.string.url_github_apk_dir);
        setSWLatestOCL();
    }


    private void setSWLatestOCL(){
        Preference prefSwLatest = findPreference( getResources().getString(R.string.preferences_dl_sw_latest_key) );
        prefSwLatest.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Context context = getContext().getApplicationContext();
                if (context instanceof MGMapApplication) {
                    MGMapApplication application = (MGMapApplication) context;

                    ArrayList<BgJob> jobs = new ArrayList(){ };
                    BgJob job = new BgJob() {
                        @Override
                        protected void doJob() throws Exception {
                            super.doJob();
                            Zipper zipper = new Zipper(null);
                            String urlString = getResources().getString(R.string.url_github_apk_latest)+((BuildConfig.DEBUG)?"debug":"release")+"/apk.zip";
                            URL url = new URL(urlString);
                            PersistenceManager persistenceManager = application.getPersistenceManager();
                            persistenceManager.cleanApkDir();
                            zipper.unpack(url, persistenceManager.getApkDir(), null, this);

                            File file = persistenceManager.getApkFile();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

                            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                            intent.setDataAndType(uri, "application/vnd.android.package-archive");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        }
                    };
                    jobs.add(job);
                    application.addBgJobs(jobs);
                } else {
                    Log.e(MGMapApplication.LABEL, NameUtil.context()+" failed to add job");
                }
                return true;
            }
        });
    }

}