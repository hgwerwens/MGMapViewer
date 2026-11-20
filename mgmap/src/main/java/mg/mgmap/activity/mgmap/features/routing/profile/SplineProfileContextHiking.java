package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.CubicSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.LinearSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.SplineUtil;

public class SplineProfileContextHiking implements IfSplineProfileContext {

    private final float watt0 = 65.0f ;
    private final int powerDurPercent ;
    boolean fullCalc;

    private static final float[] cTerr = {0.005f,0.006f,0.008f,0.01f,0.012f,0.015f,0.015f};

    public SplineProfileContextHiking(boolean fullCalc,int powerDurPercent){
        this.fullCalc = fullCalc;
        if (powerDurPercent>0)
            this.powerDurPercent = Math.max(50, Math.min(powerDurPercent, 200));
        else
            this.powerDurPercent = 100;

    }

    @Override
    public int getMaxSurfaceCat() {
        return 6;
    }

    @Override
    public int getScHeuristicRefSpline() {
        return 0;
    }

    @Override
    public int getScProfileSpline() {
        return 3;
    }

    @Override
    public IfSpline calcCostSpline(int sc) {
//        float[] slopes = {-0.4f,-.3f,0f,0.3f,0.4f};
//        float[] durations = {3f,1f,1f,4f,6f};
//        float[] durations = {3f,1f,1f,2f,4f};
//        return new LinearSpline(slopes, durations, new IfSplineDef.Linear());
        return calcDurationSpline(sc);
    }

    @Override
    public IfSpline calcDurationSpline(int sc) {
        float fd = 10.5f;
        float off = 0.1f;
        float vbase = 5.25f - 0.1f*sc;
        float t_base = 3.6f/vbase;
        float t_m10Pcnt = t_base/1.3f;
        float watt = 1.5f*watt0;

        float[] slopes = new float[]{-0.5f, -0.3f, -0.075f, 0.0f, 0.2f, 0.5f};
        float[] durations = new float[slopes.length];

        durations[0] = (-slopes[0]-off)*fd;
        durations[1] = (-slopes[1]-off)*fd;
        durations[2] = t_m10Pcnt;
 /*       durations[3] = t_base;
        durations[4] = slopes[4]*fu;
        durations[5] = slopes[5]*fu; */
        durations[3] = (float) (1f/ProfileUtil.getWalkingVelocity(0,watt0,cTerr[sc]));
        durations[4] = (float) (1f/ProfileUtil.getWalkingVelocity(slopes[4],watt,cTerr[sc]));
        durations[5] = (float) (1.1f/ProfileUtil.getWalkingVelocity(slopes[5],watt,cTerr[sc]));

        return new CubicSpline(slopes, durations, new IfSplineDef.Natural());
    }

    @Override
    public float getMinDistFactSC(int surfaceCategory) {
        return 1f;
    }

    @Override
    public String getSurfaceCatTxt(int sc) {
        return String.format("Hiking sl=%1d", sc+1);
    }
}
