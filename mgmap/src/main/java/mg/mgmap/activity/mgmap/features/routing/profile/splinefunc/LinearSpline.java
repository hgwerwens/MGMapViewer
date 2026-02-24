package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

public class LinearSpline implements IfSpline,IfReturnsDef {
    private final float[] x;
    float xr;
    float yr;
    private final float[] y;
    private final float[] sl;
    private final IfSplineDef def;

    public LinearSpline(float[] x, float[] y, IfSplineDef def){
        if (x.length != y.length || x.length < 2)
            throw new IllegalArgumentException("Need at least two points");
        if (def.type()!= IfSplineDef.SplineType.LINEAR)
            throw new IllegalArgumentException("Incorrect Spline Type");
        this.def = def;
        this.x = new float[x.length-1];
        this.y = new float[x.length-1];
        xr = x[x.length-1];
        yr = y[x.length-1];
        for (int i = 0;i<x.length-1;i++) {
            this.x[i]=x[i];
            this.y[i]=y[i];
        }
        this.sl = new float[x.length-1];
        for (int i = 0; i < sl.length; i++) {
            float dx = x[i+1] - x[i];
            if (dx <= 0f)
                throw new IllegalArgumentException("x must be strictly increasing");
            sl[i] = (y[i+1] - y[i]) / dx;
        }
    }

    private int geti(float x){
        int i = 0;
        while (i < this.x.length-1 && x >= this.x[i+1] ) i++;
        return i;
    }

    @Override
    public float valueAt(float x) {
        int in = geti(x);
        float x1 = x - this.x[in];
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
        if (in==0) return 0f;
        float x1 = x - this.x[in];
        return x1==0f? Float.NaN:0f;
    }

    public float[] getX() {
        return x;
    }

    @Override
    public IfSplineDef getSplineDef() {
        return def;
    }



    @Override
    public LinearSpline transformY(float scale, float translate){
        LinearSpline spline = new LinearSpline(this);
        for (int i = 0; i<spline.y.length;i++){
            spline.y[i] = spline.y[i]*scale + translate;
            spline.sl[i] = spline.sl[i]*scale;
        }
        return spline;
    }

    private LinearSpline(LinearSpline spline){
        x = spline.x.clone();
        y = spline.y.clone();
        sl = spline.sl.clone();
        def = spline.def;
    }
}
