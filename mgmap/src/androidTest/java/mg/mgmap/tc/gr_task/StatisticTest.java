package mg.mgmap.tc.gr_task;

import android.graphics.Point;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.statistic.TrackStatisticActivity;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class StatisticTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public StatisticTest() {
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule = new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 35000)
    public void _01_statistic_test() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814, 13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_statistic.*");
        animateToViewAndClick(R.id.menu_task);
        animateToViewAndClick(R.id.mi_statistic);
        waitForActivity(TrackStatisticActivity.class);

        animateSwipeToPos(new Point(500,1000), new Point(500,300));
        animateSwipeToPos(new Point(500,1000), new Point(500,300));

        animateToStatAndClick(".*20220712_104649.*");
        animateToStatAndClick(".*20221012_141638.*");
        animateToStatAndClick(".*20221029_122839.*");

        SystemClock.sleep(5000);
        mgLog.i("finished");

    }
}