package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineTrekkingBike implements CostCalculator {

    private static final HashMap<CostCalcSplineTrekkingBike,WayAttributs> AttributsHashMap = new HashMap<>();
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final float mfd;
    private final boolean oneway;
    private final IfSpline surfaceCatSpline;
    private final short surfaceCat;
    private final IfProfileCostCalculator mProfileCalculator;

    public CostCalcSplineTrekkingBike(WayAttributs wayTagEval, IfProfileCostCalculator profile) {
        mProfileCalculator = profile;
        oneway = wayTagEval.oneway;
        if ( mgLog.level.ordinal() <= MGLog.Level.VERBOSE.ordinal() ){
            AttributsHashMap.put(this,wayTagEval);
        }

        float  distFactor ;
        short surfaceCat = TagEval.getSurfaceCat(wayTagEval);
        if (TagEval.getNoAccess(wayTagEval)){
            distFactor = 10;
            surfaceCat = (surfaceCat>0) ? surfaceCat :2;
        } else {
            if ("path".equals(wayTagEval.highway)) {
                surfaceCat = (surfaceCat<=0) ? 4: (surfaceCat == 1) ? 2 : surfaceCat;
                if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)) {
                    distFactor = 1.1f;
                } else if ("bic_designated".equals(wayTagEval.bicycle)) {
                    distFactor = 1.5f;
                } else if ("bic_yes".equals(wayTagEval.bicycle) ) {
                    distFactor = 1.5f;
                } else {
                    distFactor = 2;
                }
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                surfaceCat = (surfaceCat>0) ? surfaceCat :4;
                if ( TagEval.isBikeRoute(wayTagEval) ) {
                    distFactor = 1.0f;
                    surfaceCat = (surfaceCat>2) ? (short) (surfaceCat-1):surfaceCat;
                } else if ( "bic_designated".equals(wayTagEval.bicycle) ) {
                    distFactor = 1.1f;
/*                } else if (surfaceCat<=1) {
                    distFactor = 1.2f;
                }else if (surfaceCat==2) {
                    distFactor = 1.3f;
                } else if (surfaceCat==3) {
                    distFactor = 1.4f; */
                } else {
                    distFactor = 1.5f;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceCat = 6;
                if (TagEval.isBikeRoute(wayTagEval))
                    distFactor = 5.0f;
                else
                    distFactor = 20f;
            } else {
                TagEval.Factors factors = TagEval.getFactors(wayTagEval, surfaceCat,false);
                surfaceCat = factors.surfaceCat;
                distFactor = (float) factors.distFactor;
            }
        }
        if (surfaceCat>6) {
            mgLog.e("Wrong surface Cat:"+ surfaceCat + " ,Tag.highway:" + wayTagEval.highway + " ,Tag.trackType:" + wayTagEval.trackType);
        }
        this.surfaceCat = surfaceCat;
        this.surfaceCatSpline =  mProfileCalculator.getCostFunc(surfaceCat);
        mfd =  distFactor;
    }


    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
        if ( oneway && !primaryDirection)
            return mfd*dist*surfaceCatSpline.valueAt(vertDist / (float) dist) + dist * 5;
        else
            return mfd*dist*surfaceCatSpline.valueAt(vertDist / (float) dist);
    }

    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }


    @Override
    public long getDuration(double dist, float vertDist) {
        IfSpline durationFunc = mProfileCalculator.getDurationFunc(surfaceCat);
        if (dist >= 0.00001) {
            mgLog.v(()-> {
               float slope = vertDist / (float) dist;
               double spm = durationFunc.valueAt(slope);
               double v = 3.6/spm;
               double cost = calcCosts( dist,  vertDist, true);
               return String.format(Locale.ENGLISH, "DurationCalc: Slope=%6.2f v=%5.2f time=%5.2f dist=%6.2f surfaceCat=%s mfd=%3.2f cost=%6.2f %s",100f*slope,v,spm*dist,dist,surfaceCat,mfd,cost,AttributsHashMap.get(this).toDetailedString());
            });
        }
        return ( dist >= 0.00001) ? (long) ( 1000 * dist * durationFunc.valueAt(vertDist/(float) dist)) : 0;
    }

}
