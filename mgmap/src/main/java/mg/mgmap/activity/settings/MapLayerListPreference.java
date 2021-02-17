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
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.ListPreference;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.MGMapLayerFactory;
import mg.mgmap.generic.util.basic.NameUtil;

public class MapLayerListPreference extends ListPreference {

    private final HashMap<MGMapLayerFactory.Types, FilenameFilter> filters = new HashMap<>();

    public MapLayerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFilters();
        this.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        String[] maps = getAvailableMapLayers(context);
        setEntries(maps);
        setEntryValues(maps);
        setDefaultValue(maps[0]);
    }

    private void initFilters(){
        filters.put(MGMapLayerFactory.Types.MAPSFORGE, (dir, name) -> {
            boolean res = !(new File(dir,name).isDirectory());
            res &= name.endsWith(".map") || name.endsWith(".ref");
            return res;
        });
        filters.put(MGMapLayerFactory.Types.MAPSTORES, (dir, name) -> (new File(dir,name).isDirectory()));
        filters.put(MGMapLayerFactory.Types.MAPONLINE, (dir, name) -> {
            File fStore = new File(dir,name);
            File fConfig = new File(fStore,MGMapLayerFactory.XML_CONFIG_NAME);
            return (fStore.isDirectory() && fConfig.exists());
        });
        FilenameFilter prop = (dir, name) -> {
            boolean res = !(new File(dir,name).isDirectory());
            res &= name.endsWith(".properties");
            return res;
        };
        filters.put(MGMapLayerFactory.Types.MAPGRID,prop);
    }

    /** Returns a list of available map layers */
    public String[] getAvailableMapLayers(Context context){
        String[] resa = new String[]{"none"};
        if (context.getApplicationContext() instanceof MGMapApplication) {
            MGMapApplication application = (MGMapApplication) context.getApplicationContext();
            File mapsDir = application.getPersistenceManager().getMapsDir();
            ArrayList<String> res = new ArrayList<>();
            res.add(resa[0]);
            for (MGMapLayerFactory.Types type: MGMapLayerFactory.Types.values()){
                File typeDir = new File(mapsDir, type.name().toLowerCase());
                String[] entries = typeDir.list(filters.get(type));
                Arrays.sort(entries);
                for (String entry : entries){
                    res.add(type+": "+entry);
                }
            }
            resa = res.toArray(resa);
        }
        return resa;
    }



    @Override
    protected void onClick() {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" key="+getKey()+" value="+getValue());
        super.onClick();
    }
}
