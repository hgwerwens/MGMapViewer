package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSpline;

public interface IfSplineProfileContext {
    float[] slopesAll = new float[]{-0.76f, -0.36f, -0.32f, -0.2f, -0.04f, 0.0f, 0.065f,0.17f,0.195f, 0.275f};
    int getMaxSurfaceCat();
    int getScHeuristicRefSpline();
    int getScProfileSpline();

    IfSpline calcCostSpline(int sc);
    IfSpline calcDurationSpline(int sc);
    default boolean getCheckAll() {return true;}; //
    float getMinDistFactSC0();
    String getSurfaceCatTxt(int sc);
    String toString();

}
