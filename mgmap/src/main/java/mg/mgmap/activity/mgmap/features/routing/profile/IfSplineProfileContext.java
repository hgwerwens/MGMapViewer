package mg.mgmap.activity.mgmap.features.routing.profile;

public interface IfSplineProfileContext {
    float[] slopesAll = new float[]{-0.76f, -0.36f, -0.32f, -0.2f, -0.04f, 0.0f, 0.065f,0.17f,0.195f, 0.275f};
    default boolean getWithRef() {return false;};
    default CostCalcSplineProfile getRefProfile(){return null;};
    default int getRefSc(int sc) { return sc;};
    int getMaxSurfaceCat();
    int getScHeuristicRefSpline();
    int getScProfileSpline();
    default boolean isValidSc(int surfaceCat){return true;}
    default int getIndRefDnSlope() {return 3;};
    default boolean fullCalc() {return true;};
    default float[] getCostSlopes(int sc){ return slopesAll;};
    default float[] getDurationSlopes(int sc){ return slopesAll;};
    float getRelSlope(int sc);
    default float getF3d(int sc){return 3f;};
    default float getF2d(int sc){return 1.1f;};
    float getSm20Dn(int sc);
    float getFactorDn(int sc);
    default float getCrDn(int sc) { return getCrUp(sc);}
    float getCrUp(int sc);
    default float getF0u(int sc) {return 1f;};
    default float getF1u(int sc){return 1.1f;};
    default float getF2u(int sc){return 1.1f*getF1u(sc);};
    default float getF3u(int sc){return 1.8f*getF2u(sc);};
    default float[] getDistFactforCostFunct(int sc) {
        return new float[]{};
    };
    String getSurfaceCatTxt(int sc);
    float getWatt0(int sc);
    float getWatt(int sc);

    float getMinDistFactSC0();


}
