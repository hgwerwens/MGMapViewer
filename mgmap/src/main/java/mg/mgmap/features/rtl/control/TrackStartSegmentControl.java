package mg.mgmap.features.rtl.control;

import android.view.View;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.util.Control;
import mg.mgmap.util.MGPref;

public class TrackStartSegmentControl extends Control {

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.FSPosition_prev_GpsOn, false);

    public TrackStartSegmentControl(){
        super(true);
    }

    public void onClick(View view) {
        super.onClick(view);
        MGMapApplication application = controlView.getApplication();
        MGMapActivity activity = controlView.getActivity();

        long timestamp = System.currentTimeMillis();
        RecordingTrackLog rtl = application.recordingTrackLogObservable.getTrackLog();
        rtl.startSegment(timestamp);
        prefGps.setValue(true);
        activity.triggerTrackLoggerService();
    }

    @Override
    public void onPrepare(View v) {
        final RecordingTrackLog rtl = controlView.getApplication().recordingTrackLogObservable.getTrackLog();
        v.setEnabled( (rtl != null) && (rtl.isTrackRecording()) && (!rtl.isSegmentRecording()) );
        setText(v, controlView.rstring(R.string.btStartSegment) );
    }

}