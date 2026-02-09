package mg.mgmap.activity.mgmap.features.routing.profile;

public class SplineProfileContextTreckingBike implements IfSplineProfileContext {

    int maxSurfaceCat = 7;

    private float watt0 = 90.0f ;
    float [] cr =      new float[]{0.0035f,0.005f,0.0076f,0.02f,0.04f,0.075f,0.13f};
    float[] relSlope = new float[]{1.7f,1.7f,0.7f,0.7f,0.9f,1.2f,1.4f};
    float[] sm20Dn = new float[]{0.425f,0.4845f,0.595f,0.765f,0.85f,1.02f,1.955f};
    float[] factorDown = new float[maxSurfaceCat];
    boolean fullCalc;


    public SplineProfileContextTreckingBike(){this(true);}
    public SplineProfileContextTreckingBike(boolean fullCalc){
        this.fullCalc = fullCalc;
        for ( int sc=0 ;sc < maxSurfaceCat;sc++){
            factorDown[sc]  = sm20Dn[sc]*10f;
        }
    }


    static float sig(double base){
        return (float) (1./(1.+Math.exp(base)));
    }

    @Override
    public boolean fullCalc(){
        return fullCalc;
    }

    @Override
    public int getMaxSurfaceCat() {
        return maxSurfaceCat;
    }

    @Override
    public int getScHeuristicRefSpline() {
        return 1;
    }

    @Override
    public int getScProfileSpline() {
        return 2;
    }


    @Override
    public float getRelSlope(int sc) {
        return relSlope[sc];
    }

    @Override
    public float getSm20Dn(int sc) {
        return  sm20Dn[sc];
    }

    @Override
    public float getFactorDn(int sc) {
        return factorDown[sc];
    }

    @Override
    public float getCrUp(int sc) {
        return cr[sc];
    }


    @Override
    public String getSurfaceCatTxt(int sc) {
        return String.format("TreckingBike sc=%1d", sc);
    }

    @Override
    public float getWatt0(int sc) {
        return watt0;
    }

    @Override
    public float getWatt(int sc) {
        return watt0 + 40f*sig(sc-5);
    }

    @Override
    public float getMinDistFactSC0() {
        return (float) TagEval.minDistfSc0;
    }
}

