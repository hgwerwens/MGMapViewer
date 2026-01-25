package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfileMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.CubicSpline;
import mg.mgmap.generic.util.basic.MGLog;

public class RoutingMTBProfileCreateTest {
    private static class SoftAssert {
        private final List<String> errors = new ArrayList<>();
        public void check(boolean condition, String message) {
            if (!condition) errors.add(message);
        }
        public void assertAll() {
            if (!errors.isEmpty()) {
                throw new AssertionError("Multiple failures:\n" + String.join("\n", errors));
            }
        }
    }

    private static SoftAssert softAssert = new SoftAssert();


    @Test
    public void singlescCompare() {
        int sUp = 100;
        int sDn = 100;
        int sc1 = 2;
        int sc2 = 3;
        CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(sUp, sDn,false));
        try {
            compareCubicSpline(costCalcSplineProfileMTB.getCostSpline(sc1),costCalcSplineProfileMTB.getCostSpline(sc2),0f,0.5f,new StringBuilder(),true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        softAssert.assertAll();
    }


    @Test
    public void singleProfile() {
        int sUp = 100;
        int sDn = 100;
        CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(sUp, sDn,false));
        checkProfile(costCalcSplineProfileMTB, new StringBuilder(String.format("sUp=%3d sDn=%3d", sUp, sDn)));
        softAssert.assertAll();
    }

    @Test
    public void VaryContext_default() throws Exception {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);
        for (int sUp = 100; sUp <= 300; sUp = sUp + 100) {
            for (int sDn = 100; sDn <= 300; sDn = sDn + 100) {
                CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(sUp, sDn,false));
                checkProfile(costCalcSplineProfileMTB, new StringBuilder(String.format("sUp=%3d sDn=%3d", sUp, sDn)));
             }
        }
        softAssert.assertAll();
    }

    @Test
    public void VaryContext_wide() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);
        for (int sUp = 0; sUp <= 400; sUp = sUp + 100) {
            for (int sDn = 0; sDn <= 400; sDn = sDn + 100) {
                for (int power = 48 + sUp / 100 * 25; power <= 150f; power = power + 25) {
                    CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new CostCalcSplineProfileMTB.Context(power, sUp, sDn,false));
                    StringBuilder msg = new StringBuilder(String.format("sUp=%3d sDn=%3d pow=%3d", sUp, sDn, power));
                    checkProfile(costCalcSplineProfileMTB, msg);
                }
            }
        }
        softAssert.assertAll();
    }
    private void checkProfile(CostCalcSplineProfileMTB costCalcSplineProfileMTB, StringBuilder msg){
        checkSurfaceCats(costCalcSplineProfileMTB, msg);
        compareSurfaceCats(costCalcSplineProfileMTB, msg);
    }

    private void checkSurfaceCats(CostCalcSplineProfileMTB costCalcSplineProfileMTB, StringBuilder rmsg){
        for (int sc = 0; sc < CostCalcSplineProfileMTB.maxSurfaceCat; sc++) {
            CubicSpline cubicSpline = null;
            try {
                cubicSpline = costCalcSplineProfileMTB.getCostSpline(sc);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            StringBuilder msg = new StringBuilder(rmsg).append(String.format(" sc=%2d",sc));
            checkCurvature(cubicSpline,msg);
            checkSlope(cubicSpline,msg);
        }
    }


    private void checkCurvature(CubicSpline cubicSpline, StringBuilder msg){
        if (cubicSpline != null) {
            ArrayList<CubicSpline.Value> curveRadiusForNegCurvaturePoint = cubicSpline.getCurveRadiusForNegCurvaturePoints();
            StringBuilder critmsg = new StringBuilder(msg);
            if (curveRadiusForNegCurvaturePoint != null){
                boolean noCritical = true;
                for (CubicSpline.Value negCurvature : curveRadiusForNegCurvaturePoint) {
                    float curvature = -negCurvature.y();
                    if (curvature < CostCalcSplineProfileMTB.minNegCurvatureRadius*10) {
                        noCritical = false;
                        String m = String.format(Locale.ENGLISH, " SLOPE=%.2f VERY CRITICAL CURVE RADIUS=%.2f", 100 * negCurvature.x(), curvature);
                        msg.append(m);
                        critmsg.append(m);
                    }
                    else if (curvature < 0.1f)
                        msg.append(String.format(Locale.ENGLISH, " Slope=%.2f      Critical Curve Radius=%.2f", 100 * negCurvature.x(), curvature));
                    else
                        msg.append(String.format(Locale.ENGLISH, " slope=%.2f curve radius=%.2f", 100 * negCurvature.x(), curvature));

                }
                System.out.println(msg);
                softAssert.check (noCritical,critmsg.toString());
            }
        }
    }

    private void checkSlope( CubicSpline cubicSpline, StringBuilder msg){
        if (cubicSpline != null) {
            float slope = cubicSpline.calcSlope(0f);
            if (slope <= 0f) {
                System.out.println(msg.append(String.format(Locale.ENGLISH, "Curve slope at 0 is negative slope=%2f")));
                softAssert.check (true,msg.toString());
            }
        }
    }

    private void compareSurfaceCats(CostCalcSplineProfileMTB costCalcSplineProfileMTB, StringBuilder msg){
        for (int sc = 0; sc < CostCalcSplineProfileMTB.maxSL - 1; sc++){
            StringBuilder imsg = new StringBuilder(msg).append(String.format(Locale.ENGLISH," compare sc=%2d sc=%2d ",sc,sc+1));
            try {
                float delAt0 = costCalcSplineProfileMTB.getCostSpline(sc+1).calc(0f) - costCalcSplineProfileMTB.getCostSpline(sc).calc(0f);
                softAssert.check(delAt0*(sc-1.5f)>0,imsg + String.format(Locale.ENGLISH, " wrong costSequence sc=%.2f",delAt0) );
                ScCompRes scCompRes = compareCubicSpline(costCalcSplineProfileMTB.getCostSpline(sc),costCalcSplineProfileMTB.getCostSpline(sc+1),0f,0.5f,imsg,false);
                softAssert.check(scCompRes.crossings.isEmpty(), scCompRes.msg());
            } catch (Exception e) {
                softAssert.check(false, imsg+"failure:"+e.getMessage());
                System.out.println(imsg+e.getMessage());
            }
        }
        for (int mtbDn = 0; mtbDn <= 6; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp < Math.min(mtbDn + CostCalcSplineProfileMTB.maxUptoDn,CostCalcSplineProfileMTB.maxUp); mtbUp++) {
                int sc1 = CostCalcSplineProfileMTB.getSurfaceCat(CostCalcSplineProfileMTB.maxSL, mtbDn, mtbUp);
                int sc2 = CostCalcSplineProfileMTB.getSurfaceCat(CostCalcSplineProfileMTB.maxSL, mtbDn, mtbUp + 1);
                StringBuilder imsg = new StringBuilder(msg).append(String.format(Locale.ENGLISH, " compare mtbDn=%2d mtbUp1=%2d mtbUp2=%2d", mtbDn, mtbUp,mtbUp+1));
                try {
                    float delAt0 = costCalcSplineProfileMTB.getCostSpline(sc2).calc(0f) - costCalcSplineProfileMTB.getCostSpline(sc1).calc(0f);
                    softAssert.check(delAt0>0,imsg + String.format(Locale.ENGLISH, " wrong costSequence sc=%.2f",delAt0) );
                    ScCompRes scCompRes =  compareCubicSpline(costCalcSplineProfileMTB.getCostSpline(sc1), costCalcSplineProfileMTB.getCostSpline(sc2), 0f, 0.5f, imsg, false);
                    softAssert.check(scCompRes.crossings.isEmpty(), scCompRes.msg());
                } catch (Exception e) {
                    System.out.println(imsg+e.getMessage());
                    softAssert.check(false,imsg + e.getMessage());
                }
            }
        }
    }

    record Point(float x, float y0,float y1, float y2){};
    record ScCompRes(ArrayList<Point> crossings, ArrayList<Point> extrema, String msg){};

    private ScCompRes compareCubicSpline(CubicSpline cubicSplinelow, CubicSpline cubicSplinehigh, float startSlope, float endSlope, StringBuilder msg, boolean allDetails) throws Exception {
        if (cubicSplinehigh == null || cubicSplinelow == null) throw new Exception(msg+" CubicSpline null");
        function y0 = x -> cubicSplinehigh.calc(x) - cubicSplinelow.calc(x);
        function y1 = x -> cubicSplinehigh.calcSlope(x) - cubicSplinelow.calcSlope(x);
        function y2 = x -> cubicSplinehigh.calcCurve(x) - cubicSplinelow.calcCurve(x);
        ArrayList<Point> crossings = new ArrayList<>();
        ArrayList<Point> extrema = new ArrayList<>();
        float d0 = 0f;
        float d1 = 0f;
        float d2 ;
        float step = 0.01f;
        for ( float sl = startSlope; sl <= endSlope; sl += step ){
            d2 = y0.apply(sl);
            if (d2*d1 < 0f  ){
                float cross = newton(sl-step/2f,1e-6f,10,y0,y1);
                if ( cross <= sl && cross > sl-step)
                    crossings.add(new Point(cross,y0.apply(cross),y1.apply(cross),y2.apply(cross)));
                else
                    System.out.println( msg + "crossing out of range");
            } else if (d2*d1 < 1e-6f && d1!=0f) {
                try {
                    float cross1 = newton(sl - step / 2f, 1e-6f, 10, y0, y1);
                    float cross2 = newton(sl - step / 2f, 1e-6f, 10, y0, y1);
                    if ( cross1 != cross2 && cross1 <= sl && cross1 > sl-step && cross2 <= sl && cross2 > sl-step) {
                        crossings.add(new Point(cross1, y0.apply(cross1),y1.apply(cross1),y2.apply(cross1)));
                        crossings.add(new Point(cross1, y0.apply(cross2),y1.apply(cross2),y2.apply(cross2)));
                    }
                } catch (RuntimeException e) {
                    System.out.println(msg + " for sl=" + Math.round(sl*100) + ":" +e.getMessage() );
                }
            }
            if ( d0 != 0 && (d1-d0)*(d2-d1)<0 ){
                try {
                    float min = newton( sl-step,1e-5f,15,y1,y2);
                    if (min<sl && min > sl -2f*step) {
                        extrema.add(new Point(min,y0.apply(min),y1.apply(min),y2.apply(min)));
                    } else
                        System.out.println( msg + "extrema out of range");
                } catch (RuntimeException e) {
                    System.out.println("Inside" + e.getMessage());
                }
            }
            d0 = d1;
            d1 = d2;
        }
        StringBuilder imsg = crossings.isEmpty() ? new StringBuilder(msg) : new StringBuilder(msg).append(" cross at");
        for (Point cross : crossings) {
            imsg.append(String.format(Locale.ENGLISH, " %.2f%s(%.2f)", cross.x * 100,cross.y1 >0?"⇧":"⇩",10*cross.y0));
        }
        if (!extrema.isEmpty() ) imsg.append(" extrema at");
        for (Point ext : extrema) {
            imsg.append(String.format(Locale.ENGLISH, " %.2f%s(%.2f)", ext.x * 100,ext.y2 <0?"⇧":"⇩",10*ext.y0));
        }
        if (!extrema.isEmpty()||!crossings.isEmpty()) {
            System.out.println(imsg);
        }
        if (allDetails) {
            StringBuilder fl  = new StringBuilder("curve scLow ");
            StringBuilder fh  = new StringBuilder("curve scHigh");
            StringBuilder f   = new StringBuilder("curve Points");
            StringBuilder f1  = new StringBuilder("first Deriva");
            StringBuilder f2  = new StringBuilder("secnd Deriva");
            StringBuilder x   = new StringBuilder("x     Points");
            for ( float sl = startSlope; sl <= endSlope; sl += step ) {
                fl.append(String.format(Locale.ENGLISH, " %+6.2f",10*cubicSplinelow.calc(sl)));
                fh.append(String.format(Locale.ENGLISH, " %+6.2f",10*cubicSplinehigh.calc(sl)));
                f.append(String.format(Locale.ENGLISH, " %+6.2f",10*y0.apply(sl)));
                f1.append(String.format(Locale.ENGLISH, " %+6.2f",10*y1.apply(sl)));
                f2.append(String.format(Locale.ENGLISH, " %+6.2f",y2.apply(sl)));
                x.append(String.format(Locale.ENGLISH, " %+6.2f",sl*100));
            }
            System.out.println(x);
            System.out.println(fl);
            System.out.println(fh);
            System.out.println(f);
            System.out.println(f1);
            System.out.println(f2);
            float[] xl = cubicSplinelow.getX();
            float[] yl = cubicSplinelow.getY();
            float[] xh = cubicSplinehigh.getX();
            float[] yh = cubicSplinehigh.getY();
            StringBuilder xbl = new StringBuilder("xbv scLow ");
            StringBuilder ybl = new StringBuilder("ybv scLow ");
            StringBuilder xbh = new StringBuilder("xbv scHigh");
            StringBuilder ybh = new StringBuilder("ybv scHigh");
            for (int i = 0; i < xl.length; i++) {
                xbl.append(String.format(Locale.ENGLISH, " %+8.4f", xl[i] * 100));
                ybl.append(String.format(Locale.ENGLISH, " %+8.4f", yl[i]));
            }
            for (int i = 0; i < xh.length; i++) {
                xbh.append(String.format(Locale.ENGLISH, " %+8.4f", xh[i] * 100));
                ybh.append(String.format(Locale.ENGLISH, " %+8.4f", yh[i]));
            }
            System.out.println(xbl);
            System.out.println(ybl);
            System.out.println(xbh);
            System.out.println(ybh);
        }
        return new ScCompRes(crossings,extrema,imsg.toString());
    }


    public interface function{
        float apply(float x);
    }

    public static float newton(float start, float minval, int maxIter, function f, function fs) throws RuntimeException{
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
        nfa = f.apply(start);
        do {
            i = i+1;
            if ( i >= maxIter)
                throw new RuntimeException("Too many Newton iterations= " + maxIter);
            a = na;
            fa = nfa;
            float fsv = fs.apply(a);
            if ( fsv == 0f)
                throw new RuntimeException("Newton iteration - First derivative is 0");
            na = a - fa / fsv;
            nfa = f.apply(na);
            nfb = fa;
            nb = a;
            j  = 0;
            while ( Math.abs(nfa) >= 0.5f*Math.abs(fa) && Math.abs(nfa) > minval ) { // fallback to regula falsi
                j = j+1;
                if ( j >= maxIter)
                    throw new RuntimeException("Too many Regula Falsi iterations= " + maxIter);
                sa = ( na * nfb - nb * nfa) / ( nfb -nfa );
                sfa = f.apply(sa) - minval;
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
        } while ( Math.abs(nfa) > minval );
        return na;
    }


}

