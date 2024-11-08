package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    protected static final int maxSL = 5; // SurfaceLevels without MTB scale
    private static final int deltaUptoDn = 2; // mtbUp = mtbDn + 2 in case only mtbDn is specified
//  first 5 are for surfaceLevel 0-4; Nr 6 is for surfaceLevel 4 on path; subsequent are for mtbscale dependant
    private static final float[] cr              = {0.005f,0.006f,0.008f,0.012f,0.016f,0.02f   ,0.01f,0.015f,0.020f,0.025f,0.04f,0.1f,0.15f,0.2f};
    private static final float[] f1u             = {1.25f ,1.25f ,1.25f ,1.4f  ,1.5f  ,2.2f    ,1.3f ,1.4f  ,1.5f  ,2.0f  ,2.5f ,3.0f,3.0f ,3.0f };
    private static final float[] f2u             = {3f    ,3f    ,3f    ,3.1f  ,3.1f  ,3.3f    ,3.3f ,3.3f  ,3.3f  ,3.3f  ,4f   ,5f  ,6f   ,6f   };
    private static final float[] vmax            = {42f   ,36f   ,28f   , 25f  ,24f   ,24f     ,27f  ,24f   ,20f   ,18f   ,12f  };
    private static final float   ref0SMDn = 0.2f;
    private static final float[] deltaSMDn       = {0.1f ,0.12f ,0.18f ,0.21f ,0.25f ,0.25f   ,0.17f,0.18f ,0.19f  ,0.3f ,0.4f ,0.6f ,0.8f};
    private static final float[] factorDown      = {3.0f ,3.0f  ,3.0f  ,3.5f  ,4f    ,4.5f    ,3.0f ,3.0f  ,3.0f   ,4.0f ,5f   ,5f   ,5f  };


    private static final float   refDnSlopeOptStart = -0.035f;
    private static final float   refDnSlope = -0.2f;
    private static final float[] slopesAll = { -2f,-0.4f,refDnSlope, refDnSlopeOptStart, 0.0f, 0.1f, 0.4f,2f};
    private static final float[] slopesNoOpt;
    private static int indRefDnSlope;

    private final CubicSpline[] SurfaceCatSpline = new CubicSpline[55];

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
        SurfaceCatSpline[maxSL] = super.getProfileSpline();
/*        for ( int surfaceLevel = 0 ; surfaceLevel <= 5;surfaceLevel++){
            getSpline(surfaceLevel,-1,-1);
            if ( surfaceLevel == 5)
                for ( int mtbDn = -1; mtbDn <= 6; mtbDn++){
                    for ( int mtbUp = -1; mtbUp <= 5; mtbUp++){
                        mgLog.i("mtbDn="+mtbDn+" mtbCatDn="+getCatDn(surfaceLevel,mtbDn,mtbUp)+" mtbUp="+mtbUp+" mtbCatUp="+getCatUp(surfaceLevel,mtbDn,mtbUp)+" SurfacCat="+getSurfaceCat(surfaceLevel,mtbDn,mtbUp));
                        getSpline(surfaceLevel,mtbDn,mtbUp);
                    }
                }
        } */
    }

    protected CubicSpline getProfileSpline(Object context ) {
        try {
            return calcSpline(maxSL,-1,-1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CubicSpline getRefHeuristicSpline(Object context) {
        try {
            return calcSpline(0,-1,-1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected float getMinDistFactSC0(){
        return (float) TagEval.minDistfSc0;
    }

    private CubicSpline calcSpline(int surfaceLevel, int mtbDn, int mtbUp) {
        long t1 = System.nanoTime();
        float ACw = 0.45f;
        float m = 90f;
//        int surfaceCat = getSurfaceCat(surfaceLevel,mtbDn,mtbUp);
        int scDn = getCatDn(surfaceLevel,mtbDn, mtbUp);
        int scUp = getCatUp(surfaceLevel,mtbDn,mtbUp);
        scUp = ( surfaceLevel == maxSL && ( mtbDn < 0 && mtbUp < 0 ) ) ? getCatUp(surfaceLevel,-1,4):scUp; // for path without mtbscale map upScale to mtbscale 4 except for zero slope
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
        CubicSpline cubicSpline;
        if (allSlopes) {
            try {
                cubicSpline = getCheckOptSpline(slopes, durations, getSurfaceCat(surfaceLevel, mtbDn, mtbUp), 3.6f / vmax[scDn], indRefDnSlope);
            } catch (Exception e){
                throw new RuntimeException(String.format(Locale.ENGLISH, "%s at optimizing surfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),surfaceLevel,mtbDn,mtbUp));
            }
            durations[indRefDnSlope] = cubicSpline.calc(slopes[indRefDnSlope]);
            long t = System.nanoTime() - t1;
            mgLog.i( ()-> {
                try {
                    float slopeMin = cubicSpline.calcMin(-0.13f);
                    float smMin = cubicSpline.calc(slopeMin);
                    float smVary = durations[indRefDnSlope];
                    return String.format(Locale.ENGLISH, "Opt Spline for SurfaceLevel=%s mtbDn=%s mtbUp=%s t[µsec]=%s. Min at Slope=%.2f, sm=%.3f, vmax=%.2f, smVary=%.3f",surfaceLevel,mtbDn,mtbUp,t/1000,100f*slopeMin,smMin,3.6f/smMin,smVary);
                } catch (Exception e) {
                   return String.format(Locale.ENGLISH, "%s for SurfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),surfaceLevel,mtbDn,mtbUp);
                }
            });
        } else {
            try {
                cubicSpline = getCheckSpline(slopes, durations, getSurfaceCat(surfaceLevel, mtbDn, mtbUp));
            } catch (Exception e) {
                throw new RuntimeException(String.format(Locale.ENGLISH, "%s surfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),surfaceLevel,mtbDn,mtbUp));
            }
            mgLog.i( ()-> {
                try {
                    float slopeMin = cubicSpline.calcMin(-0.13f);
                    float smMin = cubicSpline.calc(slopeMin);
                    return String.format(Locale.ENGLISH, "Spline for SurfaceLevel=%s mtbDn=%s mtbUp=%s.Min at Slope=%.2f, sm=%.3f, vmax=%.2f",surfaceLevel,mtbDn,mtbUp,100f*slopeMin,smMin,3.6f/smMin);
                } catch (Exception e) {
                    return String.format(Locale.ENGLISH, "%s for SurfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),surfaceLevel,mtbDn,mtbUp);
                }
            });
        }
        return cubicSpline;
    }

    protected CubicSpline getSpline(int surfaceLevel, int mtbDn, int mtbUp){
        int surfaceCat = getSurfaceCat(surfaceLevel,mtbDn,mtbUp);
        CubicSpline cubicSpline = SurfaceCatSpline[surfaceCat];
        if (cubicSpline == null) {
            try {
                cubicSpline = calcSpline(surfaceLevel,mtbDn,mtbUp);
                SurfaceCatSpline[surfaceCat] = cubicSpline;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cubicSpline;
    }

    private int getCatUp(int surfaceLevel, int mtbDn, int mtbUp){
        if (surfaceLevel >= maxSL ) {
            int mtbUpr;
            if (mtbUp > -1)
                if (mtbDn > -1)
                    mtbUpr = mtbDn-mtbUp>=0 ? mtbDn : (mtbUp - mtbDn >= 2 ? mtbDn + 2 : mtbUp);
                else mtbUpr = mtbUp;
            else if (mtbDn > -1) mtbUpr = mtbDn + deltaUptoDn;
            else mtbUpr = -1;
            return maxSL + 1 + mtbUpr;
        } else return surfaceLevel;
    }

    private int getCatDn(int surfaceLevel,int mtbDn,int mtbUp){
        if (surfaceLevel >= maxSL ) {
            if (mtbDn > -1) return mtbDn + maxSL+1;
            else if (mtbUp > -1 ) return (mtbUp == 0)? maxSL+1:mtbUp+maxSL;
            else return maxSL;
        } else return surfaceLevel;
    }

    protected int getSurfaceCat(int surfaceLevel, int mtbDn, int mtbUp) {
        if (surfaceLevel >= maxSL ) {
            int scUp;
            int scDn;
            if (mtbDn > -1)
                if (mtbUp > -1) {
                    scUp = mtbDn-mtbUp>=0 ? 0 : (mtbUp - mtbDn >= 2 ? 2 : 1);
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
            return maxSL + 1 + 3*scDn + scUp;
        } else
            return surfaceLevel;
    }


}


