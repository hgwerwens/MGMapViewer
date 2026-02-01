package mg.mgmap.activity.mgmap.features.routing.profile;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final float ACw = 0.45f;   // air friction of a typical bicycle rider
    private static final float m = 90f; // system weigth of a typical bicycle with rider
    public static final float minNegCurvatureRadius= 0.01f;
    protected final CubicSpline[] SurfaceCatDurationSpline;
    private IfSplineProfileContext context;


    public CostCalcSplineProfileMTB(Object context) {
        super(context);
        this.context = (IfSplineProfileContext) context;
        SurfaceCatDurationSpline = new CubicSpline[this.context.getMaxSurfaceCat()];
        if (fullCalc(context)) checkAll();
    }
    public int getMaxSurfaceCat(){
        return ((IfSplineProfileContext) getContext()).getMaxSurfaceCat();
    }
    protected float sig(double base){
        return (float) (1./(1.+Math.exp(base)));
    }
    private void checkAll() {
        for (int surfaceCat = 0; surfaceCat < ((IfSplineProfileContext) getContext()).getMaxSurfaceCat(); surfaceCat++){
            try {
               getDurationSpline(surfaceCat);
            } catch (Exception e) {
                mgLog.e(e.getMessage());
            }
        }
    }
    protected CubicSpline getProfileSpline(Object context ) {
        try {
            IfSplineProfileContext contxt = (IfSplineProfileContext) context;
            return getCostSpline(contxt.getScProfileSpline());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CubicSpline getHeuristicRefSpline(Object context) {
        try {
            IfSplineProfileContext contxt = (IfSplineProfileContext) context;
            CubicSpline cubicSplineTmp = getCostSpline(contxt.getScHeuristicRefSpline());
            CubicSpline cubicSpline = cubicSplineTmp.getTransYCubicSpline(-0.0001f);
            checkNegCurvature(cubicSpline,String.format(Locale.ENGLISH,"heuristic spline for %s ", context),1e7f);
            return cubicSpline;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected boolean fullCalc(Object context){
        SplineProfileContextMTB contxt = (SplineProfileContextMTB) context;
        return contxt.fullCalc();
    }

    protected float getMinDistFactSC0(){
        return 1f;
    }

    protected CubicSpline calcSpline(int surfaceCat, Object context) throws Exception {
      return calcSpline(true, surfaceCat,context );
    }

    private CubicSpline calcSpline(boolean costSpline, int surfaceCat, Object context) throws Exception {
        IfSplineProfileContext contxt = (IfSplineProfileContext) context;
        if (!contxt.isValidSc(surfaceCat)) return null;

        int indRefDnSlope = contxt.getIndRefDnSlope();
        int indRefDnSlopeOpt = indRefDnSlope+1;

        float crUp = contxt.getCrUp(surfaceCat);
        float crDn = contxt.getCrDn(surfaceCat);
        float f0up = contxt.getF0u(surfaceCat);
        float f1Up = contxt.getF1u(surfaceCat);
        float f2Up = contxt.getF2u(surfaceCat);
        float f3Up = contxt.getF3u(surfaceCat);
        float cr0 = (crDn+crUp)/2f;
        float cr1 =  (0.1f*crDn + 0.9f*crUp);
        float sm20Dn = contxt.getDeltaSM20Dn(surfaceCat);
        float factorDn = contxt.getFactorDn(surfaceCat);
        float distFactCostFunct = contxt.getDistFactforCostFunct(surfaceCat);
        float refDnSlope = contxt.getRefDnSlope();

        float watt0 = contxt.getWatt0(surfaceCat);
        float watt  = contxt.getWatt(surfaceCat);

        long t1 = System.nanoTime();

        float[] slopes ;
        int sc = 0;
        slopes = new float[contxt.getSlopesAll().length];

        float ulstrech = costSpline ? contxt.getUlstrechCost(surfaceCat): contxt.getUlstrechDuration(surfaceCat);//ulstrechCost[scUpExt] : ulstrechDuration[scUpExt];

        for (float slope : contxt.getSlopesAll()) {
            if (slope <= 0)
              slopes[sc++] = slope;
            else
              slopes[sc++] = slope * ulstrech;
        }

                float[] durations = new float[slopes.length];
        boolean allSlopes = slopes.length == contxt.getSlopesAll().length;
        //      for slopes <=20% pure heuristic formulas apply that derivative of the duration function is equal to factorDn. For smaller slopes additional factors apply (f2d,f3d) to enforce positive
        //      curvature of the duration function
        durations[0] = ( sm20Dn -(slopes[0]-refDnSlope)*factorDn) * contxt.getF3d(surfaceCat); //f3d
        durations[1] = ( sm20Dn -(slopes[1]-refDnSlope)*factorDn) * contxt.getF2d(surfaceCat);//f2d;
        durations[2] =   sm20Dn -(slopes[2]-refDnSlope)*factorDn;
        durations[3] =   sm20Dn ;
        //      for everything with slope >=0% durations (sec/m) is calculated based on the speed derived from friction and input power (Watt)
        durations[slopes.length-5] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-5], watt0, cr0, ACw, m) ;
        durations[slopes.length-4] = f0up /  getFrictionBasedVelocity(slopes[slopes.length-4], watt, cr1, ACw, m) ;
        durations[slopes.length-3] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, crUp, ACw, m)  ;
        durations[slopes.length-2] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, crUp, ACw, m)  ;
        durations[slopes.length-1] = f3Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, crUp, ACw, m)  ;
        //      duration at -4% only used for the reference profiles.
        if (allSlopes && !contxt.getWithRef()) {
            durations[indRefDnSlopeOpt] = durations[slopes.length-5]+contxt.getRelSlope(surfaceCat)*slopes[indRefDnSlopeOpt];
        }

        if (costSpline && distFactCostFunct>0) {
//            if (surfaceCat < maxSL ) {
                for (int i = 0; i < durations.length; i++) {
                    if (slopes[i] < 0) durations[i] = durations[i] * distFactCostFunct;
                    else if (slopes[i] == 0.0f)
                        durations[i] = durations[i] * (1f + (distFactCostFunct - 1f) * 0.7f);
                    else {
                        durations[i] = durations[i] * (1f + (distFactCostFunct - 1f) * 0.3f);
                    }
                }
//            }
        }

//        String OptSpline = contxt.getWithRef() ? "with ref" : allSlopes ? "Optimized ref" :"ref";
        String SplineType = costSpline ? "cost":"dura";
        String contextString = String.format(Locale.ENGLISH,"spline=%s %s ",SplineType,getSurfaceCatTxt(surfaceCat));

        float slopeTarget = 0f;
        CubicSpline cubicSplineTmp;
        if (contxt.getWithRef()) {
            /* to achieve an almost constant downhill profile for a given mtbDn scale and downhill level of the profile (sDn) independent of the mtbUp scale an the uphill level of the profile (sUp)
            a reference profile for a given combination of sDn and mtbDn with a constant uphill profile (power = 100 Watt, sUp = 2 ) and mtbUp = mtbDn is calculated. All other uphill combinations are
            calculated in such a way that the slope at -20% is taken from the reference profile und the duration is varied at -4% slope, so that the slope matches the target slope
             */
//            int mtbDn = getMtbDn(surfaceCat);

            if (costSpline)
                cubicSplineTmp = contxt.getRefProfile().getCostSpline(contxt.getRefSc(surfaceCat));//getSurfaceCat(getSurfaceLevel(surfaceCat),mtbDn,mtbDn));
            else
                cubicSplineTmp = ((CostCalcSplineProfileMTB) contxt.getRefProfile()).getDurationSpline(contxt.getRefSc(surfaceCat));//getSurfaceCat(getSurfaceLevel(surfaceCat),mtbDn,mtbDn));
            for ( int i = 0; slopes[i]<0;i++) {
                durations[i] = cubicSplineTmp.calc(slopes[i]) ;//* factor;
            }
            slopeTarget = cubicSplineTmp.getSlope(indRefDnSlope); //*factor;
            cubicSplineTmp = getSlopeOptSpline(slopes, durations, indRefDnSlope, slopeTarget, indRefDnSlopeOpt);
        }
        else {
            cubicSplineTmp = getSpline(slopes, durations);
        }
        if (contxt.getWithRef() ) {
            float slope2slopeTarget = cubicSplineTmp.getSlope(indRefDnSlope) / slopeTarget;
            if (Math.abs(slope2slopeTarget - 1f) > 0.01f) {
                String msg = String.format(Locale.ENGLISH, "for %s Slope to Slopetarget=%.3f at %.2f", contextString, slope2slopeTarget, slopes[indRefDnSlope] * 100f);
                if (slope2slopeTarget > 0.5f && slope2slopeTarget < 2f)
                    mgLog.w(msg);
                else
                    throw new Exception("Out of range " + msg);
            }
        }

        checkNegCurvature(cubicSplineTmp,contextString,minNegCurvatureRadius);

        CubicSpline cubicSpline = cubicSplineTmp;
        long t = ( System.nanoTime() - t1 ) /1000;
        mgLog.v( ()-> {
            try {
                float slopeMin = cubicSpline.calcMin(-0.13f);
                float smMinOpt = cubicSpline.calc(slopeMin);
                float smVary = durations[indRefDnSlopeOpt];
                float sm0 = cubicSpline.calc(0f);
                return String.format(Locale.ENGLISH, "For %s t[µs]=%4d. Min at Slope=%6.2f, smMin=%.3f, vmax=%5.2f, smVary=%.3f, sm0=%.3f, v0=%.2f",
                        contextString,t,100f*slopeMin,smMinOpt,3.6f/smMinOpt,smVary,sm0,3.6f/sm0);
            } catch (Exception e) {
                return String.format(Locale.ENGLISH, "%s for %s",e.getMessage(),contextString);
            }
        });
        return cubicSpline;
    }

    private void checkNegCurvature(CubicSpline cubicSpline, String context, float threshold) throws Exception{
        ArrayList<CubicSpline.Value> curveRadiusForNegCurvaturePoint = cubicSpline.getCurveRadiusForNegCurvaturePoints();
        if (curveRadiusForNegCurvaturePoint != null) {
            StringBuilder msg = new StringBuilder(String.format(Locale.ENGLISH,"Negative curvature for %s",context));
            boolean criticalthresholdReached = false;
            boolean thresholdReached = false;
            for (CubicSpline.Value negCurvature : curveRadiusForNegCurvaturePoint) {
                float curvature = -negCurvature.y();
                if (curvature < threshold)
                    criticalthresholdReached = true;
                if ( curvature < 50f ) {
                    thresholdReached = true;
                    msg.append(": ");
                    msg.append(String.format(Locale.ENGLISH, " slope=%.2f", 100 * negCurvature.x()));
                    msg.append(String.format(Locale.ENGLISH, " curve Radius=%.2f", -negCurvature.y()));
                }
            }
            if(criticalthresholdReached)
                throw new Exception( msg.toString());
            else
                if (thresholdReached) mgLog.w(msg.toString());
        }
    }



    protected CubicSpline getDurationSpline(int surfaceCat) throws Exception{
        CubicSpline cubicSpline;
        cubicSpline = SurfaceCatDurationSpline[surfaceCat];
        if (cubicSpline == null) {
              if (hasCostSpline(surfaceCat)) cubicSpline = calcSpline(false,surfaceCat,getContext() ); // calculate dedicated duration function
              else  cubicSpline = getCostSpline(surfaceCat);             // reuse existing cost function = duration function
              SurfaceCatDurationSpline[surfaceCat] = cubicSpline;
        }
        return cubicSpline;
    }

    private boolean hasCostSpline(int surfaceCat){
        return true;
    }

    /*
       SurfaceCat is devided in 3 ranges:
       -the first few ones (SurfaceCat < maxSL ) where there is no mtb classification neither up nor down available.
        In this range the surfaceCat is equal to the surfaceLevel, which starts with very smooth big asphalt streets and ends with trails(path) without any mtb classification.
       -than all values for those where both a down and up classification is available ( maxSL <= SurfaceCat <= maxCatUpDn ).
        SurfaceCat is calculated by the difference of up minus down. However the difference is limited to 3 levels ( E.g. for MtbDn = 1 1<=MtbUp<=4 ) and each combination is represented
        by one SurfaceCat
        -last but not least all values where only down classification is given ( maxCatUpDn < SurfaceCat <= maxSurfaceCat ). Here one surfaceCat is reserved for each MtbDn
     */


    public String getSurfaceCatTxt(int surfaceCat){
        IfSplineProfileContext contxt = (IfSplineProfileContext) getContext();
        return contxt.getSurfaceCatTxt(surfaceCat);
    }


}


