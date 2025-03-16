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
            this(50+25*sUp/100,sUp,sDn,true);
        }

        @NonNull
        public String toString(){
            return String.format( Locale.ENGLISH,"power=%s sUp=%s sDn=%s withRef=%s",power,sUp,sDn, withRef);
        }
    }

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    protected static final int maxSL = 6; // SurfaceLevels without MTB scale
    private static final int deltaUptoDn = 1; // mtbUp = mtbDn + 1 in case only mtbDn is specified
    private static final int maxUptoDn = 3; // maximum difference mtbUp - mtbDn considered
    private static final int maxDn = 6;
    private static final int maxScDn = maxSL+1+maxDn+1;
    private static final int maxScUp = maxScDn+maxUptoDn;

    private static final int[] HeuristicRefSurfaceCats = {7};

    private static final float[] sdistFactforCostFunct = {  3f   ,2.4f  ,2f    ,1.70f ,1.5f  ,1.4f, 1.6f };
    private static final float[] ssrelSlope            = { 1.4f  ,1.25f ,1f    ,1f    ,1f    ,1f   ,0f    ,1.4f  ,1.4f  ,1.5f  ,1.5f  ,1.2f ,1f   ,1f };

    private static final int maxSurfaceCat = maxSL + 1 + (maxUptoDn+1)*(maxDn+1);
    private static final float refDnSlopeOpt = -0.04f;
    private static final float refDnSlope = -0.2f;
    private static final float ACw = 0.45f;
    private static final float m = 90f;

    protected final CubicSpline[] SurfaceCatDurationSpline = new CubicSpline[maxSurfaceCat];
    private float[] slopesAll;

    private int indRefDnSlopeOpt;
    private int indRefDnSlope;

    private float[] f1u;
    private float[] f2u;
    private float[] f3u;
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
            slopesAll = new float[]{-2f, -0.4f, refDnSlope, refDnSlopeOpt, 0.0f, 0.1f,0.2f,0.24f, 0.34f};
            indRefDnSlope = 2;
            indRefDnSlopeOpt = indRefDnSlope + 1;
            int sc;

            double off;
            double sig;

            f1u = new float[maxScUp];
            f2u = new float[maxScUp];
            f3u = new float[maxScUp];
            crUp = new float[maxScUp];
            crDn = new float[maxScDn];
            srelSlope = new float[maxScDn];
            deltaSM20Dn = new float[maxScDn];
            factorDown  = new float[maxScDn];
            distFactforCostFunct = new float[maxScUp];

            float deltaSM20DnMin = 0.05f + dSM20scDnLow(0) + 0.52f * (float) Math.exp(-sDn/100d * 0.4d);
            for (int scDn = maxSL+1; scDn<maxScDn;scDn++){
                int lscDn = scDn - ( maxSL + 1 );
                off = lscDn - sDn/100d;
                crDn[scDn] = (float) (0.02 + 0.005*(scDn-(maxSL+1)) + 0.05*sig(2d*(2d-off)));
                srelSlope[scDn]  =  ssrelSlope[scDn]+0.5f-(float) sig((sDn/100d-2d));

                deltaSM20Dn[scDn] = deltaSM20DnMin + (float) (0.5*sig((1.5d-off)*2d))+lscDn*0.025f;
                factorDown[scDn] = deltaSM20Dn[scDn]*8.5f;
            }

            float ulStretch = ( 1f + 0.18f * sUp/100);
            float deltaSM20DnMinscLow = deltaSM20Dn[maxSL+1];
            for (sc = 0; sc<maxScUp;sc++){
                if (sc < maxSL) {

                    sig = sig((3.5-sc)*2.);
                    f1u[sc] = (float) (1.15+0.3*sig);
                    f2u[sc] = ( 1.1f )*f1u[sc] ;
                    f3u[sc] = 2.2f;

                    crUp[sc] = (float) (0.0047 + 0.029*sig((3.5-sc)*1.3));
                    crDn[sc] = crUp[sc];
                    srelSlope[sc]  = ssrelSlope[sc];

                    deltaSM20Dn[sc] = deltaSM20DnMinscLow - dSM20scDnLow(sc);
                    factorDown[sc] = deltaSM20Dn[sc]*11f;
                    distFactforCostFunct[sc] = sdistFactforCostFunct[sc];
                }
                else if(sc>maxSL){
                    int scUp = sc - ( maxSL + 1 );
                    off = scUp - sUp/100d;
                    sig = sig((1.5-off)*2.);

                    f1u[sc] = (float) (1.15+0.3*sig);
                    f2u[sc] = ( 1.1f )*f1u[sc] ;
                    f3u[sc] = 2.2f + 0.5f*(float) sig;

                    crUp[sc] = (float) (0.02 + 0.005*(sc-(maxSL+1)) + 0.05*sig(2d*(2d-off)));
                    distFactforCostFunct[sc] = 1f + 0.5f*(float) sig(-off*2.);
                }

            }

            f1u[maxSL]=1.35f;
            f2u[maxSL]=f1u[maxSL]*1.15f;
            f3u[maxSL]=2.4f;
            crUp[maxSL] = 0.025f;
            crDn[maxSL] = 0.025f;
            srelSlope[maxSL] = srelSlope[maxSL+1];

            deltaSM20Dn[maxSL] = deltaSM20Dn[maxSL+1+2]; // + 0.04f * (float) Math.exp(-(sDn/100 - 2.5)*(sDn/100 - 2.5));
            factorDown[maxSL]  = deltaSM20Dn[maxSL+1+3]*(8.5f + 5f*(float)sig(2.*(sDn/100.-1.)));

            distFactforCostFunct[maxSL] = sdistFactforCostFunct[maxSL];

            slopesAll[slopesAll.length-4] = 0.05f * ulStretch;  //basis for heuristic spline
            slopesAll[slopesAll.length-3] = 0.17f * ulStretch;
            slopesAll[slopesAll.length-2] = slopesAll[slopesAll.length-3] + 0.025f*ulStretch;
            slopesAll[slopesAll.length-1] = slopesAll[slopesAll.length-2] + 0.1f;

            watt0 =   contxt.power;
            watt = 1.7f*watt0; //(1.6f+sUp/100f*0.08f)*watt0;
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
            CubicSpline[] refCubicSplines = new CubicSpline[HeuristicRefSurfaceCats.length];
            for ( int i=0;i<HeuristicRefSurfaceCats.length;i++){
                int surfaceCat = HeuristicRefSurfaceCats[i];
                refCubicSplines[i] = getCostSpline(surfaceCat);
            }
            float[] minDurations = new  float[slopesAll.length];
            for ( int s=0;s<slopesAll.length;s++){
                minDurations[s] = Float.MAX_VALUE;
                for ( int i=0;i<HeuristicRefSurfaceCats.length;i++){
                    float duration =  ( refCubicSplines[i].calc(slopesAll[s]) - 0.0001f );// * 0.9999f;
                    if (duration < minDurations[s])
                        minDurations[s] = duration;
                }
            }
            CubicSpline cubicSpline = getSpline(slopesAll,minDurations);

            checkNegCurvature(cubicSpline,String.format(Locale.ENGLISH,"heuristic spline for %s ",context.toString()));
            return cubicSpline;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

/*    private boolean isHeuristicRefSpline(int surfaceCat){
        for (int heuristicRefSurfaceCat : HeuristicRefSurfaceCats) {
            if (surfaceCat == heuristicRefSurfaceCat) return true;
        }
        return false;
    } */

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
        Context contxt = (Context) context;
        int scDn = getCatDn(surfaceCat);
        int scUp = getCatUp(surfaceCat);
        float cr = crUp[scUp] ;
        float crD = crDn[scDn];
        float f1Up = f1u[scUp];
        float f2Up = f2u[scUp];
        float f3Up = f3u[scUp];
        float cr0 = (crD+cr)/2f;
        float cr1 = (0.1f*crD + 0.9f*cr);
        float sm20Dn = deltaSM20Dn[scDn];
        float factorDn = factorDown[scDn];

        float distFactCostFunct = scUp < distFactforCostFunct.length ? distFactforCostFunct[scUp] : 0f;
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
        if (cr0<=0.1 || contxt.withRef) {
            slopes = new float[slopesAll.length];
        } else {
            slopes = new float[slopesAll.length-1];
        }
        for (float slope : slopesAll) {
            if ( slopes.length == slopesAll.length || slope != refDnSlopeOpt){
                slopes[sc++] = slope;
            }
        }


        float[] durations = new float[slopes.length];
        boolean allSlopes = slopes.length == slopesAll.length;

        durations[0] = sm20Dn -(slopes[0]-refDnSlope)*Math.max(12f,factorDn*2f);
        durations[1] = sm20Dn -(slopes[1]-refDnSlope)*factorDn;
        durations[2] = sm20Dn -(slopes[2]-refDnSlope)*factorDn;
        durations[slopes.length-5] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-5], watt0, cr0, ACw, m) ;
        durations[slopes.length-4] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-4], watt, cr1, ACw, m) ;
        durations[slopes.length-3] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr, ACw, m)  ;
        durations[slopes.length-2] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr, ACw, m)  ;
        durations[slopes.length-1] = f3Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr, ACw, m)  ;

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
                distFactCostFunct = 1.2f;
                durations[durations.length - 1] = 1.10f * distFactCostFunct * durations[durations.length - 1];
                durations[durations.length - 2] = 1.05f * distFactCostFunct * durations[durations.length - 2];
                durations[durations.length - 3] = distFactCostFunct * durations[durations.length - 3];
            } else {
                durations[durations.length - 1] = 1.10f * distFactCostFunct * durations[durations.length - 1];
                durations[durations.length - 2] = 1.05f * distFactCostFunct * durations[durations.length - 2];
                durations[durations.length - 3] = distFactCostFunct * durations[durations.length - 3];
            }
        }

        String OptSpline = contxt.withRef ? "with ref" : allSlopes ? "Optimized ref" :"ref";
        String SplineType = costSpline ? " cost":" duration";
        String contextString = String.format(Locale.ENGLISH,"%s%s spline %s ",OptSpline,SplineType,getSurfaceCatTxt(surfaceCat));

        float slopeTarget = 0f;
        CubicSpline cubicSplineTmp;
        if (contxt.withRef) {
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

/*            if ( cubicSplineTmp.getVal(indRefDnSlopeOpt)>cubicSplineTmp.getVal(indRefDnSlopeOpt+1)&&cubicSplineTmp.getVal(indRefDnSlopeOpt)>cubicSplineTmp.getVal(indRefDnSlope)){
                mgLog.w(String.format(Locale.ENGLISH,"Correct decreasing downhill speed for %s. Set duration at slope=%.2f to duration at slope=%.2f",contextString,slopes[indRefDnSlopeOpt]*100f,slopes[indRefDnSlopeOpt+1]*100f));
                durations[indRefDnSlopeOpt] = durations[indRefDnSlopeOpt+1];
                cubicSplineTmp = getSpline(slopes,durations);
            } */
        }
        else {
            cubicSplineTmp = getSpline(slopes, durations);
/*            float curveTarget = 0.5f;
            if (allSlopes&&isHeuristicRefSpline&&cubicSplineTmp.getCurveCoeff(indRefDnSlopeOpt+1)<curveTarget){ //no negative curvature at 0 slope
                mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f",contextString,slopes[indRefDnSlopeOpt+1]*100f,slopes[indRefDnSlopeOpt]*100f));
                cubicSplineTmp = getCurveOptSpline(slopes,durations,indRefDnSlopeOpt+1,curveTarget,indRefDnSlopeOpt);
            }
            curveTarget = 0.01f;
            if (isHeuristicRefSpline&&allSlopes && cubicSplineTmp.getCurveCoeff(3)<0.01f){
                mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f",contextString,slopes[3]*100f,slopes[3]*100f));
                cubicSplineTmp = getCurveOptSpline(slopes,durations,3,curveTarget,3);
            } */
        }
/*        float curveTarget = 0.2f;
        if (cubicSplineTmp.getCurveCoeff(slopes.length-3)<curveTarget){
            mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f",contextString,slopes[slopes.length-3]*100f,slopes[slopes.length-2]*100f));
            cubicSplineTmp = getCurveOptSpline(slopes,durations,slopes.length-3,curveTarget,slopes.length-2);
        }
        if (cubicSplineTmp.getCurveCoeff(2)<curveTarget){
            mgLog.w(String.format(Locale.ENGLISH,"Autocorrect negative curvature for %s at slope=%.2f, correct at slope=%.2f", contextString,slopes[2]*100f,slopes[1]*100f));
            cubicSplineTmp = getCurveOptSpline(slopes,durations,2,curveTarget,1);
        } */

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
        ArrayList<CubicSpline.Value> curveRadiusForNegCurvaturePoint = cubicSpline.getCurveRadiusForNegCurvaturePoints();
        if (curveRadiusForNegCurvaturePoint != null) {
            StringBuilder msg = new StringBuilder(String.format(Locale.ENGLISH,"Negative curvature for %s",context));
            boolean threshold = false;
            for (CubicSpline.Value negCurvature : curveRadiusForNegCurvaturePoint) {
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
        return ( scDn < distFactforCostFunct.length  ) ||
               ( scUp < distFactforCostFunct.length  );
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
        if (surfaceCat <= maxSL)
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


