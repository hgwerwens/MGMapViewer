/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.features.motion;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;


import java.util.Observable;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.model.MultiPointModelImpl;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.MultiPointView;
import mg.mapviewer.view.PrefTextView;

public class MSBeeline extends MGMicroService {

    public static final Paint PAINT_BLACK_STROKE = CC.getStrokePaint(R.color.BLACK, 2);

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.MSPosition_prev_GpsOn, false);
    private PrefTextView ptvCenter = null;
    private PrefTextView ptvZoom = null;

    public MSBeeline(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    public PrefTextView initStatusLine(PrefTextView ptv, String info) {
        if (info.equals("center")){
            ptv.setPrefData(null, new int[]{R.drawable.distance});
            ptv.setFormat(Formatter.FormatType.FORMAT_DISTANCE);
            ptvCenter = ptv;
        }
        if (info.equals("zoom")){
            ptv.setPrefData(null, new int[]{R.drawable.zoom});
            ptv.setFormat(Formatter.FormatType.FORMAT_INT);
            ptvZoom = ptv;
        }
        return ptv;
    }

    @Override
    protected void start() {
        getMapView().getModel().mapViewPosition.addObserver(refreshObserver);
        getApplication().lastPositionsObservable.addObserver(refreshObserver);

    }



    @Override
    @SuppressWarnings("EmptyCatchBlock")
    protected void stop() {
        getMapView().getModel().mapViewPosition.removeObserver(refreshObserver);
        getApplication().lastPositionsObservable.deleteObserver(refreshObserver);
    }

    @Override
    protected void onUpdate(Observable o, Object arg) {
        ttRefreshTime = 150; // avoid refresh faster than MSPosition
    }

    @Override
    protected void doRefresh() {
        ttRefreshTime = 10;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PointModel lp = getApplication().lastPositionsObservable.lastGpsPoint;
                if (prefGps.getValue() && (lp != null)){
                    showHidePositionToCenter(lp);
                } else {
                    showHidePositionToCenter(null);
                }
//                getControlView().updateTvZoom(getMapView().getModel().mapViewPosition.getZoomLevel());
                getControlView().setStatusLineValue(ptvZoom, (int) (getMapView().getModel().mapViewPosition.getZoomLevel()));
            }
        });
    }

    private void showHidePositionToCenter(PointModel pm){
        if (msLayers.isEmpty() && (pm == null)) return; // default is fast
        unregisterAll();
        LatLong center = getMapView().getModel().mapViewPosition.getCenter();
        PointModel pmCenter = new PointModelImpl(center);
        boolean showNewValue = (pm != null);
        double distance = 0;
        if (showNewValue){
            distance = PointModelUtil.distance(pm, pmCenter);
            showNewValue &= (distance > 10.0); //m
        }
        if (showNewValue){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" pm="+pm+" pmCenter="+pmCenter);
            MultiPointModelImpl mpm = new MultiPointModelImpl();
            mpm.addPoint(pmCenter);
            mpm.addPoint(pm);
            register( new MultiPointView(mpm, PAINT_BLACK_STROKE));
        } else {
            distance = 0;
        }
        getControlView().setStatusLineValue(ptvCenter, distance);
    }

}