package mg.mgmap.activity.mgmap.features.routing.profile;

public interface IfSplineDef {
    float[] x();
    float[] y();

    LeftBoundaryCondition leftBoundary();

    RightBoundaryCondition rightBoundary();



    record Impl(
            float[] x,
            float[] y,
            LeftBoundaryCondition left,
            RightBoundaryCondition right
    ) implements IfSplineDef {
        @Override
        public LeftBoundaryCondition leftBoundary() {
            return left;
        }
        @Override
        public RightBoundaryCondition rightBoundary() {
            return right;
        }

        public Impl(float[] x, float[] y) {
            this(x, y, naturalLeft(), naturalRight());
        }
    }

    interface Delegating extends IfSplineDef {
        IfSplineDef base();

        @Override
        default float[] x() { return base().x(); }

        @Override
        default float[] y() { return base().y(); }

        @Override
        default LeftBoundaryCondition leftBoundary() { return base().leftBoundary(); }

        @Override
        default RightBoundaryCondition rightBoundary() { return base().rightBoundary(); }
    }



    @FunctionalInterface
    interface LeftBoundaryCondition {
        void apply(float[] h, float[] y, float[] mu, float[] z);
    }

    @FunctionalInterface
    interface RightBoundaryCondition {
        void apply(float[] h, float[] y, float[] mu, float[] z);
    }

    // Natural left
    static LeftBoundaryCondition naturalLeft() {
        return (h, y, mu, z) -> {
            mu[0] = 0f;
            z[0] = 0f;
        };
    }

    // Natural right
    static RightBoundaryCondition naturalRight() {
        return (h, y, mu, z) -> {
            int n = h.length;
            z[n] = 0f;
        };
    }

    // Clamped left
    static LeftBoundaryCondition clampedLeft(float m0) {
        return (h, y, mu, z) -> {
            float l = 2 * h[0];
            mu[0] = 0.5f;
            z[0] = 3 * ((y[1] - y[0]) / h[0] - m0) / l;
        };
    }

    // Clamped right
    static RightBoundaryCondition clampedRight(float mn) {
        return (h, y, mu, z) -> {
            int n = h.length;
            float l = h[n-1] * (2f - mu[n-1]);
            z[n] = (3f * (mn - (y[n] - y[n-1]) / h[n-1]) - h[n-1] * z[n-1]) / l;
        };
    }

    // Curvature left
    static LeftBoundaryCondition curvatureLeft(float k0) {
        return (h, y, mu, z) -> {
            mu[0] = 0f;
            z[0] = k0;
        };
    }

    // Curvature right
    static RightBoundaryCondition curvatureRight(float kn) {
        return (h, y, mu, z) -> {
            int n = h.length;
            z[n] = kn;
        };
    }

}
