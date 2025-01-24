package mg.mgmap.activity.mgmap.features.routing.profile;


import androidx.annotation.NonNull;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {

    public static class Context {
        private CostCalcSplineProfileMTB refProfile;
        private final int power;
        private final int sUp;
        private final int sDn;
        private final float testvar;
        private final boolean withRef;
        public Context(int power, int sUp, int sDn, float testvar,boolean withRef){
            this.power = power;
            this.sUp   = sUp;
            this.sDn   = sDn;
            this.testvar = testvar;
            this.withRef = withRef;
        }
        public Context(int power, int sUp, int sDn, float testvar){
            this(power,sUp,sDn, testvar,sUp >= 0);
        }

        @NonNull
        public String toString(){
            return String.format( Locale.ENGLISH,"power=%s sUp=%s sDn=%s testvar=%.1f withRef=%s",power,sUp,sDn, testvar, withRef);
        }
    }

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    protected static final int maxSL = 6; // SurfaceLevels without MTB scale
    private static final int deltaUptoDn = 2; // mtbUp = mtbDn + 2 in case only mtbDn is specified
    private static final int maxUptoDn = 3; // maximum difference mtbUp - mtbDn considered
    private static final int maxScDn = maxSL+1+7;
    private static final int maxScUp = maxScDn+maxUptoDn;


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
    private static final int[] HeuristicRefSurfaceCats = {0,1,2,3,6,7,8};

    private static final float[] distFactforCostFunct = { 2.7f   ,2.4f  ,2f    ,1.50f ,1.3f  ,1.2f  };
    private static final float[] scr                  = { 0.005f,0.006f ,0.008f,0.015f,0.020f,0.03f,0.025f ,0.020f,0.025f,0.030f,0.035f,0.06f,0.1f,0.15f,0.2f,0.25f,0.3f};
    private static final float[] sf1u                 = { 1.1f  ,1.15f  ,1.15f,1.15f ,1.3f  ,1.6f ,2.2f   ,1.2f  ,1.25f ,1.3f  ,1.8f  ,2.4f ,3.0f,3.0f ,3.0f,3.5f ,4f };
    private static final float[] sf2u                 = { 3f    ,3f    ,3f    ,3.1f  ,3.3f  ,3.3f ,3.3f   ,3.3f  ,3.3f  ,3.3f  ,3.3f  ,3.5f ,5f  ,6f   ,6f  ,7.0f ,8f};
    private static final float[] svmax                = { 42f   ,36f   ,28f   , 24f  ,22f   ,15f  ,21f    ,22f   ,20f   ,18f   ,15f   ,11f  };
    private static final float[] ssrelSlope           = { 1.52f ,1.33f ,1f    ,1f    ,1f    ,1f   ,3.5f   ,1.5f  ,1.5f  ,1.7f  ,1.8f  ,1.2f ,1f   ,1f };
    private static final float srefSM20Dn = 0.15f;
    private static final float[] sdeltaSM20Dn =         { 0.1f   ,0.12f ,0.18f ,0.21f ,0.25f,0.26f,0.25f  ,0.17f ,0.18f ,0.19f  ,0.3f ,0.6f ,1.0f ,1.5f};
    private static final float[] sfactorDown =          { 3.0f   ,3.0f  ,3.1f  ,3.5f  ,4f   ,4.5f ,4.5f   ,3.0f  ,3.0f  ,3.0f   ,4.0f ,6f   ,8f   ,10f  };

    private static final int maxSurfaceCat = maxSL + (maxUptoDn+1)*(sfactorDown.length-maxSL-1);
    private static final float refDnSlopeOpt = -0.04f;
    private static final float refDnSlope = -0.2f;
    private static final float ACw = 0.45f;
    private static final float m = 90f;

    protected final CubicSpline[] SurfaceCatDurationSpline = new CubicSpline[maxSurfaceCat];
    private float[] slopesAll; // { -2f,-0.4f,refDnSlope, refDnSlopeOptStart, 0.0f, 0.1f, 0.4f,2f};
    private float[] slopesNoOpt;

    private int indRefDnSlopeOpt;
    private int indRefDnSlope;
    private float[] f1u;
    private float[] f2u;
    private float[] crUp;
    private float[] crDn;
    private float[] vmax;
    private float[] srelSlope;
    private float refSM20Dn;
    private float[] deltaSM20Dn;
    private float[] factorDown;
    private float watt;
    private float watt0;


    protected CostCalcSplineProfileMTB(Object context) {
        super(context);
        if (fullCalc(context))
            checkAll();
    }



    private void setFromContext(Object context){
        if (f1u == null) {
            Context contxt = (Context) context;
            int sUp = contxt.sUp;
            int sDn = contxt.sDn;
            if (contxt.withRef)
                contxt.refProfile = new CostCalcSplineProfileMTB(new Context(100,200,contxt.sDn,contxt.testvar,false));
            slopesAll = new float[]{-2f, -0.4f, refDnSlope, refDnSlopeOpt, 0.0f, 0.1f, 0.4f, 2f};
            indRefDnSlope = 2;
            int i=0;
            if ( sUp >= 0) {
                slopesAll[slopesAll.length-3] = 0.05f + 0.02f*sUp/100f;
                slopesAll[slopesAll.length-2] = slopesAll[slopesAll.length-3] + 0.04f;
                slopesAll[slopesAll.length-1] = 0.6f;
            }
            slopesNoOpt = new float[slopesAll.length-1];
            for (float slope : slopesAll) {
                if (slope != refDnSlopeOpt){
                    slopesNoOpt[i] = slope;
                    i = i +1;
                }
                else indRefDnSlopeOpt = i;
            }

            if ( sUp < 0) {
                f1u = sf1u;
                f2u = sf2u;
                crUp = scr;
                crDn = scr;
                vmax = svmax;
                refSM20Dn = srefSM20Dn;
                deltaSM20Dn = sdeltaSM20Dn;
                factorDown = sfactorDown;
                watt0 = 100f;
                watt  = 170f;
            }
            else {
                double off;
                double sig;
                f1u = new float[maxScUp];
                f2u = new float[maxScUp];
                crUp = new float[maxScUp];
                crDn = new float[maxScDn];
 //               vmax = new float[maxScDn];
                srelSlope = new float[maxScDn];
                deltaSM20Dn = new float[maxScDn];
                factorDown  = new float[maxScDn];

                double foff = 2.5d*Math.exp(-sUp/150d)+1d;// 150d*4d/(sUp+150d);//2.73d*Math.exp(-sUp/150d)+1d;//2.5d/(sUp/100d+1d)+1d;//150d*4d/(sUp+150d);
                for (i = 0; i<maxScUp;i++){
                    if (i < maxSL) {
                        sig = sig((3-i)*2.); // sigmoid function to shift factors
                        f1u[i] = (float) (1.0 + sig*0.05);
                        f2u[i] = (float) (foff + sig*0.5);
                        crUp[i] = (float) (0.0047 + 0.029*sig((3.5-i)*1.3));
                        crDn[i] = crUp[i];
//                        vmax[i] = 42f-7f*i+7.5f*(float) sig((3d-i)*3d);
                        srelSlope[i]  = ssrelSlope[i];
                        factorDown[i] = (float) (5.-sDn/140d+sig(1.5-i+sDn/100d));
                        deltaSM20Dn[i] = (float) (0.1 + 0.16*sig((2-i)*2d));
                    }
                    else if(i>maxSL){
                        off = i - ( maxSL + 1 ) - sUp/100d;
                        sig = sig((1d-off)*2d); // sigmoid function to shift factors
                        f1u[i] = (float) (1.01 + sig*0.1);
                        f2u[i] = (float) (foff + sig*2d);
                        crUp[i] = (float) (0.02 + 0.005*(i-(maxSL+1)) + 0.1*sig(2d*(3d-off)));
                    }
                }

                refSM20Dn = 0.35f-sDn/100f*0.08f;

                for (i = maxSL+1; i<maxScDn;i++){
                    off = i - ( maxSL + 1 ) - sDn/100d;
                    crDn[i] = (float) (0.02 + 0.005*(i-(maxSL+1)) + 0.1*sig(2d*(3d-off)));
                    srelSlope[i]  =  ssrelSlope[i]+(sDn/100f-2.5f)/5;
                    sig = sig((1.5-off)*2); // sigmoid function to shift factors
                    factorDown[i] = (float) (4.5-sDn/150d + 3d*sig);
                    deltaSM20Dn[i] = (float) (0.18 + 0.65*sig);
                }
                f1u[maxSL]=f1u[maxSL+1+4];
                f2u[maxSL]=f2u[maxSL+1+4];
                crUp[maxSL] = crUp[maxSL+1+1];
                crDn[maxSL] = crDn[maxSL+1+1];
                srelSlope[maxSL] = ssrelSlope[maxSL];
                factorDown[maxSL] = factorDown[maxSL+1+4];
                deltaSM20Dn[maxSL] = 0.29f+0.15f*(float) sig(sDn/100d-1.5d) ;
                watt0 =   contxt.power;
//                watt =  (watt0 - (sUp+100f)/10f)/ (1f-2f/1000f*(sUp+100f));
                watt = (1.6f+sUp/100f*0.08f)*watt0; //( watt0 - 30f)/0.4f;
            }
        }

    }

    protected int getMaxSurfaceCat(){
        return maxSurfaceCat;
    }

    private double sig(double base){
        return 1./(1.+Math.exp(base));
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
            CubicSpline[] refCubicSplines = new CubicSpline[HeuristicRefSurfaceCats.length];
            for ( int i=0;i<HeuristicRefSurfaceCats.length;i++){
                int surfaceCat = HeuristicRefSurfaceCats[i];
                refCubicSplines[i] = getCostSpline(surfaceCat);
            }
            float[] minDurations = new  float[slopesAll.length];
            for ( int s=0;s<slopesAll.length;s++){
                minDurations[s] = Float.MAX_VALUE;
                for ( int i=0;i<HeuristicRefSurfaceCats.length;i++){
                    float duration = ( refCubicSplines[i].calc(slopesAll[s]) - 0.005f ) * 0.9999f;
                    if (duration < minDurations[s])
                        minDurations[s] = duration;
                }
            }
            CubicSpline cubicSpline = getSpline(slopesAll,minDurations);
            /*            float curveTarget = 0.5f;
            String contextString = context.toString();

            if (cubicSpline.getCurveCoeff(slopesAll.length-3)<curveTarget){
                mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for heuristic spline %s at slope=%.2f, correct at slope=%.2f",contextString,slopesAll[slopesAll.length-3]*100f,slopesAll[slopesAll.length-2]*100f));
                cubicSpline = getCurveOptSpline(slopesAll,minDurations,slopesAll.length-3,curveTarget,slopesAll.length-2);
            }
            if (cubicSpline.getCurveCoeff(2)<curveTarget){
                mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for heuristic spline %s at slope=%.2f, correct at slope=%.2f", contextString,slopesAll[2]*100f,slopesAll[1]*100f));
                cubicSpline = getCurveOptSpline(slopesAll,minDurations,2,curveTarget,1);
            }
            checkNegCurvature(cubicSpline,String.format(Locale.ENGLISH,"heuristic spline for %s ",contextString)); */
            return cubicSpline;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isHeuristicRefSpline(int surfaceCat){
        for (int heuristicRefSurfaceCat : HeuristicRefSurfaceCats) {
            if (surfaceCat == heuristicRefSurfaceCat) return true;
        }
        return false;
    }

    protected boolean fullCalc(Object context){
        Context contxt = (Context) context;
        return contxt.withRef || contxt.sUp < 0;
    }

    protected float getMinDistFactSC0(){
        return 1f;
    }

    protected CubicSpline calcSpline(int surfaceCat, Object context) throws Exception {
      return calcSpline(true, surfaceCat,context );
    }

    private CubicSpline calcSpline(boolean costSpline, int surfaceCat, Object context) throws Exception {
        setFromContext(context);
        Context contxt = (Context) context;
        int scDn = getCatDn(surfaceCat);
        int scUp = getCatUp(surfaceCat);
        float cr = crUp[scUp] ;
        float crD = crDn[scDn];
        float f1Up = f1u[scUp];
        float f2Up = f2u[scUp];
        float cr0 = (crD+cr)/2f;
        float sm20Dn = refSM20Dn + deltaSM20Dn[scDn];
        float factorDn = factorDown[scDn];

        float distFactCostFunct = scDn < distFactforCostFunct.length ? distFactforCostFunct[scDn] : 0f;
        boolean isHeuristicRefSpline = isHeuristicRefSpline(surfaceCat);

        float f0 = (float) sig((0.05d-cr0)*100d);
        float watt0_high = this.watt0+(this.watt-this.watt0)*f0;
        float watt0_base = 100f + (175f-100f)*f0;

        float f = (float) sig((0.05d-cr)*100d);
        float watt0 = watt0_high>watt0_base ? watt0_high+(watt0_base-watt0_high)*f: watt0_high;
        float watt = this.watt > 175 ? this.watt + (175-this.watt)*f : this.watt;

        long t1 = System.nanoTime();

        float[] slopes ;
        if (cr0<=0.1 || contxt.withRef) {
            slopes = slopesAll;
        } else {
            slopes = slopesNoOpt;
        }

        float[] durations = new float[slopes.length];

        boolean allSlopes = slopes.length == slopesAll.length;


        durations[0] = sm20Dn -(slopes[0]-refDnSlope)*Math.max(12f,factorDn*2f);
        durations[1] = sm20Dn -(slopes[1]-refDnSlope)*factorDn;
        durations[2] = sm20Dn -(slopes[2]-refDnSlope)*factorDn;
        durations[slopes.length-4] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-4], watt0, cr0, ACw, m) ;
        durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr, ACw, m) ;
        durations[slopes.length-2] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr, ACw, m)  ;
        durations[slopes.length-1] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr, ACw, m)  ;

        if (allSlopes && !contxt.withRef) {
            if (contxt.sUp<0) {
                float smMin = scDn < vmax.length ? 3.6f / vmax[scDn] : 0f;
                if (smMin == 0f)
                    throw new Exception(String.format(Locale.ENGLISH, "vmax not defined for scDn=%s at surfaceLevel=%s", getCatDn(surfaceCat), getSurfaceLevel(surfaceCat)));
                durations[indRefDnSlopeOpt] = smMin;
            } else
                durations[indRefDnSlopeOpt] = durations[slopes.length-4]+srelSlope[scDn]*slopes[indRefDnSlopeOpt];
        }

        if (costSpline && distFactCostFunct>0) {
            for ( int i = 0; i<durations.length;i++){
                if     (slopes[i]<0     ) durations[i] = durations[i] *  distFactCostFunct;
                else if(slopes[i]==0.0f ) durations[i] = durations[i] * ( 1f + ( distFactCostFunct - 1f)*0.7f );
                else                      durations[i] = durations[i] * ( 1f + ( distFactCostFunct - 1f)*0.3f );
            }
        }

        String OptSpline = contxt.withRef ? "with ref" : allSlopes ? "Optimized ref" :"ref";
        String SplineType = costSpline ? " cost":" duration";
        String contextString = String.format(Locale.ENGLISH,"%s%s spline %s ",OptSpline,SplineType,getSurfaceCatTxt(surfaceCat));

        float slopeTarget = 0f;
        CubicSpline cubicSplineTmp;
        if (contxt.withRef) {
            float factor = ( costSpline && distFactCostFunct>0 ) ? distFactCostFunct: 1f;
            int mtbDn = getMtbDn(surfaceCat);
            cubicSplineTmp = contxt.refProfile.getDurationSpline(getSurfaceCat(getSurfaceLevel(surfaceCat),mtbDn,mtbDn));
            for ( int i = 0; slopes[i]<0;i++) {
                durations[i] = cubicSplineTmp.calc(slopes[i]) * factor;
            }
            slopeTarget = cubicSplineTmp.getSlope(indRefDnSlope)*factor;
            cubicSplineTmp = getSlopeOptSpline(slopes, durations, indRefDnSlope, slopeTarget, indRefDnSlopeOpt);

            if ( cubicSplineTmp.getVal(indRefDnSlopeOpt)>cubicSplineTmp.getVal(indRefDnSlopeOpt+1)&&cubicSplineTmp.getVal(indRefDnSlopeOpt)>cubicSplineTmp.getVal(indRefDnSlope)){
                mgLog.w(String.format(Locale.ENGLISH,"Correct decreasing downhill speed for %s. Set duration at slope=%.2f to duration at slope=%.2f",contextString,slopes[indRefDnSlopeOpt]*100f,slopes[indRefDnSlopeOpt+1]*100f));
                durations[indRefDnSlopeOpt] = durations[indRefDnSlopeOpt+1];
                cubicSplineTmp = getSpline(slopes,durations);
            }
        }
        else {
            if (allSlopes&&contxt.sUp<0) {
                cubicSplineTmp = getsmMinSpline(slopes, durations, durations[indRefDnSlopeOpt], indRefDnSlopeOpt);
                durations[indRefDnSlopeOpt] = cubicSplineTmp.calc(slopes[indRefDnSlopeOpt]);
            } else {
                cubicSplineTmp = getSpline(slopes, durations);
            }
            float curveTarget = 0.01f;
            if (isHeuristicRefSpline&&cubicSplineTmp.getCurveCoeff(slopes.length-4)<0.01f){
                mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f",contextString,slopes[slopes.length-4]*100f,slopes[slopes.length-3]*100f));
                cubicSplineTmp = getCurveOptSpline(slopes,durations,slopes.length-4,curveTarget,slopes.length-3);
            }
            if (isHeuristicRefSpline&&allSlopes && cubicSplineTmp.getCurveCoeff(3)<0.01f){
                mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f",contextString,slopes[3]*100f,slopes[3]*100f));
                cubicSplineTmp = getCurveOptSpline(slopes,durations,3,curveTarget,3);
            }
        }
        float curveTarget = 0.2f;
        if (cubicSplineTmp.getCurveCoeff(slopes.length-3)<curveTarget){
            mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f",contextString,slopes[slopes.length-3]*100f,slopes[slopes.length-2]*100f));
            cubicSplineTmp = getCurveOptSpline(slopes,durations,slopes.length-3,curveTarget,slopes.length-2);
        }
        if (cubicSplineTmp.getCurveCoeff(2)<curveTarget){
            mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f", contextString,slopes[2]*100f,slopes[1]*100f));
            cubicSplineTmp = getCurveOptSpline(slopes,durations,2,curveTarget,1);
        }

        if (contxt.withRef ) {
            float slope2slopeTarget = cubicSplineTmp.getSlope(indRefDnSlope) / slopeTarget;
            if (Math.abs(slope2slopeTarget - 1f) > 0.01f) {
                String msg = String.format(Locale.ENGLISH, "for %s Slope to Slopetarget=%.3f at %.2f", contextString, slope2slopeTarget, slopes[indRefDnSlope] * 100f);
                if (slope2slopeTarget > 0.5f && slope2slopeTarget < 2f)
                    mgLog.w(msg);
                else
                    throw new Exception("Out of range " + msg);
            }
        }

        checkNegCurvature(cubicSplineTmp,contextString);

        CubicSpline cubicSpline = cubicSplineTmp;
        long t = System.nanoTime() - t1;
        mgLog.i( ()-> {
            try {
                float slopeMin = cubicSpline.calcMin(-0.13f);
                float smMinOpt = cubicSpline.calc(slopeMin);
                float smVary = durations[indRefDnSlopeOpt];
                float sm0 = cubicSpline.calc(0f);
                return String.format(Locale.ENGLISH, "For %s t[µsec]=%s. Min at Slope=%.2f, smMin=%.3f, vmax=%.2f, smVary=%.3f, sm0=%.3f, v0=%.2f", contextString,t/1000,100f*slopeMin,smMinOpt,3.6f/smMinOpt,smVary,sm0,3.6f/sm0);
            } catch (Exception e) {
                return String.format(Locale.ENGLISH, "%s for %s",e.getMessage(),contextString);
            }
        });
        return cubicSpline;
    }

    private void checkNegCurvature(CubicSpline cubicSpline, String context) throws Exception{
        ArrayList<CubicSpline.Value> negativeCurvatures = cubicSpline.getCurveRadiusForNegCurvaturePoints();
        if (negativeCurvatures != null) {
            StringBuilder msg = new StringBuilder(String.format(Locale.ENGLISH,"Negative curvature for %s",context));
            boolean threshold = false;
            for (CubicSpline.Value negCurvature : negativeCurvatures) {
                if (-negCurvature.y() < 0.01f) threshold = true;
                msg.append(": ");
                msg.append(String.format(Locale.ENGLISH," slope=%.2f",100*negCurvature.x()));
                msg.append(String.format(Locale.ENGLISH," curve Radius=%.2f",-negCurvature.y()));
            }
            if(threshold)
                throw new Exception( msg.toString());
            else
                mgLog.w(msg.toString());
        }
    }

    protected String getSurfaceCatTxt(int surfaceCat){
        return String.format(Locale.ENGLISH,"%s SurfaceCat=%s SurfaceLevel=%s mtbDn=%s mtbUp=%s",getContext().toString(),surfaceCat,getSurfaceLevel(surfaceCat), getMtbDn(surfaceCat), getMtbUp(surfaceCat));
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
        if (surfaceLevel >= maxSL ) {
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
            return surfaceLevel;
    }

    protected int getSurfaceLevel(int surfaceCat){
        return Math.min(surfaceCat, maxSL);
    }


    protected int getMtbUp(int surfaceCat){
        if (surfaceCat <= maxSL) return -1;
        else                     return (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
    }

    private int getCatUp(int surfaceCat){
        if (surfaceCat==maxSL )
            return getCatUp(getSurfaceCat(maxSL,-1,4));// for path without mtbscale map upScale to mtbscale 4
        else if (surfaceCat < maxSL)
            return surfaceCat;
        else
            return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
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


