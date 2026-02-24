package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

public interface IfIsFuncWithDeriv {
    float valueAt(float x);
    float derivativeAt(float x);
    float secondDerivativeAt(float x);
}
