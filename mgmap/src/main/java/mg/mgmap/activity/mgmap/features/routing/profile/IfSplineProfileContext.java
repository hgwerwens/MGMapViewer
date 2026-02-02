package mg.mgmap.activity.mgmap.features.routing.profile;

public interface IfSplineProfileContext {
    float[] slopesAll = new float[]{-0.76f, -0.36f, -0.32f, -0.2f, -0.04f, 0.0f, 0.065f,0.17f,0.195f, 0.275f};
    float[] distFactorCostFunct = new float[]{1f,1f,1f,1f,1f,1f,1f,1f,1f,1f};
    boolean getWithRef();
    CostCalcSplineProfile getRefProfile();
    default int getRefSc(int sc) { return sc;};
    int getMaxSurfaceCat();
    int getScHeuristicRefSpline();
    int getScProfileSpline();
    default boolean isValidSc(int surfaceCat){return true;}
    default float getRefDnSlope() {return -0.2f;}
    default int getIndRefDnSlope() {return 3;};
    default boolean fullCalc() {return true;};
    default float[] getSlopesAll(){ return slopesAll;};
    float getRelSlope(int sc);
    float getF3d(int sc);
    float getF2d(int sc);
    float getSm20Dn(int sc);
    float getFactorDn(int sc);
    float getCrDn(int sc);
    float getCrUp(int sc);
    float getF0u(int sc);
    float getF1u(int sc);
    float getF2u(int sc);
    float getF3u(int sc);
    float getUlstrechCost(int sc);
    float getUlstrechDuration(int sc);
    default float[] getDistFactforCostFunct(int sc) {
        return distFactorCostFunct;
    };
    String getSurfaceCatTxt(int sc);
    float getWatt0(int sc);
    float getWatt(int sc);


}
