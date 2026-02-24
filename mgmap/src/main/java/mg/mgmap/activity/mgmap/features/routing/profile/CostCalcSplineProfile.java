package mg.mgmap.activity.mgmap.features.routing.profile;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.SplineUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfile implements IfProfileCostCalculator {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    public static final float minNegCurvatureRadius= 0.01f;

    private final IfSpline profileSpline;
    private final IfSpline heuristicSpline;
    protected final IfSpline[] SurfaceCatCostSpline ;
    protected final IfSpline[] SurfaceCatDurationSpline;
    private final IfSplineProfileContext context;
    private static final float lowstart = -0.15f;
    private static final float highstart = 0.15f;

    public final float refCosts;

    public CostCalcSplineProfile(IfSplineProfileContext context) {
        this.context = context;
        SurfaceCatCostSpline = new IfSpline[getMaxSurfaceCat()];
        SurfaceCatDurationSpline = new IfSpline[context.getMaxSurfaceCat()];
        heuristicSpline = getCubicHeuristicSpline();
        profileSpline = getProfileSpline();
        refCosts = profileSpline.valueAt(0f);

        if (context.getCheckAll()) checkAll();
    }
    public int getMaxSurfaceCat(){
        return context.getMaxSurfaceCat();
    }



    public IfSpline getCubicHeuristicSpline(){
        if (heuristicSpline == null )  {
            mgLog.i("Spline Profile for " + this.getClass().getName() + " " + context.toString());
            IfSpline cubicHeuristicRefSpline = getHeuristicRefSpline();
            return calcHeuristicSpline(cubicHeuristicRefSpline);
        } else {
            return heuristicSpline;
        }
    }

    public double calcCosts(double dist, float vertDist, boolean primaryDirection) {
        if (dist <= 0.0000001) {
            return 0.0001;
        }
        return dist * profileSpline.valueAt(vertDist / (float) dist) ;
    }

    public double heuristic(double dist, float vertDist) {
        if (dist <= 0.0000001) {
            return 0.0;
        }
        return dist * heuristicSpline.valueAt(vertDist / (float) dist) ;
    }


    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            double spm = profileSpline.valueAt(slope);
            double v = 3.6/spm;
            mgLog.v(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f costf=%.2f",100f*slope,v,spm*dist,dist, profileSpline.valueAt(vertDist / (float) dist)));
        }
        return (dist >= 0.00001) ? (long) (1000 * dist * profileSpline.valueAt(vertDist / (float) dist)) : 0;
    }


    private void checkAll() {
        boolean negativeCurvature = false;
        IfSpline spline = null;
        StringBuilder msgTxt = new StringBuilder();
        //noinspection unchecked
        ArrayList<Value>[] violations = (ArrayList<Value>[]) new ArrayList[getMaxSurfaceCat()+1];
        for ( int surfaceCat = 0 ; surfaceCat < getMaxSurfaceCat(); surfaceCat++){
            try {
                spline = getCostSpline(surfaceCat);
            } catch (Exception e) {
                mgLog.e(e.getMessage());
                msgTxt.append(e.getMessage());
                negativeCurvature = true;
            }
            if (spline!= null ) violations[surfaceCat] = checkSplineHeuristic(spline, surfaceCat);
        }
        boolean heuristicViolation = false;
        for ( int surfaceCat=0; surfaceCat<violations.length;surfaceCat++) {
            if (violations[surfaceCat]!=null && !violations[surfaceCat].isEmpty()) {
                heuristicViolation = true;
                msgTxt.append(String.format(Locale.ENGLISH,"\rViolation of Heuristic for %s at",getSurfaceCatTxt(surfaceCat)));
                for (Value violationAt : violations[surfaceCat]){
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
            return getCostSpline(surfaceCat);
    }


    public IfSpline getCostSpline(int surfaceCat) {
        IfSpline spline = SurfaceCatCostSpline[surfaceCat];
        if (spline == null) {
            spline = calcSpline(true,surfaceCat );
            SurfaceCatCostSpline[surfaceCat] = spline;
        }
        return spline;
    }


    protected ArrayList<Value> checkSplineHeuristic(IfSpline spline, int surfaceCat)  {
        float xs = lowstart - 0.3f;
        float minmfd = (surfaceCat == 0) ? context.getMinDistFactSC0() : 1f;
        ArrayList<Value> violations = new ArrayList<>();
        do {
            xs = xs + 0.001f;
            // make sure that costs are always larger than Heuristic
            if (heuristicSpline !=null ) {
                float heuristic = heuristicSpline.valueAt(xs);
                float splineValue    = spline.valueAt(xs);
                float delta = minmfd * splineValue - heuristic;
                if (delta <= 0.00005)
                    violations.add(new Value(xs,delta));
            }
        } while (xs < highstart + 0.3f);
        return violations;
    }
    private IfSpline getProfileSpline( ) {
        try {
            return getCostSpline(context.getScProfileSpline());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private IfSpline getHeuristicRefSpline() {
        try {
            IfSpline splineTmp = getCostSpline(context.getScHeuristicRefSpline());
            IfSpline spline    =  splineTmp.transformY(1.0f,-0.0001f);
            checkNegCurvature(spline,String.format(Locale.ENGLISH,"heuristic spline for %s ", context),1e7f);
            return spline;
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


    private IfSpline calcSpline(boolean costSpline, int surfaceCat)  {
        long t1 = System.nanoTime();
        IfSpline spline = costSpline ? context.calcCostSpline(surfaceCat):context.calcDurationSpline(surfaceCat);
        if (spline==null) return null;
        String SplineType = costSpline ? "cost":"dura";
        String contextString = String.format(Locale.ENGLISH,"spline=%s %s ",SplineType,getSurfaceCatTxt(surfaceCat));

        checkNegCurvature(spline,contextString,minNegCurvatureRadius);
        long t = ( System.nanoTime() - t1 ) /1000;
        mgLog.v( ()-> {
            try {
                float slopeMin = SplineUtil.getMin(spline,-0.13f);
                float smMinOpt = spline.valueAt(slopeMin);
                float sm0 = spline.valueAt(0f);
                return String.format(Locale.ENGLISH, "For %s t[µs]=%4d. Min at Slope=%6.2f smMin=%.3f vmax=%5.2f sm0=%.3f v0=%.2f",
                        contextString,t,100f*slopeMin,smMinOpt,3.6f/smMinOpt,sm0,3.6f/sm0);
            } catch (Exception e) {
                return String.format(Locale.ENGLISH, "%s for %s",e.getMessage(),contextString);
            }
        });
        return spline;
    }

    private void checkNegCurvature(IfSpline spline, String context, float threshold) {

        ArrayList<Float> pointsWithNegativeCurvature =SplineUtil.getPointsWithNegativeCurvature(spline);
        if (!pointsWithNegativeCurvature.isEmpty()) {
            StringBuilder msg = new StringBuilder(String.format(Locale.ENGLISH,"Negative curvature for %s",context));
            boolean criticalthresholdReached = false;
            boolean thresholdReached = false;
            for (Float negCurvaturePoint : pointsWithNegativeCurvature) {
                float curvature = -SplineUtil.curveRadiusAt(negCurvaturePoint,spline);
                if (curvature < threshold)
                    criticalthresholdReached = true;
                if ( curvature < 50f ) {
                    thresholdReached = true;
                }
                if (curvature < threshold || curvature < 50f ){
                    msg.append(": ");
                    msg.append(String.format(Locale.ENGLISH, " slope=%.2f", 100 * negCurvaturePoint));
                    msg.append(String.format(Locale.ENGLISH, " curve Radius=%.2f", curvature));
                }
            }

            if(criticalthresholdReached && getContext().getCheckAll())
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

    IfSpline getDurationSpline(int surfaceCat) {
        IfSpline spline;
        spline = SurfaceCatDurationSpline[surfaceCat];
        if (spline == null) {
            spline = calcSpline(false,surfaceCat );
            SurfaceCatDurationSpline[surfaceCat] = spline;
        }
        return spline;
    }

    public String getSurfaceCatTxt(int surfaceCat){
         return context.getSurfaceCatTxt(surfaceCat);
    }


    /* How the heuristic is determined:
       Given two points with a distance d and vertical distance v and a continuously differentiable
       cost IfFunction of the elevation f(e) = f(v/d) (given here as a spline IfFunction) and the costs from source to target point is
       given by: c(d,v) = d*f(v/d). If the vertical distance is given and constant, one can vary the path to the
       target and thereby increase the distance. A criteria of this minimum cost is that the first
       derivative varying d is 0, c'(d) = f(v/d) - d * f'(d) v/d^2 = f(e) - f'(e)*e = 0 or
       f(e) = f'(e)*e. This is a tangent on f(e) crossing the origin.
       If f(e) has a single minimum and curvature is positive (f''(e)>=0), there will be two such
       tangents. So a heuristic is given by the two tangents above and below the touch points of those tangents
       and by f(e) between those touch points.
       Equations solved via Newton Iteration.
     */

    public IfSpline calcHeuristicSpline(IfSpline refspline ){
        // cut out a new cubic spline out of the existing one using the to touch points of the tangents
        StringBuilder msg = new StringBuilder(context.toString());
        IfSpline cutSpline = SplineUtil.calcCutSpline( refspline, msg);
        mgLog.i(()-> msg.toString());
        return cutSpline;
    }


    private record Value(float x, float y) {
    }
}


