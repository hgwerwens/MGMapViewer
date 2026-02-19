package mg.mgmap.activity.mgmap.features.routing.profile;

public interface IfSpline {

    float valueAt(float x);
    float derivativeAt(float x);
    float secondDerivativeAt(float x);

}
