package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfileMTB;
import mg.mgmap.generic.util.basic.MGLog;

public class RoutingMTBProfileCreateTest {
    @Test
    public void VaryContext_default(){
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        for (int sUp = 100; sUp <= 300; sUp = sUp + 100) {
            for (int sDn = 100; sDn <= 300; sDn = sDn + 100) {
                new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(sUp, sDn));
            }
        }
    }
    @Test
    public void VaryContext_wide(){
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        for (int sUp = 0; sUp <= 400; sUp = sUp + 100) {
            for (int sDn = 0; sDn <= 400; sDn = sDn + 100) {
                for ( int power = 48 + sUp/100*25; power <= 150f; power = power + 25 ) {
                    new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(power,sUp, sDn));
                };
            }
        }
    }



}
