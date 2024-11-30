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
package mg.mgmap.activity.mgmap.features.bb;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.mgmap.view.HgtGridView;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.tilestore.MGTileStoreLayer;
import mg.mgmap.activity.mgmap.features.tilestore.TileStoreLoader;
import mg.mgmap.application.util.HgtProvider;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
//import mg.mgmap.generic.util.hints.AbstractHint;
//import mg.mgmap.generic.util.hints.NewHgtHint;
import mg.mgmap.generic.view.DialogView;
import mg.mgmap.generic.view.ExtendedTextView;

public class FSBB extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final Pref<Boolean> triggerBboxOn = new Pref<>(Boolean.FALSE);
    private final Pref<Boolean> prefBboxOn = getPref(R.string.FSBB_qc_bboxOn, false);
    private final Pref<Boolean> prefFilterOn = getPref(R.string.Statistic_pref_FilterOn, false);
    private final Pref<Boolean> prefAutoDlHgt = getPref(R.string.FSBB_pref_autoDlHgt_key, true);
    private final Pref<String> prefAutoDlHgtAsked = getPref(R.string.FSBB_pref_autoDlHgt_AskedList, "");
    private final Pref<Integer> prefBBActionLayer = getPref(R.string.FSBB_pref_bb_action_layer, 0);
    private final Pref<Boolean> prefBBLoadTransparentLayers = getPref(R.string.FSBB_pref_load_transparent_layer, false);

    private final Pref<Boolean> triggerLoadFromBB = new Pref<>(Boolean.FALSE);
    private final Pref<Boolean> prefLoadFromBBEnabled = new Pref<>(Boolean.FALSE);
    private final Pref<Boolean> triggerTSLoadRemain = new Pref<>(Boolean.FALSE);
    private final Pref<Boolean> prefTSActionsEnabled = new Pref<>(Boolean.FALSE);
    private final Pref<Boolean> triggerTSLoadAll = new Pref<>(Boolean.FALSE);
    private final Pref<Boolean> triggerTSDeleteAll = new Pref<>(Boolean.FALSE);

    private final TreeMap<String, Layer> tsAndHgtLayerEntries = identifyTsAndHgt();
    private TreeMap<String, Layer> visibleTsAndHgtLayerEntries;
    private boolean initSquare = false;
//    final private AbstractHint hgtHint;

    public FSBB(MGMapActivity mmActivity) {
        super(mmActivity);

        triggerBboxOn.addObserver( (e) -> prefBboxOn.toggle());
        triggerLoadFromBB.addObserver( (e) -> loadFromBB());
        triggerTSLoadRemain.addObserver( (e) -> action1(false, false));
        triggerTSLoadAll.addObserver( (e) -> action1(false, true));
        triggerTSDeleteAll.addObserver( (e) -> action1(true, true));

        prefBboxOn.addObserver(refreshObserver);
        prefAutoDlHgt.addObserver((e) -> {
            if (!prefAutoDlHgt.getValue()) prefAutoDlHgtAsked.setValue("");
        });
//        hgtHint = new NewHgtHint(getActivity(), getApplication().getHgtProvider());
//        hgtHint.addGotItAction(()-> getApplication().getHgtProvider().loadHgt(getActivity(), true, getApplication().getHgtProvider().getEhgtList(), null, null) );
    }

    private WriteablePointModel p1 = null;
    private WriteablePointModel p2 = null;
    private final Runnable ttHide = () -> prefBboxOn.setValue(false);
    private final Runnable ttCheckHgt = this::checkHgtAvailability;
    final long ttHideTime = 30000;
    private void refreshTTHide(){
        getTimer().removeCallbacks(ttHide);
        getTimer().postDelayed(ttHide,ttHideTime);
    }
    public void cancelTTHide(){
        getTimer().removeCallbacks(ttHide);
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        super.initQuickControl(etv,info);
        if ("group_bbox".equals(info)){
            Pref<Boolean> prefEtvBB = new Pref<>(Boolean.FALSE);
            prefEtvBB.addObserver(ev->{
                mgLog.d("prefEtvBB="+prefEtvBB.getValue());
                doRefreshResumed();
            });
            etv.setPrAction(prefEtvBB);
            etv.setData(prefBboxOn,R.drawable.group_bbox1,R.drawable.group_bbox2);
        } else if ("loadFromBB".equals(info)){
            etv.setPrAction(triggerLoadFromBB);
            etv.setData(R.drawable.load_from_bb);
            etv.setDisabledData(prefLoadFromBBEnabled, R.drawable.load_from_bb_dis);
            etv.setHelp(r(R.string.FSBB_qcLoadFromBB_Help));
        } else if ("bbox_on".equals(info)){
            etv.setPrAction(triggerBboxOn);
            etv.setData(prefBboxOn,R.drawable.bbox2,R.drawable.bbox);
            etv.setHelp(r(R.string.FSBB_qcBBox_Help)).setHelp(r(R.string.FSBB_qcBBox_Help1),r(R.string.FSBB_qcBBox_Help2));
        } else if ("TSLoadRemain".equals(info)){
            etv.setPrAction(triggerTSLoadRemain);
            etv.setData(R.drawable.bb_ts_load_remain);
            etv.setDisabledData(prefTSActionsEnabled, R.drawable.bb_ts_load_remain_dis);
            etv.setHelp(r(R.string.FSBB_qcTSLoadRemainFromBB_Help));
        }else if ("TSLoadAll".equals(info)){
            etv.setPrAction(triggerTSLoadAll);
            etv.setData(R.drawable.bb_ts_load_all);
            etv.setDisabledData(prefTSActionsEnabled, R.drawable.bb_ts_load_all_dis);
            etv.setHelp(r(R.string.FSBB_qcTSLoadAllFromBB_Help));
        }else if ("TSDeleteAll".equals(info)){
            etv.setPrAction(triggerTSDeleteAll);
            etv.setData(R.drawable.bb_ts_delete_all);
            etv.setDisabledData(prefTSActionsEnabled, R.drawable.bb_ts_delete_all_dis);
            etv.setHelp(r(R.string.FSBB_qcTSDeleteAllFromBB_Help));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefBboxOn.setValue(false);
        refreshObserver.onChange();
        getTimer().postDelayed(ttCheckHgt, 100) ;
//        getTimer().postDelayed(hgtHint, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getTimer().removeCallbacks(ttHide);
        getTimer().removeCallbacks(ttCheckHgt);
    }

    @Override
    protected void doRefreshResumedUI() {
        if (prefBboxOn.getValue()){
            if (bbcl == null){
                bbcl = new BBControlLayer();
                register(bbcl);
                initSquare = true;
                refreshObserver.onChange();
            } else if (initSquare){
                if (bbcl.initFromScreen()){
                    initSquare = false;
                } else {
                    refreshObserver.onChange();
                }
            }
        } else {
            hideBB();
        }
        prefLoadFromBBEnabled.setValue(isLoadAllowed());
        visibleTsAndHgtLayerEntries = identifyVisibleTsAndHgt();
        boolean tsOpsAllowed = isLoadAllowed() && !visibleTsAndHgtLayerEntries.isEmpty(); // allow if either one TileStore or the hgt layer is visible
        prefTSActionsEnabled.setValue(tsOpsAllowed);
    }

    public class BBControlLayer extends ControlMVLayer<WriteablePointModel> {

        private BBControlLayer(){
            changed();
        }

        @Override
        protected boolean onTap(WriteablePointModel point) {
            if (p2 == null){
                p2 = point;
                changed();
                return true;
            } else if (p1 == null){
                p1 = point;
                changed();
                return true;
            }
            return false;
        }

        @Override
        protected boolean checkDrag(PointModel pmStart) {
            double dp1 = Double.MAX_VALUE;
            double dp2 = Double.MAX_VALUE;
            // 3 Fälle:
            //   1.) p1 wird verschoben
            //   2.) p2 wird verschoben
            //   3.) noch keine Punkte gesetzt und es wird ein Fenster aufgezogen
            if (p1 != null){
                dp1 = PointModelUtil.distance(p1, pmStart);
            }
            if (p2 != null){
                dp2 = PointModelUtil.distance(p2, pmStart);
            }

            double close = getMapViewUtility().getCloseThresholdForZoomLevel();
            if ((dp1 < close) && (dp2 > close)){
                setDragObject(p1);
            }
            if ((dp2 < close) && (dp1 > close)){
                setDragObject(p2);
            }

            if ((p1 == null) && (p2 == null)){
                p1 = new WriteablePointModelImpl(pmStart);
                p2 = new WriteablePointModelImpl(pmStart);
                setDragObject(p1);
            }
            return (getDragObject() != null);
        }

        @Override
        protected void handleDrag(WriteablePointModel pmCurrent) {
            WriteablePointModel pmDrag = getDragObject();
            pmDrag.setLat(pmCurrent.getLat());
            pmDrag.setLon(pmCurrent.getLon());
            changed();
        }


        public boolean initFromScreen(){
            if (topLeftPoint == null) return false;
            Dimension dimension = getMapView().getModel().mapViewDimension.getDimension();
            if (dimension == null) {
                DisplayMetrics dm = getActivity().getResources().getDisplayMetrics();
                dimension = new Dimension(dm.widthPixels, dm.heightPixels);
            }
            double x1 = dimension.width / 3.0;
            double x2 = x1 * 2;
            double y1 = (dimension.height / 2.0) - (x1 / 2);
            double y2 = (dimension.height / 2.0) + (x1 / 2);
            p1 = new WriteablePointModelImpl(y2lat(y1),x2lon(x1));
            p2 = new WriteablePointModelImpl(y2lat(y2),x2lon(x2));
            changed();
            return true;
        }

        @Override
        protected boolean onLongPress(PointModel pm) {
            if ((p1!= null) && (p2!= null)){
                BBox bBox = new BBox().extend(p1).extend(p2);
                mgLog.i("bBox="+bBox);
                if (bBox.contains(pm)){
                    if (loadFromBB(bBox)){
                        prefBboxOn.setValue(false);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public void changed(){
        unregisterAll();
        if (p1 != null){
            register(new PointViewBB(p1));
        }
        if (p2 != null){
            register(new PointViewBB(p2));
        }
        if ((p1 != null) && (p2 != null)){
            register(new BoxViewBB(new BBox().extend(p1).extend(p2)));
        }
        refreshTTHide();
    }


    private BBControlLayer bbcl = null;

    public BBox getBBox(){
        return new BBox().extend(p1).extend(p2);
    }

    public PointModel getP1(){
        return p1;
    }
    public PointModel getP2(){
        return p2;
    }

    public void loadFromBB(){
        if ((p1!= null) && (p2!= null)){
            BBox bBox = new BBox().extend(p1).extend(p2);
            mgLog.i("bBox="+bBox);
            loadFromBB(bBox);
        }
    }

     void hideBB(){
        p1 = null;
        p2 = null;
        if (bbcl != null){
            unregisterAllControl();
        }
        bbcl = null;
        unregisterAll();
        cancelTTHide();
    }

    boolean isLoadAllowed(){
        return ((p1 != null) && (p2 != null) && (bbcl!= null));
    }

    private TreeMap<String, Layer> identifyTsAndHgt(){
        TreeMap<String, Layer> layerEntries = new TreeMap<>();
        for (Map.Entry<String, Layer> entry : getMapLayerFactory().getMapLayers().entrySet()){
            if (entry.getValue() instanceof MGTileStoreLayer mgTileStoreLayer) {
                if (mgTileStoreLayer.getMGTileStore().hasConfig()){
                    layerEntries.put(entry.getKey(), entry.getValue());
                }
            } else if (entry.getValue() instanceof HgtGridView) {
                layerEntries.put(entry.getKey(), entry.getValue());
            }
        }
        return layerEntries;
    }

    private TreeMap<String, Layer> identifyVisibleTsAndHgt() {
        TreeMap<String, Layer> visibleTsAndHgtLayerEntries = new TreeMap<>();
        for (Map.Entry<String, Layer> entry : tsAndHgtLayerEntries.entrySet()) {
            if (entry.getValue().isVisible() || prefBBLoadTransparentLayers.getValue()){
                visibleTsAndHgtLayerEntries.put(entry.getKey(), entry.getValue());
            }
        }
        return visibleTsAndHgtLayerEntries;
    }


    void checkHgtAvailability(){
        if (prefAutoDlHgt.getValue()){
            for (Map.Entry<String, Layer> entry : getMapLayerFactory().getMapLayers().entrySet()){
                if ((entry.getValue() instanceof TileRendererLayer) && (!prefAutoDlHgtAsked.getValue().contains("\"" + entry.getKey() + "\""))){
                    prefAutoDlHgtAsked.setValue(prefAutoDlHgtAsked.getValue()+" \""+entry.getKey()+"\"");
                    BBox bBox = BBox.fromBoundingBox(((TileRendererLayer)entry.getValue()).getMapDataStore().boundingBox());
                    mgLog.i("layer="+entry.getKey()+" bbox="+bBox);
                    loadHgt(bBox, false, entry.getKey(), null);
                    break; // cannot handle multiple layers at the same time
                }
            }
        }
    }

    private void action1(boolean bDrop, boolean bAll) {
        if (visibleTsAndHgtLayerEntries.size() >= 2){
            Context context = getActivity();
            DialogView dialogView = getActivity().findViewById(R.id.dialog_parent);

            RadioGroup radioGroup = new RadioGroup(context);
            ArrayList<Integer> checkedList = new ArrayList<>();
            for (String layerName : visibleTsAndHgtLayerEntries.keySet()) {
                RadioButton rb = new RadioButton(context);
                rb.setText(layerName);
                rb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                rb.setId(layerName.hashCode());
                rb.setPadding(ControlView.dp(10), ControlView.dp(10), ControlView.dp(10), ControlView.dp(10));
                radioGroup.addView(rb);
            }
            radioGroup.check(prefBBActionLayer.getValue());
            checkedList.add(prefBBActionLayer.getValue());
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> checkedList.add(0, checkedId));

            dialogView.lock(() -> dialogView
                    .setTitle("Select Layer")
                    .setContentView(radioGroup)
                    .setPositive("OK", evt -> {
                        prefBBActionLayer.setValue(checkedList.get(0));
                        for (Map.Entry<String, Layer> entry : tsAndHgtLayerEntries.entrySet()){
                            if (entry.getKey().hashCode() == checkedList.get(0)){
                                action2(entry.getValue(), bDrop, bAll);
                            }
                        }
                    })
                    .setNegative("Cancel", null)
                    .show());
        } else if (visibleTsAndHgtLayerEntries.size() == 1){
            // noinspection ConstantConditions
            action2(visibleTsAndHgtLayerEntries.firstEntry().getValue(), bDrop, bAll);
        }
    }

    private void action2(Layer layer, boolean bDrop, boolean bAll){
        try {
            if (layer instanceof MGTileStoreLayer tsLayer){
                TileStoreLoader tileStoreLoader = new TileStoreLoader(getActivity(), getApplication(), tsLayer.getMGTileStore(), getTimer());
                if (bDrop){
                    tileStoreLoader.dropFromBB(getBBox());
                } else {
                    tileStoreLoader.loadFromBB(getBBox(), bAll);
                }
            } else if (layer instanceof HgtGridView hgtLayer){
                if (bDrop){
                    dropHgt(getBBox(), hgtLayer);
                } else {
                    loadHgt(getBBox(),bAll, null, hgtLayer);
                }
            }
        } catch (Exception e) {
            mgLog.e(e);
        }

    }

    public boolean loadFromBB(BBox bBox2Load){
        boolean changed = false;
        boolean filtered = false;
        BBox bBox2show = new BBox();
        if (bBox2Load != null){
            for (TrackLog aTrackLog : new ArrayList<>( getApplication().metaTrackLogs.values() )){
                if (aTrackLog.isFilterMatched() || !prefFilterOn.getValue()){
                    if (getApplication().getMetaDataUtil().checkLaLoRecords(aTrackLog, bBox2Load)){
                        getApplication().availableTrackLogsObservable.availableTrackLogs.add(aTrackLog);
                        bBox2show.extend(aTrackLog.getBBox());
                        changed = true;
                    }
                } else {
                    filtered = true;
                }
            }
            if (changed){
                getApplication().availableTrackLogsObservable.changed();
            }
            if (filtered){
                Toast.makeText(getActivity(), "Warning: Some Tracklogs are filtered!", Toast.LENGTH_SHORT).show();
            }
        }
        return changed;
    }

    private void loadHgt(BBox bBox, boolean all, String layerName, HgtGridView hgtLayer){
        ArrayList<String> hgtNames = new ArrayList<>();
        for (int latitude = PointModelUtil.getLower(bBox.minLatitude); latitude<PointModelUtil.getLower(bBox.maxLatitude)+1; latitude++ ) {
            for (int longitude = PointModelUtil.getLower(bBox.minLongitude); longitude < PointModelUtil.getLower(bBox.maxLongitude) + 1; longitude++) {
                String hgtName = HgtProvider.getHgtName(latitude, longitude);
                hgtNames.add(hgtName);
            }
        }
        getApplication().getHgtProvider().loadHgt(getActivity(), all, hgtNames, layerName, hgtLayer);
    }

    private void dropHgt(BBox bBox, HgtGridView hgtLayer){
        ArrayList<String> hgtNames = new ArrayList<>();
        for (int latitude = PointModelUtil.getLower(bBox.minLatitude); latitude<PointModelUtil.getLower(bBox.maxLatitude)+1; latitude++ ) {
            for (int longitude = PointModelUtil.getLower(bBox.minLongitude); longitude < PointModelUtil.getLower(bBox.maxLongitude) + 1; longitude++) {
                String hgtName = HgtProvider.getHgtName(latitude, longitude);
                hgtNames.add(hgtName);
            }
        }
        getApplication().getHgtProvider().dropHgt(getActivity(), hgtNames, hgtLayer);
    }


}
