package mg.mgmap.activity.mgmap.features.routing.profile;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * computes natural cubic spline. Algorithm: <a href="https://en.wikipedia.org/wiki/Spline_(mathematics)">...</a>
 */
public class CubicSpline implements IfFunction{

    public enum linsec {left,right}
    private final float[][] polynominals;
    private final float[] x;

    public record Value(float x, float y) {
    }


    public CubicSpline (float[] x, float[] y) throws Exception {
        this(new IfSplineDefinition() {
            @Override
            public float[] x() { return x; }
            @Override
            public float[] y() { return y; }
            // Natural ist Default, also NICHT überschreiben
            // leftBoundary() und rightBoundary() kommen aus default-Methoden
        });

    }

    public CubicSpline(IfSplineDefinition def) throws Exception {
        x = def.x();
        float [] y = def.y();
        if (x.length < 3) {
            throw new Exception("input array too short");
        } else if (x.length != y.length) {
            throw new Exception("x and y vector size don't match");
        }
        float min = -Float.MAX_VALUE;
        for (float v : x) {
            if (v <= min) throw new Exception("x not sorted in ascending order");
            min = v;
        }
        int n = x.length - 1;
        float[] h = new float[n];
        float[] b = new float[n];
        float[] c = new float[n+1];
        float[] d = new float[n];

        for (int i = 0; i < n; i++) {
            h[i] = x[i + 1] - x[i];
        }
        float[] mu = new float[n];
        float[] z = new float[n + 1];
        float l;

        def.leftBoundary().apply(h,y,mu,z,c);

        for (int i = 1; i < n; i++) {
            l = 2.0f * (x[i+1] - x[i-1]) - h[i-1] * mu[i-1];
            mu[i] = h[i] / l;
            z[i] = (3.0f * (y[i+1] * h[i-1] - y[i] * (x[i+1] - x[i-1])+ y[i-1] * h[i]) /
                    (h[i-1] * h[i]) - h[i-1] * z[i-1])/ l;
        }

        def.rightBoundary().apply(h,y,mu,z,c);

        for (int j = n -1; j >=0; j--) {
            c[j] = z[j] - mu[j] * c[j+1];
            b[j] = (y[j + 1] - y[j])/h[j] - h[j] * (c[j+1] + 2.0f*c[j])/3.0f;
            d[j] = (c[j+1] - c[j]) / (3.0f* h[j]);
        }
        polynominals = new float[n+2][];
        polynominals[0] = new float[] {y[0],b[0]}; // initial linear section
        for ( int i = 0; i< n;i++){
            polynominals[i+1] = new float[] {y[i],b[i],c[i],d[i]};
        }
        float x1 = this.x[n] - this.x[n-1];
        float x2 = x1*x1;
        polynominals[n+1] = new float[] {y[n],polynominals[n][1] + 2* polynominals[n][2]*x1 + 3*polynominals[n][3]*x2}; // final linear section
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

    public float yintercept_linsec(linsec linsec){
        int in = (linsec == linsec.left)? 0 : x.length;
        float x1 = - this.x[(in == 0)?0:in-1];
        return polynominals[in][0] + polynominals[in][1]*x1;
    }

    public float calc(float x){
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

    public float calcSlope(float x){
        int in = geti(x);
        if (in==0 || in==this.x.length)
            return polynominals[in][1];
        else {
            float x1 = x - this.x[in-1];
            float x2 = x1*x1;
            return polynominals[in][1] + 2f*polynominals[in][2]*x1 + 3f*polynominals[in][3]*x2;
        }
    }

    public float calcCurve(float x){
        int in = geti(x);
        if (in==0 || in==this.x.length)
            return 0f;
        else {
            float x1 = x - this.x[in - 1];
            return 2f * polynominals[in][2] + 6f * polynominals[in][3] * x1;
        }
    }

    public float getCurveCoeff(int pos){
        return polynominals[pos+1][2] ;
    }
    public float getSlope(int pos){
        return polynominals[pos+1][1] ;
    }
    public float getVal(int pos){
        return polynominals[pos+1][0] ;
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
        if ( x < this.x[0])
            i = 0;
        else {
            i = 1;
            while (i < this.x.length && x > this.x[i] ) i = i + 1;
        }
        return i;
    }

    public CubicSpline getCutCubicSpline(float min, float max){
        return new CubicSpline(this,min,max);
    }

    /* derive a Spline by cutting out a piece between min and max of an existing
    reference splin and continue as linear function. Used as heuristic */
    private CubicSpline(CubicSpline cubicSpline, float min, float max){
        int minInd = cubicSpline.geti(min);
        if (minInd < 1 )
            throw new RuntimeException("Slope of lower tangent too small for heuristic Spline:" + min);
        int maxInd = cubicSpline.geti(max);
        if (maxInd >= cubicSpline.x.length )
            throw new RuntimeException("Slope of upper tangent too large for heuristic Spline:" + max );
        int n = maxInd - minInd + 1;
        polynominals = new float[n+2][4];
        x = new float[n+1];
        /*first linear section */
        polynominals[0] = new float[] {cubicSpline.calc(min),cubicSpline.calcSlope(min)};
        x[0] = min;
        /* translation to new point min */
        polynominals[1][0] = cubicSpline.calc(min);
        polynominals[1][1] = cubicSpline.calcSlope(min);
        polynominals[1][2] = cubicSpline.calcCurve(min)/2f; // translation requires 2nd derivative / 2
        polynominals[1][3] = cubicSpline.polynominals[minInd][3]; // 3rd derivative does not vary
        /* using existing */
        for ( int i = 2; i<n+1;i++){
           x[i-1] = cubicSpline.x[minInd+i-2];
//           for ( int j = 0; j<4;j++){
//               polynominals[i][j] = cubicSpline.polynominals[minInd+i-1][j];
//           }
           System.arraycopy(cubicSpline.polynominals[minInd + i - 1], 0, polynominals[i], 0, 4);
        }
        /* last linear section*/
        x[n] = max;
        polynominals[n+1] = new float[] {cubicSpline.calc(max),cubicSpline.calcSlope(max)};
    }



    public CubicSpline getFactCubicSpline(float factor){
        return new CubicSpline(factor,this);
    }

    private CubicSpline( float fact,CubicSpline cubicSpline){
        polynominals = new float[cubicSpline.x.length+1][4];
        x = Arrays.copyOf(cubicSpline.x,cubicSpline.x.length);
        polynominals[0][0] = cubicSpline.polynominals[0][0] * fact;
        polynominals[0][1] = cubicSpline.polynominals[0][1] * fact;
        polynominals[cubicSpline.x.length][0] = cubicSpline.polynominals[cubicSpline.x.length][0] * fact ;
        polynominals[cubicSpline.x.length][1] = cubicSpline.polynominals[cubicSpline.x.length][1] * fact;
        for ( int i = 1; i < cubicSpline.x.length; i++ )   {
            polynominals[i][0] = cubicSpline.polynominals[i][0] * fact;
            polynominals[i][1] = cubicSpline.polynominals[i][1] * fact;
            polynominals[i][2] = cubicSpline.polynominals[i][2] * fact;
            polynominals[i][3] = cubicSpline.polynominals[i][3] * fact;
        }
    }

    public CubicSpline getTransYCubicSpline(float transY){
        return new CubicSpline(this,transY);
    }
    private CubicSpline(CubicSpline cubicSpline, float transY){
        polynominals = new float[cubicSpline.x.length+1][4];

        x = Arrays.copyOf(cubicSpline.x,cubicSpline.x.length);
        polynominals[0][0] = cubicSpline.polynominals[0][0] + transY;
        polynominals[0][1] = cubicSpline.polynominals[0][1];
        polynominals[cubicSpline.x.length][0] = cubicSpline.polynominals[cubicSpline.x.length][0] + transY ;
        polynominals[cubicSpline.x.length][1] = cubicSpline.polynominals[cubicSpline.x.length][1];
        for ( int i = 1; i < cubicSpline.x.length; i++ )   {
            polynominals[i][0] = cubicSpline.polynominals[i][0] + transY;
            polynominals[i][1] = cubicSpline.polynominals[i][1];
            polynominals[i][2] = cubicSpline.polynominals[i][2];
            polynominals[i][3] = cubicSpline.polynominals[i][3];
        }
    }

}
