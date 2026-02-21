package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

public class LinearSpline implements IfSpline {
    private final float[] x;
    private final float[] y;
    private final float[] sl;
    private final IfSplineDef def;

    public LinearSpline(float[] x, float[] y, IfSplineDef def){
        if (x.length != y.length || x.length < 2)
            throw new IllegalArgumentException("Need at least two points");
        if (def.type()!= IfSplineDef.SplineType.LINEAR)
            throw new IllegalArgumentException("Incorrect Spline Type");
        this.def = def;
        this.x = x;
        this.y = y;
        this.sl = new float[x.length - 1];
        for (int i = 0; i < sl.length; i++) {
            float dx = x[i+1] - x[i];
            if (dx <= 0f)
                throw new IllegalArgumentException("x must be strictly increasing");
            sl[i] = (y[i+1] - y[i]) / dx;
        }
    }

    private int geti(float x){
        int i;
        if ( x <= this.x[0])
            i = 0;
        else {
            i = 1;
            while (i < this.x.length && x >= this.x[i] ) i = i + 1;
        }
        return i;
    }

    @Override
    public float valueAt(float x) {
        int in = geti(x);
        float x1 = x - this.x[(in == 0)?0:in-1];
        return y[in] + x1*sl[in];
    }

    @Override
    public float derivativeAt(float x) {
        int in = geti(x);
        return sl[in];
    }

    @Override
    public float secondDerivativeAt(float x) {
        int in = geti(x);
        float x1 = x - this.x[(in == 0)?0:in-1];
        return x1==0f? Float.NaN:0f;
    }

    public float[] getX() {
        return x;
    }

    @Override
    public IfSplineDef getSplineDef() {
        return def;
    }
}
