package mg.mgmap.activity.mgmap.features.routing;

import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfile;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB;
import mg.mgmap.generic.graph.WayAttributs;

public class xMTBProf extends RoutingProfile{
    String id;
    public xMTBProf( int sUp, int sDn ) {
        super(new CostCalcSplineProfile( SplineProfileTestContextMTB.get(sUp,sDn,false) ));
        id = calcID(sUp,sDn);
    }

    public xMTBProf( CostCalculator costCalculator ) {
        super(costCalculator);
    }

    public String calcID(int sUp, int sDn){
        float up = sUp/100f;
        float dn = sDn/100f;
        String uf = (up == Math.floor(up))?"NW_MTB_K%1.0f":"ID_MTB_K%.3f";
        String df = (up == Math.floor(dn))?"S%1.0f":"S%.3f";
        return String.format(uf+df,up,dn);
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineMTB(wayAttributs, (CostCalcSplineProfile) profileCalculator);
    }
    public void setID(String id){
        this.id = id;
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