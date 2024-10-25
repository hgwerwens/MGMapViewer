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
//        int slevel = 2;
        mProfileCalculator = profile;
        oneway = wayTagEval.onewayBic;
        int mtbUp = -1;
        int mtbDn = -1;
        float  distFactor ;
        short surfaceLevel = TagEval.getSurfaceCat(wayTagEval);
        if (TagEval.getNoAccess(wayTagEval)){
            distFactor = 10;
            surfaceLevel = (surfaceLevel>0) ? surfaceLevel :2;
        } else {
            if ("path".equals(wayTagEval.highway)) {
                surfaceLevel = (surfaceLevel<=0) ? 4: surfaceLevel;
                if (surfaceLevel == 1){
                    distFactor = 1.2f;
                    surfaceLevel = 2;
                } else {
                    distFactor = 1f;
                    if (wayTagEval.mtbScaleUp != null) {
                        switch (wayTagEval.mtbScaleUp) {
                            case "mtbu_0":
                                mtbUp = 0;
                                break;
                            case "mtbu_1":
                                mtbUp = 1;
                                break;
                            case "mtbu_2":
                                mtbUp = 2;
                                break;
                            case "mtbu_3":
                                mtbUp = 3;
                                break;
                            case "mtbu_4":
                                mtbUp = 4;
                                break;
                            default:
                                mtbUp = 5;
                        }
                    }
                    if (wayTagEval.mtbScale != null) {
                        switch (wayTagEval.mtbScale) {
                            case "mtbs_0":
                                mtbDn = 0;
                                break;
                            case "mtbs_1":
                                mtbDn = 1;
                                break;
                            case "mtbs_2":
                                mtbDn = 2;
                                break;
                            case "mtbs_3":
                                mtbDn = 3;
                                break;
                            case "mtbs_4":
                                mtbDn = 4;
                                break;
                            case "mtbs_5":
                                mtbDn = 5;
                                break;
                            default:
                                mtbDn = 6;
                        }
                    }
                }
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                surfaceLevel = (surfaceLevel>0) ? surfaceLevel :4;
                distFactor = 1.35f;
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceLevel = 4; // treat steps as mtb scale 3.
                mtbDn = 3;
                mtbUp = 6;
                distFactor = 2f;
            } else {
                TagEval.Factors factors = TagEval.getFactors(wayTagEval, surfaceLevel);
                if (factors.surfaceCat <= 4) {
                    surfaceLevel = factors.surfaceCat;
                } else {
                    surfaceLevel = 4;
                    mtbDn = 3;
                    mtbUp = 6;
                }
                distFactor = (float) ((surfaceLevel <= 2) ? factors.distFactor * 1.35d : factors.distFactor);
            }
        }
        if (surfaceLevel>6) {
            throw new RuntimeException("Wrong surface Cat:"+ surfaceLevel + " ,Tag.highway:" + wayTagEval.highway + " ,Tag.trackType:" + wayTagEval.trackType);
        }

        this.surfaceCat = (short) mProfileCalculator.getSurfaceCat(surfaceLevel,mtbDn,mtbUp);
        this.surfaceCatSpline = mProfileCalculator.getSpline(surfaceLevel,mtbDn,mtbUp);
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
            double spm = surfaceCatSpline.calc(slope);
            double v = 3.6/spm;
            mgLog.d(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f surfaceCat=%s mfd=%.2f",100f*slope,v,spm*dist,dist,surfaceCat,mfd));
        }
        return ( dist >= 0.00001) ? (long) ( 1000 * dist * surfaceCatSpline.calc(vertDist/(float) dist)) : 0;
    }
}
