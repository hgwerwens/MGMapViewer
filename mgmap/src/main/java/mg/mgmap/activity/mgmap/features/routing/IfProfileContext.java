package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfile;

public interface IfProfileContext {
    boolean getWithRef();
    CostCalcSplineProfile getRefProfile();
    int getRefSc(int sc);
    default int getMaxSurfaceCat(){return 0;}
    default int getScHeuristicRefSpline(){return 0;}
    default int getScProfileSpline(){return 0;};
    default boolean isValidSc(int surfaceCat){return true;}
    default float getRefDnSlope() {return -0.2f;}

    int getIndRefDnSlope();
    float[] getSlopesAll();
    float getRelSlope(int sc);
    float getF3d(int sc);
    float getF2d(int sc);
    float getDeltaSM20Dn(int sc);
    float getFactorDn(int sc);
    float getCrDn(int sc);
    float getCrUp(int sc);
    float getF0u(int sc);
    float getF1u(int sc);
    float getF2u(int sc);
    float getF3u(int sc);
    float getUlstrechCost(int sc);
    float getUlstrechDuration(int sc);
    float getDistFactforCostFunct(int sc);
    String getSurfaceCatTxt(int sc);
    float getWatt0(int sc);
    float getWatt(int sc);

}
