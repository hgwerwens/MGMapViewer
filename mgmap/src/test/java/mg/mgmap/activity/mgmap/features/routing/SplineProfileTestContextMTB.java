package mg.mgmap.activity.mgmap.features.routing;

import androidx.annotation.NonNull;

import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfile;
import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfileMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.IfSplineProfileContext;
import mg.mgmap.activity.mgmap.features.routing.profile.IfSplineProfileContextMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.SurfCat2MTBCat;

public class SplineProfileTestContextMTB implements IfSplineProfileContextMTB {
    public static boolean testProfile = true;

    private final IfSplineProfileContextMTB splineProfileContextMTB;
    private SplineProfileTestContextMTB(int power, int sUp, int sDn, boolean checkAll, boolean withRef){
        if (testProfile)
            splineProfileContextMTB = new TestContextMTBInternal( power, sUp, sDn, checkAll,  withRef);
        else
            splineProfileContextMTB = new SplineProfileContextMTB(power, sUp, sDn, checkAll,  withRef);
    }

    SplineProfileTestContextMTB(int power, int sUp, int sDn, boolean checkAll) {
        this(power, sUp, sDn, checkAll, true);
    }

   SplineProfileTestContextMTB(int power, int sUp, int sDn) {
        this(power, sUp, sDn, true);
    }
    SplineProfileTestContextMTB(int sUp, int sDn, boolean checkAll) {
        this((int) (47.5 + 25 * sUp / 100d), sUp, sDn, checkAll, true);
    }

    public SplineProfileTestContextMTB(int sUp, int sDn) {
        this(sUp, sDn, true);
    }

    @Override
    public int getSDn() {
        return splineProfileContextMTB.getSDn();
    }


    @Override
    public boolean getWithRef() {
        return splineProfileContextMTB.getWithRef();
    }

    @Override
    public CostCalcSplineProfile getRefProfile() {
        return splineProfileContextMTB.getRefProfile();
    }

    @Override
    public int getRefSc(int sc) {
        return splineProfileContextMTB.getRefSc(sc);
    }

    @Override
    public int getMaxSurfaceCat() {
        return splineProfileContextMTB.getMaxSurfaceCat();
    }

    @Override
    public int getScHeuristicRefSpline() {
        return splineProfileContextMTB.getScHeuristicRefSpline();
    }

    @Override
    public int getScProfileSpline() {
        return splineProfileContextMTB.getScProfileSpline();
    }

    @Override
    public boolean isValidSc(int surfaceCat) {
        return splineProfileContextMTB.isValidSc(surfaceCat);
    }

    @Override
    public int getIndRefDnSlope() {
        return splineProfileContextMTB.getIndRefDnSlope();
    }

    @Override
    public boolean fullCalc() {
        return splineProfileContextMTB.fullCalc();
    }

    @Override
    public float[] getCostSlopes(int sc) {
        return splineProfileContextMTB.getCostSlopes(sc);
    }

    @Override
    public float[] getDurationSlopes(int sc) {
        return splineProfileContextMTB.getDurationSlopes(sc);
    }

    @Override
    public float getRelSlope(int sc) {
        return 0;
    }

    @Override
    public float getF3d(int sc) {
        return splineProfileContextMTB.getF3d(sc);
    }

    @Override
    public float getF2d(int sc) {
        return splineProfileContextMTB.getF2d(sc);
    }

    @Override
    public float getSm20Dn(int sc) {
        return splineProfileContextMTB.getSm20Dn(sc);
    }

    @Override
    public float getFactorDn(int sc) {
        return splineProfileContextMTB.getFactorDn(sc);
    }

    @Override
    public float getCrDn(int sc) {
        return splineProfileContextMTB.getCrDn(sc);
    }

    @Override
    public float getCrUp(int sc) {
        return splineProfileContextMTB.getCrUp(sc);
    }

    @Override
    public float getF0u(int sc) {
        return splineProfileContextMTB.getF0u(sc);
    }

    @Override
    public float getF1u(int sc) {
        return splineProfileContextMTB.getF1u(sc);
    }

    @Override
    public float getF2u(int sc) {
        return splineProfileContextMTB.getF2u(sc);
    }

    @Override
    public float getF3u(int sc) {
        return splineProfileContextMTB.getF3u(sc);
    }

    @Override
    public float[] getDistFactforCostFunct(int sc) {
        return splineProfileContextMTB.getDistFactforCostFunct(sc);
    }

    @Override
    public String getSurfaceCatTxt(int sc) {
        return splineProfileContextMTB.getSurfaceCatTxt(sc);
    }

    @Override
    public float getWatt0(int sc) {
        return splineProfileContextMTB.getWatt0(sc);
    }

    @Override
    public float getWatt(int sc) {
        return splineProfileContextMTB.getWatt(sc);
    }

    @Override
    public float getMinDistFactSC0() {
        return splineProfileContextMTB.getMinDistFactSC0();
    }
}
