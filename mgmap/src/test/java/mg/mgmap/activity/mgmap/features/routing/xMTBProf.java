package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfileMTB;
import mg.mgmap.generic.graph.WayAttributs;

public class xMTBProf extends RoutingProfile{
    String id;
    public xMTBProf( int sUp, int sDn ) {
        super(new CostCalcSplineProfileMTB( new SplineProfileTestContextMTB(sUp,sDn) ));
        float up = sUp/100f;
        float dn = sDn/100f;
        String uf = (up == Math.floor(up))?"NW_MTB_K%1.0f":"ID_MTB_K%.3f";
        String df = (up == Math.floor(dn))?"S%1.0f":"S%.3f";
        id = String.format(uf+df,up,dn);
    }

    public xMTBProf(int power, int sUp, int sDn ) {
        super(new CostCalcSplineProfileMTB( new SplineProfileTestContextMTB(power,sUp,sDn) ));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineMTB(wayAttributs, (CostCalcSplineProfileMTB) profileCalculator);
    }
    public String getId(){
        return id;
    }
    public int getIconIdActive() {  return 0; }
    @Override
    protected int getIconIdInactive() { return 0; }
    @Override
    protected int getIconIdCalculating() { return 0; }
}