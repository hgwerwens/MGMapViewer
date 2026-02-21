package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

public interface IfSplineDef {
    enum SplineType {
        NATURAL,
        CLAMPED,
        COMPOSITE,
        LINEAR,
    }
    SplineType type();

    public record Natural(
    ) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.NATURAL;
        }
    }

    public record Clamped( float slopeLeft, float slopeRight) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.CLAMPED;
        }
    }

    public record Composite( int leftLinearSegments, int rightLinearSegments) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.COMPOSITE;
        }
    }
    public record Linear(
    ) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.LINEAR;
        }
    }
}
