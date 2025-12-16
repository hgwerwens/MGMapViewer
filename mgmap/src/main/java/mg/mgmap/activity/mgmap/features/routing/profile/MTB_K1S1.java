package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_K1S1 extends RoutingProfile {

    public MTB_K1S1( ) {
        super(new CostCalcSplineProfileMTB( new CostCalcSplineProfileMTB.Context(100,100) ));
/* successfully tested combination
        for (int sUp = 0; sUp <= 400; sUp = sUp + 100) {
            for (int sDn = 0; sDn <= 400; sDn = sDn + 100) {
                for ( int power = 48 + sUp/100*25; power <= 150f; power = power + 25 ) {
                    new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(power,sUp, sDn));
                };
            }
        }
*/
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineMTB(wayAttributs, (CostCalcSplineProfileMTB) profileCalculator);
    }


    @Override
    public int getIconIdActive() {
        return R.drawable.rp_mtb_k1s1_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb_k1s1_i;
    }
    @Override
    protected int getIconIdCalculating() {
        return R.drawable.rp_mtb_k1s1_c;
    }
}
