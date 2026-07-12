package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class Hiking_Sp extends RoutingProfile {

    public Hiking_Sp( ) {
        super(new CostCalcSplineProfile(new SplineProfileContextHiking(true,-1)));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineHiking(wayAttributs,(CostCalcSplineProfile) profileCalculator);
    }
    @Override
    public int getIconIdActive() {
        return R.drawable.rp_hiking_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_hiking_i;
    }

    @Override
    protected int getIconIdCalculating() {
        return R.drawable.rp_hiking_c;
    }
}
