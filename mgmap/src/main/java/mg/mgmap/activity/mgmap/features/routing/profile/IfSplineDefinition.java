package mg.mgmap.activity.mgmap.features.routing.profile;

public interface IfSplineDefinition {
    float[] x();
    float[] y();

    default BoundaryCondition leftBoundary() {
        return naturalLeft();
    }

    default BoundaryCondition rightBoundary() {
        return naturalRight();
    }

    @FunctionalInterface
    interface BoundaryCondition {
        void apply(float[] h, float[] y, float[] mu, float[] z, float[] c);
    }
    // Natural left
    static BoundaryCondition naturalLeft() {
        return (h, y, mu, z, c) -> {
            mu[0] = 0f;
            z[0] = 0f;
        };
    }

    // Natural right
    static BoundaryCondition naturalRight() {
        return (h, y, mu, z, c) -> {
            int n = h.length;
            z[n] = 0f;
            c[n] = 0f;
        };
    }

    // Clamped left
    static BoundaryCondition clampedLeft(float m0) {
        return (h, y, mu, z, c) -> {
            float l = 2 * h[0];
            mu[0] = 0.5f;
            z[0] = 3 * ((y[1] - y[0]) / h[0] - m0) / l;
        };
    }

    // Clamped right
    static BoundaryCondition clampedRight(float mn) {
        return (h, y, mu, z, c) -> {
            int n = h.length;
            float l = h[n-1] * (2f - mu[n-1]);
            z[n] = (3f * (mn - (y[n] - y[n-1]) / h[n-1]) - h[n-1] * z[n-1]) / l;
            c[n] = z[n];
        };
    }

    // Curvature left
    static BoundaryCondition curvatureLeft(float k0) {
        return (h, y, mu, z, c) -> {
            mu[0] = 0f;
            z[0] = k0;
            c[0] = k0;
        };
    }

    // Curvature right
    static BoundaryCondition curvatureRight(float kn) {
        return (h, y, mu, z, c) -> {
            int n = h.length;
            z[n] = kn;
            c[n] = kn;
        };
    }

}
