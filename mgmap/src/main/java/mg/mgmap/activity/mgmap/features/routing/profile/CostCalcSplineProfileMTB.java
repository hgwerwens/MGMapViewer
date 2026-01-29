package mg.mgmap.activity.mgmap.features.routing.profile;


import androidx.annotation.NonNull;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.IfProfileContextMTB;
import mg.mgmap.activity.mgmap.features.routing.IfProfileContext;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {

    public static class Context implements IfProfileContextMTB {
        private CostCalcSplineProfileMTB refProfile;
        public final int power;
        public final int sUp;
        public final int sDn;
        private final boolean withRef;
        private final boolean checkAll;
        float[] ulstrechDuration;
        float[] ulstrechCost;
        private final float[] fpower;
        private final float[] f0u;
        private final float[] f1u;
        private final float[] f2u;
        private final float[] f3u;
        private final float f2d;
        private final float f3d;
        private final float[] crUp;
        private final float[] crDn;
        private final float[] srelSlope;
        private final float[] deltaSM20Dn;
        private final float[] factorDown;
        private final float[] distFactforCostFunct;
        private final float[] watt;
        private final float[] watt0;
        private final float[] slopesAll;
        int indRefDnSlope;
        int indRefDnSlopeOpt;

        private Context(int power, int sUp, int sDn,boolean checkAll, boolean withRef ){
            this.power = power;
            this.sUp   = sUp;
            this.sDn   = sDn;
            this.withRef = withRef;
            this.checkAll = checkAll;
            if (withRef) refProfile = new CostCalcSplineProfileMTB(new Context(100,200, sDn,false,false));
            slopesAll = new float[]{-0.76f, -0.36f, -0.32f, refDnSlope, refDnSlopeOpt, 0.0f, 0.065f,0.17f,0.195f, 0.275f};
            slopesAll[2] = -0.26f -  (0.05f*sDn/100f);
            slopesAll[1] = slopesAll[2]-0.04f;
            slopesAll[0] = slopesAll[1]-0.4f;
            indRefDnSlope = 3;
            indRefDnSlopeOpt = indRefDnSlope + 1;
            int sc;

            double off;
            float sig;

            ulstrechDuration = new float[maxScUpExt];
            ulstrechCost     = new float[maxScUpExt];
            fpower = new float[maxScUpExt];
            f0u = new float[maxScUpExt];
            f1u = new float[maxScUpExt];  // factor on top of friction based duration calculation
            f2u = new float[maxScUpExt];  // factor on top of friction based duration calculation
            f3u = new float[maxScUpExt];  // factor on top of friction based duration calculation
            crUp = new float[maxScUpExt]; // uphill friction
            crDn = new float[maxScDn]; // downhill friction
            srelSlope = new float[maxScDn]; // slope of auxiliary function for duration function at 0% slope to get to -4% slope
            deltaSM20Dn = new float[maxScDn]; // duration (sec/m) at -20% slope
            factorDown  = new float[maxScDn]; // slope of the duration function lower -20%
            distFactforCostFunct = sdistFactforCostFunct;// factor on top of duration function for certain slopes to get a better cost function
            watt = new float[maxSurfaceCat];
            watt0 = new float[maxSurfaceCat];

            float deltaSM20DnMin = 0.05f + IfProfileContextMTB.dSM20scDnLow(0) + 0.52f * (float) Math.exp(-sDn/100d * 0.4d);
            for (int scDn = maxSL+1; scDn<maxScDn;scDn++){
                int lscDn = scDn - ( maxSL + 1 );
                off = lscDn - sDn/100d;
                crDn[scDn] =  (0.02f + 0.005f*(scDn-(maxSL+1)) + 0.05f* IfProfileContextMTB.sig(2d*(2d-off)));
                srelSlope[scDn]  =  ssrelSlope[scDn]+0.5f- IfProfileContextMTB.sig((sDn/100d-2d));

                deltaSM20Dn[scDn] = deltaSM20DnMin +  (0.5f* IfProfileContextMTB.sig((1.5d-off)*2d))+lscDn*0.025f;
                factorDown[scDn] = deltaSM20Dn[scDn]*7.5f;
            }
            float deltaSM20DnMinscLow = deltaSM20Dn[maxSL+1];


            for (sc = 0; sc<maxScUpExt;sc++){
                if (sc < maxSL) {

                    ulstrechDuration[sc] = 1f+0.18f*sUp/100;
                    ulstrechCost[sc]     = 1.3f+0.18f*sUp/100 - 0.5f *  IfProfileContextMTB.sig((3.5-sc)*2.); //( sc > 2 ? 0.2f * (sc - 2) : 0f );
                    sig =  IfProfileContextMTB.sig((3.5-sc)*2.);
                    fpower[sc] = 1.0f;// - ( sc > 2 ? 0.05f * (sc - 2) : 0f );
                    f0u[sc] =  1.0f + 0.15f * sig;// + ( sc > 2 ? 0.05f * (sc - 2) : 0f );
                    f1u[sc] =  1.1f + 0.15f *  sig;
                    f2u[sc] = ( 1.1f )*f1u[sc] ;
                    f3u[sc] = 2.2f + 0.4f *  sig;

                    crUp[sc] =  (0.0047f + 0.029f* IfProfileContextMTB.sig((3.5-sc)*1.3));
                    crDn[sc] = crUp[sc];
                    srelSlope[sc]  = ssrelSlope[sc] +  ( 0.5f *( 0.5f - IfProfileContextMTB.sig(sDn/100f-2)));
                    deltaSM20Dn[sc] = deltaSM20DnMinscLow - IfProfileContextMTB.dSM20scDnLow(sc);
                    factorDown[sc] = deltaSM20Dn[sc]*10f;
                }
                else if(sc>maxSL && sc <maxScUp){
                    int scUp = sc - ( maxSL + 1 );
                    off = scUp - sUp/100d;
                    sig =  IfProfileContextMTB.sig((0.5-off)*2.);
                    ulstrechDuration[sc] =  (1f  +0.18f*sUp/100 - 0.1f*sig);
                    ulstrechCost[sc] =      (0.80f+0.18f*sUp/100 - 0.4f*sig);

                    fpower[sc] = 1.0f; // (1.0f-0f*sig((1.5-off)*2.));
                    f0u[sc] =  1.0f;
                    f1u[sc] =  (1.2f+0.15f* IfProfileContextMTB.sig((1.5-off)*2.));
                    f2u[sc] =  1.1f*f1u[sc] ;
                    f3u[sc] = 2.2f;
                    crUp[sc] =  (0.02f + 0.005f*scUp + 0.05f* IfProfileContextMTB.sig(2d*(2d-off)));
                } else if (sc!=maxSL) {
                    int scUp = sc -  maxScUp;
                    off = scUp - sUp/100d;
                    sig =  IfProfileContextMTB.sig((-0.5-off)*2.);
                    ulstrechDuration[sc] =  (1f  +0.18f*sUp/100 - 0.1f*sig);
                    ulstrechCost[sc] =      (0.70f+0.18f*sUp/100 - 0.4f*sig);

                    fpower[sc] = 1.0f; //  (1.0f-0f*sig((0.5d-off)*2.));
                    f0u[sc] =  1.0f;
                    f1u[sc] =  (1.25f+0.15f* IfProfileContextMTB.sig((0.5d-off)*2.));
                    f2u[sc] = 1.10f * f1u[sc]; // ( 1.1 + 0.03*sig )*f1u[sc] ;
                    f3u[sc] = 2.35f;
                    crUp[sc] =  (0.025f + 0.005f*scUp + 0.05f* IfProfileContextMTB.sig(2d*(1d-off)));
                }
            }

            f2d = 1.07f;
            f3d = 3.0f;

            ulstrechDuration[maxSL] = 1f+0.18f*sUp/100;
            ulstrechCost[maxSL]     = (0.40f+0.18f*sUp/100);

            f0u[maxSL] =  1.0f;
            f1u[maxSL]=1.35f;
            f2u[maxSL]=f1u[maxSL]*1.15f;
            f3u[maxSL]=2.9f;
            crUp[maxSL] = 0.03f;
            crDn[maxSL] = 0.02f;
            srelSlope[maxSL] = srelSlope[maxSL+1];

            deltaSM20Dn[maxSL] = deltaSM20Dn[maxSL+1+2];
            factorDown[maxSL]  = deltaSM20Dn[maxSL+1+3]*(7.5f + 5f* IfProfileContextMTB.sig(2.*(sDn/100.-1.)));

            float watt0 = power;
            float watt  = 1.7f*power;
            for (sc=0;sc<maxSurfaceCat;sc++){
               int scDn = IfProfileContextMTB.getCatDn(sc) ;
               int scUp = IfProfileContextMTB.getCatUpExt(sc);
               float crUp = this.crUp[scUp];
               float crDn = this.crDn[scDn];
               float cr0 = (crUp + crDn)/2f;
               float f0 =  IfProfileContextMTB.sig((0.05d-cr0)*100d);
               float watt0_high =  watt0+(watt-watt0)*f0;
               float watt0_base = 100f + (175f-100f)*f0;
               float f =  IfProfileContextMTB.sig((0.05d-crUp)*100d);
               this.watt0[sc] = watt0_high>watt0_base ? watt0_high+(watt0_base-watt0_high)*f: watt0_high;
               this.watt[sc] =  watt > 175 ? watt + (175-watt)*f : watt;
            }
        }

        public Context(int power, int sUp, int sDn,boolean checkAll){ this(power,sUp,sDn,checkAll,true); }

        public Context(int power, int sUp, int sDn){
            this(power,sUp,sDn,true);
        }

        public Context( int sUp, int sDn,boolean checkAll){
            this((int)(47.5+25*sUp/100d),sUp,sDn,checkAll,true);
        }
        public Context( int sUp, int sDn){this(sUp,sDn,true);}

        @NonNull
        public String toString(){
            return String.format( Locale.ENGLISH,"power=%s sUp=%s sDn=%s withRef=%s",power,sUp,sDn, withRef);
        }

        public boolean fullCalc(){
            return checkAll && withRef;
        }

        @Override
        public boolean getWithRef() {
            return withRef;
        }

        @Override
        public CostCalcSplineProfile getRefProfile() {
            return refProfile;
        }

        @Override
        public int getRefSc(int sc) {
            int scDn = IfProfileContextMTB.getMtbDn(sc);
            return IfProfileContextMTB.getSurfaceCat(IfProfileContextMTB.getSurfaceLevel(sc),scDn,scDn);
        }

        @Override
        public int getIndRefDnSlope() {
            return 3;
        }

        @Override
        public float[] getSlopesAll() {
            return slopesAll;
        }

        @Override
        public float getRelSlope(int sc) {
            return srelSlope[IfProfileContextMTB.getCatDn(sc)];
        }

        @Override
        public float getF3d(int sc) {
            return f3d;
        }

        @Override
        public float getF2d(int sc) {
            return f2d;
        }

        @Override
        public float getDeltaSM20Dn(int sc) {
            return deltaSM20Dn[IfProfileContextMTB.getCatDn(sc)];
        }

        @Override
        public float getFactorDn(int sc) {
            return factorDown[IfProfileContextMTB.getCatDn(sc)];
        }

        @Override
        public float getCrDn(int sc) {
            return crDn[IfProfileContextMTB.getCatDn(sc)];
        }

        @Override
        public float getCrUp(int sc) {
            return crUp[IfProfileContextMTB.getCatUpExt(sc)];
        }

        @Override
        public float getF0u(int sc) {
            return f0u[IfProfileContextMTB.getCatUpExt(sc)];
        }

        @Override
        public float getF1u(int sc) {
            return f1u[IfProfileContextMTB.getCatUpExt(sc)];
        }

        @Override
        public float getF2u(int sc) {
            return f2u[IfProfileContextMTB.getCatUpExt(sc)];
        }

        @Override
        public float getF3u(int sc) {
            return f3u[IfProfileContextMTB.getCatUpExt(sc)];
        }

        @Override
        public float getUlstrechCost(int sc) {
            return ulstrechCost[IfProfileContextMTB.getCatUpExt(sc)];
        }

        @Override
        public float getUlstrechDuration(int sc) {
            return ulstrechDuration[IfProfileContextMTB.getCatUpExt(sc)];
        }

        @Override
        public float getDistFactforCostFunct(int sc) {
            int scUpExt = IfProfileContextMTB.getCatUpExt(sc);
            return scUpExt < distFactforCostFunct.length ? distFactforCostFunct[scUpExt] : 0f;
        }

        @Override
        public String getSurfaceCatTxt(int sc) {
            return String.format(Locale.ENGLISH,"%s SurfaceCat=%s SurfaceLevel=%s mtbDn=%s mtbUp=%s",this,sc,IfProfileContextMTB.getSurfaceLevel(sc), IfProfileContextMTB.getMtbDn(sc), IfProfileContextMTB.getMtbUp(sc));
        }

        @Override
        public float getWatt0(int sc) {
            return watt0[sc];
        }

        @Override
        public float getWatt(int sc) {
            return watt[sc];
        }
    }

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final float ACw = 0.45f;   // air friction of a typical bicycle rider
    private static final float m = 90f; // system weigth of a typical bicycle with rider
    public static final float minNegCurvatureRadius= 0.01f;
    protected final CubicSpline[] SurfaceCatDurationSpline;


    public CostCalcSplineProfileMTB(Object context) {
        super(context);
        SurfaceCatDurationSpline = new CubicSpline[((IfProfileContext) context).getMaxSurfaceCat()];
        if (fullCalc(context)) checkAll();
    }
    public int getMaxSurfaceCat(){
        return IfProfileContextMTB.maxSurfaceCat;
    }
    protected float sig(double base){
        return (float) (1./(1.+Math.exp(base)));
    }
    private void checkAll() {
        for ( int surfaceCat = 0 ; surfaceCat < IfProfileContextMTB.maxSurfaceCat; surfaceCat++){
            try {
               getDurationSpline(surfaceCat);
            } catch (Exception e) {
                mgLog.e(e.getMessage());
            }
        }
    }
    protected CubicSpline getProfileSpline(Object context ) {
        try {
            IfProfileContext contxt = (IfProfileContext) context;
            return getCostSpline(contxt.getScProfileSpline());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CubicSpline getHeuristicRefSpline(Object context) {
        try {
            IfProfileContext contxt = (IfProfileContext) context;
            CubicSpline cubicSplineTmp = getCostSpline(contxt.getScHeuristicRefSpline());
            CubicSpline cubicSpline = cubicSplineTmp.getTransYCubicSpline(-0.0001f);
            checkNegCurvature(cubicSpline,String.format(Locale.ENGLISH,"heuristic spline for %s ", context),1e7f);
            return cubicSpline;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected boolean fullCalc(Object context){
        Context contxt = (Context) context;
        return contxt.fullCalc();
    }

    protected float getMinDistFactSC0(){
        return 1f;
    }

    protected CubicSpline calcSpline(int surfaceCat, Object context) throws Exception {
      return calcSpline(true, surfaceCat,context );
    }

    private CubicSpline calcSpline(boolean costSpline, int surfaceCat, Object context) throws Exception {
        IfProfileContext contxt = (IfProfileContext) context;
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
        float cr1 = (0.1f*crDn + 0.9f*crUp);
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
                    else if ( i < slopes.length-1)
                        durations[i] = durations[i] * (1f + (distFactCostFunct - 1f) * 0.3f);
                    else
                        durations[i] = durations[i] * (1f + (distFactCostFunct - 1f) * 0.3f);
                }
//            }
        }

        String OptSpline = contxt.getWithRef() ? "with ref" : allSlopes ? "Optimized ref" :"ref";
        String SplineType = costSpline ? " cost":" duration";
        String contextString = String.format(Locale.ENGLISH,"%s%s spline %s ",OptSpline,SplineType,getSurfaceCatTxt(surfaceCat));

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
        IfProfileContext contxt = (IfProfileContext) getContext();
        return contxt.getSurfaceCatTxt(surfaceCat);
    }


}


