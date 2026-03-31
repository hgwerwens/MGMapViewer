package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.activity.mgmap.features.routing.profile.IfSplineProfileContext;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB_old;

public class SplineProfileTestContextMTB  {

    public interface Factory {
        IfSplineProfileContext create(float powerCost, float powerDur, int sUp, int sDn, boolean checkAll);
    }

    public static Factory factory = SplineProfileContextMTB::new;

    public static IfSplineProfileContext get(float powerCost, float powerDur, int sUp, int sDn, boolean checkAll){
        return factory.create(powerCost,powerDur, sUp, sDn, checkAll);
    }

    public static IfSplineProfileContext  get(int sUp, int sDn, boolean checkAll) {
        return get(-1,-1, sUp, sDn, checkAll);
    }

    public static IfSplineProfileContext     get( int sUp, int sDn) {
        return get( sUp, sDn,true);
    }



}
