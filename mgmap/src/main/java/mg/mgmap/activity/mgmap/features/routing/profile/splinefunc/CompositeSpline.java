package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

public class CompositeSpline implements IfSpline {
    private CubicSpline cubicSpline;
    private LinearSpline leftLinSpline;
    private LinearSpline rightLinSpline;
    private float leftCubicLimit;
    private float rightCubicLimit;
    private IfSplineDef def;
    private float[] x;
    private static IfSplineDef linSpline = new IfSplineDef.Linear();
    public CompositeSpline(float[]x,float[]y,IfSplineDef def){
        if (def.type()!= IfSplineDef.SplineType.COMPOSITE)  throw new IllegalArgumentException("Not a Composite Spline Definition");
        this.def = def;
        this.x   = x;
        IfSplineDef.Composite compDef = (IfSplineDef.Composite) def;
        int leftLinSegs = compDef.leftLinearSegments();
        int rightLinSegs = compDef.rightLinearSegments();
        if (x.length-leftLinSegs-rightLinSegs- 4 < 0 ) throw new IllegalArgumentException("Not enough Points for thisComposite Spline Definition");
        float xLeft[] = new float[leftLinSegs+1];
        float yLeft[] = new float[leftLinSegs+1];
        float xMid[] = new float[x.length-leftLinSegs-rightLinSegs];
        float yMid[] = new float[x.length-leftLinSegs-rightLinSegs];

        float xRight[] = new float[rightLinSegs+1];
        float yRight[] = new float[rightLinSegs+1];
        int j=0;
        int k=0;
        leftCubicLimit = x[leftLinSegs];
        rightCubicLimit = x[x.length-rightLinSegs-1];
        for (int i= 0;i<x.length;i++) {
            if (i <= leftLinSegs) {
                xLeft[i] = x[i];
                yLeft[i] = y[i];
            }
            if (i>=leftLinSegs && i <= x.length-rightLinSegs-1){
                xMid[j] = x[i];
                yMid[j] = y[i];
                j++;
            }
            if (i >= x.length-rightLinSegs-1){
                xRight[k] = x[i];
                yRight[k] = y[i];
                k++;
            }

        }
        leftLinSpline = new LinearSpline(xLeft,yLeft,linSpline);
        rightLinSpline = new LinearSpline(xRight,yRight,linSpline);
        float leftSlope = leftLinSpline.derivativeAt(xLeft[xLeft.length]);
        float rightSlope = rightLinSpline.derivativeAt(xRight[0]);
        cubicSpline      = new CubicSpline(xMid,yMid,new IfSplineDef.Clamped(leftSlope,rightSlope));

    }

    @Override
    public float valueAt(float x) {
        if (x <= leftCubicLimit) return leftLinSpline.valueAt(x);
        else if ( x<rightCubicLimit ) return cubicSpline.valueAt(x);
        else return leftLinSpline.valueAt(x);
    }

    @Override
    public float derivativeAt(float x) {
        if (x <= leftCubicLimit) return leftLinSpline.derivativeAt(x);
        else if ( x<rightCubicLimit ) return cubicSpline.derivativeAt(x);
        else return leftLinSpline.derivativeAt(x);
    }

    @Override
    public float secondDerivativeAt(float x) {
        if (x <= leftCubicLimit) return leftLinSpline.secondDerivativeAt(x);
        else if ( x<rightCubicLimit ) return cubicSpline.secondDerivativeAt(x);
        else return leftLinSpline.secondDerivativeAt(x);
    }


    float[] getX() {
        return x;
    }

    @Override
    public IfSplineDef getSplineDef() {
        return def;
    }
}
