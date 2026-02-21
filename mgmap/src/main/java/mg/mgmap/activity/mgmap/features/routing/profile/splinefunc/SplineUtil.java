package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;


import static mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef.SplineType.CLAMPED;
import static mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef.SplineType.COMPOSITE;
import static mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef.SplineType.LINEAR;
import static mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef.SplineType.NATURAL;

import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.IfFunction;
import mg.mgmap.activity.mgmap.features.routing.profile.ProfileUtil;

public class SplineUtil {

    public static ArrayList<Float> getPointsWithNegativeCurvature(IfSpline spline){
        IfSplineDef def = spline.getSplineDef();
        float[] x;
        int leftSegs = 0;
        int rigtSegs = 0;
        if (def.type()== COMPOSITE){
            IfSplineDef.Composite compdef = (IfSplineDef.Composite) def;
            leftSegs = compdef.leftLinearSegments();
            rigtSegs  = compdef.rightLinearSegments();
            CompositeSpline compositeSpline = (CompositeSpline) spline;
            x = compositeSpline.getX();
        } else if ( def.type()== NATURAL ) {
            x = ((CubicSpline) spline).getX();
        } else {
            throw new RuntimeException("Not supported Spline Type for this method");
        }
        ArrayList<Float> list = new ArrayList<>();
        for (int i=0; i< x.length;i++){
            if ( i < leftSegs){
                if (spline.derivativeAt(x[i])<spline.derivativeAt(x[i+1])) list.add(x[i]);
            } else if ( i< x.length-rigtSegs) {
                if (spline.secondDerivativeAt(x[i])<0) list.add(x[i]);
            } else
                if (spline.derivativeAt(x[i-1])<spline.derivativeAt(x[i])) list.add(x[i]);
        }
        return list;
    }

    public static float getMin(IfSpline spline,float start) throws Exception {
        IfSplineDef def = spline.getSplineDef();
        if ( def.type()== NATURAL )
            return ((CubicSpline)spline).calcMin(start);
        else
          return Float.NaN;
    }


    public static float curveRadiusAt(float x, IfSpline spline){
        float sec = spline.secondDerivativeAt(x);
        float der = spline.derivativeAt(x);
        return (float) Math.pow((1d+der*der),1.5d)/sec;
    }

    public static IfSpline calcCutSpline(IfSpline refCubicSpline, StringBuilder msg){
        IfSplineDef def = refCubicSpline.getSplineDef();
        float[] slopes;
        if ( def.type()== NATURAL )
            slopes = ((CubicSpline)refCubicSpline).getX();
        else
            throw new RuntimeException("not yet implemented");

        IfFunction tangent = x -> refCubicSpline.valueAt(x) - refCubicSpline.derivativeAt(x) * x - 0.0001f;
        IfFunction tangDeriv = x -> -refCubicSpline.secondDerivativeAt(x) * x;
        float heuristicDnSlopeLimit = ProfileUtil.newton(-0.15f,0.00005f,10,tangent,tangDeriv);
        float dnSlopeLimitValue = refCubicSpline.valueAt(heuristicDnSlopeLimit);
        float dnSlopeLimitSlope = refCubicSpline.derivativeAt(heuristicDnSlopeLimit);
        float yintercept_left = dnSlopeLimitValue - heuristicDnSlopeLimit*dnSlopeLimitSlope;
        if (yintercept_left <= 0.00003)
            throw new RuntimeException( String.format(Locale.ENGLISH,"Heuristic left tangent too small intercept with %.2f", yintercept_left ));

        float heuristicUpSlopeLimit = ProfileUtil.newton(0.15f,0.00005f,10,tangent,tangDeriv);
        float upSlopeLimitValue = refCubicSpline.valueAt(heuristicUpSlopeLimit);
        float upSlopeLimitSlope = refCubicSpline.derivativeAt(heuristicUpSlopeLimit);
        float yintercept_right = upSlopeLimitValue - heuristicUpSlopeLimit*upSlopeLimitSlope;
        if (yintercept_right <= 0.00003)
            throw new RuntimeException( String.format(Locale.ENGLISH,"Heuristic right tangent too small intercept with %.2f", yintercept_right ));


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
                newDurations[i]=refCubicSpline.valueAt(slopes[j]);
                i++;
            }
        }
        CubicSpline cubicCutSpline = new CubicSpline(newSlopes,newDurations,new IfSplineDef.Clamped(dnSlopeLimitSlope,upSlopeLimitSlope));

        msg.append( String.format(Locale.ENGLISH, "Heuristic DnSlopeLim=%.2f UpSlopeLim=%.2f",heuristicDnSlopeLimit*100,heuristicUpSlopeLimit*100));
        return cubicCutSpline; //refCubicSpline.getCutCubicSpline(heuristicDnSlopeLimit,heuristicUpSlopeLimit);
    }


    public static String toDetailedString(IfSpline spline) {
        IfSplineDef def = spline.getSplineDef();
        float[] x = switch (def.type()){
            case NATURAL,CLAMPED -> ((CubicSpline)spline).getX();
            case COMPOSITE -> ((CompositeSpline)spline).getX();
            case LINEAR -> ((LinearSpline)spline).getX();
        };
        StringBuilder xbl = new StringBuilder("x ");
        StringBuilder ybl = new StringBuilder("y ");
        for (int i = 0; i < x.length; i++) {
            xbl.append(String.format(Locale.ENGLISH, " %+11.6f", x[i] * 100));
            ybl.append(String.format(Locale.ENGLISH, " %+11.6f", spline.valueAt(x[i])));
        }
        return xbl.append("\n").append(ybl).toString();
    }

}
