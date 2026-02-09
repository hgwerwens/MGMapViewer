package mg.mgmap.activity.mgmap.features.routing.profile;


import java.util.Locale;

public class CostCalcSplineProfileTreckingBike extends CostCalcSplineProfile {

    private static final int maxSurfaceCat = 7;
    private static final float[] slopesAll = new float[]{ -0.6f,-0.4f,-0.2f, -0.02f, 0.0f, 0.08f, 0.2f, 0.4f};
    public CostCalcSplineProfileTreckingBike() {
        super(new Object());
    }

    public int getMaxSurfaceCat(){
        return maxSurfaceCat;
    }

    protected CubicSpline getProfileSpline(Object context) {
        try {
            return calcSpline(1,context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CubicSpline getHeuristicRefSpline(Object context) {
        try {
            int[] refSurfaceCats = {1};
            float mfd = 1f; //  float mfd = refSurfaceCats[i]==0 ? getMinDistFactSC0():1f;
            CubicSpline[] refCubicSplines = new CubicSpline[refSurfaceCats.length];
            for ( int i=0;i<refSurfaceCats.length;i++){
                int surfaceCat = refSurfaceCats[i];
                refCubicSplines[i] = getCostSpline(surfaceCat);
            }
            float[] minDurations = new  float[slopesAll.length];
            for ( int s=0;s<slopesAll.length;s++){
                minDurations[s] = 1e6f;
                for ( int i=0;i<refSurfaceCats.length;i++){
                    float duration = ( mfd*refCubicSplines[i].calc(slopesAll[s]) - 0.0001f );
                    if (duration < minDurations[s])
                        minDurations[s] = duration;
                }
            }
            return getSpline(slopesAll,minDurations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected float getMinDistFactSC0(Object contxt){
        return (float) TagEval.minDistfSc0;
    }

    protected CubicSpline calcSpline(int surfaceLevel, Object context)  {
        float watt0 = 90.0f ;
        float watt = 130.0f;
        float ACw = 0.45f;
        float fdown = 8.5f;
        float m = 90f;
        float [] cr = new float[] {0.0035f,0.005f,0.0076f,0.02f,0.04f,0.075f,0.13f};
        float [] highdowndoffset = new float[] {0.15f,0.143f,0.13f,0.11f,0.1f,0.08f,-0.03f};
        float[] relSlope;
        float[] slopes;
        float[] durations;
        if (surfaceLevel <= 3) {
            slopes = slopesAll;
            relSlope = new float[]{2.2f,2.3f,1.15f,1.0f};
            durations = new float[slopes.length];
            durations[4] = 1f / getFrictionBasedVelocity(0.0f, watt0, cr[surfaceLevel], ACw, m);
            durations[3] = durations[4] + slopes[3]*relSlope[surfaceLevel];
        } else {
            slopes = new float[]{-0.6f,-0.4f,-0.2f, 0.0f, 0.08f,0.2f, 0.4f};
            durations = new float[slopes.length];
            durations[3] = 1 / getFrictionBasedVelocity(0.0, watt0, cr[surfaceLevel], ACw, m);
        }
        durations[0] = -(slopes[0]+highdowndoffset[surfaceLevel])*fdown*1.5f;
        durations[1] = -(slopes[1]+highdowndoffset[surfaceLevel])*fdown;
        durations[2] = -(slopes[2]+highdowndoffset[surfaceLevel])*fdown;
        durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr[surfaceLevel], ACw, m)  ;
        durations[slopes.length-2] = 1.5f /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr[surfaceLevel], ACw, m)  ;
        durations[slopes.length-1] = 3.0f /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr[surfaceLevel], ACw, m)  ;
        return getSpline(slopes, durations);
    }

    public String getSurfaceCatTxt(int surfaceCat){
        return String.format(Locale.ENGLISH,"SurfaceLevel=%s",surfaceCat);
    }

    protected CubicSpline getSpline(short surfaceLevel) {
        try {
            return getCostSpline(surfaceLevel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IfFunction getDurationFunc(int surfaceCat) {
        return getSpline((short) surfaceCat);
    }
}

