package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final int maxSL = 4;
//  first 5 are for surfaceLevel 0-4; subsequent are for mtbscale dependant
    private static final float[] cr              = {0.005f,0.006f,0.008f,0.015f,0.02f ,0.02f,0.025f,0.03f,0.06f,0.1f ,0.15f,0.3f};
    private static final float[] f1u             = {1.25f ,1.25f ,1.25f ,1.6f  ,2.2f  ,1.3f ,1.4f  ,1.6f ,1.8f ,2.4f ,2.5f,3f  };
    private static final float[] f2u             = {3f    ,3f    ,3f    ,3.1f  ,3.3f  ,3.3f ,3.3f  ,3.3f ,3.3f ,4f   ,5f  ,6f  };
    private static final float[] vmax            = {42f   ,36f   ,29f   , 28f  ,25f   ,26f  ,25f   ,22f};
    private static final float   ref0SMDn = 0.248f;
    private static final float[] deltaSMDn       = {0.05f ,0.08f ,0.13f ,0.18f ,0.248f,0.13f,0.15f,0.17f ,0.3f  ,0.4f ,0.6f,0.8f};
    private static final float[] factorDown      = {3.1f  ,3.2f  ,3.4f  ,3.8f  ,5f    ,3.8f ,3.8f ,3.8f  ,3.8f  ,5f   ,5f  ,5f  };


    private static final float   refDnSlopeOptStart = -0.035f;
    private static final float   refDnSlope = -0.2f;
    private static final float[] slopesAll = { -2f,-0.4f,refDnSlope, refDnSlopeOptStart, 0.0f, 0.1f, 0.4f,2f};
    private static final float[] slopesNoRefDnSlope;
    private static int indRefDnSlope;

    private final CubicSpline[] SurfaceCatSpline = new CubicSpline[55];

    static {
        int i=0;
        slopesNoRefDnSlope = new float[slopesAll.length-1];
        for (float slope : slopesAll) {
            if (slope != refDnSlopeOptStart){
                slopesNoRefDnSlope[i] = slope;
                i = i +1;
            }
            else indRefDnSlope = i;

        }
    }

    protected CostCalcSplineProfileMTB() {
        super(new Object() );
        SurfaceCatSpline[maxSL] = super.getProfileSpline();
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
        float watt =  160f;
        float watt0 = surfaceLevel<=(short)4 ? 120f:watt;
        float ACw = 0.45f;
        float m = 90f;
        int scUp0 = getCatUp(surfaceLevel,mtbDn,mtbUp);
        int scUp = ( surfaceLevel == 4 && ( mtbDn < 0 || mtbUp < 0 ) ) ? getCatUp(surfaceLevel,-1,3):scUp0; // for path without mtbscale map upScale to mtbscale 3 except for zero slope
        int scDn = getCatDn(surfaceLevel,mtbDn, mtbUp);
        float[] slopes;
        float[] durations;
        if (cr[scUp0] <= 0.05) {
            slopes = slopesAll;
            durations = new float[slopes.length];
            durations[indRefDnSlope] = 3.6f/vmax[scDn];
        } else {
            watt0 = watt;
            slopes = slopesNoRefDnSlope;
            durations = new float[slopes.length];
        }

        float refSMDn = ref0SMDn + deltaSMDn[scDn];
        durations[0] = refSMDn -(slopes[0]-refDnSlope)*12f;
        durations[1] = refSMDn -(slopes[1]-refDnSlope)*factorDown[scDn];
        durations[2] = refSMDn -(slopes[2]-refDnSlope)*factorDown[scDn];
        durations[slopes.length-4] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-4], watt0, cr[scUp0], ACw, m) ;
        durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr[scUp], ACw, m) ;
        durations[slopes.length-2] = f1u[scUp] /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr[scUp], ACw, m)  ;
        durations[slopes.length-1] = f2u[scUp] /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr[scUp], ACw, m)  ;
        CubicSpline cubicSpline;
        if (cr[scUp0] <= 0.05) {
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
                    return String.format(Locale.ENGLISH, "Spline for SurfaceCat=%s t[µsec]=%s. Min at Slope=%.2f, sm=%.3f, vmax=%.2f, smVary=%.3f",surfaceLevel,t/1000,100f*slopeMin,smMin,3.6f/smMin,smVary);
                } catch (Exception e) {
                    mgLog.d("Cubic Spline Creation failed for surfaceLevel " + surfaceLevel);
                    throw new RuntimeException(e);
                }
            });

        } else {
            try {
                cubicSpline = getCheckSpline(slopes, durations, getSurfaceCat(surfaceLevel, mtbDn, mtbUp));
            } catch (Exception e) {
                throw new RuntimeException(String.format(Locale.ENGLISH, "%s surfaceLevel=%s mtbDn=%s mtbUp=%s",e.getMessage(),surfaceLevel,mtbDn,mtbUp));
            }
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
        if (surfaceLevel == maxSL ) {
            int scUp;
            if (mtbUp > -1)
                if (mtbDn > -1)
                    scUp = mtbDn-mtbUp>0 ? mtbDn : (mtbUp - mtbDn > 2 ? mtbDn + 2 : mtbUp);
                else scUp = mtbUp;
            else if (mtbDn > -1) scUp = mtbDn + 1;
            else scUp = -1;
            return maxSL + 1 + scUp;
        } else return surfaceLevel;
    }

    private int getCatDn(int surfaceLevel,int mtbDn,int mtbUp){
        if (surfaceLevel == maxSL ) {
            if (mtbDn > -1) return mtbDn + maxSL+1;
            else if (mtbUp > -1 ) return (mtbUp == 0)?mtbUp:mtbUp-1;
            else return maxSL;
        } else return surfaceLevel;
    }

    protected int getSurfaceCat(int surfaceLevel, int mtbDn, int mtbUp) {
        if (surfaceLevel == maxSL ) {
            int scUp;
            int scDn;
            if (mtbDn > -1)
                if (mtbUp > -1) {
                    scUp = mtbDn-mtbUp>0 ? 0 : (mtbUp - mtbDn > 2 ? 2 : 1);
                    scDn = mtbDn;
                } else {
                    scUp = 1;
                    scDn = mtbDn;
                }
            else if (mtbUp > -1) {
                scUp = mtbUp == 0?0:1;
                scDn = mtbUp == 0?0:mtbUp-1;
            }
            else {
                scUp = 0;
                scDn = -1;
            }
            return maxSL + 1 + scDn + 3*scUp;
        } else
            return surfaceLevel;
    }


}


