package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;

public interface IfProfileCostCalculator extends CostCalculator {
    IfSpline getCostFunc(int surfaceCat);
    IfSpline getDurationFunc(int surfaceCat);
    float getRefCosts();
    IfSplineProfileContext getContext();
}
