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

    public static float getFrictionBasedVelocity(double slope, double watt, double Cr ){
        return (float) getFrictionBasedVelocity( slope, watt, Cr,1.2,90d,0.45,0.95);

    }

    public static double getFrictionBasedVelocity(  double slope, double watt, double Cr, double rho, double mass, double ACw, double eta  ) {
        double mg = mass * 9.81;
        double ACwr = 0.5 * ACw * rho;
        double p = mg * (Cr + slope) / ACwr;
        double q = -watt / ACwr * eta;
        return solveP3(p, q);
    }



    public static float solveP3(double p, double q){
        double D = Math.pow(q,2)/4. + Math.pow(p,3)/27.;

        return (float) ((D>=0) ? Math.cbrt(- q*0.5 + Math.sqrt(D)) + Math.cbrt(- q*0.5 - Math.sqrt(D)) :
                Math.sqrt(-4.*p/3.) * Math.cos(1./3.*Math.acos(-q/2*Math.sqrt(-27./Math.pow(p,3.)))));

    }



}
