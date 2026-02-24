package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;


import static mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef.SplineType.CLAMPED;
import static mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef.SplineType.COMPOSITE;
import static mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSplineDef.SplineType.NATURAL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.IfFunction;
import mg.mgmap.activity.mgmap.features.routing.profile.ProfileUtil;

public class SplineUtil {

    public static ArrayList<Float> getPointsWithNegativeCurvature(IfSpline spline){
        IfSplineDef def = ((IfReturnsDef)spline).getSplineDef();
        float[] x;
        int leftSegs = 0;
        int rightSegs = 0;
        if (def.type()== COMPOSITE){
            IfSplineDef.Composite compdef = (IfSplineDef.Composite) def;
            leftSegs = compdef.leftLinearSegments();
            rightSegs  = compdef.rightLinearSegments();
            CompositeSpline compositeSpline = (CompositeSpline) spline;
            x = compositeSpline.getX();
        } else if ( def.type()== NATURAL || def.type()==CLAMPED ) {
            x = ((CubicSpline) spline).getX();
        } else {
            throw new RuntimeException("Not supported Spline Type for this method");
        }
        ArrayList<Float> list = new ArrayList<>();
        for (int i=1; i< x.length-1;i++){
            if ( i < leftSegs || i >= x.length-rightSegs ){
                if (spline.derivativeAt(x[i-1])>spline.derivativeAt(x[i])) list.add(x[i]);
            } else {
                if (spline.secondDerivativeAt(x[i])<0) list.add(x[i]);
            }
        }
        return list;
    }

    public static float getMin(IfSpline spline,float start) throws Exception {
        IfSplineDef def = ((IfReturnsDef)spline).getSplineDef();
        if ( def.type()== NATURAL )
            return ((CubicSpline)spline).calcMin(start);
        else if ( def.type()==COMPOSITE){
            CompositeSpline compSpline = (CompositeSpline) spline;
            if (start > compSpline.getLeftCubicLimit())
                return compSpline.getCubicSpline().calcMin(compSpline.getLeftCubicLimit()+0.01f);

        }
        return Float.NaN;
    }


    public static float curveRadiusAt(float x, IfSpline spline){
        float sec = spline.secondDerivativeAt(x);
        float der = spline.derivativeAt(x);
        return (float) Math.pow((1d+der*der),1.5d)/sec;
    }

    public static IfSpline calcCutSpline(IfSpline refCubicSpline, StringBuilder msg){
        IfSplineDef def = ((IfReturnsDef)refCubicSpline).getSplineDef();
        float[] x;
        Touple dnSlopeLimit;
        Touple upSlopeLimit ;
        IfSpline cutSpline;
        if ( def.type()== NATURAL  ) {
            CubicSpline cubicSpline = (CubicSpline) refCubicSpline;
            x = cubicSpline.getX();
            dnSlopeLimit = tangentAt(cubicSpline,-0.15f);
            upSlopeLimit = tangentAt(cubicSpline, 0.15f);
            XYArrays xy = buildNewXYVector(cubicSpline,x,dnSlopeLimit,upSlopeLimit);
            cutSpline = new CubicSpline(xy.x,xy.y,new IfSplineDef.Clamped(dnSlopeLimit.b, upSlopeLimit.b));
        }
        else if ( def.type()==COMPOSITE){
            CompositeSpline compositeSpline = (CompositeSpline) refCubicSpline;
            x= compositeSpline.getX();
            dnSlopeLimit = getCompositeSlopeLimit(compositeSpline,Direction.LEFT);
            upSlopeLimit = getCompositeSlopeLimit(compositeSpline,Direction.RIGHT);
            XYArrays xy = buildNewXYVector(compositeSpline,x,dnSlopeLimit,upSlopeLimit);
            int leftSegs =0;
            int rightSegs = 0;
            for (int i =0; i<xy.x.length;i++){
                if (xy.x[i]< compositeSpline.getLeftCubicLimit())
                    leftSegs++;
                if (xy.x[i]> compositeSpline.getRightCubicLimit())
                    rightSegs++;
            }
            if ( leftSegs > 0 || rightSegs > 0) {
                float[] xn = new float[xy.x.length+2];
                float[] yn = new float[xy.x.length+2];
                System.arraycopy(xy.x,0,xn,1,xy.x.length);
                System.arraycopy(xy.y,0,yn,1,xy.y.length);
                xn[0] = xy.x[0] - 0.2f;
                yn[0] = xn[0]*dnSlopeLimit.b;
                xn[xn.length-1] = xy.x[xy.x.length-1] + 0.2f;
                yn[xn.length-1] = xn[xn.length-1]*upSlopeLimit.b;
                cutSpline = new CompositeSpline(xn, yn, new IfSplineDef.Composite(leftSegs + 1, rightSegs + 1));
            }
            else
                cutSpline = new CubicSpline(xy.x,xy.y,new IfSplineDef.Clamped(dnSlopeLimit.b,upSlopeLimit.b));
        } else
            throw new RuntimeException("not yet implemented");

        msg.append( String.format(Locale.ENGLISH, " Heuristic DnSlopeLim=%.2f UpSlopeLim=%.2f",dnSlopeLimit.a*100,upSlopeLimit.a*100));
        return cutSpline; //refCubicSpline.getCutCubicSpline(heuristicDnSlopeLimit,heuristicUpSlopeLimit);
    }

    private record XYArrays(float[]x,float[]y){}
    private static XYArrays buildNewXYVector(IfSpline cubicSpline, float[] x,  Touple dnSlopeLimit, Touple upSlopeLimit){
        int i=0;
        for ( float slope :x) if (slope > dnSlopeLimit.a && slope < upSlopeLimit.a ) i++;
        float[] newSlopes = new float[i+2];
        float[] newDurations = new float[i+2];
        newSlopes[0] = dnSlopeLimit.a;
        newDurations[0] = cubicSpline.valueAt(newSlopes[0]);
        newSlopes[newSlopes.length-1]=upSlopeLimit.a;
        newDurations[newSlopes.length-1]=cubicSpline.valueAt(newSlopes[newSlopes.length-1]);
        i=1;
        for ( int j=0; j < x.length; j++) {
            if (x[j] > dnSlopeLimit.a && x[j]< upSlopeLimit.a ) {
                newSlopes[i]=x[j];
                newDurations[i]=cubicSpline.valueAt(x[j]);
                i++;
            }
        }
        return new XYArrays(newSlopes,newDurations);
    }

    private record Touple(float a,float b){}
    private enum Direction{
        LEFT(+1,0),RIGHT(-1,1);
        final int step;
        final int back;
        Direction(int step, int back) {
            this.step = step;
            this.back = back;
        }
    }
    private static Touple getCompositeSlopeLimit(CompositeSpline compositeSpline, Direction dir){

        float[] x = compositeSpline.getX();
        int i = switch (dir) {
            case LEFT -> 0;
            case RIGHT -> x.length-1;
        };
        float leftCubeLimit = compositeSpline.getLeftCubicLimit();
        float rightCubeLimit = compositeSpline.getRightCubicLimit();
        while ( compositeSpline.valueAt(x[i]) - compositeSpline.derivativeAt(x[i])*x[i]<0) i+=dir.step;
        i +=dir.back;
        if ( x[i] <= leftCubeLimit || x[i] >= rightCubeLimit) {
            return new Touple(x[i],compositeSpline.valueAt(x[i])/x[i]);
        } else {
            return tangentAt(compositeSpline.getCubicSpline(),x[i-dir.step]);
        }
    }
    private static Touple tangentAt(CubicSpline cubicSpline, float start){
        IfFunction tangent = x -> cubicSpline.valueAt(x) - cubicSpline.derivativeAt(x) * x - 0.0001f;
        IfFunction tangDeriv = x -> -cubicSpline.secondDerivativeAt(x) * x;
        float heuristicLimit = ProfileUtil.newton(start,0.00005f,10,tangent,tangDeriv);
        float upSlopeLimitValue = cubicSpline.valueAt(heuristicLimit);
        float upSlopeLimitSlope = cubicSpline.derivativeAt(heuristicLimit);
        float yintercept_right = upSlopeLimitValue - heuristicLimit*upSlopeLimitSlope;
        if (yintercept_right <= 0.00003)
            throw new RuntimeException( String.format(Locale.ENGLISH,"Heuristic right tangent too small intercept with %.2f", yintercept_right ));
        return new Touple(heuristicLimit,upSlopeLimitSlope);
    }

    public static String toDetailedString(IfSpline spline) {
        IfSplineDef def = ((IfReturnsDef)spline).getSplineDef();
        float[] x = switch (def.type()){
            case NATURAL,CLAMPED -> ((CubicSpline)spline).getX();
            case COMPOSITE -> ((CompositeSpline)spline).getX();
            case LINEAR -> ((LinearSpline)spline).getX();
        };

        StringBuilder xbl = new StringBuilder("\nx ");
        StringBuilder ybl = new StringBuilder("\ny ");
        for (int i = 0; i < x.length; i++) {
            xbl.append(String.format(Locale.ENGLISH, " %+11.6f", x[i] * 100));
            ybl.append(String.format(Locale.ENGLISH, " %+11.6f", spline.valueAt(x[i])));
        }
        return def.toString() + xbl + ybl;
    }

    public static String valuesToString(IfSpline spline) {
        return valuesToString(spline, -0.4f,0.3f,0.01f);
    }
    public static String valuesToString(IfIsFuncWithDeriv spline, float from, float to, float step) {
        StringBuilder xbl = new StringBuilder("x     ");
        StringBuilder ybl = new StringBuilder("\nvalue ");
        StringBuilder dbl = new StringBuilder("\nderiv ");
        StringBuilder sbl = new StringBuilder("\nsecdr ");
        for (float x = from; x <= to ; x+=step) {
            xbl.append(String.format(Locale.ENGLISH, " %+11.6f", x * 100));
            ybl.append(String.format(Locale.ENGLISH, " %+11.6f", spline.valueAt(x)));
            dbl.append(String.format(Locale.ENGLISH, " %+11.6f", spline.derivativeAt(x)));
            sbl.append(String.format(Locale.ENGLISH, " %+11.6f", spline.secondDerivativeAt(x)));
        }
        return xbl.append(ybl).append(dbl).append(sbl).toString();
    }

}
