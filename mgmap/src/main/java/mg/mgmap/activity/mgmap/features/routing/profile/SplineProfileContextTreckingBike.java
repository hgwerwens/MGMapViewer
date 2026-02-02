package mg.mgmap.activity.mgmap.features.routing.profile;

public class SplineProfileContextTreckingBike implements IfSplineProfileContext {

    int maxSurfaceCat = 7;

    private static float[] slopesAll = new float[]{-0.76f, -0.36f, -0.32f, -0.2f, -0.04f, 0.0f, 0.065f,0.17f,0.195f, 0.275f};
    private float[] distFactSc0 = new float[slopesAll.length];
    private float watt0 = 90.0f ;
    float watt = 130.0f;
    float [] cr =      new float[]{0.0035f,0.005f,0.0076f,0.02f,0.04f,0.075f,0.13f};
    float[] relSlope = new float[]{1.7f,1.7f,0.7f,0.7f,0.9f,1.2f,1.4f}; //new float[]{2.2f,2.3f,1.15f,1.0f,1.0f,1.0f,1.0f};
    float[] sm20Dn = new float[]{0.425f,0.4845f,0.595f,0.765f,0.85f,1.02f,1.955f};
    float[] factorDown = new float[maxSurfaceCat];
    boolean fullCalc;

    public SplineProfileContextTreckingBike(boolean fullCalc){
        this.fullCalc = fullCalc;
        for (int i=0;i<distFactSc0.length;i++) distFactSc0[i] = 1.21f;
        for ( int sc=0 ;sc < maxSurfaceCat;sc++){
            factorDown[sc]  = sm20Dn[sc]*10f;
        }
    }


    static float sig(double base){
        return (float) (1./(1.+Math.exp(base)));
    }
    @Override
    public boolean getWithRef() {
        return false;
    }

    @Override
    public boolean fullCalc(){
        return fullCalc;
    }
    @Override
    public CostCalcSplineProfile getRefProfile() {
        return null;
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
    public float[] getSlopesAll() {
        return slopesAll;
    }

    @Override
    public float getRelSlope(int sc) {
        return relSlope[sc];
    }

    @Override
    public float getF3d(int sc) {
        return 3.0f;
    }

    @Override
    public float getF2d(int sc) {
        return 1.1f;
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

    public float getCrDn(int sc) {
        return cr[sc];
    }

    @Override
    public float getF0u(int sc) {
        return 1.0f;
    }

    @Override
    public float getF1u(int sc) {
        return 1.1f;
    }

    @Override
    public float getF2u(int sc) {
        return 1.1f*getF1u(sc);
    }

    @Override
    public float getF3u(int sc) {
        return 1.8f*getF2u(sc);
    }

    @Override
    public float getUlstrechCost(int sc) {
        return 1f;
    }

    @Override
    public float getUlstrechDuration(int sc) {
        return 1f;
    }

    @Override
    public float[] getDistFactforCostFunct(int sc) {
        if (sc==0)
            return distFactSc0;
        return new float[]{};
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
}

