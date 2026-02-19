package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.activity.mgmap.features.routing.profile.IfSplineProfileContext;
import mg.mgmap.activity.mgmap.features.routing.profile.IfMTBContextDetails;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB;

public class SplineProfileTestContextMTB  {

    public interface Factory {
        IfSplineProfileContext create(int power, int sUp, int sDn, boolean checkAll, boolean withRef);
    }

    public static Factory factory = SplineProfileContextMTB::new;

    private static IfSplineProfileContext get(int power, int sUp, int sDn, boolean checkAll, boolean withRef){
        return factory.create(power, sUp, sDn, checkAll, withRef);
    }

    public static IfSplineProfileContext     get(int power, int sUp, int sDn, boolean checkAll) {
        return get(power, sUp, sDn, checkAll, true);
    }

    public static IfSplineProfileContext     get(int power, int sUp, int sDn) {
        return get(power, sUp, sDn);
    }

    public static IfSplineProfileContext  get(int sUp, int sDn, boolean checkAll) {
        return get((int) (47.5 + 25 * sUp / 100d), sUp, sDn, checkAll, true);
    }

    public static IfSplineProfileContext     get( int sUp, int sDn) {
        return get( sUp, sDn,true);
    }



}
