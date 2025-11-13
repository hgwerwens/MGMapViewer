package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineMTB implements CostCalculator {
    enum dir {up,down,none,oneway}
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final float mfd;
    private final dir direction;
    private final CubicSpline surfaceCatSpline;
    private final short surfaceCat;
    private final CostCalcSplineProfileMTB mProfileCalculator;

    public CostCalcSplineMTB(WayAttributs wayTagEval, CostCalcSplineProfileMTB profile) {
        mProfileCalculator = profile;
        dir direction = null;
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
                if (surfaceLevel<=0 || surfaceLevel >= 4 || mtbUp >= 0 || mtbDn >= 0) {
                    surfaceLevel = 6;
                    distFactor = 1f;
                } else { // a path, which is not raw might be anything ...
                    surfaceLevel = surfaceLevel <= 1 ? 2 : surfaceLevel;
                    distFactor = 1.15f;
                }
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                if (  surfaceLevel >= 4  && ( mtbUp >= 0 || mtbDn >= 0)) {
                    surfaceLevel = 6;
                    distFactor = 1.05f;
                } else {
                    surfaceLevel = (surfaceLevel > 0) ? surfaceLevel : 4;
                    distFactor = 1.0f;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceLevel = 6; // treat steps as mtb scale 3.
                mtbDn = 3;
                mtbUp = 6;
                distFactor = 3f;
                if ("up".equals(wayTagEval.incline_dir))
                    direction = dir.up;
                else if ("down".equals(wayTagEval.incline_dir))
                    direction = dir.down;
                else
                    direction = dir.none;
            } else {
                TagEval.Factors factors = TagEval.getFactors(wayTagEval, (short) surfaceLevel, true);
                surfaceLevel = factors.surfaceCat;
                if (surfaceLevel==0)
                    distFactor = (float) (factors.distFactor / TagEval.minDistfSc0);
                else
                    distFactor = (float) factors.distFactor;
            }
        }
        try {
            this.surfaceCat = (short) mProfileCalculator.getSurfaceCat(surfaceLevel,mtbDn,mtbUp);
            this.surfaceCatSpline = mProfileCalculator.getCostSpline(surfaceCat);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (wayTagEval.oneway)
            this.direction = dir.oneway;
        else
            this.direction = direction;
        mfd =  distFactor;
    }


    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
        if (dist<0.0000001) return 0.00001;
        float costf = surfaceCatSpline.calc(vertDist / (float) dist);
        double cost;
        if (direction!=null)
            if ( direction == dir.oneway && !primaryDirection)
                cost = mfd*dist*costf + dist * 5 + 0.00001;
            else if ((direction==dir.up&&primaryDirection) || (direction==dir.down&&!primaryDirection))
                cost = mfd*dist*costf + 0.00001;
            else if (direction==dir.none)
                cost = mfd/2*dist*costf + 0.00001;
            else if (direction!=dir.oneway) {
                cost = (1d + mfd / 2d * mProfileCalculator.sig(2d*(((CostCalcSplineProfileMTB.Context) mProfileCalculator.getContext()).sDn / 100d - 1d))) * dist * costf + 0.00001;
            }
            else
                cost = mfd*dist*costf + 0.00001;
        else
            cost = mfd*dist*costf + 0.00001;

//        mgLog.d("cost="+cost+" costf="+costf + " dist="+dist+" vertDist="+vertDist+" primaryDir="+primaryDirection+" direction="+direction+" mfd="+mfd);
        return cost;
    }

    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }


    @Override
    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            float spm ; //mfd*surfaceCatSpline.calc(slope); //
            try {
                spm = mProfileCalculator.getDurationSpline(surfaceCat).calc(slope);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            float costf = surfaceCatSpline.calc(slope)/mProfileCalculator.refCosts;
            float v = 3.6f/spm;
            float finalSpm = spm;
            mgLog.v(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f cost=%.2f surfaceCat=%s surfaceLevel=%s scUp=%s scDn=%s mfd=%.2f costf=%.2f",
                    100f*slope,v, finalSpm *dist,dist, calcCosts(dist,vertDist,true),surfaceCat,mProfileCalculator.getSurfaceLevel(surfaceCat),mProfileCalculator.getMtbUp(surfaceCat),mProfileCalculator.getMtbDn(surfaceCat),mfd,costf));
            return (long) (1000*dist*spm);
        } else return 0;
    }
}
