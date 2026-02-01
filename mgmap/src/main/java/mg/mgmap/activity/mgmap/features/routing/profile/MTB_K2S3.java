package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_K2S3 extends RoutingProfile {

    public MTB_K2S3( ) {
        super(new CostCalcSplineProfileMTB( new SplineProfileContextMTB(200,300) ));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineMTB(wayAttributs, (CostCalcSplineProfileMTB) profileCalculator);
    }

    @Override
    public int getIconIdActive() {
        return R.drawable.rp_mtb_k2s3_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb_k2s3_i;
    }
    protected int getIconIdCalculating() {
        return R.drawable.rp_mtb_k2s3_c;
    }

}
