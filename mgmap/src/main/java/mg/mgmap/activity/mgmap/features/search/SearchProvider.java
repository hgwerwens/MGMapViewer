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
package mg.mgmap.activity.mgmap.features.search;

import android.content.SharedPreferences;

import mg.mgmap.activity.mgmap.MGMapActivity;

public abstract class SearchProvider {

    protected MGMapActivity activity = null;
    protected SearchView searchView = null;
    protected FSSearch fsSearch = null;
    protected SharedPreferences preferences = null;

    protected void init(MGMapActivity activity, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences){
        this.activity = activity;
        this.fsSearch = fsSearch;
        this.searchView = searchView;
        this.preferences = preferences;
    }

    public abstract void doSearch(SearchRequest searchRequest);
}
