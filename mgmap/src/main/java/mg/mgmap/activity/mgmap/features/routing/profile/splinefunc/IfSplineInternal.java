package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

interface IfSplineInternal {
    IfSplineDef getSplineDef();
    float[] getX();
    enum SplineCalcAt {Left,Right}
    float derivativeAt(float x, SplineCalcAt calcAt);
    float secondDerivativeAt(float x, SplineCalcAt calcAt);
}

