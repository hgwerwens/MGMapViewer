package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineMTB implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final float mfd;
    private final boolean oneway;
    private final CubicSpline surfaceCatSpline;
    private final short surfaceCat;
    private final CostCalcSplineProfileMTB mProfileCalculator;

    public CostCalcSplineMTB(WayAttributs wayTagEval, CostCalcSplineProfileMTB profile) {
        mProfileCalculator = profile;
        oneway = wayTagEval.onewayBic;
        int mtbUp = -1;
        int mtbDn = -1;
        float  distFactor;
        int surfaceLevel = TagEval.getSurfaceCat(wayTagEval);
        if (TagEval.getNoAccess(wayTagEval)){
            distFactor = 10;
            surfaceLevel = (surfaceLevel>0) ? surfaceLevel :2;
        } else {
            if (wayTagEval.mtbScaleUp != null) {
                mtbUp = switch (wayTagEval.mtbScaleUp) {
                    case "mtbu_0" -> 0;
                    case "mtbu_1" -> 1;
                    case "mtbu_2" -> 2;
                    case "mtbu_3" -> 3;
                    case "mtbu_4" -> 4;
                    default -> 5;
                };
            }
            if (wayTagEval.mtbScale != null) {
                mtbDn = switch (wayTagEval.mtbScale) {
                    case "mtbs_0" -> 0;
                    case "mtbs_1" -> 1;
                    case "mtbs_2" -> 2;
                    case "mtbs_3" -> 3;
                    case "mtbs_4" -> 4;
                    case "mtbs_5" -> 5;
                    default -> 6;
                };
            }
            if ("path".equals(wayTagEval.highway)) {
                surfaceLevel = (surfaceLevel<=0 || surfaceLevel == 4) ? 5: surfaceLevel; //surfacelevel 5 for surfacelevel 4 on path
                //noinspection NonStrictComparisonCanBeEquality
                surfaceLevel = surfaceLevel <= 1 ? 2: surfaceLevel;
                distFactor = 1f;
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                surfaceLevel = (surfaceLevel>0) ? surfaceLevel :4;
                distFactor = 1.0f;
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceLevel = 5; // treat steps as mtb scale 3.
                mtbDn = 3;
                mtbUp = 6;
                distFactor = 1f;
            } else {
                TagEval.Factors factors = TagEval.getFactors(wayTagEval, (short) surfaceLevel);
                surfaceLevel = factors.surfaceCat;
                if (surfaceLevel==0)
                    distFactor = (float) (factors.distFactor / TagEval.minDistfSc0);
                else
                    distFactor = (float) factors.distFactor;
            }
        }
        this.surfaceCat = (short) mProfileCalculator.getSurfaceCat(surfaceLevel,mtbDn,mtbUp);
        this.surfaceCatSpline = mProfileCalculator.getCostSpline(surfaceCat);
        mfd =  distFactor;
    }


    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
        if ( oneway && !primaryDirection)
            return mfd*dist*surfaceCatSpline.calc(vertDist / (float) dist) + dist * 5;
        else
            return mfd*dist*surfaceCatSpline.calc(vertDist / (float) dist);
    }

    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }


    @Override
    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            float spm =  mProfileCalculator.getDurationSpline(surfaceCat).calc(slope); //mfd*surfaceCatSpline.calc(slope); //
            float cost = mfd*surfaceCatSpline.calc(slope);
            float v = 3.6f/spm;
            mgLog.d(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f surfaceCat=%s surfaceLevel=%s scUp=%s scDn=%s mfd=%.2f cost=%.2f",
                    100f*slope,v,spm*dist,dist,surfaceCat,mProfileCalculator.getSurfaceLevel(surfaceCat),mProfileCalculator.getMtbUp(surfaceCat),mProfileCalculator.getMtbDn(surfaceCat),mfd,dist*cost));
            return (long) (1000*dist*spm);
        } else return 0;
    }
}
