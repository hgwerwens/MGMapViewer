package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

public interface IfSpline extends IfIsFuncWithDeriv {
    IfSpline transformY(float scale, float translate);

}
