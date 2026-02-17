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
    private static final float lowstart = -0.15f;
    private static final float highstart = 0.15f;

    public final float refCosts;

    public CostCalcSplineProfile(IfSplineProfileContext context) {
        this.context = context;
        SurfaceCatCostSpline = new CubicSpline[getMaxSurfaceCat()];
        SurfaceCatDurationSpline = new CubicSpline[context.getMaxSurfaceCat()];
        if (context.fullCalc()) {
            cubicHeuristicSpline = getCubicHeuristicSpline();
            cubicProfileSpline = getProfileSpline();
            checkAll();
            refCosts = cubicProfileSpline.calc(0f);
        } else {
            cubicHeuristicSpline = null;
            cubicProfileSpline = null;
            refCosts = 0f;
        }

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
        return dist * cubicProfileSpline.calc(vertDist / (float) dist) ;
    }

    public double heuristic(double dist, float vertDist) {
        if (dist <= 0.0000001) {
            return 0.0;
        }
        return dist * cubicHeuristicSpline.calc(vertDist / (float) dist) ;
    }


    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            double spm = cubicProfileSpline.calc(slope);
            double v = 3.6/spm;
            mgLog.v(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f costf=%.2f",100f*slope,v,spm*dist,dist,cubicProfileSpline.calc(vertDist / (float) dist)));
        }
        return (dist >= 0.00001) ? (long) (1000 * dist * cubicProfileSpline.calc(vertDist / (float) dist)) : 0;
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

        float yintercept_right = cubicHeuristicSpline.yintercept_linsec(CubicSpline.linsec.right);
        if (yintercept_right <= 0.00003)
            throw new RuntimeException( String.format(Locale.ENGLISH,"Heuristic right tangent too small intercept with %.2f", yintercept_right ));
        float yintercept_left = cubicHeuristicSpline.yintercept_linsec(CubicSpline.linsec.left);
        if (yintercept_left <= 0.00003)
            throw new RuntimeException( String.format(Locale.ENGLISH,"Heuristic right tangent too small intercept with %.2f", yintercept_left ));


        for (int surfaceCat = 0; surfaceCat < context.getMaxSurfaceCat(); surfaceCat++){
            try {
                getDurationSpline(surfaceCat);
            } catch (Exception e) {
                mgLog.e(e.getMessage());
            }
        }
    }

    public IfFunction getCostFunc(int surfaceCat) {
        try {
            return getCostSpline(surfaceCat);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CubicSpline getCostSpline(int surfaceCat) throws Exception{
        CubicSpline cubicSpline = SurfaceCatCostSpline[surfaceCat];
        if (cubicSpline == null) {
            cubicSpline = calcSpline(true,surfaceCat );
            SurfaceCatCostSpline[surfaceCat] = cubicSpline;
        }
        return cubicSpline;
    }

    protected CubicSpline getSlopeOptSpline(float[] slopes, float[] durations, int targetat, float slopeTarget, int varyat) {
        //     IfFunction of Minimum duration value of a spline based on input duration varied at slope[varyat] (for MTB splines at slope -3.5% )
        IfFunction slope = smvary -> {
            try {
                durations[varyat] = smvary;
                CubicSpline cubicSpline = new CubicSpline(slopes,durations);
                return cubicSpline.getSlope(targetat) - slopeTarget;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
//      Newton iteration with numerical derivation is used to optimize the input duration[varyat] so that the Min duration matches the Target smMinTarget
        durations[varyat] = ProfileUtil.newtonNumeric(durations[varyat],0.00001f,slope,0.0001f);
        try {
            return new CubicSpline(slopes, durations);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() + " in getSlopeOptSpline");
        }
    }



    private CubicSpline getSpline(float[] slopes, float[] durations)  {
        try {
            return new CubicSpline(slopes, durations);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }



    protected ArrayList<CubicSpline.Value> checkSplineHeuristic(CubicSpline cubicSpline, int surfaceCat)  {
        float xs = lowstart - 0.3f;
        float minmfd = (surfaceCat == 0) ? context.getMinDistFactSC0() : 1f;
        ArrayList<CubicSpline.Value> violations = new ArrayList<>();
        do {
            xs = xs + 0.001f;
            // make sure that costs are always larger than Heuristic
            if (cubicHeuristicSpline !=null ) {
                float heuristic = cubicHeuristicSpline.calc(xs);
                float spline    = cubicSpline.calc(xs);
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
            CubicSpline cubicSpline = cubicSplineTmp.getTransYCubicSpline(-0.0001f);
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


    private CubicSpline calcSpline(boolean costSpline, int surfaceCat) throws Exception {
        if (!context.isValidSc(surfaceCat)) return null;

        int indRefDnSlope = context.getIndRefDnSlope();
        int indRefDnSlopeOpt = indRefDnSlope+1;

        float crUp = context.getCrUp(surfaceCat);
        float crDn = context.getCrDn(surfaceCat);
        float f0up = context.getF0u(surfaceCat);
        float f1Up = context.getF1u(surfaceCat);
        float f2Up = context.getF2u(surfaceCat);
        float f3Up = context.getF3u(surfaceCat);
        float cr0 = (crDn+crUp)/2f;
        float cr1 =  (0.1f*crDn + 0.9f*crUp);
        float sm20Dn = context.getSm20Dn(surfaceCat);
        float factorDn = context.getFactorDn(surfaceCat);
        float f2d      = context.getF2d(surfaceCat);
        float f3d      = context.getF3d(surfaceCat);
        float[] distFactCostFunct = context.getDistFactforCostFunct(surfaceCat);
        float[] slopes = costSpline ? context.getCostSlopes(surfaceCat): context.getDurationSlopes(surfaceCat);
        float refDnSlope = slopes[indRefDnSlope];
        float watt0 = context.getWatt0(surfaceCat);
        float watt  = context.getWatt(surfaceCat);

        long t1 = System.nanoTime();


        float[] durations = new float[slopes.length];

        //      for slopes <=20% pure heuristic formulas apply that derivative of the duration function is equal to factorDn. For smaller slopes additional factors apply (f2d,f3d) to enforce positive
        //      curvature of the duration function
        durations[0] = ( sm20Dn -(slopes[0]-refDnSlope)*factorDn) *f3d; //f3d
        durations[1] = ( sm20Dn -(slopes[1]-refDnSlope)*factorDn) *f2d;//f2d;
        durations[2] =   sm20Dn -(slopes[2]-refDnSlope)*factorDn;
        durations[3] =   sm20Dn ;
        //      for everything with slope >=0% durations (sec/m) is calculated based on the speed derived from friction and input power (Watt)
        durations[slopes.length-5] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-5], watt0, cr0) ;
        durations[slopes.length-4] = f0up /  getFrictionBasedVelocity(slopes[slopes.length-4], watt, cr1) ;
        durations[slopes.length-3] = f1Up /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, crUp)  ;
        durations[slopes.length-2] = f2Up /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, crUp)  ;
        durations[slopes.length-1] = f3Up /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, crUp)  ;
        //      duration at -4% only used for the reference profiles.
        if (!context.getWithRef()) {
            durations[indRefDnSlopeOpt] = durations[slopes.length-5]+context.getRelSlope(surfaceCat)*slopes[indRefDnSlopeOpt];
        }

        if (costSpline&&distFactCostFunct.length>0){
            for (int i = 0; i < distFactCostFunct.length; i++) {
                durations[i] = durations[i] * distFactCostFunct[i];
            }

        }

        String SplineType = costSpline ? "cost":"dura";
        String contextString = String.format(Locale.ENGLISH,"spline=%s %s ",SplineType,getSurfaceCatTxt(surfaceCat));

        float slopeTarget = 0f;
        CubicSpline cubicSplineTmp;
        if (context.getWithRef()) {
            /* to achieve an almost constant downhill profile for a given mtbDn scale and downhill level of the profile (sDn) independent of the mtbUp scale and the uphill level of the profile (sUp)
            a reference profile for a given combination of sDn and mtbDn with a constant uphill profile (power = 100 Watt, sUp = 2 ) and mtbUp = mtbDn is calculated. All other uphill combinations are
            calculated in such a way that the slope at -20% is taken from the reference profile und the duration is varied at -4% slope, so that the slope matches the target slope
             */
//            int mtbDn = getMtbDn(surfaceCat);

            if (costSpline)
                cubicSplineTmp = context.getRefProfile().getCostSpline(context.getRefSc(surfaceCat));//getSurfaceCat(getSurfaceLevel(surfaceCat),mtbDn,mtbDn));
            else
                cubicSplineTmp = context.getRefProfile().getDurationSpline(context.getRefSc(surfaceCat));//getSurfaceCat(getSurfaceLevel(surfaceCat),mtbDn,mtbDn));
            for ( int i = 0; slopes[i]<0;i++) {
                durations[i] = cubicSplineTmp.calc(slopes[i]) ;//* factor;
            }
            slopeTarget = cubicSplineTmp.getSlope(indRefDnSlope); //*factor;
            cubicSplineTmp = getSlopeOptSpline(slopes, durations, indRefDnSlope, slopeTarget, indRefDnSlopeOpt);
        }
        else {
            cubicSplineTmp = getSpline(slopes, durations);
        }
        if (context.getWithRef() ) {
            float slope2slopeTarget = cubicSplineTmp.getSlope(indRefDnSlope) / slopeTarget;
            if (Math.abs(slope2slopeTarget - 1f) > 0.01f) {
                String msg = String.format(Locale.ENGLISH, "for %s Slope to Slopetarget=%.3f at %.2f", contextString, slope2slopeTarget, slopes[indRefDnSlope] * 100f);
                if (slope2slopeTarget > 0.5f && slope2slopeTarget < 2f)
                    mgLog.w(msg);
                else
                    throw new Exception("Out of range " + msg);
            }
        }

        checkNegCurvature(cubicSplineTmp,contextString,minNegCurvatureRadius);

        CubicSpline cubicSpline = cubicSplineTmp;
        long t = ( System.nanoTime() - t1 ) /1000;
        mgLog.v( ()-> {
            try {
                float slopeMin = cubicSpline.calcMin(-0.13f);
                float smMinOpt = cubicSpline.calc(slopeMin);
                float smVary = durations[indRefDnSlopeOpt];
                float sm0 = cubicSpline.calc(0f);
                return String.format(Locale.ENGLISH, "For %s t[µs]=%4d. Min at Slope=%6.2f, smMin=%.3f, vmax=%5.2f, smVary=%.3f, sm0=%.3f, v0=%.2f",
                        contextString,t,100f*slopeMin,smMinOpt,3.6f/smMinOpt,smVary,sm0,3.6f/sm0);
            } catch (Exception e) {
                return String.format(Locale.ENGLISH, "%s for %s",e.getMessage(),contextString);
            }
        });
        return cubicSpline;
    }

    private void checkNegCurvature(CubicSpline cubicSpline, String context, float threshold) throws Exception{
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
                throw new Exception( msg.toString());
            else
                if (thresholdReached) mgLog.w(msg.toString());
        }
    }


    public IfFunction getDurationFunc(int surfaceCat) {
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

    private CubicSpline getDurationSpline(int surfaceCat) throws Exception{
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
        IfFunction tangent = x -> refCubicSpline.calc(x) - refCubicSpline.calcSlope(x) * x - 0.0001f;
        IfFunction tangDeriv = x -> -refCubicSpline.calcCurve(x) * x;
        float tDnSlope = ProfileUtil.newton(lowstart,0.00005f,10,tangent,tangDeriv);
        float tUpSlope = ProfileUtil.newton(highstart,0.00005f,10,tangent,tangDeriv);
        mgLog.i(()-> String.format(Locale.ENGLISH, "Heuristic for %s: DnSlopeLim=%.2f UpSlopeLim=%.2f",context.toString(),tDnSlope*100,tUpSlope*100));
        return refCubicSpline.getCutCubicSpline(tDnSlope,tUpSlope);
    }

    private float getFrictionBasedVelocity(double slope, double watt, double Cr ){
        double rho = 1.2;
        double m = 90d;
        double ACw = 0.45;
        double mg = m *9.81;
        double ACwr = 0.5 *  ACw * rho;
        double eta = 0.95;
        double p =  mg*(Cr+slope)/ACwr;
        double q =  -watt/ACwr*eta;
        double D = Math.pow(q,2)/4. + Math.pow(p,3)/27.;

        return (float) ((D>=0) ? Math.cbrt(- q*0.5 + Math.sqrt(D)) + Math.cbrt(- q*0.5 - Math.sqrt(D)) :
                Math.sqrt(-4.*p/3.) * Math.cos(1./3.*Math.acos(-q/2*Math.sqrt(-27./Math.pow(p,3.)))));
    }


}


