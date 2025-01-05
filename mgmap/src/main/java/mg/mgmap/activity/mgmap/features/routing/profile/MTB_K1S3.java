package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_K1S3 extends RoutingProfile {
    public MTB_K1S3( ) {
        super(new CostCalcSplineProfileMTB( new CostCalcSplineProfileMTB.Context(100,200,200,0.5f) ));
        for( int power = 50;power <= 250;power = power + 50) {
            for (int sUp = 0; sUp <= 300; sUp = sUp + 100) {
                for (int sDn = 0; sDn <= 300; sDn = sDn + 100) {
                    new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(power, sUp, sDn, 0.5f));
                }
            }
        }
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineMTB(wayAttributs, (CostCalcSplineProfileMTB) profileCalculator);
    }

    /*    public MTB_K1S3( ) {
        super(new CostCalculatorTwoPieceFunc( (short)1,  (short)3, (short)1));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorMTB(wayAttributs, (CostCalculatorTwoPieceFunc) profileCalculator); //0.0,0.0,0.7,0.0,0.7,0.0);
    } */

    @Override
    public int getIconIdActive() {
        return R.drawable.rp_mtb_k1s3_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb_k1s3_i;
    }
    protected int getIconIdCalculating() {
        return R.drawable.rp_mtb_k1s3_c;
    }

}
