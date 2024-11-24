package mg.mgmap.activity.mgmap.features.routing.profile;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.CubicSpline.Value;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    protected static final int maxSL = 7; // SurfaceLevels without MTB scale
    private static final int deltaUptoDn = 2; // mtbUp = mtbDn + 2 in case only mtbDn is specified
    private static final int maxUptoDn = 3; // maximum difference mtbUp - mtbDn considered

/*  parameters of last commit
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
    private static int indRefDnSlope;   */

//  Element 0 for heuristic, than subsequent 5 are for surfaceLevel 0-5; Nr 6 is for path; subsequent are for mtbscale dependant
    private static final float[] distFactforCostFunct  = {0.0f   , 2.7f  ,2.3f  ,1.9f  ,1.50f ,1.3f  ,1.2f ,0.0f   ,0.0f  ,0.0f  ,0.0f  ,0.0f };
    private static final float[] cr                    = {0.0170f, 0.005f,0.006f,0.008f,0.015f,0.020f,0.03f,0.025f ,0.020f,0.025f,0.030f,0.035f,0.06f,0.1f,0.15f,0.2f,0.25f,0.3f};
    private static final float[] f1u                   = {1.2f   , 1.1f  ,1.15f  ,1.15f,1.15f ,1.3f  ,1.6f ,2.2f   ,1.2f  ,1.25f ,1.3f  ,1.8f  ,2.4f ,3.0f,3.0f ,3.0f,3.5f ,4f };
    private static final float[] f2u                   = {3.3f   , 3f    ,3f    ,3f    ,3.1f  ,3.3f  ,3.3f ,3.3f   ,3.3f  ,3.3f  ,3.3f  ,3.3f  ,3.5f ,5f  ,6f   ,6f  ,7.0f ,8f};
    private static final float[] vmax                  = {24.0f  , 42f   ,36f   ,28f   , 24f  ,22f   ,15f  ,21f    ,22f   ,20f   ,18f   ,15f   ,10f  };
    private static final float refSM20Dn = 0.15f;
    private static final float[] deltaSM20Dn =           {0.16f  , 0.1f   ,0.12f ,0.18f ,0.21f ,0.25f,0.26f,0.25f  ,0.17f ,0.18f ,0.19f  ,0.3f ,0.4f ,1.0f ,1.5f};
    private static final float[] factorDown            = {3.0f   , 3.0f   ,3.0f  ,3.1f  ,3.5f  ,4f   ,4.5f ,4.5f   ,3.0f  ,3.0f  ,3.0f   ,4.0f ,6f   ,8f   ,10f  };

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
        boolean negativeCurvature = false;
        //noinspection unchecked
        ArrayList<Value>[] violations = (ArrayList<Value>[]) new ArrayList[maxSurfaceCat+1];
        for ( int surfaceCat = 0 ; surfaceCat <= maxSurfaceCat; surfaceCat++){
            try {
                CubicSpline cubicSpline = getCostSpline(surfaceCat);
                if (surfaceCat > 0) {
                    violations[surfaceCat] = checkSplineHeuristic(cubicSpline, surfaceCat);
                    if (hasCostSpline(surfaceCat)) getDurationSpline(surfaceCat);
                }
            } catch (Exception e) {
                mgLog.e(e.getMessage());
                negativeCurvature = true;
            }
        }
        boolean heuristicViolation = false;
        for ( int surfaceCat=0; surfaceCat<violations.length;surfaceCat++) {
            if (violations[surfaceCat]!=null && !violations[surfaceCat].isEmpty()) {
                heuristicViolation = true;
                int fSurfaceCat = surfaceCat;
                mgLog.e(() -> {
                    StringBuilder msgTxt = new StringBuilder(String.format(Locale.ENGLISH,"Violation of Heuristic for SurfaceLevel=%s mtbDn=%s mtbUp=%s at",getSurfaceLevel(fSurfaceCat), getMtbDn(fSurfaceCat), getMtbUp(fSurfaceCat)));
                    for (Value violationAt : violations[fSurfaceCat]){
                        msgTxt.append(String.format(Locale.ENGLISH, "(%.1f,%.5f)", violationAt.x() * 100, violationAt.y()));
                    }
                    return msgTxt.toString();
                });
            }
        }
        if (heuristicViolation||negativeCurvature) throw new RuntimeException( heuristicViolation ? "Heuristic Violation" : "Curvature Violation" );
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



    private CubicSpline calcSpline(boolean costSpline, int surfaceCat) throws Exception {
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

        return calcSplineSC(costSpline,surfaceCat,crUp,f1Up,f2Up,cr0,sm20Dn,factorDn,smMin,distFactCostFunct,watt,watt0);
    }

    private CubicSpline calcSplineSC(boolean costSpline, int surfaceCat,float crUp,float f1Up, float f2Up, float cr0,float sm20Dn,float factorDn,float smMin, float distFactCostFunct, float watt, float watt0) throws Exception {
        long t1 = System.nanoTime();
        float ACw = 0.45f;
        float m = 90f;

        float[] slopes;
        float[] durations;
        boolean allSlopes = cr0 <= 0.05;

        if (allSlopes) {
            slopes = slopesAll;
            durations = new float[slopes.length];
            if (smMin==0f) throw new Exception(String.format( Locale.ENGLISH,"vmax not defined for scDn=%s at surfaceLevel=%s",getCatDn(surfaceCat),getSurfaceLevel(surfaceCat)));
            durations[indRefDnSlope] = smMin;
        } else {
            slopes = slopesNoOpt;
            durations = new float[slopes.length];
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

        CubicSpline cubicSpline;
        if (allSlopes) {
            cubicSpline = getOptSpline(slopes, durations, durations[indRefDnSlope], indRefDnSlope);
            durations[indRefDnSlope] = cubicSpline.calc(slopes[indRefDnSlope]);
        } else {
            cubicSpline = getSpline(slopes, durations);
        }

        String OptSpline = allSlopes ? "Optimized" :"";
        String SplineType = costSpline ? " Cost":" Duration";

        ArrayList<CubicSpline.Value> negativeCurvatures = cubicSpline.getNegativeCurvaturePoints();
        if (negativeCurvatures != null) {
            StringBuilder msg = new StringBuilder(String.format(Locale.ENGLISH,"%s%s Spline for SurfaceLevel=%s mtbDn=%s mtbUp=%s has negative curvature at",OptSpline,SplineType,getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat)));
            for (CubicSpline.Value negCurvature : negativeCurvatures) {
                msg.append(": ");
                msg.append(String.format(Locale.ENGLISH," slope=%.2f",100*negCurvature.x()));
                msg.append(String.format(Locale.ENGLISH," curve=%.2f",negCurvature.y()));
            }
            throw new Exception( msg.toString());
        }

        long t = System.nanoTime() - t1;
        mgLog.i( ()-> {
            try {
                float slopeMin = cubicSpline.calcMin(-0.13f);
                float smMinOpt = cubicSpline.calc(slopeMin);
                float smVary = durations[indRefDnSlope];
                float sm0 = cubicSpline.calc(0f);
                return String.format(Locale.ENGLISH, "%s%s Spline for SurfaceLevel=%s mtbDn=%s mtbUp=%s t[µsec]=%s. Min at Slope=%.2f, smMin=%.3f, vmax=%.2f, smVary=%.3f, sm0=%3f, v0=%2f", OptSpline,SplineType,getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat),t/1000,100f*slopeMin,smMinOpt,3.6f/smMin,smVary,sm0,3.6f/sm0);
            } catch (Exception e) {
                return String.format(Locale.ENGLISH, "%s for SurfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat));
            }
        });
        return cubicSpline;
    }

    protected CubicSpline getCostSpline(int surfaceCat) throws Exception{
        CubicSpline cubicSpline = SurfaceCatCostSpline[surfaceCat];
        if (cubicSpline == null) {
             cubicSpline = calcSpline(true,surfaceCat);
             SurfaceCatCostSpline[surfaceCat] = cubicSpline;
        }
        return cubicSpline;
    }

    protected CubicSpline getDurationSpline(int surfaceCat) throws Exception{
        CubicSpline cubicSpline;
        cubicSpline = SurfaceCatDurationSpline[surfaceCat];
        if (cubicSpline == null) {
              if (hasCostSpline(surfaceCat)) cubicSpline = calcSpline(false,surfaceCat); // calculate dedicated duration function
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


