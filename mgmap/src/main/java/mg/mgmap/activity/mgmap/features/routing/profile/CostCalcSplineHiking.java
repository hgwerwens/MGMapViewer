package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.WeakHashMap;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSpline;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineHiking implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final WeakHashMap<CostCalcSplineHiking,WayAttributs> AttributsHashMap = new WeakHashMap<>();
    private final double distFactor;
    private final IfSpline surfaceCatSpline;
    private final int surfaceCat;
    private final CostCalcSplineProfile mProfileCalculator;
    public CostCalcSplineHiking(WayAttributs wayTagEval, CostCalcSplineProfile profile) {
        mProfileCalculator = profile;
        if ( mgLog.level.ordinal() <= MGLog.Level.VERBOSE.ordinal() ){
            AttributsHashMap.put(this,wayTagEval);
        }
        double distFactor ;
        int surfaceLevel = TagEval.getSurfaceCat(wayTagEval);
        if ("path".equals(wayTagEval.highway) && ( surfaceLevel <=0 || surfaceLevel >= 4 )) {
            surfaceLevel = 6;
        }
        if (wayTagEval.highway == null || "acc_no".equals(wayTagEval.access) || "private".equals(wayTagEval.access) || "motorway".equals(wayTagEval.highway)){
            distFactor = 10;
        } else if ("primary".equals(wayTagEval.highway)){
            distFactor = 3;
        } else if ("secondary".equals(wayTagEval.highway)){
            distFactor = 2;
        } else if ("tertiary".equals(wayTagEval.highway)){
            distFactor = 1.5;
        } else
            distFactor = 1.0;
        this.distFactor = distFactor;
        this.surfaceCat = surfaceLevel;
        try {
            this.surfaceCatSpline = mProfileCalculator.getCostFunc(surfaceCat);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
        if (dist<0.0000001) return 0.000001;
        float costf = surfaceCatSpline.valueAt(vertDist / (float) dist);
        double cost;
        cost = dist*distFactor*costf;
        return cost;
    }

    @Override
    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

    @Override
    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            float spm;
            try {
                spm = mProfileCalculator.getDurationSpline(surfaceCat).valueAt(slope);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mgLog.v(()-> {
                float costf = surfaceCatSpline.valueAt(slope)/mProfileCalculator.getRefCosts();
                float v = 3.6f/spm;
                WayAttributs wayAttributs = AttributsHashMap.get(this);
                String wayAttrString = (wayAttributs!=null) ? wayAttributs.toDetailedString() : "";
                return String.format(Locale.ENGLISH, "%s Slope=%6.2f v=%5.2f time=%6.2f dist=%5.2f cost=%6.2f mfd=%3.2f costf=%5.2f %s",
                        mProfileCalculator.getContext().getSurfaceCatTxt(surfaceCat),100f*slope,v, spm *dist,dist, calcCosts(dist,vertDist,true),distFactor,costf, wayAttrString);
            });
            return (long) (1000*dist*spm);
        } else return 0;
    }


}
