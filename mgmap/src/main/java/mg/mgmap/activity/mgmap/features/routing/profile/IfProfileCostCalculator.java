package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSpline;

public interface IfProfileCostCalculator extends CostCalculator {
    IfSpline getCostFunc(int surfaceCat);
    IfSpline getDurationFunc(int surfaceCat);
    float getRefCosts();
    IfSplineProfileContext getContext();
}
