package mg.mgmap.activity.mgmap.features.routing.profile;


import androidx.annotation.NonNull;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {

    public static class Context {
        private CostCalcSplineProfileMTB refProfile;
        public final int power;
        public final int sUp;
        public final int sDn;
        private final boolean withRef;
        public Context(int power, int sUp, int sDn, boolean withRef){
            this.power = power;
            this.sUp   = sUp;
            this.sDn   = sDn;
            this.withRef = withRef;
        }
        public Context(int power, int sUp, int sDn){
            this(power,sUp,sDn,true);
        }

        public Context( int sUp, int sDn){
//            this(60+20*sUp/100+sUp*sUp/20000,sUp,sDn,0,true);
            this((int)(47.5+25*sUp/100d),sUp,sDn,true);
        }

        @NonNull
        public String toString(){
            return String.format( Locale.ENGLISH,"power=%s sUp=%s sDn=%s withRef=%s",power,sUp,sDn, withRef);
        }
    }

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    protected static final int maxSL = 6; // Surface categories without MTB scale
    private static final int maxUptoDn = 3; // maximum difference mtbUp - mtbDn considered
    private static final int maxDn = 6;     // maximum downhill category
    private static final int maxScDn = maxSL+1+maxDn +1; // maximum number of factors depending on the surfaced category for downhill calculation
    private static final int maxScUp = maxScDn;         // maximum number of factors depending on the surfaced category for uphill calculation
    private static final int maxScUpExt = maxScUp + maxDn+1;
    // Heuristic is derived from a path with mtbDn = 0 and mtbUp = 0. All other surface categories have higher costs, either because they are disfavored like for anything without mtb classification or because they more difficult
    private static final int HeuristicRefSurfaceCat = 7;
    private static final float[] sdistFactforCostFunct = {  3.05f   ,2.4f ,2.0f  ,1.70f ,1.5f  ,1.4f }; //factors to increase costs compared to durations to get better routing results
    private static final float[] ssrelSlope            = {  1.4f   ,1.2f ,1f    ,1f    ,1f    ,1f  , 0f    ,1.2f  ,1.2f  ,1.2f  ,1.2f  ,1.2f ,1f   ,1f }; //slope of auxiliary function for duration function at 0% slope to get to -4% slope

    private static final int maxCatUpDn    = maxSL + 1 + (maxUptoDn+1)*(maxDn+1); // all surface categories including those ones without any mtb classification and those ones with up and down classification
    private static final int maxSurfaceCat = maxCatUpDn + maxDn + 1 ; // includes on top those ones, which have only downhill classification
    private static final float refDnSlopeOpt = -0.04f; // slope at which cost function is optimized against slope of reference function
    private static final float refDnSlope = -0.2f; // slope which all profiles use
    private static final float ACw = 0.45f;   // air friction of a typical bicycle rider
    private static final float m = 90f; // system weigth of a typical bicycle with rider

    protected final CubicSpline[] SurfaceCatDurationSpline = new CubicSpline[maxSurfaceCat];
    private float[] slopesAll;

    private int indRefDnSlopeOpt;
    private int indRefDnSlope;

    float[] ulstrechDuration;
    float[] ulstrechCost;
    private float[] f1u;
    private float[] f2u;
    private float[] f3u;
    private float f2d;
    private float f3d;
    private float[] crUp;
    private float[] crDn;
    private float[] srelSlope;
    private float[] deltaSM20Dn;
    private float[] factorDown;
    private float[] distFactforCostFunct;
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
                contxt.refProfile = new CostCalcSplineProfileMTB(new Context(100,200,contxt.sDn,false));
            slopesAll = new float[]{-0.76f, -0.36f, -0.32f, refDnSlope, refDnSlopeOpt, 0.0f, 0.065f,0.17f,0.195f, 0.275f};
            indRefDnSlope = 3;
            indRefDnSlopeOpt = indRefDnSlope + 1;
            int sc;

            double off;
            double sig;

            ulstrechDuration = new float[maxScUpExt];
            ulstrechCost     = new float[maxScUpExt];
            f1u = new float[maxScUpExt];  // factor on top of friction based duration calculation
            f2u = new float[maxScUpExt];  // factor on top of friction based duration calculation
            f3u = new float[maxScUpExt];  // factor on top of friction based duration calculation
            crUp = new float[maxScUpExt]; // uphill friction
            crDn = new float[maxScDn]; // downhill friction
            srelSlope = new float[maxScDn]; // slope of auxiliary function for duration function at 0% slope to get to -4% slope
            deltaSM20Dn = new float[maxScDn]; // duration (sec/m) at -20% slope
            factorDown  = new float[maxScDn]; // slope of the duration function lower -20%
            distFactforCostFunct = sdistFactforCostFunct;// factor on top of duration function for certain slopes to get a better cost function

            float deltaSM20DnMin = 0.05f + dSM20scDnLow(0) + 0.52f * (float) Math.exp(-sDn/100d * 0.4d);
            for (int scDn = maxSL+1; scDn<maxScDn;scDn++){
                int lscDn = scDn - ( maxSL + 1 );
                off = lscDn - sDn/100d;
                crDn[scDn] = (float) (0.02 + 0.005*(scDn-(maxSL+1)) + 0.05*sig(2d*(2d-off)));
                srelSlope[scDn]  =  ssrelSlope[scDn]+0.5f-(float) sig((sDn/100d-2d));

                deltaSM20Dn[scDn] = deltaSM20DnMin + (float) (0.5*sig((1.5d-off)*2d))+lscDn*0.025f;
                factorDown[scDn] = deltaSM20Dn[scDn]*7.5f;
            }
            float deltaSM20DnMinscLow = deltaSM20Dn[maxSL+1];

            for (sc = 0; sc<maxScUpExt;sc++){
                if (sc < maxSL) {

                    ulstrechDuration[sc] = 1f+0.18f*sUp/100;
                    ulstrechCost[sc] = 0.9f+0.18f*sUp/100;

                    f1u[sc] =  1.1f;
                    f2u[sc] = ( 1.1f )*f1u[sc] ;
                    f3u[sc] = 2.2f;

                    crUp[sc] = (float) (0.0047 + 0.029*sig((3.5-sc)*1.3));
                    crDn[sc] = crUp[sc];
                    srelSlope[sc]  = ssrelSlope[sc] + (float) ( 0.5 *( 0.5 - sig(sDn/100f-2)));
                    deltaSM20Dn[sc] = deltaSM20DnMinscLow - dSM20scDnLow(sc);
                    factorDown[sc] = deltaSM20Dn[sc]*10f;
                }
                else if(sc>maxSL && sc <maxScUp){
                    int scUp = sc - ( maxSL + 1 );
                    off = scUp - sUp/100d;
                    sig = sig((0.5-off)*2.);
                    ulstrechDuration[sc] = (float) (1f  +0.18f*sUp/100 - 0.1f*sig);
                    ulstrechCost[sc] =     (float) (0.8f+0.18f*sUp/100 - 0.4f*sig);

                    f1u[sc] = (float) (1.1+0.15*sig((1.5-off)*2.));
                    f2u[sc] =  1.07f*f1u[sc] ;
                    f3u[sc] = 2.2f;
                    crUp[sc] = (float) (0.02 + 0.005*scUp + 0.05*sig(2d*(2d-off)));
                } else if (sc!=maxSL) {
                    int scUp = sc -  maxScUp;
                    off = scUp - sUp/100d;
                    sig = sig((-0.5-off)*2.);
                    ulstrechDuration[sc] = (float) (1f  +0.18f*sUp/100 - 0.1f*sig);
                    ulstrechCost[sc] =     (float) (0.7f+0.18f*sUp/100 - 0.4f*sig);

                    f1u[sc] = (float) (1.1+0.15*sig((0.5d-off)*2.));
                    f2u[sc] = 1.08f * f1u[sc]; //(float) ( 1.1 + 0.03*sig )*f1u[sc] ;
                    f3u[sc] = 2.45f;
                    crUp[sc] = (float) (0.02 + 0.005*(scUp+1d) + 0.05*sig(2d*(1d-off)));
                }
            }

            f2d = 1.07f;
            f3d = 3.0f;

            ulstrechDuration[maxSL] = 1f+0.18f*sUp/100;
            ulstrechCost[maxSL]     = 0.7f+0.18f*sUp/100;

            f1u[maxSL]=1.35f;
            f2u[maxSL]=f1u[maxSL]*1.15f;
            f3u[maxSL]=2.4f;
            crUp[maxSL] = 0.03f;
            crDn[maxSL] = 0.02f;
            srelSlope[maxSL] = srelSlope[maxSL+1];

            deltaSM20Dn[maxSL] = deltaSM20Dn[maxSL+1+2];
            factorDown[maxSL]  = deltaSM20Dn[maxSL+1+3]*(7.5f + 5f*(float)sig(2.*(sDn/100.-1.)));

            slopesAll[2] = -0.26f - (float) (0.05*sDn/100.);
            slopesAll[1] = slopesAll[2]-0.04f;
            slopesAll[0] = slopesAll[1]-0.4f;

            watt0 =   contxt.power;
            watt = 1.7f*watt0; // once one rides uphill the power used typically goes up quite substantially and reduces the effective duration
        }

    }

    protected int getMaxSurfaceCat(){
        return maxSurfaceCat;
    }

    protected double sig(double base){
        return 1./(1.+Math.exp(base));
    }

    private float dSM20scDnLow(int scDn){
        return 0.2f * ((float) sig(1.5 * (scDn - 2.)) - 0.5f);
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
            return getCostSpline(maxSL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CubicSpline getHeuristicRefSpline(Object context) {
        try {
            CubicSpline cubicSplineTmp = getCostSpline(HeuristicRefSurfaceCat);
            CubicSpline cubicSpline = cubicSplineTmp.getTransYCubicSpline(-0.0001f);
            checkNegCurvature(cubicSpline,String.format(Locale.ENGLISH,"heuristic spline for %s ",context.toString()),1e7f);
            return cubicSpline;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected boolean fullCalc(Object context){
        Context contxt = (Context) context;
        return contxt.withRef;
    }

    protected float getMinDistFactSC0(){
        return 1f;
    }

    protected CubicSpline calcSpline(int surfaceCat, Object context) throws Exception {
      return calcSpline(true, surfaceCat,context );
    }

    private CubicSpline calcSpline(boolean costSpline, int surfaceCat, Object context) throws Exception {
        setFromContext(context);

        if (getCatUp(surfaceCat) >= maxScUp) return null;

        int scUpExt = getCatUpExt(surfaceCat);
        int scDn = getCatDn(surfaceCat);

        Context contxt = (Context) context;

        float cr = crUp[scUpExt];
        float crD = crDn[scDn];
        float f1Up = f1u[scUpExt];
        float f2Up = f2u[scUpExt];
        float f3Up = f3u[scUpExt];
        float cr0 = (crD+cr)/2f;          // friction at 0% slope
        float cr1 = (0.1f*crD + 0.9f*cr); // friction at first slope after 0%
        float sm20Dn = deltaSM20Dn[scDn]; // duration (sec/m) at -20% slope
        float factorDn = factorDown[scDn];

        float distFactCostFunct = scUpExt < distFactforCostFunct.length ? distFactforCostFunct[scUpExt] : 0f;
//        boolean isHeuristicRefSpline = isHeuristicRefSpline(surfaceCat);

        float f0 = (float) sig((0.05d-cr0)*100d);
        float watt0_high = this.watt0+(this.watt-this.watt0)*f0;
        float watt0_base = 100f + (175f-100f)*f0;

        float f = (float) sig((0.05d-cr)*100d);
        float watt0 = watt0_high>watt0_base ? watt0_high+(watt0_base-watt0_high)*f: watt0_high;
        float watt = this.watt > 175 ? this.watt + (175-this.watt)*f : this.watt;

        long t1 = System.nanoTime();

        float[] slopes ;
        int sc = 0;
        slopes = new float[slopesAll.length];

        float ulstrech = costSpline ? ulstrechCost[scUpExt] : ulstrechDuration[scUpExt];

        for (float slope : slopesAll) {
            if (slope <= 0)
              slopes[sc++] = slope;
            else
              slopes[sc++] = slope * ulstrech;
        }

                float[] durations = new float[slopes.length];
        boolean allSlopes = slopes.length == slopesAll.length;
        //      for slopes <=20% pure heuristic formulas apply that derivative of the duration function is equal to factorDn. For smaller slopes additional factors apply (f2d,f3d) to enforce positive
        //      curvature of the duration function
        durations[0] = ( sm20Dn -(slopes[0]-refDnSlope)*factorDn) * f3d;
        durations[1] = ( sm20Dn -(slopes[1]-refDnSlope)*factorDn) * f2d;
        durations[2] =   sm20Dn -(slopes[2]-refDnSlope)*factorDn;
        durations[3] =   sm20Dn ;
        //      for everything with slope >=0% durations (sec/m) is calculated based on the speed derived from friction and input power (Watt)
        durations[slopes.length-5] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-5], watt0, cr0, ACw, m) ;
        durations[slopes.length-4] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-4], watt, cr1, ACw, m) ;
        durations[slopes.length-3] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr, ACw, m)  ;
        durations[slopes.length-2] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr, ACw, m)  ;
        durations[slopes.length-1] = f3Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr, ACw, m)  ;
        //      duration at -4% only used for the reference profiles.
        if (allSlopes && !contxt.withRef) {
            durations[indRefDnSlopeOpt] = durations[slopes.length-5]+srelSlope[scDn]*slopes[indRefDnSlopeOpt];
        }

        if (costSpline && distFactCostFunct>0) {
            if (surfaceCat < maxSL ) {
                for (int i = 0; i < durations.length; i++) {
                    if (slopes[i] < 0) durations[i] = durations[i] * distFactCostFunct;
                    else if (slopes[i] == 0.0f)
                        durations[i] = durations[i] * (1f + (distFactCostFunct - 1f) * 0.7f);
                    else durations[i] = durations[i] * (1f + (distFactCostFunct - 1f) * 0.3f);
                }
            }
        }

        String OptSpline = contxt.withRef ? "with ref" : allSlopes ? "Optimized ref" :"ref";
        String SplineType = costSpline ? " cost":" duration";
        String contextString = String.format(Locale.ENGLISH,"%s%s spline %s ",OptSpline,SplineType,getSurfaceCatTxt(surfaceCat));

        float slopeTarget = 0f;
        CubicSpline cubicSplineTmp;
        if (contxt.withRef) {
            /* to achieve an almost constant downhill profile for a given mtbDn scale and downhill level of the profile (sDn) independent of the mtbUp scale an the uphill level of the profile (sUp)
            a reference profile for a given combination of sDn and mtbDn with a constant uphill profile (power = 100 Watt, sUp = 2 ) and mtbUp = mtbDn is calculated. All other uphill combinations are
            calculated in such a way that the slope at -20% is taken from the reference profile und the duration is varied at -4% slope, so that the slope matches the target slope
             */
            int mtbDn = getMtbDn(surfaceCat);
            if (costSpline)
                cubicSplineTmp = contxt.refProfile.getCostSpline(getSurfaceCat(getSurfaceLevel(surfaceCat),mtbDn,mtbDn));
            else
                cubicSplineTmp = contxt.refProfile.getDurationSpline(getSurfaceCat(getSurfaceLevel(surfaceCat),mtbDn,mtbDn));
            for ( int i = 0; slopes[i]<0;i++) {
                durations[i] = cubicSplineTmp.calc(slopes[i]) ;//* factor;
            }
            slopeTarget = cubicSplineTmp.getSlope(indRefDnSlope); //*factor;
            cubicSplineTmp = getSlopeOptSpline(slopes, durations, indRefDnSlope, slopeTarget, indRefDnSlopeOpt);
        }
        else {
            cubicSplineTmp = getSpline(slopes, durations);
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

        checkNegCurvature(cubicSplineTmp,contextString,0.01f);

        CubicSpline cubicSpline = cubicSplineTmp;
        long t = System.nanoTime() - t1;
        mgLog.v( ()-> {
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
        return ( scDn < distFactforCostFunct.length  ) ||
               ( scUp < distFactforCostFunct.length  );
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
                    return maxCatUpDn+mtbDn;
//                    scUp = 1; scDn = mtbDn;
                }
            else if (mtbUp > -1) {
                scUp = mtbUp == 0?0:1;
                scDn = mtbUp == 0?0:mtbUp-1;
            } else {
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
        if (surfaceCat <= maxSL  )           return -1;
        else  if (surfaceCat < maxCatUpDn)   return (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                                 return -1;
    }

    // calculates uphill category based on surface category. Its either between 0 and 6 for anything without MTB classification or 6 + 1 + uphill classification (mtbUp)
    private int getCatUp(int surfaceCat){
        if (surfaceCat <= maxSL)           return surfaceCat;
        else if (surfaceCat < maxCatUpDn)  return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                               return maxSL;
    }

    private int getCatUpExt(int surfaceCat){
        if (surfaceCat <= maxSL)           return surfaceCat;
        else if (surfaceCat < maxCatUpDn)  return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                               return surfaceCat - maxCatUpDn + maxScUp;
    }


    // calculates uphill category based on surface category. Its either between 0 and 6 for anything without MTB classification or 6 + 1 + downhill classification (mtbDn)
    protected int getMtbDn(int surfaceCat){
        if (surfaceCat <= maxSL)          return -1;
        else if (surfaceCat < maxCatUpDn) return (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                              return surfaceCat - maxCatUpDn;
    }

    protected int getCatDn(int surfaceCat){
        if (surfaceCat <= maxSL)          return surfaceCat;
        else if (surfaceCat < maxCatUpDn) return maxSL+1 + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else
            return maxSL+1 + surfaceCat - maxCatUpDn;
    }
}


