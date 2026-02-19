package mg.mgmap.activity.mgmap.features.routing.profile;

import static mg.mgmap.activity.mgmap.features.routing.profile.ProfileUtil.getFrictionBasedVelocity;

import androidx.annotation.NonNull;

import java.util.Locale;

public class SplineProfileContextMTB implements IfSplineProfileContext, IfMTBContextDetails {
    public static float sf2d = 1.04f;
    public static float facDnStrech = 8f;
    public static float dlstrechFac = 1.9f;

    public final int power;
    public final int sUp;
    public final int sDn;
    private final boolean withRef;
    private final boolean checkAll;
    float[] ulstrechDuration;
    float[] ulstrechCost;
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
    private final float[][] distFactforCostFunct;
    private final float[] watt;
    private final float[] watt0;
    private final float[] slopesAll;
    int indRefDnSlope;
    int heuristicRefSurfaceCat = 7;
    float[] sdistFactforCostFunct = {  3.0f   ,2.4f ,2.0f,1.85f ,1.5f  ,1.4f }; //factors to increase costs compared to durations to get better routing results
    float[] ssrelSlope            = {  1.4f   ,1.2f ,1f    ,1f    ,1f    ,1f  , 0f    ,1.2f  ,1.2f  ,1.2f  ,1.2f  ,1.2f ,1f   ,1f }; //slope of auxiliary function for duration function at 0% slope to get to -4% slope

    static SurfCat2MTBCat sc2MTBc = new SurfCat2MTBCat();
    private final CubicSpline[] costRefSplines;
    private final CubicSpline[] durationRefSplines;
    private final IfSplineProfileContext refContext;

    public SplineProfileContextMTB(int power, int sUp, int sDn, boolean checkAll, boolean withRef) {
        if (withRef) {
            costRefSplines = new CubicSpline[sc2MTBc.maxScDn];
            durationRefSplines = new CubicSpline[sc2MTBc.maxScDn];
            this.refContext = new SplineProfileContextMTB(100, 200, sDn, false, false);
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

        slopesAll = IfSplineProfileContext.slopesAll.clone();
        indRefDnSlope = 3;
//        slopesAll[indRefDnSlope]=slopesAll[indRefDnSlope] -  0.02f + 0.01f*sDn/100f;
        float refslope = slopesAll[indRefDnSlope];
        float dlstretch = 0.13f + dlstrechFac * sig((2d - sDn / 100d)*2d);

        for (int i = 0; i < indRefDnSlope; i++)
            slopesAll[i] = refslope + (slopesAll[i] - refslope) * dlstretch;

        int sc;

        double off;
        float sig;

        ulstrechDuration = new float[sc2MTBc.maxScUpExt];
        ulstrechCost = new float[sc2MTBc.maxScUpExt];
        f0u = new float[sc2MTBc.maxScUpExt];
        f1u = new float[sc2MTBc.maxScUpExt];  // factor on top of friction based duration calculation
        f2u = new float[sc2MTBc.maxScUpExt];  // factor on top of friction based duration calculation
        f3u = new float[sc2MTBc.maxScUpExt];  // factor on top of friction based duration calculation
        crUp = new float[sc2MTBc.maxScUpExt]; // uphill friction
        crDn = new float[sc2MTBc.maxScDn]; // downhill friction
        srelSlope = new float[sc2MTBc.maxScDn]; // slope of auxiliary function for duration function at 0% slope to get to -4% slope
        deltaSM20Dn = new float[sc2MTBc.maxScDn]; // duration (sec/m) at -20% slope
        factorDown = new float[sc2MTBc.maxScDn]; // slope of the duration function lower -20%
        distFactforCostFunct = new float[sdistFactforCostFunct.length][slopesAll.length];// factor on top of duration function for certain slopes to get a better cost function
        watt = new float[sc2MTBc.maxSurfaceCat];
        watt0 = new float[sc2MTBc.maxSurfaceCat];

        float facDn = 12f - facDnStrech*sig((2d-sDn / 100d)*2d);


        for (sc = 0; sc < sdistFactforCostFunct.length; sc++) {
            for (int i = 0; i < slopesAll.length; i++) {
                if (slopesAll[i] < 0)
                    distFactforCostFunct[sc][i] = sdistFactforCostFunct[sc];
                else if (slopesAll[i] == 0.0f)
                    distFactforCostFunct[sc][i] = (1f + (sdistFactforCostFunct[sc] - 1f) * 0.7f);
                else {
                    distFactforCostFunct[sc][i] = (1f + (sdistFactforCostFunct[sc] - 1f) * 0.3f);
                }
            }
        }
        // factor how much higher the reference surface level will be if the sDn changes by 100
        float deltaSlToSl2 = dSM20scDnLow(Math.pow(1.65d, 2d - sDn / 100f) + 1d, sDn); // for sc 7 (mtbDn=0,mtbUp=0) the delta which surface level has same value, with lower sDn sl becomes higher and vice versa, if deltaSlToSl2=0 (for sDn=2) than deltaSM20DnMin=deltaSM20Dn(2)
        float deltaSM20DnMin = 0.05f + deltaSlToSl2 - dSM20scDnLow(0, sDn) + 0.52f * (float) Math.exp(-sDn / 100d * 0.4d); //deltaSM20DnMin value for sc 7 (mtbDn=0,mtbUp=0)

        for (int scDn = sc2MTBc.maxSL + 1; scDn < sc2MTBc.maxScDn; scDn++) {
            int lscDn = scDn - (sc2MTBc.maxSL + 1);
            off = lscDn - sDn / 100d;
            crDn[scDn] = (0.02f + 0.005f * (scDn - (sc2MTBc.maxSL + 1)) + 0.05f * sig(2d * (2d - off)));
            srelSlope[scDn] = ssrelSlope[scDn] + 0.5f - sig((sDn / 100d - 2d)) - 0.5f * sig(2d * (1d - off));
            deltaSM20Dn[scDn] = deltaSM20DnMin + (0.8f * sig((1.5d - off) * 2d)) + lscDn * 0.025f;
            factorDown[scDn] = deltaSM20Dn[scDn] * facDn;
        }
        float deltaSM20DnMinscLow = deltaSM20Dn[sc2MTBc.maxSL + 1] - deltaSlToSl2;


        for (sc = 0; sc < sc2MTBc.maxScUpExt; sc++) {
            if (sc < sc2MTBc.maxSL) {
                ulstrechDuration[sc] = 1f + 0.18f * sUp / 100;
                ulstrechCost[sc] = 1.3f + 0.18f * sUp / 100 - 0.5f * sig((3.5 - sc) * 2.); //( sc > 2 ? 0.2f * (sc - 2) : 0f );
                sig = sig((3.5 - sc) * 2.);
                f0u[sc] = 1.0f + 0.15f * sig;// + ( sc > 2 ? 0.05f * (sc - 2) : 0f );
                f1u[sc] = 1.1f + 0.15f * sig;
                f2u[sc] = (1.1f) * f1u[sc];
                f3u[sc] = 2.2f + 0.4f * sig;

                crUp[sc] = (0.0047f + 0.029f * sig((3.5 - sc) * 1.3));
                crDn[sc] = crUp[sc];
                srelSlope[sc] = ssrelSlope[sc] + (0.5f * (0.5f - sig(sDn / 100d - 2d)));
                deltaSM20Dn[sc] = deltaSM20DnMinscLow + dSM20scDnLow(sc, sDn);
                factorDown[sc] = deltaSM20Dn[sc] * facDn;
            } else if (sc > sc2MTBc.maxSL && sc < sc2MTBc.maxScUp) {
                int scUp = sc - (sc2MTBc.maxSL + 1);
                off = scUp - sUp / 100d;
                sig = sig((0.5 - off) * 2.);
                ulstrechDuration[sc] = (1f + 0.18f * sUp / 100 - 0.1f * sig);
                ulstrechCost[sc] = (0.80f + 0.18f * sUp / 100 - 0.4f * sig);

                f0u[sc] = 1.0f;
                f1u[sc] = (1.2f + 0.15f * sig((1.5 - off) * 2.));
                f2u[sc] = 1.1f * f1u[sc];
                f3u[sc] = 2.2f;
                crUp[sc] = (0.02f + 0.005f * scUp + 0.05f * sig(2d * (2d - off)));
            } else if (sc != sc2MTBc.maxSL) {
                int scUp = sc - sc2MTBc.maxScUp;
                off = scUp - sUp / 100d;
                sig = sig((-0.5 - off) * 2.);
                ulstrechDuration[sc] = (1f + 0.18f * sUp / 100 - 0.1f * sig);
                ulstrechCost[sc] = (0.70f + 0.18f * sUp / 100 - 0.4f * sig);

                f0u[sc] = 1.0f;
                f1u[sc] = (1.25f + 0.15f * sig((0.5d - off) * 2.));
                f2u[sc] = 1.10f * f1u[sc]; // ( 1.1 + 0.03*sig )*f1u[sc] ;
                f3u[sc] = 2.35f;
                crUp[sc] = (0.025f + 0.005f * scUp + 0.05f * sig(2d * (1d - off)));
            }
        }

        f2d = sf2d;
        f3d = 3.0f;

        ulstrechDuration[sc2MTBc.maxSL] = 1f + 0.18f * sUp / 100;
        ulstrechCost[sc2MTBc.maxSL] = (0.40f + 0.18f * sUp / 100);

        f0u[sc2MTBc.maxSL] = 1.0f;
        f1u[sc2MTBc.maxSL] = 1.35f;
        f2u[sc2MTBc.maxSL] = f1u[sc2MTBc.maxSL] * 1.15f;
        f3u[sc2MTBc.maxSL] = 2.9f;
        crUp[sc2MTBc.maxSL] = 0.03f;
        crDn[sc2MTBc.maxSL] = 0.02f;
        srelSlope[sc2MTBc.maxSL] = srelSlope[sc2MTBc.maxSL + 1];

        deltaSM20Dn[sc2MTBc.maxSL] = deltaSM20Dn[sc2MTBc.maxSL + 1 + 3];
        factorDown[sc2MTBc.maxSL] = deltaSM20Dn[sc2MTBc.maxSL + 1 + 3] * (facDn + 1f + 5f * sig(2. * (sDn / 100. - 1.)));

        float watt0 = (float) power;
        float watt = 1.7f * power;
        for (sc = 0; sc < sc2MTBc.maxSurfaceCat; sc++) {
            int scDn = sc2MTBc.getCatDn(sc);
            int scUp = sc2MTBc.getCatUpExt(sc);
            float crUp = this.crUp[scUp];
            float crDn = this.crDn[scDn];
            float cr0 = (crUp + crDn) / 2f;
            float f0 = sig((0.05d - cr0) * 100d);
            float watt0_high = watt0 + (watt - watt0) * f0;
            float watt0_base = 100f + (175f - 100f) * f0;
            float f = sig((0.05d - crUp) * 100d);
            this.watt0[sc] = watt0_high > watt0_base ? watt0_high + (watt0_base - watt0_high) * f : watt0_high;
            this.watt[sc] = watt > 175 ? watt + (175 - watt) * f : watt;
        }
    }

    static float sig(double base) {
        return (float) (1. / (1. + Math.exp(base)));
    }

    static float dSM20scDnLow(double scDn, int sDn) {
        double off = scDn - sDn / 100d;
        float sigOff = sig(1.5 * (2 - off));
        float scDnLow = 0.2f * (sig(1.5 * (2. - scDn)) - 0.5f) + 0.15f * sigOff; // is zero for scDn = 2;
        return scDnLow;
    }

    public SplineProfileContextMTB(int power, int sUp, int sDn, boolean checkAll) {
        this(power, sUp, sDn, checkAll, true);
    }

    public SplineProfileContextMTB(int power, int sUp, int sDn) {
        this(power, sUp, sDn, true);
    }

    public SplineProfileContextMTB(int sUp, int sDn, boolean checkAll) {
        this((int) (47.5 + 25 * sUp / 100d), sUp, sDn, checkAll, true);
    }

    public SplineProfileContextMTB(int sUp, int sDn) {
        this(sUp, sDn, true);
    }

    private CubicSpline calcCubicSpline(int sc, boolean isCostSpline){
        if (!isValidSc(sc)) return null;

        int indRefDnSlope = 3;
        int indRefDnSlopeOpt = indRefDnSlope+1;

        float crUp = getCrUp(sc);
        float crDn = getCrDn(sc);
        float f0up = getF0u(sc);
        float f1Up = getF1u(sc);
        float f2Up = getF2u(sc);
        float f3Up = getF3u(sc);
        float cr0 = (crDn+crUp)/2f;
        float cr1 =  (0.1f*crDn + 0.9f*crUp);
        float sm20Dn = getSm20Dn(sc);
        float factorDn = getFactorDn(sc);
        float f2d      = getF2d(sc);
        float f3d      = getF3d(sc);
        float[] distFactCostFunct = getDistFactforCostFunct(sc);
        float[] slopes = isCostSpline ? getCostSlopes(sc): getDurationSlopes(sc);
        float refDnSlope = slopes[indRefDnSlope];
        float watt0 = getWatt0(sc);
        float watt  = getWatt(sc);

        long t1 = System.nanoTime();


        float[] durations = new float[slopes.length];

        //      for slopes <=20% pure heuristic formulas apply that derivative of the duration function is equal to factorDn. For smaller slopes additional factors apply (f2d,f3d) to enforce positive
        //      curvature of the duration function
        durations[0] = ( sm20Dn -(slopes[0]-refDnSlope)*factorDn) *f3d; //f3d
        durations[1] = ( sm20Dn -(slopes[1]-refDnSlope)*factorDn) *f2d;//f2d;
        durations[2] =   sm20Dn -(slopes[2]-refDnSlope)*factorDn;
        durations[3] =   sm20Dn ;
        //      for everything with slope >=0% durations (sec/m) is calculated based on the speed derived from friction and input power (Watt)
        durations[slopes.length-5] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-5], watt0, cr0) ;
        durations[slopes.length-4] = f0up /  getFrictionBasedVelocity(slopes[slopes.length-4], watt, cr1) ;
        durations[slopes.length-3] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, crUp)  ;
        durations[slopes.length-2] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, crUp)  ;
        durations[slopes.length-1] = f3Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, crUp)  ;
        //      duration at -4% only used for the reference profiles.
        if (!getWithRef()) {
            durations[indRefDnSlopeOpt] = durations[slopes.length-5]+getRelSlope(sc)*slopes[indRefDnSlopeOpt];
        }

        if (isCostSpline&&distFactCostFunct.length>0){
            for (int i = 0; i < distFactCostFunct.length; i++) {
                durations[i] = durations[i] * distFactCostFunct[i];
            }

        }

        String SplineType = isCostSpline ? "cost":"dura";
        String contextString = String.format(Locale.ENGLISH,"spline=%s %s ",SplineType,getSurfaceCatTxt(sc));


        float slopeTarget = 0f;
        CubicSpline cubicSplineTmp;
        CubicSpline test=null;
        if (getWithRef()) {
            /* to achieve an almost constant downhill profile for a given mtbDn scale and downhill level of the profile (sDn) independent of the mtbUp scale and the uphill level of the profile (sUp)
            a reference profile for a given combination of sDn and mtbDn with a constant uphill profile (power = 100 Watt, sUp = 2 ) and mtbUp = mtbDn is calculated. All other uphill combinations are
            calculated in such a way that the slope at -20% is taken from the reference profile und the duration is varied at -4% slope, so that the slope matches the target slope
             */
//            int mtbDn = getMtbDn(sc);

            if (isCostSpline)
                cubicSplineTmp = getCostRefCubicSpline(sc);//getRefProfile().getCostSpline(getRefSc(sc));
            else
                cubicSplineTmp = getDurationRefCubicSpline(sc); //getRefProfile().getDurationSpline(getRefSc(sc));
            for ( int i = 0; slopes[i]<0;i++) {
                durations[i] = cubicSplineTmp.valueAt(slopes[i]) ;//* factor;
            }
            slopeTarget = cubicSplineTmp.derivativeAt(slopes[indRefDnSlope]); //*factor;
            cubicSplineTmp = getSlopeOptSpline(slopes, durations, indRefDnSlope, slopeTarget, indRefDnSlopeOpt);
        }
        else {
            cubicSplineTmp = new CubicSpline(slopes, durations);
        }
        if (getWithRef() ) {
            float slope2slopeTarget = cubicSplineTmp.derivativeAt(slopes[indRefDnSlope]) / slopeTarget;
            if (Math.abs(slope2slopeTarget - 1f) > 0.01f) {
                String msg = String.format(Locale.ENGLISH, "for %s Slope to Slopetarget=%.3f at %.2f", contextString, slope2slopeTarget, slopes[indRefDnSlope] * 100f);
                if (slope2slopeTarget > 0.5f && slope2slopeTarget < 2f)
                    throw new RuntimeException("Almost Out of range " + msg); //mgLog.w(msg);
                else
                    throw new RuntimeException("Out of range " + msg);
            }
        }
       cubicSplineTmp.equals(test);
        return cubicSplineTmp;
    }


    private CubicSpline getSlopeOptSpline(float[] slopes, float[] durations, int targetat, float slopeTarget, int varyat) {
        //     IfFunction of Minimum duration value of a spline based on input duration varied at slope[varyat] (for MTB splines at slope -3.5% )
        IfFunction slope = smvary -> {
            durations[varyat] = smvary;
            CubicSpline cubicSpline = new CubicSpline(slopes,durations);
            return cubicSpline.derivativeAt(slopes[targetat]) - slopeTarget;
        };
//      Newton iteration with numerical derivation is used to optimize the input duration[varyat] so that the Min duration matches the Target smMinTarget
        durations[varyat] = ProfileUtil.newtonNumeric(durations[varyat],0.00001f,slope,0.0001f);
        try {
            return new CubicSpline(slopes, durations);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() + " in getSlopeOptSpline");
        }
    }

    private CubicSpline getCostRefCubicSpline(int sc){
        int scDn = sc2MTBc.getCatDn(sc);
        CubicSpline cubicSpline = costRefSplines[scDn];
        if (cubicSpline==null){
            cubicSpline = refContext.calcCostSpline(getRefSc(sc));
            costRefSplines[scDn]= cubicSpline;
        }
        return cubicSpline;
    }

    private CubicSpline getDurationRefCubicSpline(int sc){
        int scDn = sc2MTBc.getCatDn(sc);
        CubicSpline cubicSpline = durationRefSplines[scDn];
        if (cubicSpline==null){
            cubicSpline = refContext.calcDurationSpline(getRefSc(sc));
            durationRefSplines[scDn]= cubicSpline;
        }
        return cubicSpline;
    }


    @NonNull
    public String toString() {
        return String.format(Locale.ENGLISH, "power=%3d sUp=%3d sDn=%3d hasRef=%s", power, sUp, sDn, withRef ? "x" : " ");
    }

    public boolean checkAll() {
        return checkAll;
    }

    
    private boolean getWithRef() {
        return withRef;
    }

    

    private int getRefSc(int sc) {
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

    
    public CubicSpline calcCostSpline(int sc) {
        return calcCubicSpline(sc,true);
    }

    
    public CubicSpline calcDurationSpline(int sc) {
        return  calcCubicSpline(sc,false);
    }

    
    private boolean isValidSc(int surfaceCat) {
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

    
    private float getRelSlope(int sc) {
        return srelSlope[sc2MTBc.getCatDn(sc)];
    }

    
    private float getF3d(int sc) {
        return f3d;
    }

    
    private float getF2d(int sc) {
        return f2d;
    }

    
    private float getSm20Dn(int sc) {
        return deltaSM20Dn[sc2MTBc.getCatDn(sc)];
    }

    
    private float getFactorDn(int sc) {
        return factorDown[sc2MTBc.getCatDn(sc)];
    }

    
    private float getCrDn(int sc) {
        return crDn[sc2MTBc.getCatDn(sc)];
    }

    
    private float getCrUp(int sc) {
        return crUp[sc2MTBc.getCatUpExt(sc)];
    }


    
    private float getF0u(int sc) {
        return f0u[sc2MTBc.getCatUpExt(sc)];
    }

    
    private float getF1u(int sc) {
        return f1u[sc2MTBc.getCatUpExt(sc)];
    }

    
    private float getF2u(int sc) {
        return f2u[sc2MTBc.getCatUpExt(sc)];
    }

    
    private float getF3u(int sc) {
        return f3u[sc2MTBc.getCatUpExt(sc)];
    }

    
    private float[] getDistFactforCostFunct(int sc) {
        if (sc < distFactforCostFunct.length)
            return distFactforCostFunct[sc];
        else if ( sc2MTBc.getMtbDn(sc) >= 2 && sDn >= 300  ) { // for sDn=300 do explicitly prefer scDn = 3 ( and to a lesser extent 2 and 4 )
            double off = sDn/100f - sc2MTBc.getMtbDn(sc);
            float bell = (float) Math.exp(-0.6d*(off*off));
            float[] distFact = new float[slopesAll.length];
            for (int i=0;i<distFact.length;i++) {
                if (slopesAll[i]<-0.2f)
                    distFact[i] = 1f - 0.3f*bell;
                else if (slopesAll[i] < 0f)
                    distFact[i] = 1f - 0.2f*bell;
                else
                    distFact[i] = 1f;
            }
            return distFact;
        } else
            return new float[]{};
    }

    
    public String getSurfaceCatTxt(int sc) {
        int mtbUp = sc2MTBc.getMtbUp(sc);
        String mtbUpTxt = String.format("%s", (mtbUp < 0 ? "-" : mtbUp));
        int mtbDn = sc2MTBc.getMtbDn(sc);
        String mtbDnTxt = String.format("%s", (mtbDn < 0 ? "-" : mtbDn));
        return String.format(Locale.ENGLISH, "%s SurfCat=%2d SurfLvl=%1d mtbDn=%s mtbUp=%s", this, sc, sc2MTBc.getSurfaceLevel(sc), mtbDnTxt, mtbUpTxt);
    }

    
    private float getWatt0(int sc) {
        return watt0[sc];
    }

    
    private float getWatt(int sc) {
        return watt[sc];
    }

    
    public float getMinDistFactSC0() {
        return 1f;
    }

    
    public int getSDn() {
        return sDn;
    }
}
