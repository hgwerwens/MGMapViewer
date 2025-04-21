package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_K2S1_2F extends RoutingProfile {

    public MTB_K2S1_2F( ) {
        super(new CostCalculatorTwoPieceFunc( (short) 2, (short)1, (short)1));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorMTB(wayAttributs, (CostCalculatorTwoPieceFunc) profileCalculator);
    }
    @Override
    public int getIconIdActive() {
        return R.drawable.rp_mtb_k2s1_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb_k2s1_i;
    }
    protected int getIconIdCalculating() {
        return R.drawable.rp_mtb_k2s1_c;
    }

}