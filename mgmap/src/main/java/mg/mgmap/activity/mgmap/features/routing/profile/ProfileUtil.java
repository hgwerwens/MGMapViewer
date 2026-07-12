package mg.mgmap.activity.mgmap.features.routing.profile;

public class ProfileUtil {

    public static float sig(double base){
        return (float) (1./(1.+Math.exp(base)));
    }

    public static float newtonNumeric(float start, float minval, IfFunction f, float deltax){
        IfFunction fs = x -> ( f.calc(x + deltax) - f.calc(x - deltax) ) / ( 2f*deltax);
        return newton( start, minval,5, f, fs);
    }

    public static float newton (float start, float threshold, int maxIter, IfFunction f, IfFunction fs) throws RuntimeException{
        return newton (start, threshold, -Float.MAX_VALUE,Float.MAX_VALUE, maxIter, f, fs);
    }

    public static float newton(float start, float threshold, float leftLimit, float rightLimit, int maxIter, IfFunction f, IfFunction fs) throws RuntimeException{
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
            if (na<leftLimit){
                na = leftLimit;
                nfa = f.calc(na);
            }
            if (na>rightLimit){
                na = rightLimit;
                nfa = f.calc(na);
            }
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

            while ( Math.abs(nfa) >= 0.5f*Math.abs(fa) && Math.abs(nfa) > threshold &&!numLimit ) { // fallback to regula falsi
                j = j+1;
                if ( j >= maxIter)
                    throw new RuntimeException("Too many Regula Falsi iterations= " + maxIter);
                sa = ( na * nfb - nb * nfa) / ( nfb -nfa );
                if (sa==na) {
                    System.out.println("Numerical Limit of Newton iteration reached");
                    numLimit = true;
                }
                sfa = f.calc(sa) - threshold;
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
        } while ( Math.abs(nfa) > threshold &&!numLimit);
        return na;
    }

    private record VelParm(double rho, double mass, double ACw, double eta){};
    private static VelParm velParm = new VelParm(1.2,90d,0.45,0.95);

    private record HikeParm(double rho, double mass, double ACw, double eta,double a, double b){};
    private static HikeParm hikeParm = new HikeParm(1.2,75d,0.6,1d,30d, 5);



    public static float getFloatFrictionBasedVelocity(double slope, double watt, double Cr ){
        return  (float) getFrictionBasedVelocity( slope, watt, Cr);
    }

    public static double getFrictionBasedVelocity(double slope, double watt, double Cr ){
        return  getFloatFrictionBasedVelocity( slope, watt, Cr,velParm.rho,velParm.mass,velParm.ACw,velParm.eta);
    }

    public static double getSlopeDerivative(double slope, double v, double Cr){
        return getSlopeDerivative( slope, v, Cr,velParm.rho,velParm.mass,velParm.ACw,velParm.eta);
    }


    public static double getSlopeDerivative(double slope, double v, double Cr, double rho, double mass, double ACw, double eta){
       double mg =  mass * 9.81;
       double ACwr = 0.5 * ACw * rho;
       double dvds = - (mg*v)/(3*ACwr*v*v + mg*(slope+Cr));
       return dvds;
    }

    public static double getFloatFrictionBasedVelocity(double slope, double watt, double Cr, double rho, double mass, double ACw, double eta  ) {
        double mg = mass * 9.81;
        double ACwr = 0.5 * ACw * rho;
        double p = mg * (Cr + slope) / ACwr;
        double q = -watt / ACwr * eta;
        return solveP3(p, q);
    }

    public static double getWalkingVelocity(double slope, double watt,double cterr){
        return getWalkingVelocity( slope, watt,cterr, hikeParm.mass, hikeParm.a, hikeParm.b, hikeParm.rho, hikeParm.ACw, hikeParm.eta);
    }
    private static double getWalkingVelocity(
            double slope,
            double watt,
            double cTerr,
            double mass,
            double a,
            double b,
            double rho,
            double ACw,
            double eta
  // neuer Untergrund-Parameter
    ) {
        double g  = 9.81;
        double mg = mass * g;
        double K0 = a + mg * (slope + cTerr);
        double K2 = b + 0.5 * rho * ACw;
        double p = K0 / K2;
        double q = - (watt * eta) / K2;
        return solveP3(p, q);
    }




    private static FloatTol floatTol = new FloatTol(1e-7f,1e-7f);

    public static double solveP3(double p, double q){
        // stabiler Spezialfall (typisch für Fahrradphysik)
        if (p > 0) {
            double a = Math.sqrt(p / 3.0);
            double b = (3.0 * q) / (2.0 * p) * Math.sqrt(3.0 / p);
            return  - 2.0 * a * Math.sinh(asinh(b) / 3.0);
        }
        // allgemeine Cardano-Lösung
        double D = Math.pow(q,2)/4. + Math.pow(p,3)/27.;
        return  ((D>=0) ? Math.cbrt(- q*0.5 + Math.sqrt(D)) + Math.cbrt(- q*0.5 - Math.sqrt(D)) :
                Math.sqrt(-4.*p/3.) * Math.cos(1./3.*Math.acos(-q/2*Math.sqrt(-27./Math.pow(p,3.)))));
    }
    public static double asinh(double x) {
        return Math.log(x + Math.sqrt(x*x + 1.0));
    }


    public static final class FloatTol {

        private final float absEps;
        private final float relEps;

        public FloatTol( ){
           this(1e-6f,1e-6f);
        }
        public FloatTol(float absEps, float relEps) {
            this.absEps = absEps;
            this.relEps = relEps;
        }
        private float tol(float a, float b) {
            return absEps + relEps * Math.max(Math.abs(a), Math.abs(b));
        }
        public boolean eq(float a, float b) {
            return Math.abs(a - b) <= tol(a, b);
        }
        public boolean gt(float a, float b) {
            return a > b + tol(a, b);
        }
        public boolean lt(float a, float b) {
            return a < b - tol(a, b);
        }

        public boolean ge(float a, float b) {
            return gt(a, b) || eq(a, b);
        }

        public boolean le(float a, float b) {
            return lt(a, b) || eq(a, b);
        }
    }
}
