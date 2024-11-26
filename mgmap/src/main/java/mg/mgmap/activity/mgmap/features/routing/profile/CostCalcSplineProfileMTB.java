package mg.mgmap.activity.mgmap.features.routing.profile;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    protected static final int maxSL = 6; // SurfaceLevels without MTB scale
    private static final int deltaUptoDn = 2; // mtbUp = mtbDn + 2 in case only mtbDn is specified
    private static final int maxUptoDn = 3; // maximum difference mtbUp - mtbDn considered

/*  parameters of last commit
//     private static final float[] distFactforCostFunct  = {0.0f   , 2.7f  ,2.3f  ,1.9f  ,1.50f ,1.3f  ,1.2f ,0.0f   ,0.0f  ,0.0f  ,0.0f  ,0.0f };
//    private static final float[] cr                    = {0.0170f, 0.005f,0.006f,0.008f,0.015f,0.020f,0.03f,0.025f ,0.020f,0.025f,0.030f,0.035f,0.06f,0.1f,0.15f,0.2f,0.25f,0.3f};
//    private static final float[] f1u                   = {1.2f   , 1.1f  ,1.15f  ,1.15f,1.15f ,1.3f  ,1.6f ,2.2f   ,1.2f  ,1.25f ,1.3f  ,1.8f  ,2.4f ,3.0f,3.0f ,3.0f,3.5f ,4f };
//    private static final float[] f2u                   = {3.3f   , 3f    ,3f    ,3f    ,3.1f  ,3.3f  ,3.3f ,3.3f   ,3.3f  ,3.3f  ,3.3f  ,3.3f  ,3.5f ,5f  ,6f   ,6f  ,7.0f ,8f};
//    private static final float[] vmax                  = {24.0f  , 42f   ,36f   ,28f   , 24f  ,22f   ,15f  ,21f    ,22f   ,20f   ,18f   ,15f   ,10f  };
//    private static final float refSM20Dn = 0.15f;
//    private static final float[] deltaSM20Dn =           {0.16f  , 0.1f   ,0.12f ,0.18f ,0.21f ,0.25f,0.26f,0.25f  ,0.17f ,0.18f ,0.19f  ,0.3f ,0.6f ,1.0f ,1.5f};
//    private static final float[] factorDown            = {3.0f   , 3.0f   ,3.0f  ,3.1f  ,3.5f  ,4f   ,4.5f ,4.5f   ,3.0f  ,3.0f  ,3.0f   ,4.0f ,6f   ,8f   ,10f  };
//
//    private static final int maxSurfaceCat = maxSL + (maxUptoDn+1)*(factorDown.length-maxSL-1);
//    private static final float   refDnSlopeOptStart = -0.035f;
//    private static final float   refDnSlope = -0.2f;
//    private static final float[] slopesAll = { -2f,-0.4f,refDnSlope, refDnSlopeOptStart, 0.0f, 0.1f, 0.4f,2f};
//    private static final float[] slopesNoOpt;
//    private static int indRefDnSlope;
//    private static final float ACw = 0.45f;
//    private static final float m = 90f;   */

//   than subsequent 5 are for surfaceLevel 0-5; Nr 6 is for path; subsequent are for mtbscale dependant
    private static final float[] distFactforCostFunct  = { 2.7f  ,2.3f  ,1.9f  ,1.50f ,1.3f  ,1.2f ,0.0f   ,0.0f  ,0.0f  ,0.0f  ,0.0f };
    private static final float[] cr                    = { 0.005f,0.006f,0.008f,0.015f,0.020f,0.03f,0.025f ,0.020f,0.025f,0.030f,0.035f,0.06f,0.1f,0.15f,0.2f,0.25f,0.3f};
    private static final float[] f1u                   = { 1.1f  ,1.15f  ,1.15f,1.15f ,1.3f  ,1.6f ,2.2f   ,1.2f  ,1.25f ,1.3f  ,1.8f  ,2.4f ,3.0f,3.0f ,3.0f,3.5f ,4f };
    private static final float[] f2u                   = { 3f    ,3f    ,3f    ,3.1f  ,3.3f  ,3.3f ,3.3f   ,3.3f  ,3.3f  ,3.3f  ,3.3f  ,3.5f ,5f  ,6f   ,6f  ,7.0f ,8f};
    private static final float[] vmax                  = { 42f   ,36f   ,28f   , 24f  ,22f   ,15f  ,21f    ,22f   ,20f   ,18f   ,15f   ,10f  };
    private static final float refSM20Dn = 0.15f;
    private static final float[] deltaSM20Dn =           { 0.1f   ,0.12f ,0.18f ,0.21f ,0.25f,0.26f,0.25f  ,0.17f ,0.18f ,0.19f  ,0.3f ,0.6f ,1.0f ,1.5f};
    private static final float[] factorDown            = { 3.0f   ,3.0f  ,3.1f  ,3.5f  ,4f   ,4.5f ,4.5f   ,3.0f  ,3.0f  ,3.0f   ,4.0f ,6f   ,8f   ,10f  };

    private static final int maxSurfaceCat = maxSL + (maxUptoDn+1)*(factorDown.length-maxSL-1);
    private static final float   refDnSlopeOptStart = -0.035f;
    private static final float   refDnSlope = -0.2f;
    private static final float[] slopesAll = { -2f,-0.4f,refDnSlope, refDnSlopeOptStart, 0.0f, 0.1f, 0.4f,2f};
    private static final float[] slopesNoOpt;
    private static int indRefDnSlope;
    private static final float ACw = 0.45f;
    private static final float m = 90f;

    protected final CubicSpline[] SurfaceCatDurationSpline = new CubicSpline[maxSurfaceCat];

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

    protected int getMaxSurfaceCat(){
        return maxSurfaceCat;
    }
    protected CostCalcSplineProfileMTB() {
        super(new Object() );
//        SurfaceCatCostSpline[maxSL] = super.getProfileSpline();
        checkAll();
    }

    private void checkAll() {
        for ( int surfaceCat = 0 ; surfaceCat < maxSurfaceCat; surfaceCat++){
            try {
                  if (hasCostSpline(surfaceCat))
                      getDurationSpline(surfaceCat);
            } catch (Exception e) {
                mgLog.e(e.getMessage());
            }
        }
    }

    protected CubicSpline getProfileSpline(Object context ) {
        try {
            return calcSpline(maxSL,context );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CubicSpline getHeuristicRefSpline(Object context) {
        try {
            int[] refSurfaceCats = {0,1,2,3,6,7,8};
            CubicSpline[] refCubicSplines = new CubicSpline[refSurfaceCats.length];
            for ( int i=0;i<refSurfaceCats.length;i++){
                int surfaceCat = refSurfaceCats[i];
                refCubicSplines[i] = getCostSpline(surfaceCat);
                float[] slopes = getSlopes(surfaceCat);
                if (slopes.length != slopesAll.length) throw new RuntimeException("Invalid SurfaceCat for Heuristic determination at " + getSurfaceCatTxt(surfaceCat));
            }
            float[] minDurations = new  float[slopesAll.length];
            for ( int s=0;s<slopesAll.length;s++){
                minDurations[s] = 1e6f;
                for ( int i=0;i<refSurfaceCats.length;i++){
                    float duration = ( refCubicSplines[i].calc(slopesAll[s]) - 0.002f ) * 0.9999f;
                    if (duration < minDurations[s])
                        minDurations[s] = duration;
                }
            }
            return getSpline(slopesAll,minDurations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected float getMinDistFactSC0(){
        return 1f;
    }

    protected CubicSpline calcSpline(int surfaceCat, Object context) throws Exception {
      return calcSpline(true, surfaceCat,context );
    }

    private CubicSpline calcSpline(boolean costSpline, int surfaceCat, Object context) throws Exception {
        int scDn = getCatDn(surfaceCat);
        int scUp = getCatUp(surfaceCat);

        float crUp = cr[scUp];
        float f1Up = f1u[scUp];
        float f2Up = f2u[scUp];
        float cr0 = cr[scDn];
        float sm20Dn = refSM20Dn + deltaSM20Dn[scDn];
        float factorDn = factorDown[scDn];
        float smMin = scDn < vmax.length ? 3.6f/vmax[scDn]:0f;
        float distFactCostFunct = scDn < distFactforCostFunct.length ? distFactforCostFunct[scDn] : 0f;
        float watt =  170f;
        float watt0 = cr0 <= 0.05 ? 100f:(watt+100)/ 2f;

        long t1 = System.nanoTime();

        float[] slopes = getSlopes(surfaceCat);
        float[] durations = new float[slopes.length];

        boolean allSlopes = slopes.length == slopesAll.length;

        if (allSlopes) {
            if (smMin==0f) throw new Exception(String.format( Locale.ENGLISH,"vmax not defined for scDn=%s at surfaceLevel=%s",getCatDn(surfaceCat),getSurfaceLevel(surfaceCat)));
            durations[indRefDnSlope] = smMin;
        }
        durations[0] = sm20Dn -(slopes[0]-refDnSlope)*12f;
        durations[1] = sm20Dn -(slopes[1]-refDnSlope)*factorDn;
        durations[2] = sm20Dn -(slopes[2]-refDnSlope)*factorDn;
        durations[slopes.length-4] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-4], watt0, cr0, ACw, m) ;
        durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, crUp, ACw, m) ;
        durations[slopes.length-2] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, crUp, ACw, m)  ;
        durations[slopes.length-1] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, crUp, ACw, m)  ;

        if (costSpline && distFactCostFunct>0) {
            for ( int i = 0; i<durations.length;i++){
                if     (slopes[i]<0     ) durations[i] = durations[i] *  distFactCostFunct;
                else if(slopes[i]==0.0f ) durations[i] = durations[i] * ( 1f + ( distFactCostFunct - 1f)*0.7f );
                else                      durations[i] = durations[i] * ( 1f + ( distFactCostFunct - 1f)*0.3f );
            }
        }

        CubicSpline cubicSplineTmp;
        if (allSlopes) {
            cubicSplineTmp = getsmMinSpline(slopes, durations, durations[indRefDnSlope], indRefDnSlope);
            durations[indRefDnSlope] = cubicSplineTmp.calc(slopes[indRefDnSlope]);
        } else {
            cubicSplineTmp = getSpline(slopes, durations);
        }

        String OptSpline = allSlopes ? "Optimized" :"";
        String SplineType = costSpline ? " Cost":" Duration";

        if (cubicSplineTmp.getCurve(slopes.length-3)<0){
            cubicSplineTmp = getCurveOptSpline(slopes,durations,slopes.length-3,slopes.length-2);
        }
        if (cubicSplineTmp.getCurve(2)<0){
            cubicSplineTmp = getCurveOptSpline(slopes,durations,2,1);
        }

        ArrayList<CubicSpline.Value> negativeCurvatures = cubicSplineTmp.getNegativeCurvaturePoints();
        if (negativeCurvatures != null) {
            StringBuilder msg = new StringBuilder(String.format(Locale.ENGLISH,"%s%s Spline for %s has negative curvature at",OptSpline,SplineType,getSurfaceCatTxt(surfaceCat)));
            for (CubicSpline.Value negCurvature : negativeCurvatures) {
                msg.append(": ");
                msg.append(String.format(Locale.ENGLISH," slope=%.2f",100*negCurvature.x()));
                msg.append(String.format(Locale.ENGLISH," curve=%.2f",negCurvature.y()));
            }
            throw new Exception( msg.toString());
        }
        CubicSpline cubicSpline = cubicSplineTmp;
        long t = System.nanoTime() - t1;
        mgLog.i( ()-> {
            try {
                float slopeMin = cubicSpline.calcMin(-0.13f);
                float smMinOpt = cubicSpline.calc(slopeMin);
                float smVary = durations[indRefDnSlope];
                float sm0 = cubicSpline.calc(0f);
                return String.format(Locale.ENGLISH, "%s%s Spline for %s t[µsec]=%s. Min at Slope=%.2f, smMin=%.3f, vmax=%.2f, smVary=%.3f, sm0=%3f, v0=%2f", OptSpline,SplineType,getSurfaceCatTxt(surfaceCat),t/1000,100f*slopeMin,smMinOpt,3.6f/smMinOpt,smVary,sm0,3.6f/sm0);
            } catch (Exception e) {
                return String.format(Locale.ENGLISH, "%s for %s",e.getMessage(),getSurfaceCatTxt(surfaceCat));
            }
        });
        return cubicSpline;
    }

    protected String getSurfaceCatTxt(int surfaceCat){
        return String.format(Locale.ENGLISH,"SurfaceCat=%s SurfaceLevel=%s mtbDn=%s mtbUp=%s",surfaceCat,getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat));
    }

    private float[] getSlopes(int surfaceCat){
        float[] slopes;
        if (cr[getCatDn(surfaceCat)]<=0.07) {
            slopes = slopesAll;
        } else {
            slopes = slopesNoOpt;
        }
        return slopes;
    }

    protected CubicSpline getDurationSpline(int surfaceCat) throws Exception{
        CubicSpline cubicSpline;
        cubicSpline = SurfaceCatDurationSpline[surfaceCat];
        if (cubicSpline == null) {
              if (hasCostSpline(surfaceCat)) cubicSpline = calcSpline(false,surfaceCat,getContext() ); // calculate dedicated duration function
              else                           cubicSpline = getCostSpline(surfaceCat);             // reuse existing cost function = duration function
              SurfaceCatDurationSpline[surfaceCat] = cubicSpline;
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
        int surfaceCat = surfaceLevel;
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
        return Math.min(surfaceCat, maxSL);
    }


    protected int getMtbUp(int surfaceCat){
        if (surfaceCat <= maxSL) return -1;
        else                     return (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
    }

    private int getCatUp(int surfaceCat){
        if (surfaceCat==maxSL )      return getCatUp(getSurfaceCat(maxSL-1,-1,4));// for path without mtbscale map upScale to mtbscale 4 except for zero slope
        else if (surfaceCat < maxSL) return surfaceCat;
        else                         return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
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


