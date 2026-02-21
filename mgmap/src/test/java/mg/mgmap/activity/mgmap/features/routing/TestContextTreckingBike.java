package mg.mgmap.activity.mgmap.features.routing;

import static mg.mgmap.activity.mgmap.features.routing.profile.ProfileUtil.getFrictionBasedVelocity;

import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.CubicSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.IfSplineProfileContext;
import mg.mgmap.activity.mgmap.features.routing.profile.TagEval;

public class TestContextTreckingBike implements IfSplineProfileContext {

    int maxSurfaceCat = 7;

    private float watt0 = 90.0f ;
    float [] cr =      new float[]{0.0035f,0.005f,0.0076f,0.02f,0.04f,0.075f,0.13f};
    float[] relSlope = new float[]{1.7f,1.7f,0.7f,0.7f,0.9f,1.2f,1.4f};
    float[] sm20Dn = new float[]{0.425f,0.4845f,0.595f,0.765f,0.85f,1.02f,1.955f};
    float[] factorDown = new float[maxSurfaceCat];
    boolean fullCalc;


    public TestContextTreckingBike(){this(true);}
    public TestContextTreckingBike(boolean fullCalc){
        this.fullCalc = fullCalc;
        for ( int sc=0 ;sc < maxSurfaceCat;sc++){
            factorDown[sc]  = sm20Dn[sc]*10f;
        }
    }

    private CubicSpline calcCubicSpline(int sc){

        int indRefDnSlope = 3; //getIndRefDnSlope();
        int indRefDnSlopeOpt = indRefDnSlope+1;

        //   default float getF0u(int sc) {return 1f;};
        //   default float getF1u(int sc){return 1.1f;};
        //   default float getF2u(int sc){return 1.1f*getF1u(sc);};
        //    default float getF3u(int sc){return 1.8f*getF2u(sc);};

        float crUp = getCrUp(sc);
        float f0up = 1.0f;
        float f1Up = 1.1f;
        float f2Up = 1.1f*f1Up;
        float f3Up = 1.8f*f2Up;

        //    default float getF3d(int sc){return 3f;};
        //    default float getF2d(int sc){return 1.1f;};
        float sm20Dn = getSm20Dn(sc);
        float factorDn = getFactorDn(sc);
        float f2d      = 1.1f;
        float f3d      = 3f;
//        float[] distFactCostFunct = getDistFactforCostFunct(sc);
        float[] slopes = IfSplineProfileContext.slopesAll.clone();
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
        durations[slopes.length-5] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-5], watt0,crUp) ;
        durations[slopes.length-4] = f0up /  getFrictionBasedVelocity(slopes[slopes.length-4], watt, crUp) ;
        durations[slopes.length-3] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, crUp)  ;
        durations[slopes.length-2] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, crUp)  ;
        durations[slopes.length-1] = f3Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, crUp)  ;
        //      duration at -4% only used for the reference profiles.
        durations[indRefDnSlopeOpt] = durations[slopes.length-5]+getRelSlope(sc)*slopes[indRefDnSlopeOpt];

        CubicSpline cubicSplineTmp;
        cubicSplineTmp = new CubicSpline(slopes, durations);
        return cubicSplineTmp;
    }
    static float sig(double base){
        return (float) (1./(1.+Math.exp(base)));
    }




    public int getMaxSurfaceCat() {
        return maxSurfaceCat;
    }


    public int getScHeuristicRefSpline() {
        return 1;
    }


    public int getScProfileSpline() {
        return 2;
    }


    public CubicSpline calcCostSpline(int sc) {
        return calcCubicSpline(sc);
    }

    public CubicSpline calcDurationSpline(int sc) {
        return calcCubicSpline(sc);
    }


    private float getRelSlope(int sc) {
        return relSlope[sc];
    }


    private float getSm20Dn(int sc) {
        return  sm20Dn[sc];
    }


    private float getFactorDn(int sc) {
        return factorDown[sc];
    }


    private float getCrUp(int sc) {
        return cr[sc];
    }



    public String getSurfaceCatTxt(int sc) {
        return String.format("TreckingBike sc=%1d", sc);
    }


    private float getWatt0(int sc) {
        return watt0;
    }


    private float getWatt(int sc) {
        return watt0 + 40f*sig(sc-5);
    }


    public float getMinDistFactSC0() {
        return (float) TagEval.minDistfSc0;
    }


    public String toString(){
        return "Trekking Bike";
    }
}





