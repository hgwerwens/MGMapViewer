package mg.mgmap.activity.mgmap.features.routing.profile;

import java.util.Arrays;
import java.util.HashMap;

public class DurationSplineFunctionFactory {
    static DurationSplineFunctionFactory durationSplineFunctionFactory = new DurationSplineFunctionFactory();

    private static class Id {
        private final short[] ids;
        Id(short[] ids){
            this.ids = ids;
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(ids);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Id other = (Id) obj;
            return Arrays.equals(ids, other.ids);
        }
    }
    private final HashMap<Id, CubicSpline> map = new HashMap<>();

    public static DurationSplineFunctionFactory getInst(){
        return durationSplineFunctionFactory;
    }


    public CubicSpline getDurationSplineFunction(short klevel, short slevel, short surfaceLevel, short bicType){

        Id id = new Id(new short[] {klevel,slevel,surfaceLevel,bicType});
        CubicSpline cubicSpline = map.get(id);
        if (cubicSpline == null) {
            float[] slopes ;
            float[] durations;
            if (bicType > 0) {
//                slopes = new float[]{-0.4f, -0.2f, -0.05f, 0f, 0.05f, 0.10f, 0.24f,0.6f};
//                durations = new float[slopes.length];
                float watt;
                float watt0;
                float ACw;
                float Cr;
                float fd;
                float highdowndoffset;
                float fdown;
                float fr;
                float f1u;
                float f2u;
                float m = 90f;
                if (bicType == 1){
                    slopes = new float[]{-0.4f, -0.2f, -0.05f, 0f, 0.05f, 0.10f, 0.24f,0.6f};
                    durations = new float[slopes.length];
                    watt = 90f + 35f*klevel;
                    ACw = 0.4f + surfaceLevel * 0.05f ;
                    highdowndoffset = 0.075f;
//                fd = (float)Math.exp(-(slevel-2)*Math.log(Math.sqrt(2.0f)))/1.6f;
//                    fd = 1.104f - slevel/4.53f;
                    fd = 1.1f - slevel/4.55f;
                    if (surfaceLevel <= 3){
                        Cr = 0.004f + 0.001f*surfaceLevel;
                    } else {
                        Cr = 0.015f + 0.015f*fd  + 0.01f*(surfaceLevel - 4);
                    }
//                    Cr = 0.004f + 0.002f*surfaceLevel;
                    fr = 1.1f - fd * (0.5f+surfaceLevel/20.0f);
//                    fr = 0.715f+0.045f*slevel-0.08375f*surfaceLevel+0.02375f*slevel*surfaceLevel;
                    f1u = 1.0f + 0.5f*surfaceLevel*surfaceLevel/16.0f;
                    f2u = 1.2f + 1.4f*surfaceLevel*surfaceLevel/16.0f;
                    fdown =  fd*(3.5f+surfaceLevel*0.6f);
                    slopes[5] = 0.07f+0.015f*klevel;
                    slopes[6] = 0.24f+0.02f*klevel;
                    durations[0] = (-slopes[0]-highdowndoffset)*fdown;
                    durations[1] = (-slopes[1]-0.075f)*fdown;
                    durations[2] = 1f / (getFrictionBasedVelocity(slopes[2], watt, Cr, ACw, m) * fr);
                    for (int i = 3; i < slopes.length - 2; i++) {
                        durations[i] = 1f / getFrictionBasedVelocity(slopes[i], watt, Cr, ACw, m);
                    }
                    durations[6] = f1u /  getFrictionBasedVelocity(slopes[6], watt, Cr, ACw, m)  ;
                    durations[7] = f2u /  getFrictionBasedVelocity(slopes[7], watt, Cr, ACw, m)  ;
                } else { //if (bicType ==3) {
                    watt0 = 90.0f ;
                    watt = 130.0f;
                    ACw = 0.45f;
                    if (surfaceLevel <= 2) {
//                      ACw = 0.45f + 0.1f * surfaceLevel;
                        Cr = 0.0035f + 0.0015f * surfaceLevel;
//                      fr = 0.875f - 0.075f * surfaceLevel;
                        highdowndoffset = 0.0f;
                        fdown = 3.5f + 0.5f*surfaceLevel;
//                        fdown = 2.5f + 0.5f*surfaceLevel;
                    } else {
//                        ACw = 0.8f + 0.3f * (surfaceLevel - 3);
                        Cr = 0.012f + 0.023f * (surfaceLevel - 3);
                        //                       fr = 1.0f; // 0.6f;
                        highdowndoffset = -0.1f;
                        fdown = 8.0f + (surfaceLevel - 3);
//                        fdown = 6f + 2f*(surfaceLevel - 3);
                    }
                    int i0;
                    if (surfaceLevel <= 3) {
                        slopes = new float[]{-10.0f, -0.6f, -0.2f, 50.0f, 0.0f, 0.1f, 0.6f,10.0f};
                        durations = new float[slopes.length];
                        float[] freeRollSlope = new float[] {-0.049f,-0.033f,-0.0195f,-0.0215f};
                        slopes[3] = freeRollSlope[surfaceLevel];
                        durations[3] = 1f / (getFrictionBasedVelocity(slopes[3], 0.0f, Cr, ACw, m));
                        i0 = 4;
                    } else {
                        slopes = new float[]{-10.0f, -0.6f,-0.2f, 0.0f, 0.1f, 0.6f,10.0f};
                        durations = new float[slopes.length];
                        i0 = 3;
                    }
                    durations[0] = (-slopes[0]-highdowndoffset+0.4f)*fdown;
                    durations[1] = (-slopes[1]-highdowndoffset)*fdown;
                    durations[2] = (-slopes[2]-0.075f)*fdown;
                    durations[i0] = 1f / getFrictionBasedVelocity(slopes[i0], watt0, Cr, ACw, m);
                    for (int i = i0+1; i < slopes.length - 2; i++) {
                        durations[i] = 1f / getFrictionBasedVelocity(slopes[i], watt, Cr, ACw, m);
                    }
                    durations[slopes.length-2] = 1.5f /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, Cr, ACw, m)  ;
                    durations[slopes.length-1] = 1.8f /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, Cr, ACw, m)  ;
                }
            } else { // bicType == 0 -> hiking
                float fu = 9.0f;
                float fd = 10.5f;
                float off = 0.1f;
                float vbase = 5.25f - 0.1f*surfaceLevel;
                float t_base = 3.6f/vbase;
                float t_m10Pcnt = t_base/1.3f;

                slopes = new float[]{-0.5f, -0.3f, -0.075f, 0.0f, 0.2f, 0.5f};
                durations = new float[slopes.length];
                durations[0] = (-slopes[0]-off)*fd;
                durations[1] = (-slopes[1]-off)*fd;
                durations[2] = t_m10Pcnt;
                durations[3] = t_base;
                durations[4] = slopes[4]*fu;
                durations[5] = slopes[5]*fu;
            }


            try {
                cubicSpline = new CubicSpline(slopes, durations);
                map.put(id,cubicSpline);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cubicSpline;
    }

    /**
     * see <a href="https://www.michael-konczer.com/de/training/rechner/rennrad-leistung-berechnen">...</a>
     * @param slope slope of the trail
     * @param watt power measured in watt
     * @param Cr rolling friction coefficient
     * @param ACw Surface times dimensionless air resistance coefficient
     * @param m system mass [driver + bike ]
     * @return velocity (v in [m/s]
     * watt = P air + P roll + P slope
     * P air = 1/2 * Acw * rho * v^3 ; P roll = mg * Cr * v; P slope = mg * slope
     * solved for velocity via cardanic equations
     */
    private float getFrictionBasedVelocity(double slope, double watt, double Cr, double ACw, double m ){
        double rho = 1.2;
        double mg = m*9.81;
        double ACwr = 0.5 * ACw * rho;
        double eta = 0.95;
        double p =  mg*(Cr+slope)/ACwr;
        double q =  -watt/ACwr*eta;
        double D = Math.pow(q,2)/4. + Math.pow(p,3)/27.;

        return (float) ((D>=0) ? Math.cbrt(- q*0.5 + Math.sqrt(D)) + Math.cbrt(- q*0.5 - Math.sqrt(D)) :
                Math.sqrt(-4.*p/3.) * Math.cos(1./3.*Math.acos(-q/2*Math.sqrt(-27./Math.pow(p,3.)))));
    }


}
