package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_K3S3_2F extends RoutingProfile {
    static String id = "OL_MTB_K3S3";
    @Override
    public String getId() {
        return id;
    }
    public MTB_K3S3_2F( ) {
        super(new CostCalculatorTwoPieceFunc( (short) 3, (short)3, (short)1));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorMTB(wayAttributs, (CostCalculatorTwoPieceFunc) profileCalculator);
    }
    @Override
    public int getIconIdActive() {
        return R.drawable.rp_mtb_k3s3_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb_k3s3_i;
    }
    protected int getIconIdCalculating() {
        return R.drawable.rp_mtb_k3s3_c;
    }
}
