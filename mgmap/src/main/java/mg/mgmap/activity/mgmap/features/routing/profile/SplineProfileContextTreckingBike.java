package mg.mgmap.activity.mgmap.features.routing.profile;

public class SplineProfileContextTreckingBike implements IfSplineProfileContext {

    int maxSurfaceCat = 7;

    private static float[] slopesAll = new float[]{-0.76f, -0.36f, -0.32f, -0.2f, -0.04f, 0.0f, 0.065f,0.17f,0.195f, 0.275f};
    private float watt0 = 90.0f ;
    float watt = 130.0f;
    float [] cr =      new float[]{0.0035f,0.005f,0.0076f,0.02f,0.04f,0.075f,0.13f};
    float[] relSlope = new float[]{2.2f,2.3f,1.15f,1.0f,1.0f,1.0f,1.0f};
    float[] deltaSM20Dn = new float[maxSurfaceCat];
    float[] factorDown = new float[maxSurfaceCat];

    public SplineProfileContextTreckingBike(){
        float deltaSM20DnMin = 0.05f + dSM20scDnLow(0);
        for ( int sc=0 ;sc < maxSurfaceCat;sc++){
            deltaSM20Dn[sc] = deltaSM20DnMin - dSM20scDnLow(sc);
            factorDown[sc]  = deltaSM20Dn[sc]*7.5f;
        }
    }

    static float dSM20scDnLow(int scDn){
        return 0.2f * ( sig(1.5 * (scDn - 2.)) - 0.5f);
    }
    static float sig(double base){
        return (float) (1./(1.+Math.exp(base)));}
    @Override
    public boolean getWithRef() {
        return false;
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
        return 1.07f;
    }

    @Override
    public float getDeltaSM20Dn(int sc) {
        return  deltaSM20Dn[sc];
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
        return 1.05f;
    }

    @Override
    public float getF1u(int sc) {
        return 1.1f;
    }

    @Override
    public float getF2u(int sc) {
        return 1.2f*getF1u(sc);
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
    public float getDistFactforCostFunct(int sc) {
        return 1.f;
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
        return watt;
    }
}

