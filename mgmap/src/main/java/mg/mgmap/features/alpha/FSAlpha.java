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
package mg.mgmap.features.alpha;

import java.util.Observer;

import mg.mgmap.MGMapActivity;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.util.MGPref;
import mg.mgmap.view.ExtendedTextView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class FSAlpha extends FeatureService {

    private final MGPref<Boolean> prefAlpha = MGPref.get(R.string.Layers_qc_showAlphaSlider, false);
    private final MGPref<Boolean> prefAlpha2 = MGPref.get(R.string.Layers_qc_showAlphaSlider2, false);

    private final MGPref<Boolean> prefStlVisibility = MGPref.get(R.string.FSATL_pref_STL_visibility, false);
    private final MGPref<Boolean> prefAtlVisibility = MGPref.get(R.string.FSATL_pref_ATL_visibility, false);
    private final MGPref<Boolean> prefMtlVisibility = MGPref.get(R.string.FSMarker_pref_MTL_visibility, false);
    private final MGPref<Boolean> prefRtlVisibility = MGPref.get(R.string.FSRecording_pref_RTL_visibility, false);
    private final MGPref<Boolean> prefSliderTracksEnabled = MGPref.anonymous(false);

    public FSAlpha(MGMapActivity activity){
        super(activity);

        Observer  prefSliderTracksObserver =
                (o,args)-> prefSliderTracksEnabled.setValue( prefStlVisibility.getValue() || prefAtlVisibility.getValue() || prefMtlVisibility.getValue() || prefRtlVisibility.getValue() );
        prefStlVisibility.addObserver(prefSliderTracksObserver);
        prefAtlVisibility.addObserver(prefSliderTracksObserver);
        prefMtlVisibility.addObserver(prefSliderTracksObserver);
        prefRtlVisibility.addObserver(prefSliderTracksObserver);
        prefSliderTracksObserver.update(null, null);

        prefAlpha.addObserver(refreshObserver);
        prefAlpha2.addObserver(refreshObserver);
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        super.initQuickControl(etv,info);
        if ("alpha_layers".equals(info)){
            etv.setPrAction(prefAlpha);
            etv.setData(prefAlpha,R.drawable.slider_layer2,R.drawable.slider_layer1);
            etv.setHelp(r(R.string.FSAlpha_qcAlphaLayers_Help)).setHelp(r(R.string.FSAlpha_qcAlphaLayers_Help1),r(R.string.FSAlpha_qcAlphaLayers_Help2));
        } else if ("alpha_tracks".equals(info)){
            etv.setPrAction(prefAlpha2);
            etv.setData(prefAlpha2,R.drawable.slider_track2,R.drawable.slider_track1);
            etv.setDisabledData(prefSliderTracksEnabled, R.drawable.slider_track_dis);
            etv.setHelp(r(R.string.FSAlpha_qcAlphaTracks_Help)).setHelp(r(R.string.FSAlpha_qcAlphaTracks_Help1),r(R.string.FSAlpha_qcAlphaTracks_Help2));
        }
        setSliderVisibility();
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefAlpha.setValue(false);
        prefAlpha2.setValue(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void doRefreshResumedUI() {
        setSliderVisibility();
    }

    private void setSliderVisibility(){
        int visibility = prefAlpha.getValue()?VISIBLE:INVISIBLE;
        int visibility2 = prefAlpha2.getValue()?VISIBLE:INVISIBLE;
        if ((visibility == VISIBLE) && (visibility2 == VISIBLE)){
            if (refreshObserver.last == prefAlpha2){
                prefAlpha.setValue(false);
            } else {
                prefAlpha2.setValue(false);
            }
        } else {
            getActivity().findViewById(R.id.bars).setVisibility(visibility);
            getActivity().findViewById(R.id.bars2).setVisibility(visibility2);
        }
        getControlView().reworkLabeledSliderVisibility();
    }
}