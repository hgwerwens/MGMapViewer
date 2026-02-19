package mg.mgmap.activity.mgmap.features.routing.profile;

public interface IfSplineLinearExtDef {
    float leftX();
    float leftSlope();

    float rightX();
    float rightSlope();

    public record Impl(
            IfSplineDef base,
            float leftX,
            float leftSlope,
            float rightX,
            float rightSlope
    ) implements IfSplineDef.Delegating, IfSplineLinearExtDef {}

}
