package mg.mgmap.activity.mgmap.features.routing.profile;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfile implements IfProfileCostCalculator {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    public static final float minNegCurvatureRadius= 0.01f;

    private final CubicSpline cubicProfileSpline;
    private final CubicSpline cubicHeuristicSpline;
    protected final CubicSpline[] SurfaceCatCostSpline ;
    protected final CubicSpline[] SurfaceCatDurationSpline;
    private final IfSplineProfileContext context;

    private float heuristicDnSlopeLimit;
    private float heuristicUpSlopeLimit;

    private static final float lowstart = -0.15f;
    private static final float highstart = 0.15f;

    public final float refCosts;

    public CostCalcSplineProfile(IfSplineProfileContext context) {
        this.context = context;
        SurfaceCatCostSpline = new CubicSpline[getMaxSurfaceCat()];
        SurfaceCatDurationSpline = new CubicSpline[context.getMaxSurfaceCat()];
        cubicHeuristicSpline = getCubicHeuristicSpline();
        cubicProfileSpline = getProfileSpline();
        if (context.checkAll()) checkAll();
        refCosts = cubicProfileSpline.valueAt(0f);
    }
    public int getMaxSurfaceCat(){
        return context.getMaxSurfaceCat();
    }



    public CubicSpline getCubicHeuristicSpline(){
        if (cubicHeuristicSpline == null) {
            mgLog.i("Spline Profile for " + this.getClass().getName() + " " + context.toString());
            CubicSpline cubicHeuristicRefSpline = getHeuristicRefSpline();
            return calcHeuristicSpline(cubicHeuristicRefSpline);
        } else {
            return cubicHeuristicSpline;
        }
    }

    public double calcCosts(double dist, float vertDist, boolean primaryDirection) {
        if (dist <= 0.0000001) {
            return 0.0001;
        }
        return dist * cubicProfileSpline.valueAt(vertDist / (float) dist) ;
    }

    public double heuristic(double dist, float vertDist) {
        if (dist <= 0.0000001) {
            return 0.0;
        }
        return dist * cubicHeuristicSpline.valueAt(vertDist / (float) dist) ;
    }


    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            double spm = cubicProfileSpline.valueAt(slope);
            double v = 3.6/spm;
            mgLog.v(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f costf=%.2f",100f*slope,v,spm*dist,dist,cubicProfileSpline.valueAt(vertDist / (float) dist)));
        }
        return (dist >= 0.00001) ? (long) (1000 * dist * cubicProfileSpline.valueAt(vertDist / (float) dist)) : 0;
    }


    private void checkAll() {
        boolean negativeCurvature = false;
        CubicSpline cubicSpline = null;
        StringBuilder msgTxt = new StringBuilder();
        //noinspection unchecked
        ArrayList<CubicSpline.Value>[] violations = (ArrayList<CubicSpline.Value>[]) new ArrayList[getMaxSurfaceCat()+1];
        for ( int surfaceCat = 0 ; surfaceCat < getMaxSurfaceCat(); surfaceCat++){
            try {
                cubicSpline = getCostSpline(surfaceCat);
            } catch (Exception e) {
                mgLog.e(e.getMessage());
                msgTxt.append(e.getMessage());
                negativeCurvature = true;
            }
            if (cubicSpline!= null ) violations[surfaceCat] = checkSplineHeuristic(cubicSpline, surfaceCat);
        }
        boolean heuristicViolation = false;
        for ( int surfaceCat=0; surfaceCat<violations.length;surfaceCat++) {
            if (violations[surfaceCat]!=null && !violations[surfaceCat].isEmpty()) {
                heuristicViolation = true;
                msgTxt.append(String.format(Locale.ENGLISH,"\rViolation of Heuristic for %s at",getSurfaceCatTxt(surfaceCat)));
                for (CubicSpline.Value violationAt : violations[surfaceCat]){
                    msgTxt.append(String.format(Locale.ENGLISH, "(%.1f,%.5f)", violationAt.x() * 100, violationAt.y()));
                }
                mgLog.e(msgTxt::toString);
            }
        }
        if (negativeCurvature||heuristicViolation)
            throw new RuntimeException( msgTxt.toString() );


        for (int surfaceCat = 0; surfaceCat < context.getMaxSurfaceCat(); surfaceCat++){
            try {
                getDurationSpline(surfaceCat);
            } catch (Exception e) {
                mgLog.e(e.getMessage());
            }
        }
    }

    public IfSpline getCostFunc(int surfaceCat) {
            CubicSpline costSpline = getCostSpline(surfaceCat);
            return costSpline;
    }


    public CubicSpline getCostSpline(int surfaceCat) {
        CubicSpline cubicSpline = SurfaceCatCostSpline[surfaceCat];
        if (cubicSpline == null) {
            cubicSpline = calcSpline(true,surfaceCat );
            SurfaceCatCostSpline[surfaceCat] = cubicSpline;
        }
        return cubicSpline;
    }


    protected ArrayList<CubicSpline.Value> checkSplineHeuristic(CubicSpline cubicSpline, int surfaceCat)  {
        float xs = lowstart - 0.3f;
        float minmfd = (surfaceCat == 0) ? context.getMinDistFactSC0() : 1f;
        ArrayList<CubicSpline.Value> violations = new ArrayList<>();
        do {
            xs = xs + 0.001f;
            // make sure that costs are always larger than Heuristic
            if (cubicHeuristicSpline !=null ) {
                float heuristic = cubicHeuristicSpline.valueAt(xs);
                float spline    = cubicSpline.valueAt(xs);
                float delta = minmfd * spline - heuristic;
                if (delta <= 0.00005)
                    violations.add(new CubicSpline.Value(xs,delta));
            }
        } while (xs < highstart + 0.3f);
        return violations;
    }
    private CubicSpline getProfileSpline( ) {
        try {
            return getCostSpline(context.getScProfileSpline());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CubicSpline getHeuristicRefSpline() {
        try {
            CubicSpline cubicSplineTmp = getCostSpline(context.getScHeuristicRefSpline());
            CubicSpline cubicSpline = cubicSplineTmp.translateY(-0.0001f);
            checkNegCurvature(cubicSpline,String.format(Locale.ENGLISH,"heuristic spline for %s ", context),1e7f);
            return cubicSpline;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public IfSplineProfileContext getContext() {
        return context;
    }


    public float getMinDistFactSC0(){
        return  context.getMinDistFactSC0();
    }


    private CubicSpline calcSpline(boolean costSpline, int surfaceCat)  {
        long t1 = System.nanoTime();
        CubicSpline cubicSpline = costSpline ? context.calcCostSpline(surfaceCat):context.calcDurationSpline(surfaceCat);
        if (cubicSpline==null) return null;
        String SplineType = costSpline ? "cost":"dura";
        String contextString = String.format(Locale.ENGLISH,"spline=%s %s ",SplineType,getSurfaceCatTxt(surfaceCat));

        checkNegCurvature(cubicSpline,contextString,minNegCurvatureRadius);
        long t = ( System.nanoTime() - t1 ) /1000;
        mgLog.v( ()-> {
            try {
                float slopeMin = cubicSpline.calcMin(-0.13f);
                float smMinOpt = cubicSpline.valueAt(slopeMin);
                float sm0 = cubicSpline.valueAt(0f);
                return String.format(Locale.ENGLISH, "For %s t[µs]=%4d. Min at Slope=%6.2f smMin=%.3f vmax=%5.2f sm0=%.3f v0=%.2f",
                        contextString,t,100f*slopeMin,smMinOpt,3.6f/smMinOpt,sm0,3.6f/sm0);
            } catch (Exception e) {
                return String.format(Locale.ENGLISH, "%s for %s",e.getMessage(),contextString);
            }
        });
        return cubicSpline;
    }

    private void checkNegCurvature(CubicSpline cubicSpline, String context, float threshold) {
        ArrayList<CubicSpline.Value> curveRadiusForNegCurvaturePoint = cubicSpline.getCurveRadiusForNegCurvaturePoints();
        if (curveRadiusForNegCurvaturePoint != null) {
            StringBuilder msg = new StringBuilder(String.format(Locale.ENGLISH,"Negative curvature for %s",context));
            boolean criticalthresholdReached = false;
            boolean thresholdReached = false;
            for (CubicSpline.Value negCurvature : curveRadiusForNegCurvaturePoint) {
                float curvature = -negCurvature.y();
                if (curvature < threshold)
                    criticalthresholdReached = true;
                if ( curvature < 50f ) {
                    thresholdReached = true;
                }
                if (curvature < threshold || curvature < 50f ){
                    msg.append(": ");
                    msg.append(String.format(Locale.ENGLISH, " slope=%.2f", 100 * negCurvature.x()));
                    msg.append(String.format(Locale.ENGLISH, " curve Radius=%.2f", -negCurvature.y()));
                }
            }
            if(criticalthresholdReached)
                throw new RuntimeException( msg.toString());
            else
                if (thresholdReached) mgLog.w(msg.toString());
        }
    }


    public IfSpline getDurationFunc(int surfaceCat) {
        try {
            return getDurationSpline(surfaceCat);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getRefCosts() {
        return refCosts;
    }

    CubicSpline getDurationSpline(int surfaceCat) {
        CubicSpline cubicSpline;
        cubicSpline = SurfaceCatDurationSpline[surfaceCat];
        if (cubicSpline == null) {
            cubicSpline = calcSpline(false,surfaceCat );
            SurfaceCatDurationSpline[surfaceCat] = cubicSpline;
        }
        return cubicSpline;
    }

    public String getSurfaceCatTxt(int surfaceCat){
         return context.getSurfaceCatTxt(surfaceCat);
    }


    /* How the heuristic is determined:
       Given two points with a distance d and vertical distance v and a continuously differentiable
       cost IfFunction of the elevation f(e) = f(v/d) (given here as a cubicSpline IfFunction) and the costs from source to target point is
       given by: c(d,v) = d*f(v/d). If the vertical distance is given and constant, one can vary the path to the
       target and thereby increase the distance. A criteria of this minimum cost is that the first
       derivative varying d is 0, c'(d) = f(v/d) - d * f'(d) v/d^2 = f(e) - f'(e)*e = 0 or
       f(e) = f'(e)*e. This is a tangent on f(e) crossing the origin.
       If f(e) has a single minimum and curvature is positive (f''(e)>=0), there will be two such
       tangents. So a heuristic is given by the two tangents above and below the touch points of those tangents
       and by f(e) between those touch points.
       Equations solved via Newton Iteration.
     */

    public CubicSpline calcHeuristicSpline(CubicSpline refCubicSpline ){
        // cut out a new cubic spline out of the existing one using the to touch points of the tangents
        IfFunction tangent = x -> refCubicSpline.valueAt(x) - refCubicSpline.derivativeAt(x) * x - 0.0001f;
        IfFunction tangDeriv = x -> -refCubicSpline.secondDerivativeAt(x) * x;
        heuristicDnSlopeLimit = ProfileUtil.newton(lowstart,0.00005f,10,tangent,tangDeriv);
        float dnSlopeLimitValue = refCubicSpline.valueAt(heuristicDnSlopeLimit);
        float dnSlopeLimitSlope = refCubicSpline.derivativeAt(heuristicDnSlopeLimit);
        float yintercept_left = dnSlopeLimitValue - heuristicDnSlopeLimit*dnSlopeLimitSlope;
        if (yintercept_left <= 0.00003)
            throw new RuntimeException( String.format(Locale.ENGLISH,"Heuristic left tangent too small intercept with %.2f", yintercept_left ));

        heuristicUpSlopeLimit = ProfileUtil.newton(highstart,0.00005f,10,tangent,tangDeriv);
        float upSlopeLimitValue = refCubicSpline.valueAt(heuristicUpSlopeLimit);
        float upSlopeLimitSlope = refCubicSpline.derivativeAt(heuristicUpSlopeLimit);
        float yintercept_right = upSlopeLimitValue - heuristicUpSlopeLimit*upSlopeLimitSlope;
        if (yintercept_right <= 0.00003)
            throw new RuntimeException( String.format(Locale.ENGLISH,"Heuristic right tangent too small intercept with %.2f", yintercept_right ));

        float[] slopes = refCubicSpline.getX();
        float[] durations = refCubicSpline.getY();

        int i = 0;
        for ( float slope :slopes) if (slope > heuristicDnSlopeLimit && slope< heuristicUpSlopeLimit ) i++;
        float[] newSlopes = new float[i+2];
        float[] newDurations = new float[i+2];
        newSlopes[0] = heuristicDnSlopeLimit;
        newDurations[0] = dnSlopeLimitValue;
        newSlopes[newSlopes.length-1]=heuristicUpSlopeLimit;
        newDurations[newSlopes.length-1]=upSlopeLimitValue;
        i=1;
        for ( int j=0; j < slopes.length; j++) {
            if (slopes[j] > heuristicDnSlopeLimit && slopes[j]< heuristicUpSlopeLimit ) {
                newSlopes[i]=slopes[j];
                newDurations[i]=durations[j];
                i++;
            }
        }
        IfSplineDef def = new IfSplineDef.Impl(newSlopes, newDurations, IfSplineDef.clampedLeft(dnSlopeLimitSlope), IfSplineDef.clampedRight(upSlopeLimitSlope));
        CubicSpline cubicCutSpline = new CubicSpline(def);

        mgLog.i(()-> String.format(Locale.ENGLISH, "Heuristic for %s: DnSlopeLim=%.2f UpSlopeLim=%.2f",context.toString(),heuristicDnSlopeLimit*100,heuristicUpSlopeLimit*100));
        return cubicCutSpline; //refCubicSpline.getCutCubicSpline(heuristicDnSlopeLimit,heuristicUpSlopeLimit);
    }



}


