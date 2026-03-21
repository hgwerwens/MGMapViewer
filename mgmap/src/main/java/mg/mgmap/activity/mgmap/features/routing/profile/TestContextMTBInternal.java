package mg.mgmap.activity.mgmap.features.routing.profile;


import static java.lang.Math.exp;
import static java.lang.Math.pow;

import androidx.annotation.NonNull;

import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.CompositeSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef;

public class TestContextMTBInternal implements IfSplineProfileContext, IfMTBContextDetails {

    public static float[] fs = new float[3];

    public final int power;
    public final int sUp;
    public final int sDn;
    private final boolean withRef;
    private final boolean checkAll;
    float[] ulstrechDuration;
    float[] ulstrechCost;
    private final float[] f0u;
    private final float[] f1u;
    private final float[] f3u;
    private final float[] fuSlope;
    private final float f3d;
    private final float fdSlope;
    private final float[] crUp;
    private final float[] crDn;
    private final float[] srelSlope;
    private final float[] frelSlopeCost;
    private final float[] sm20Dn;
    private final float[] factorDown;
    private final float[][] distFactforCostFunct;
    private final float[] watt;
    private final float[] watt0;
    private final float[] slopesAll;
    int indRefDnSlope;
    int heuristicRefSurfaceCat = 7;
    float[] sslopesAll = new float[]{-0.76f, -0.36f, -0.2f, -0.05f, 0.0f, 0.09f,0.19f, 0.24f};
    float[] sdistFactforCostFunct = {  3.0f   ,2.4f ,2.0f  ,1.85f ,1.5f  ,1.2f }; //factors to increase costs compared to durations to get better routing results
    float[] ssrelSlope            = {  1.4f   ,1.2f ,1f    ,1f    ,1f    ,1f  , 0f    ,1.4f  ,1.3f  ,1.2f  ,1.2f  ,1.2f ,1.2f   ,1.2f }; //slope of auxiliary function for duration function at 0% slope to get to -4% slope

    static SurfCat2MTBCat sc2MTBc = new SurfCat2MTBCat();
    private final IfSpline[] costRefSplines;
    private final IfSpline[] durationRefSplines;
    private final TestContextMTBInternal refContext;

    public TestContextMTBInternal(int power, int sUp, int sDn, boolean checkAll, boolean withRef) {

        if (withRef) {
            costRefSplines = new CompositeSpline[sc2MTBc.maxScDn];
            durationRefSplines = new CompositeSpline[sc2MTBc.maxScDn];
            this.refContext = new TestContextMTBInternal(100, 200, sDn, false, false);
        } else {
            this.refContext = null;
            costRefSplines = null;
            durationRefSplines = null;
        }
        this.power = power;
        this.sUp = sUp;
        this.sDn = sDn;
        this.withRef = withRef;
        this.checkAll = checkAll;
        slopesAll = sslopesAll.clone();
        indRefDnSlope = 2;
//        slopesAll[indRefDnSlope]=slopesAll[indRefDnSlope] -  0.02f + 0.01f*sDn/100f;
        float refslope = slopesAll[indRefDnSlope];

        float dlstretch = 0.13f + 1.9f * sig((2d - sDn / 100d)*2d);
        for (int i = 0; i < indRefDnSlope; i++)
            slopesAll[i] = refslope + (slopesAll[i] - refslope) * dlstretch;

        int sc;

        double off;
        float sig;

        ulstrechDuration = new float[sc2MTBc.maxScUpExt];
        ulstrechCost = new float[sc2MTBc.maxScUpExt];
        f0u = new float[sc2MTBc.maxScUpExt];
        f1u = new float[sc2MTBc.maxScUpExt];  // factor on top of friction based duration calculation
        f3u = new float[sc2MTBc.maxScUpExt];  // factor on top of friction based duration calculation
        fuSlope = new float[sc2MTBc.maxScUpExt];
        crUp = new float[sc2MTBc.maxScUpExt]; // uphill friction
        crDn = new float[sc2MTBc.maxScDn]; // downhill friction
        srelSlope = new float[sc2MTBc.maxScDn]; // slope of auxiliary function for duration function at 0% slope to get to -4% slope
        frelSlopeCost = new float[sc2MTBc.maxScDn];
        sm20Dn = new float[sc2MTBc.maxScDn]; // duration (sec/m) at -20% slope
        factorDown = new float[sc2MTBc.maxScDn]; // slope of the duration function lower -20%
        distFactforCostFunct = new float[sdistFactforCostFunct.length][slopesAll.length];// factor on top of duration function for certain slopes to get a better cost function
        watt = new float[sc2MTBc.maxSurfaceCat];
        watt0 = new float[sc2MTBc.maxSurfaceCat];

        float facDn = 10.8f - 9.9f*(sig(2d*(2d-sDn / 100d))-sig(2d)); //for sDn=100 -> facDn=10.8

        for (sc = 0; sc < sdistFactforCostFunct.length; sc++) {
            for (int i = 0; i < slopesAll.length; i++) {
                if (slopesAll[i] < 0)
                    distFactforCostFunct[sc][i] = sdistFactforCostFunct[sc];
                else if (slopesAll[i] == 0.0f)
                    distFactforCostFunct[sc][i] = (1f + (sdistFactforCostFunct[sc] - 1f) * 0.70f);
                else {
                    distFactforCostFunct[sc][i] = (1f + (sdistFactforCostFunct[sc] - 1f) * 0.3f);
                }
            }
        }
        // factor how much higher the reference surface level will be if the sDn changes by 100
        float deltaSlToSl2 = dSM20scDnLow(pow(1.25d, 1.8d - sDn / 100f) + 1d, sDn); //for sc 7 (mtbDn=0,mtbUp=0) the delta which surface level has same value, with lower sDn sl becomes higher and vice versa, if deltaSlToSl2=0 (for sDn=2) than deltaSM20DnMin=deltaSM20Dn(2)
        float deltaSM20DnMin = 0.05f + deltaSlToSl2 - dSM20scDnLow(0, sDn) + 0.68f * (float) exp(-sDn / 100d * 0.5d); //sig((sDn/100d-0.5)*0.675);//0.52f * (float) Math.exp(-sDn / 100d * 0.4d); //deltaSM20DnMin value for sc 7 (mtbDn=0,mtbUp=0)
        for (int scDn = sc2MTBc.maxSL + 1; scDn < sc2MTBc.maxScDn; scDn++) {
            int lscDn = scDn - (sc2MTBc.maxSL + 1);
            off = lscDn - sDn / 100d;
            crDn[scDn] = 0.02f + 0.005f * lscDn + 0.035f * sig(2d * (2d - off));
            srelSlope[scDn] = ssrelSlope[scDn];
            frelSlopeCost[scDn] = 0.3f + sig(2d*(sDn/100d-2))*0.7f*sig(2d * (off));
            sm20Dn[scDn] = deltaSM20DnMin + ( 0.8f * sig((1.5d - off) * 2d)) + lscDn * 0.025f;
            factorDown[scDn] = sm20Dn[scDn] * facDn ;
        }
        float deltaSM20DnMinscLow = sm20Dn[sc2MTBc.maxSL + 1] - deltaSlToSl2;


        for (sc = 0; sc < sc2MTBc.maxScUpExt; sc++) {
            if (sc < sc2MTBc.maxSL) {
                sig = sig((3.5 - sc) * 2d);
                ulstrechDuration[sc] = 1.2f*(float) pow(1.2,sUp /100d-1d) - 0.4f*sig; //1f + 0.18f * sUp / 100;
                ulstrechCost[sc] = 1.15f*(float) pow(1.3,sUp /100d-1d) - 0.55f*sig; //1.1f + 0.24f * sUp / 100 - 0.8f * sig; //1.34f*(float) pow(1.58/1.34,sUp /100d-1d) -0.4f*sig; //
                f0u[sc] = 1f   + 0.15f * sig;
                f1u[sc] = 1.1f * f0u[sc];
                f3u[sc] = 1.4f*f1u[sc]*f0u[sc];
                fuSlope[sc] = 0.1f;
                crUp[sc] = (0.0047f + 0.029f * sig((3.5 - sc) * 1.3));
                crDn[sc] = crUp[sc];
                srelSlope[sc] = ssrelSlope[sc];
                frelSlopeCost[sc] = 0.3f + sig(2d*(sDn/100d-2))*0.1f;
                sm20Dn[sc] = deltaSM20DnMinscLow + dSM20scDnLow(sc, sDn);
                factorDown[sc] = sm20Dn[sc] * facDn;
            } else if (sc > sc2MTBc.maxSL && sc < sc2MTBc.maxScUp) {
                int scUp = sc - (sc2MTBc.maxSL + 1);
                off = scUp - sUp / 100d;
                sig = sig((0.5 - off) * 2.);
                ulstrechDuration[sc] = 0.9f*(float) pow(1.2,sUp /100d-1d) - 0.1f*sig;
                ulstrechCost[sc] = 0.86f*(float) pow(1.32,sUp /100d-1d) - 0.35f*sig;// 0.94f*(float) pow(1.18/0.94,sUp /100d-1d) -0.4f*sig; //(0.7f + 0.24f * sUp / 100 - 0.4f * sig); //
                f0u[sc] = 1.0f;
                f1u[sc] = 1.18f;
                f3u[sc] = 1.65f*f1u[sc];
                fuSlope[sc] = 0.4f;
                crUp[sc] = (0.0185f + 0.004f * scUp + 0.035f * sig(2d * (2d - off)));
            } else if (sc != sc2MTBc.maxSL) {
                int scUp = sc - sc2MTBc.maxScUp; // it's actually the scDn category without scUp used to derive factors for cost function
                off = scUp - sUp / 100d;
                sig = sig((-0.5 - off) * 2d);
                ulstrechDuration[sc] = 0.9f*(float) pow(1.2,sUp /100d-1d) - 0.1f*sig;
                ulstrechCost[sc] = 0.85f*(float) pow(1.27,sUp /100d-1d) - 0.35f*sig; //(0.65f + 0.2f * sUp / 100 - 0.4f * sig); //
                f0u[sc] = 1.0f;
                f1u[sc] = 1.18f;
                f3u[sc] = 1.8f*f1u[sc];
                fuSlope[sc] = 0.5f;
                crUp[sc] = (0.019f + 0.004f * scUp + 0.035f * sig(2d * (1d - off)));
            }
        }

        f3d = 2.5f;
        fdSlope = 0.95f*sig(2d*(sDn/100d-2));

        ulstrechDuration[sc2MTBc.maxSL] = 0.8f*(float) pow(1.2,sUp /100d-1d) ;
        ulstrechCost[sc2MTBc.maxSL] = 0.7f*(float) pow(1.25,sUp /100d-1d);// (0.45f + 0.2f * sUp / 100);//0.45f*(float) pow(1.3,sUp /100d-1d);//
        f0u[sc2MTBc.maxSL] = 1.0f;
        f1u[sc2MTBc.maxSL] = 1.35f;
        f3u[sc2MTBc.maxSL] = 1.85f*f1u[sc2MTBc.maxSL] ;
        fuSlope[sc2MTBc.maxSL] = 0.2f;
        crUp[sc2MTBc.maxSL] = 0.03f;
        crDn[sc2MTBc.maxSL] = 0.02f;
        srelSlope[sc2MTBc.maxSL] = srelSlope[sc2MTBc.maxSL + 1];
        frelSlopeCost[sc2MTBc.maxSL] = (0.3f + sig(2d*(sDn/100d-2))*0.4f);
        sm20Dn[sc2MTBc.maxSL] = sm20Dn[sc2MTBc.maxSL + 1 + 3];
        factorDown[sc2MTBc.maxSL] =  sm20Dn[sc2MTBc.maxSL] * facDn;//*(1f+0.15f*sig((3d-sDn/100d)*2d));

        float watt0 = (float) power;
        float watt = 1.7f * power;
        for (sc = 0; sc < sc2MTBc.maxSurfaceCat; sc++) {
            this.watt[sc] = watt;
            float cr0 = (crUp[sc2MTBc.getCatUpExt(sc)]+crDn[sc2MTBc.getCatDn(sc)])/2f;
            float fact = sig((0.05-cr0)*100d);
            this.watt0[sc] = watt0 * ( 1 + (watt/watt0-1f)*fact) ;
        }

    }

    TestContextMTBInternal( int sUp, int sDn) {
        this((int) (47.5 + 25 * sUp / 100d),sUp,sDn,true,true);
    }


    static float sig(double base) {
        return (float) (1. / (1. + exp(base)));
    }

    static float dSM20scDnLow(double scDn, int sDn) {
        double off = scDn - sDn / 100d;
        float sigOff = sig(1.5 * (2 - off));
        float scDnLow = 0.2f * (sig(1.5 * (2. - scDn)) - 0.5f) + 0.15f * sigOff; // is zero for scDn = 2;
        return scDnLow;
    }

    private IfSpline calcspline(int sc, boolean isCostSpline){
        if (!isValidSc(sc)) return null;

        int indRefDnSlope = 2;//getIndRefDnSlope();
        int indRefDnSlopeOpt = indRefDnSlope+1;
        float[] slopes = isCostSpline ? getCostSlopes(sc): getDurationSlopes(sc);

        int scUp = sc2MTBc.getCatUpExt(sc);
        int scDn = sc2MTBc.getCatDn(sc);

        float crUp = this.crUp[scUp];
        float crDn = this.crDn[scDn];
        float f0up = f0u[scUp];
        float f1Up = f1u[scUp];

        float f3Up = f3u[scUp];
        float cr0 = (crDn+crUp)/2f;
        float cr1 =  (0.1f*crDn + 0.9f*crUp);
        float sm20Dn = this.sm20Dn[scDn];

//        IfSpline profContextMTBSpline = (isCostSpline) ? profileContextMTB.calcCostSpline(sc) : profileContextMTB.calcDurationSpline(sc);
//        float slopeDn = -profContextMTBSpline.derivativeAt(-0.2f);
//        float slopeUp = profContextMTBSpline.derivativeAt(slopes[slopes.length-3]);
        float factorDn = this.factorDown[scDn];
        float f3d      = this.f3d;
        float[] distFactCostFunct = getDistFactforCostFunct(sc);

        float refDnSlope = slopes[indRefDnSlope];
        float watt0 = this.watt0[sc];
        float watt  = this.watt[sc];


        float[] durations = new float[slopes.length];

        durations[0] = (sm20Dn -(slopes[0]-refDnSlope)*factorDn)*f3d; //f3d
        durations[1] = (sm20Dn -(slopes[1]-refDnSlope)*factorDn);
        durations[2] =  sm20Dn ;
        //      for everything with slope >=0% durations (sec/m) is calculated based on the speed derived from friction and input power (Watt)
        durations[slopes.length-4] = 1.0f /  ProfileUtil.getFloatFrictionBasedVelocity(slopes[slopes.length-4], watt0, cr0) ;
        durations[slopes.length-3] = f0up /  ProfileUtil.getFloatFrictionBasedVelocity(slopes[slopes.length-3], watt, cr1) ;
        durations[slopes.length-2] = f1Up /  ProfileUtil.getFloatFrictionBasedVelocity(slopes[slopes.length-2], watt, crUp)  ;
        durations[slopes.length-1] = f3Up /  ProfileUtil.getFloatFrictionBasedVelocity(slopes[slopes.length-1], watt, crUp)  ;

        if (isCostSpline) {
            if (distFactCostFunct.length>0) {
                for (int i = 0; i < distFactCostFunct.length; i++) {
                    durations[i] = durations[i] * distFactCostFunct[i];
                }
            }
            durations[indRefDnSlopeOpt] = durations[slopes.length-4]+frelSlopeCost[scDn]*srelSlope[scDn]*slopes[indRefDnSlopeOpt];
        } else {
            durations[indRefDnSlopeOpt] = durations[slopes.length-4]+srelSlope[scDn]*slopes[indRefDnSlopeOpt];
        }


        float innerSplineRightSlopeMax = (durations[slopes.length-3]-durations[slopes.length-2])/(slopes[slopes.length-3]-slopes[slopes.length-2]);
        float innerSplineRightSlopeMin = (durations[slopes.length-4]-durations[slopes.length-3])/(slopes[slopes.length-4]-slopes[slopes.length-3]);
        float innerSplineRightSlope = ( innerSplineRightSlopeMax + fuSlope[scUp]*innerSplineRightSlopeMin )/(1f+fuSlope[scUp]);

        float innerSplineLeftSlopeMin = (durations[1]-durations[2])/(slopes[1]-slopes[2]); // idendentical to -factorDn in case distfactor does not change
        float innerSplineLeftSlope2 = durations[2]/slopes[2];
        float innerSplineLeftSlope = (innerSplineLeftSlope2>innerSplineLeftSlopeMin)?(innerSplineLeftSlopeMin+fdSlope*innerSplineLeftSlope2)/(1+fdSlope):innerSplineLeftSlopeMin;


        IfSplineDef innerSplineDef = new IfSplineDef.Clamped(innerSplineLeftSlope,innerSplineRightSlope);
        IfSplineDef splineDef = new IfSplineDef.Composite(2,2,innerSplineDef);

        IfSpline spline;
        IfSpline refSpline;
        if (getWithRef()) {
            /* to achieve an almost constant downhill profile for a given mtbDn scale and downhill level of the profile (sDn) independent of the mtbUp scale and the uphill level of the profile (sUp)
            a reference profile for a given combination of sDn and mtbDn with a constant uphill profile (power = 100 Watt, sUp = 2 ) and mtbUp = mtbDn is calculated. All other uphill combinations are
            calculated in such a way that the slope at -20% is taken from the reference profile und the duration is varied at -4% slope, so that the slope matches the target slope
             */
            if (isCostSpline)
                refSpline = getCostRefSpline(sc);//getRefProfile().getCostSpline(getRefSc(sc));
            else
                refSpline = getDurationRefSpline(sc); //getRefProfile().getDurationSpline(getRefSc(sc));
            for ( int i = 0; slopes[i]<0;i++) {
                durations[i] = refSpline.valueAt(slopes[i]) ;//* factor;
            }
            float secondDerivativeTarget = refSpline.secondDerivativeAt(slopes[indRefDnSlope]); //*factor;
            spline = getRefOptSpline(slopes, durations, splineDef, indRefDnSlope, secondDerivativeTarget, indRefDnSlopeOpt);
        }
        else {
            spline = new CompositeSpline(slopes, durations,splineDef);
        }
        return spline;
    }


    protected CompositeSpline getRefOptSpline(float[] slopes, float[] durations,IfSplineDef splineDef, int targetat, float secondDerivativeTarget, int varyat) {
        //     IfFunction of Minimum duration value of a spline based on input duration varied at slope[varyat] (for MTB splines at slope -3.5% )
        IfFunction slope = smvary -> {
            durations[varyat] = smvary;
            CompositeSpline spline = new CompositeSpline(slopes,durations,splineDef);
            return spline.secondDerivativeAt(slopes[targetat]) - secondDerivativeTarget;
        };
//      Newton iteration with numerical derivation is used to optimize the input duration[varyat] so that the Min duration matches the Target smMinTarget
        durations[varyat] = ProfileUtil.newtonNumeric(durations[varyat],0.0001f,slope,0.001f);
        try {
            return new CompositeSpline(slopes, durations,splineDef);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() + " in getSlopeOptSpline");
        }
    }

    private IfSpline getCostRefSpline(int sc){
        int scDn = sc2MTBc.getCatDn(sc);
        IfSpline spline = costRefSplines[scDn];
        if (spline==null){
            spline = refContext.calcCostSpline(getRefSc(sc));
            costRefSplines[scDn]= spline;
        }
        return spline;

    }

    private IfSpline getDurationRefSpline(int sc){
        int scDn = sc2MTBc.getCatDn(sc);
        IfSpline spline = durationRefSplines[scDn];
        if (spline==null){
            spline = refContext.calcDurationSpline(getRefSc(sc));
            durationRefSplines[scDn]= spline;
        }
        return spline;
    }


    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "TEST power=%3d sUp=%3d sDn=%3d hasRef=%s", power, sUp, sDn, withRef ? "x" : " ");
    }

    public boolean getCheckAll() {
        return checkAll;
    }

    public boolean getWithRef() {
        return withRef;
    }


    public int getRefSc(int sc) {
        int scDn = sc2MTBc.getMtbDn(sc);
        return sc2MTBc.getSurfaceCat(sc2MTBc.getSurfaceLevel(sc), scDn, scDn);
    }


    public int getMaxSurfaceCat() {
        return sc2MTBc.maxSurfaceCat;
    }


    public int getScHeuristicRefSpline() {
        return   heuristicRefSurfaceCat;
    }


    public int getScProfileSpline() {
        return sc2MTBc.maxSL;
    }


    public IfSpline calcCostSpline(int sc) {
        return calcspline(sc,true);
    }


    public IfSpline calcDurationSpline(int sc) {
        return  calcspline(sc,false);
    }


    public boolean isValidSc(int surfaceCat) {
        return sc2MTBc.isValidSc(surfaceCat);
    }


    private float[] getCostSlopes(int sc) {
        return getSlopes(ulstrechCost[sc2MTBc.getCatUpExt(sc)] );
    }

    private float[] getDurationSlopes(int sc) {
        return getSlopes( ulstrechDuration[sc2MTBc.getCatUpExt(sc)] );
    }

    private float[] getSlopes(float ulstrech){
        float[] slopes ;
        slopes = new float[slopesAll.length];
        int i =0;
        for (float slope : slopesAll) {
            if (slope <= 0)
                slopes[i++] = slope;
            else
                slopes[i++] = slope * ulstrech;
        }
        return slopes;
    }


    public float[] getDistFactforCostFunct(int sc) {
        if (sc < distFactforCostFunct.length)
            return distFactforCostFunct[sc];
        else
            return new float[]{};
    }


    public String getSurfaceCatTxt(int sc) {
        int mtbUp = sc2MTBc.getMtbUp(sc);
        String mtbUpTxt = String.format("%s", (mtbUp < 0 ? "-" : mtbUp));
        int mtbDn = sc2MTBc.getMtbDn(sc);
        String mtbDnTxt = String.format("%s", (mtbDn < 0 ? "-" : mtbDn));
        return String.format(Locale.ENGLISH, "%s SurfCat=%2d SurfLvl=%1d mtbDn=%s mtbUp=%s", this, sc, sc2MTBc.getSurfaceLevel(sc), mtbDnTxt, mtbUpTxt);
    }


    public float getMinDistFactSC(int sc) {
        return CostCalcSplineMTB.getMinDistFactSC(sc);
    }


    public int getSDn() {
        return sDn;
    }

    @Override
    public SurfCat2MTBCat getSc2MTBc() {
        return sc2MTBc;
    }
}
