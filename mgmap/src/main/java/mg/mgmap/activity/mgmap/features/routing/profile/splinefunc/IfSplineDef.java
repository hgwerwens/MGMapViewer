package mg.mgmap.activity.mgmap.features.routing.profile.splinefunc;

import androidx.annotation.NonNull;

import java.util.Locale;

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
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%10s", SplineType.NATURAL);};
    }

    public record Clamped( float slopeLeft, float slopeRight) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.CLAMPED;
        }
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%10s slopeLeft=%.2f slopeRight=%.2f", SplineType.CLAMPED, slopeLeft,slopeRight);};
    }

    public record Composite( int leftLinearSegments, int rightLinearSegments) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.COMPOSITE;
        }
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%s segmentsLeft=%d segmentsRight=%d", SplineType.CLAMPED, leftLinearSegments,rightLinearSegments);};
    }
    public record Linear(
    ) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.LINEAR;
        }
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%10s", SplineType.LINEAR);};
    }
}
