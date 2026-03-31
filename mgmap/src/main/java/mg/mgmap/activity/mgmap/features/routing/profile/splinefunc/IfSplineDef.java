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

    record Natural(
    ) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.NATURAL;
        }
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%10s", SplineType.NATURAL);};
    }

    record Clamped( float slopeLeft, float slopeRight) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.CLAMPED;
        }
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%10s slopeLeft=%.2f slopeRight=%.2f", SplineType.CLAMPED, slopeLeft,slopeRight);};
    }

    record Composite( int leftLinearSegments, int rightLinearSegments, IfSplineDef innerSpline) implements IfSplineDef {
        public Composite(int leftLinearSegments, int rightLinearSegments){
            this(leftLinearSegments, rightLinearSegments,null);
        }
        @Override
        public SplineType type() {
            return SplineType.COMPOSITE;
        }
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%s segmentsLeft=%d segmentsRight=%d", SplineType.COMPOSITE, leftLinearSegments,rightLinearSegments);};
    }
    record Linear(
    ) implements IfSplineDef {
        @Override
        public SplineType type() {
            return SplineType.LINEAR;
        }
        @NonNull
        public String toString(){return String.format(Locale.ENGLISH,"%10s", SplineType.LINEAR);};
    }
}
