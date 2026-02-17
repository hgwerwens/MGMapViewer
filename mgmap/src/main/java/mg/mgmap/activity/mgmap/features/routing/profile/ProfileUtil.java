package mg.mgmap.activity.mgmap.features.routing.profile;

public class ProfileUtil {

    public static float sig(double base){
        return (float) (1./(1.+Math.exp(base)));
    }

    public static float newtonNumeric(float start, float minval, IfFunction f, float deltax){
        IfFunction fs = x -> ( f.calc(x + deltax) - f.calc(x - deltax) ) / ( 2f*deltax);
        return newton( start, minval,5, f, fs);
    }

    public static float newton(float start, float minval, int maxIter, IfFunction f, IfFunction fs) throws RuntimeException{
        float a;
        float na = start;
        float nb;
        float sa;
        float fa;
        float nfa;
        float nfb;
        float sfa;
        int i = 0;
        int j;
        boolean numLimit = false;
        nfa = f.calc(start);
        do {
            i = i+1;
            if ( i >= maxIter)
                throw new RuntimeException("Too many Newton iterations= " + maxIter);
            a = na;
            fa = nfa;
            float fsv = fs.calc(a);
            if ( fsv == 0f)
                throw new RuntimeException("Newton iteration - First derivative is 0");
            na = a - fa / fsv;
            nfa = f.calc(na);
            nfb = fa;
            nb = a;
            j  = 0;

            while ( Math.abs(nfa) >= 0.5f*Math.abs(fa) && Math.abs(nfa) > minval &&!numLimit ) { // fallback to regula falsi
                j = j+1;
                if ( j >= maxIter)
                    throw new RuntimeException("Too many Regula Falsi iterations= " + maxIter);
                sa = ( na * nfb - nb * nfa) / ( nfb -nfa );
                if (sa==na) {
                    System.out.println("Numerical Limit of Newton iteration reached");
                    numLimit = true;
                }
                sfa = f.calc(sa) - minval;
                if (Math.signum(sfa) == Math.signum(nfa)) {
                    na = sa;
                    nfa = sfa;
                } else {
                    nb = na;
                    nfb = nfa;
                    na = sa;
                    nfa = sfa;
                }
            }
        } while ( Math.abs(nfa) > minval &&!numLimit);
        return na;
    }

}
