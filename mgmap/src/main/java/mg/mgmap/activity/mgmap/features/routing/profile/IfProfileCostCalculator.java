package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;

public interface IfProfileCostCalculator extends CostCalculator {
    IfFunction getCostFunc(int surfaceCat);
    IfFunction getDurationFunc(int surfaceCat);
}
