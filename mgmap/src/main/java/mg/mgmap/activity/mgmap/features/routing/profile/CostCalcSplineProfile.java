package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.util.basic.MGLog;

public abstract class CostCalcSplineProfile implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final float lowstart = -0.15f;
    private static final float highstart = 0.1f;

    private final CubicSpline cubicProfileSpline;
    private final CubicSpline cubicHeuristicSpline;

    protected CostCalcSplineProfile(Object context) {
        cubicHeuristicSpline = calcHeuristicSpline(getHeuristicRefSpline(context));
        cubicProfileSpline = getProfileSpline(context);
    }

    protected abstract  CubicSpline getProfileSpline(Object context);
    protected abstract  CubicSpline getHeuristicRefSpline(Object context);

    protected abstract  float getMinDistFactSC0();

    protected CubicSpline getProfileSpline(){
        return cubicProfileSpline;
    }

    public double calcCosts(double dist, float vertDist, boolean primaryDirection) {
        if (dist <= 0.0000001) {
            return 0.0001;
        }
        return dist * cubicProfileSpline.calc(vertDist / (float) dist) + 0.0001;
    }


    public double heuristic(double dist, float vertDist) {
        if (dist <= 0.0000001) {
            return 0.0;
        }
        return dist * cubicHeuristicSpline.calc(vertDist / (float) dist) * 0.9999;
    }

    @Override
    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            double spm = cubicProfileSpline.calc(slope);
            double v = 3.6/spm;
            mgLog.d(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f",100f*slope,v,spm*dist,dist));
        }
        return (dist >= 0.00001) ? (long) (1000 * dist * cubicProfileSpline.calc(vertDist / (float) dist)) : 0;
    }

    protected CubicSpline getOptSpline(float[] slopes, float[] durations, float smMinTarget, int varyat) {
 //     function of Minimum duration value of a spline based on input duration varied at slope[varyat] (for MTB splines at slope -3.5% )
        function smMin = smvary -> {
            try {
                durations[varyat] = smvary;
                CubicSpline cubicSpline = new CubicSpline(slopes,durations);
                float minSlope = cubicSpline.calcMin(-0.06f);
                float smMinVal = cubicSpline.calc(minSlope);
                return smMinVal - smMinTarget;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
//      Newton iteration with numerical derivation is used to optimize the input duration[varyat] so that the Min duration matches the Target smMinTarget
        durations[varyat] = newtonNumeric(durations[varyat],0.00001f,smMin,0.0001f);
        try {
            return new CubicSpline(slopes, durations);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected CubicSpline getSpline(float[] slopes, float[] durations)  {
        try {
            return new CubicSpline(slopes, durations);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected ArrayList<CubicSpline.Value> checkSplineHeuristic(CubicSpline cubicSpline, int surfaceCat)  {
        float xs = lowstart - 0.1f;
        float minmfd = (surfaceCat == 0) ? getMinDistFactSC0() : 1f;
        ArrayList<CubicSpline.Value> violations = new ArrayList<>();
        do {
            xs = xs + 0.01f;
            // make sure that costs are always lager than Heuristic
            if (cubicHeuristicSpline !=null ) {
                float delta = minmfd * cubicSpline.calc(xs) + 0.0001f - cubicHeuristicSpline.calc(xs)*0.9999f;
                if (delta <0) violations.add(new CubicSpline.Value(xs,delta));
            }
        } while (xs < highstart + 0.2f);
        return violations;
    }


    protected float getFrictionBasedVelocity(double slope, double watt, double Cr, double ACw, double m ){
        double rho = 1.2;
        double mg = m*9.81;
        double ACwr = 0.5 * ACw * rho;
        double eta = 0.95;
        double p =  mg*(Cr+slope)/ACwr;
        double q =  -watt/ACwr*eta;
        double D = Math.pow(q,2)/4. + Math.pow(p,3)/27.;

        return (float) ((D>=0) ? Math.cbrt(- q*0.5 + Math.sqrt(D)) + Math.cbrt(- q*0.5 - Math.sqrt(D)) :
                Math.sqrt(-4.*p/3.) * Math.cos(1./3.*Math.acos(-q/2*Math.sqrt(-27./Math.pow(p,3.)))));
    }

    /* How the heuristic is determined:
       Given two points with a distance d and vertical distance v and a continuously differentiable
       cost function of the elevation f(e) = f(v/d) (given here as a cubicSpline function) and the costs from source to target point is
       given by: c(d,v) = d*f(v/d). If the vertical distance is given and constant, one can vary the path to the
       target and thereby increase the distance. A criteria of this minimum cost is that the first
       derivative varying d is 0, c'(d) = f(v/d) - d * f'(d) v/d^2 = f(e) - f'(e)*e = 0 or
       f(e) = f'(e)*e. This is a tangent on f(e) crossing the origin.
       If f(e) has a single minimum and curvature is positive (f''(e)>=0), there will be two such
       tangents. So a heuristic is given by the two tangents above and below the touch points of those tangents
       and by f(e) between those touch points.
       Equations solved via Newton Iteration.
     */

    private CubicSpline calcHeuristicSpline(CubicSpline refCubicSpline ){
        // cut out a new cubic spline out of the existing one using the to touch points of the tangents
        function tangent = x -> refCubicSpline.calc(x) - refCubicSpline.calcSlope(x) * x + 0.0001f;
        function tangDeriv = x -> -refCubicSpline.calcCurve(x) * x;
        float tDnSlope = newton(lowstart,0.0001f,10,tangent,tangDeriv);
        float tUpSlope = newton(highstart,0.0001f,10,tangent,tangDeriv);
        mgLog.i(()-> String.format(Locale.ENGLISH, "Heuristic: DnSlopeLim=%.2f UpSlopeLim=%.2f",tDnSlope*100,tUpSlope*100));

        return refCubicSpline.getCutCubicSpline(tDnSlope,tUpSlope);
    }


    private interface function{
       float apply(float x);
    }

    private float newtonNumeric(float start, float minval, function f, float deltax){
        function fs = x -> ( f.apply(x + deltax) - f.apply(x - deltax) ) / ( 2f*deltax);
        return newton( start, minval,4, f, fs);
    }

    private float newton( float start, float minval,int maxIter, function f, function fs){
        float a;
        float xsp;
        int i = 0;
        do {
            i = i+1;
            if ( i >= maxIter)
                throw new RuntimeException("Too many Newton iterations");
            xsp = start;
            a = f.apply(xsp) - minval;
            start = xsp - a / fs.apply(xsp);
        } while ( (a > minval || a < -minval) );
        return start;
    }

}
