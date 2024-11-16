package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    protected static final int maxSL = 6; // SurfaceLevels without MTB scale
    private static final int deltaUptoDn = 2; // mtbUp = mtbDn + 2 in case only mtbDn is specified
    private static final int maxUptoDn = 2; // maximum difference mtbUp - mtbDn considered

//  Element 0 for heuristic, than subsequent 5 are for surfaceLevel 0-4; Nr 5 is for surfaceLevel 4 on path; subsequent are for mtbscale dependant
    private static final float[] distFactforCostFunct  = {0.0f  , 2.2f  ,1.8f  ,1.5f  ,1.25f ,1.2f  ,0.0f    ,1.1f ,0.0f  ,0.0f  ,0.0f };
    private static final float[] cr                    = {0.009f, 0.005f,0.006f,0.008f,0.012f,0.016f,0.02f   ,0.01f,0.015f,0.020f,0.025f,0.04f,0.1f,0.15f,0.2f,0.25f};
    private static final float[] f1u                   = {1.25f , 1.25f ,1.25f ,1.4f  ,1.5f  ,1.6f  ,2.2f    ,1.5f ,1.6f  ,1.7f  ,2.0f  ,2.5f ,3.0f,3.0f ,3.0f,3.0f};
    private static final float[] f2u                   = {3f    , 3f    ,3f    ,3f    ,3.1f  ,3.1f  ,3.3f    ,3.3f ,3.3f  ,3.3f  ,3.3f  ,4f   ,5f  ,6f   ,6f  ,6.0f};
    private static final float[] vmax                  = {28f   , 42f   ,36f   ,28f   , 25f  ,24f   ,24f     ,27f  ,24f   ,20f   ,18f   ,12f  };
    private static final float   ref0SMDn = 0.2f;
    private static final float[] deltaSMDn             = {0.17f , 0.1f   ,0.12f ,0.18f ,0.21f ,0.25f ,0.25f   ,0.17f,0.18f ,0.19f  ,0.3f ,0.4f ,0.6f ,0.8f};
    private static final float[] factorDown            = {3.0f , 3.0f   ,3.0f  ,3.0f  ,3.5f  ,4f    ,4.5f     ,3.0f ,3.0f  ,3.0f   ,4.0f ,5f   ,6f   ,6f  };

    private static final int maxSurfaceCat = maxSL + (maxUptoDn+1)*(factorDown.length-maxSL-1);
    private static final float   refDnSlopeOptStart = -0.035f;
    private static final float   refDnSlope = -0.2f;
    private static final float[] slopesAll = { -2f,-0.4f,refDnSlope, refDnSlopeOptStart, 0.0f, 0.1f, 0.4f,2f};
    private static final float[] slopesNoOpt;
    private static int indRefDnSlope;

    protected final CubicSpline[] SurfaceCatCostSpline = new CubicSpline[maxSurfaceCat+1];
    protected final CubicSpline[] SurfaceCatDurationSpline = new CubicSpline[maxSurfaceCat+1];

    static {
        int i=0;
        slopesNoOpt = new float[slopesAll.length-1];
        for (float slope : slopesAll) {
            if (slope != refDnSlopeOptStart){
                slopesNoOpt[i] = slope;
                i = i +1;
            }
            else indRefDnSlope = i;
        }
    }

    protected CostCalcSplineProfileMTB() {
        super(new Object() );
        SurfaceCatCostSpline[maxSL] = super.getProfileSpline();
        checkAll();
    }

    private void checkAll() {
        //noinspection unchecked
        ArrayList<float[]>[] violations = new ArrayList[maxSurfaceCat+1];
        for ( int surfaceCat = 1 ; surfaceCat <= maxSurfaceCat; surfaceCat++){
            violations[surfaceCat] = checkSplineHeuristic(getCostSpline(surfaceCat),surfaceCat);
        }
        boolean heuristicViolation = false;
        for ( int surfaceCat=0; surfaceCat<violations.length;surfaceCat++) {
            if (violations[surfaceCat]!=null && !violations[surfaceCat].isEmpty()) {
                heuristicViolation = true;
                int finalSurfaceCat = surfaceCat;
                mgLog.e(() -> {
                    StringBuilder msgTxt = new StringBuilder("Violation of Heuristic for SurfaceCat " + finalSurfaceCat + " at: ");
                    for (float[] violationAt : violations[finalSurfaceCat]){
                        msgTxt.append(String.format(Locale.ENGLISH, "(%2f,%2f)", violationAt[0] * 100, violationAt[1]));
                    }
                    return msgTxt.toString();
                });
            }
        }
        if (heuristicViolation) throw new RuntimeException("Heuristic Violation!");
    }

    protected CubicSpline getProfileSpline(Object context ) {
        try {
            return calcSpline(hasCostSpline(maxSL),maxSL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CubicSpline getHeuristicRefSpline(Object context) {
        try {
            return calcSpline(hasCostSpline(0),0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected float getMinDistFactSC0(){
        return 1f;
    }


    private CubicSpline calcSpline(boolean costSpline, int surfaceCat) {
        long t1 = System.nanoTime();
        float ACw = 0.45f;
        float m = 90f;
        int scDn = getCatDn(surfaceCat);
        int scUp = getCatUp(surfaceCat);
        float cr0 = cr[scDn];

        float[] slopes;
        float[] durations;
        float watt =  160f;
        float watt0 = cr0 < 0.075 ? 120f:watt;
        boolean allSlopes = cr0 <= 0.04;

        if (allSlopes) {
            slopes = slopesAll;
            durations = new float[slopes.length];
            durations[indRefDnSlope] = 3.6f/vmax[scDn];
        } else {
            slopes = slopesNoOpt;
            durations = new float[slopes.length];
        }
        float refSMDn = ref0SMDn + deltaSMDn[scDn];
        durations[0] = refSMDn -(slopes[0]-refDnSlope)*12f;
        durations[1] = refSMDn -(slopes[1]-refDnSlope)*factorDown[scDn];
        durations[2] = refSMDn -(slopes[2]-refDnSlope)*factorDown[scDn];
        durations[slopes.length-4] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-4], watt0, cr0, ACw, m) ;
        durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr[scUp], ACw, m) ;
        durations[slopes.length-2] = f1u[scUp] /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr[scUp], ACw, m)  ;
        durations[slopes.length-1] = f2u[scUp] /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr[scUp], ACw, m)  ;

        if (costSpline) {
            for ( int i = 0; i<durations.length;i++){
                if     (slopes[i]<=0     && scDn < distFactforCostFunct.length && distFactforCostFunct[scDn]>=1) durations[i] = durations[i] *  distFactforCostFunct[scDn];
                else if(slopes[i]<=0.12f && scUp < distFactforCostFunct.length && distFactforCostFunct[scUp]>=1) durations[i] = durations[i] * ( 1f + ( distFactforCostFunct[scUp] - 1f)*0.4f );
                else if(                    scUp < distFactforCostFunct.length && distFactforCostFunct[scUp]>=1) durations[i] = durations[i] * ( 1f + ( distFactforCostFunct[scUp] - 1f)*0.6f );
            }
        }

        CubicSpline cubicSpline;
        if (allSlopes) {
            try {
                cubicSpline = getCheckOptSpline(slopes, durations, surfaceCat, durations[indRefDnSlope], indRefDnSlope);
            } catch (Exception e){
                throw new RuntimeException(String.format(Locale.ENGLISH, "%s at optimizing surfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat)));
            }
            durations[indRefDnSlope] = cubicSpline.calc(slopes[indRefDnSlope]);
            long t = System.nanoTime() - t1;
            mgLog.i( ()-> {
                try {
                    float slopeMin = cubicSpline.calcMin(-0.13f);
                    float smMin = cubicSpline.calc(slopeMin);
                    float smVary = durations[indRefDnSlope];
                    String type = costSpline ? "Cost":"Duration";
                    return String.format(Locale.ENGLISH, "Opt %s Spline for SurfaceLevel=%s mtbDn=%s mtbUp=%s t[µsec]=%s. Min at Slope=%.2f, sm=%.3f, vmax=%.2f, smVary=%.3f", type,getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat),t/1000,100f*slopeMin,smMin,3.6f/smMin,smVary);
                } catch (Exception e) {
                   return String.format(Locale.ENGLISH, "%s for SurfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat));
                }
            });
        } else {
            try {
                cubicSpline = getCheckSpline(slopes, durations, surfaceCat);
            } catch (Exception e) {
                throw new RuntimeException(String.format(Locale.ENGLISH, "%s surfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat)));
            }
            mgLog.i( ()-> {
                try {
                    float slopeMin = cubicSpline.calcMin(-0.13f);
                    float smMin = cubicSpline.calc(slopeMin);
                    return String.format(Locale.ENGLISH, "Spline for SurfaceLevel=%s mtbDn=%s mtbUp=%s.Min at Slope=%.2f, sm=%.3f, vmax=%.2f",getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat),100f*slopeMin,smMin,3.6f/smMin);
                } catch (Exception e) {
                    return String.format(Locale.ENGLISH, "%s for SurfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat));
                }
            });
        }
        return cubicSpline;
    }

    protected CubicSpline getCostSpline(int surfaceCat) {
        CubicSpline cubicSpline = SurfaceCatCostSpline[surfaceCat];
        if (cubicSpline == null) {
            try {
                cubicSpline = calcSpline(true,surfaceCat);
                SurfaceCatCostSpline[surfaceCat] = cubicSpline;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cubicSpline;
    }

    protected CubicSpline getDurationSpline(int surfaceCat){
        CubicSpline cubicSpline;
        cubicSpline = SurfaceCatDurationSpline[surfaceCat];
        if (cubicSpline == null) {
            try {
                if (hasCostSpline(surfaceCat)) cubicSpline = calcSpline(false,surfaceCat); // calculate dedicated duration function
                else                           cubicSpline = getCostSpline(surfaceCat);             // reuse existing cost function = duration function
                SurfaceCatDurationSpline[surfaceCat] = cubicSpline;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cubicSpline;
    }

    private boolean hasCostSpline(int surfaceCat){
        int scDn = getCatDn(surfaceCat);
        int scUp = getCatUp(surfaceCat);
        return ( scDn < distFactforCostFunct.length && distFactforCostFunct[scDn]>=1f ) ||
               ( scUp < distFactforCostFunct.length && distFactforCostFunct[scUp]>=1f );
    }

    protected int getSurfaceCat(int surfaceLevel, int mtbDn, int mtbUp) {
        if (surfaceLevel < 0 || surfaceLevel > maxSL) throw new RuntimeException("invalid Surface Level");
        int surfaceCat = surfaceLevel + 1;
        if (surfaceCat >= maxSL ) {
            int scUp;
            int scDn;
            if (mtbDn > -1)
                if (mtbUp > -1) {
                    scUp = mtbDn-mtbUp>=0 ? 0 : (Math.min(mtbUp - mtbDn, maxUptoDn));
                    scDn = mtbDn;
                } else {
                    scUp = deltaUptoDn;
                    scDn = mtbDn;
                }
            else if (mtbUp > -1) {
                scUp = mtbUp == 0?0:1;
                scDn = mtbUp == 0?0:mtbUp-1;
            }
            else {
                scUp = -1;
                scDn =  0;
            }
            return maxSL + 1 + (maxUptoDn+1)*scDn + scUp;
        } else
            return surfaceCat;
    }

    protected int getSurfaceLevel(int surfaceCat){
        return Math.min(surfaceCat, maxSL)-1;
    }


    protected int getMtbUp(int surfaceCat){
        if (surfaceCat <= maxSL) return -1;
        else                     return (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
    }

    private int getCatUp(int surfaceCat){
        if (surfaceCat==maxSL ) return getCatUp(getSurfaceCat(maxSL-1,-1,4));// for path without mtbscale map upScale to mtbscale 4 except for zero slope
        if (surfaceCat < maxSL) return surfaceCat;
        else                     return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
    }

    protected int getMtbDn(int surfaceCat){
        if (surfaceCat <= maxSL) return -1;
        else                     return (surfaceCat-maxSL-1)/(maxUptoDn+1);
    }

    protected int getCatDn(int surfaceCat){
        if (surfaceCat <= maxSL) return surfaceCat;
        else                     return maxSL+1 + (surfaceCat-maxSL-1)/(maxUptoDn+1);
    }
}


