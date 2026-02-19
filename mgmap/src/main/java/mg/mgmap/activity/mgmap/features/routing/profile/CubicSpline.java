package mg.mgmap.activity.mgmap.features.routing.profile;

import java.util.ArrayList;

/**
 * computes natural cubic spline. Algorithm: <a href="https://en.wikipedia.org/wiki/Spline_(mathematics)">...</a>
 */
public class CubicSpline implements IfSpline {

    private final float[][] polynominals;
    private final float[] x;

    public record Value(float x, float y) {
    }


    public CubicSpline (float[] x, float[] y)  {
        this(new IfSplineDef.Impl(x,y, IfSplineDef.naturalLeft(), IfSplineDef.naturalRight()));
    }

    public CubicSpline(IfSplineDef def){
        x = def.x();
        float [] y = def.y();
        if (x.length < 3) {
            throw new IllegalArgumentException("input array too short");
        } else if (x.length != y.length) {
            throw new IllegalArgumentException("x and y vector size don't match");
        }
        float min = -Float.MAX_VALUE;
        for (float v : x) {
            if (v <= min) throw new IllegalArgumentException("x not sorted in ascending order");
            min = v;
        }
        int n = x.length - 1;
        float[] h = new float[n];
        float[] b = new float[n];
        float[] mu = new float[n+1];
        float[] z  = new float[n+1];
        float[] c  = new float[n+1];
        float[] d  = new float[n];

        for (int i = 0; i < n; i++) {
            h[i] = x[i+1] - x[i];
            b[i] = (y[i+1] - y[i]) / h[i];
        }
        def.leftBoundary().apply(h,y,mu,z);

        for (int i = 1; i < n; i++) {
            float l = 2f * (x[i+1] - x[i-1]) - h[i-1] * mu[i-1];
            mu[i] = h[i] / l;
            z[i]  = (3f * (b[i] - b[i-1]) - h[i-1] * z[i-1]) / l;
        }
        def.rightBoundary().apply(h,y,mu,z);

        c[n] = z[n];
        for (int j = n - 1; j >= 0; j--) {
            c[j] = z[j] - mu[j] * c[j+1];
            d[j] = (c[j+1] - c[j]) / (3f * h[j]);
            b[j] = b[j] - h[j] * (2f*c[j] + c[j+1]) / 3f;
        }

        polynominals = new float[n+2][];
        polynominals[0] = new float[] {y[0],b[0]}; // initial linear section
        for ( int i = 0; i< n;i++){
            polynominals[i+1] = new float[] {y[i],b[i],c[i],d[i]};
        }
        float x1 = this.x[n] - this.x[n-1];
        float x2 = x1*x1;
        polynominals[n+1] = new float[] {y[n],polynominals[n][1] + 2* polynominals[n][2]*x1 + 3*polynominals[n][3]*x2};

    }



    public float[] getX(){
        return x;
    }

    public float[] getY(){
        float[] y = new float[x.length];
        for ( int i = 0; i < x.length; i++){
            y[i]=polynominals[i+1][0];
        }
        return y;
    }

    @Override
    public float valueAt(float x) {
        int in = geti(x);
        float x1 = x - this.x[(in == 0)?0:in-1];
        if (in==0 || in==this.x.length)
            return polynominals[in][0] + polynominals[in][1]*x1;
        else {
            float x2 = x1*x1;
            float x3 = x2*x1;
            return polynominals[in][0] + polynominals[in][1] * x1 + polynominals[in][2] * x2 + polynominals[in][3] * x3;
        }
    }


    @Override
    public float derivativeAt(float x){
        int in = geti(x);
        if (in==0 || in==this.x.length)
            return polynominals[in][1];
        else {
            float x1 = x - this.x[in-1];
            float x2 = x1*x1;
            return polynominals[in][1] + 2f*polynominals[in][2]*x1 + 3f*polynominals[in][3]*x2;
        }
    }

    public float secondDerivativeAt(float x){
        int in = geti(x);
        if (in==0 || in==this.x.length)
            return 0f;
        else {
            float x1 = x - this.x[in - 1];
            return 2f * polynominals[in][2] + 6f * polynominals[in][3] * x1;
        }
    }

    public ArrayList<Value> getCurveRadiusForNegCurvaturePoints(){
        ArrayList<Value> result = new ArrayList<>(0);
        for (int i=1; i<x.length;i++){
           if (polynominals[i][2]<0)
               result.add(new Value(x[i - 1], (float) Math.pow((1f+polynominals[i][1]*polynominals[i][1]),1.5d) / (2f*polynominals[i][2])));
        }
        return result.isEmpty()? null : result;
    }

    public float calcMin(float start) throws Exception{
        int in = geti(start);
        return calcMin(in, 0);
    }

    private float calcMin(int in, int depth) throws Exception{
        // slope needs to be 0 -> quadratic equation
        if ( in == 0 || in == this.x.length)
            throw new Exception("Start in first or last linear segment without minimum");
        float p2 = polynominals[in][2]/( 3f*polynominals[in][3]);
        float q  = polynominals[in][1]/(3f*polynominals[in][3]);
        float xmin = polynominals[in][3] <0 ? -p2-(float)Math.sqrt(p2*p2-q)+this.x[in-1] : -p2+(float)Math.sqrt(p2*p2-q)+this.x[in-1]  ;
        if ( xmin < this.x[in-1] || xmin > this.x[in] || Float.isNaN(xmin))
            if (depth < 3)
               return calcMin(in+1, ++depth);
            else
              throw new Exception("No Minimum found in recursion depth" + depth);
        return xmin;
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




    public CubicSpline scaleY(float sy){
        CubicSpline cubicSpline = new CubicSpline(this);
        cubicSpline.polynominals[0][0] = cubicSpline.polynominals[0][0] * sy;
        cubicSpline.polynominals[0][1] = cubicSpline.polynominals[0][1] * sy;
        cubicSpline.polynominals[cubicSpline.x.length][0] = cubicSpline.polynominals[cubicSpline.x.length][0] * sy ;
        cubicSpline.polynominals[cubicSpline.x.length][1] = cubicSpline.polynominals[cubicSpline.x.length][1] * sy;
        for ( int i = 1; i < cubicSpline.x.length; i++ )   {
            cubicSpline.polynominals[i][0] = cubicSpline.polynominals[i][0] * sy;
            cubicSpline.polynominals[i][1] = cubicSpline.polynominals[i][1] * sy;
            cubicSpline.polynominals[i][2] = cubicSpline.polynominals[i][2] * sy;
            cubicSpline.polynominals[i][3] = cubicSpline.polynominals[i][3] * sy;
        }
        return cubicSpline;
    }


    public CubicSpline translateY(float transY){
        CubicSpline cubicSpline = new CubicSpline(this);
        cubicSpline.polynominals[0][0] = cubicSpline.polynominals[0][0] + transY;
        cubicSpline.polynominals[0][1] = cubicSpline.polynominals[0][1];
        cubicSpline.polynominals[cubicSpline.x.length][0] = cubicSpline.polynominals[cubicSpline.x.length][0] + transY ;
        cubicSpline.polynominals[cubicSpline.x.length][1] = cubicSpline.polynominals[cubicSpline.x.length][1];
        for ( int i = 1; i < cubicSpline.x.length; i++ )   {
            cubicSpline.polynominals[i][0] = cubicSpline.polynominals[i][0] + transY;
            cubicSpline.polynominals[i][1] = cubicSpline.polynominals[i][1];
            cubicSpline.polynominals[i][2] = cubicSpline.polynominals[i][2];
            cubicSpline.polynominals[i][3] = cubicSpline.polynominals[i][3];
        }
        return cubicSpline;
    }


    private CubicSpline( CubicSpline cubicSpline){
        polynominals = new float[cubicSpline.x.length+1][4];
        x = cubicSpline.x.clone();
        for ( int i = 0; i <= cubicSpline.x.length; i++ )  {
            polynominals[i] = cubicSpline.polynominals[i].clone();
        }
    }

}
