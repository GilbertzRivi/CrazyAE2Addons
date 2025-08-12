package net.oktawia.crazyae2addons.renderer;

public class LabGradient {
    private static double[] rgbToXyz(float r, float g, float b) {
        double R = r;
        double G = g;
        double B = b;

        R = (R > 0.04045) ? Math.pow((R + 0.055) / 1.055, 2.4) : (R / 12.92);
        G = (G > 0.04045) ? Math.pow((G + 0.055) / 1.055, 2.4) : (G / 12.92);
        B = (B > 0.04045) ? Math.pow((B + 0.055) / 1.055, 2.4) : (B / 12.92);

        double X = R * 0.4124564 + G * 0.3575761 + B * 0.1804375;
        double Y = R * 0.2126729 + G * 0.7151522 + B * 0.0721750;
        double Z = R * 0.0193339 + G * 0.1191920 + B * 0.9503041;

        return new double[]{X, Y, Z};
    }

    private static double[] xyzToLab(double X, double Y, double Z) {
        double Xr = 0.95047;
        double Yr = 1.00000;
        double Zr = 1.08883;

        double x = X / Xr;
        double y = Y / Yr;
        double z = Z / Zr;

        x = fxyz(x);
        y = fxyz(y);
        z = fxyz(z);

        double L = 116 * y - 16;
        double a = 500 * (x - y);
        double b = 200 * (y - z);

        return new double[]{L, a, b};
    }

    private static double fxyz(double t) {
        double delta = 6.0 / 29.0;
        if (t > Math.pow(delta, 3)) {
            return Math.cbrt(t);
        } else {
            return t / (3 * delta * delta) + 4.0 / 29.0;
        }
    }

    private static double[] labToXyz(double L, double a, double b) {
        double y = (L + 16) / 116.0;
        double x = a / 500.0 + y;
        double z = y - b / 200.0;

        double x3 = Math.pow(x, 3);
        double y3 = Math.pow(y, 3);
        double z3 = Math.pow(z, 3);

        double delta = 6.0 / 29.0;

        double xr = (x3 > Math.pow(delta, 3)) ? x3 : (x - 4.0 / 29.0) * 3 * delta * delta;
        double yr = (L > (delta * 903.3)) ? y3 : L / 903.3;
        double zr = (z3 > Math.pow(delta, 3)) ? z3 : (z - 4.0 / 29.0) * 3 * delta * delta;

        double Xr = 0.95047;
        double Yr = 1.00000;
        double Zr = 1.08883;

        double X = xr * Xr;
        double Y = yr * Yr;
        double Z = zr * Zr;

        return new double[]{X, Y, Z};
    }

    private static float[] xyzToRgb(double X, double Y, double Z) {
        double R = X * 3.2406 + Y * (-1.5372) + Z * (-0.4986);
        double G = X * (-0.9689) + Y * 1.8758 + Z * 0.0415;
        double B = X * 0.0557 + Y * (-0.2040) + Z * 1.0570;

        R = (R > 0.0031308) ? (1.055 * Math.pow(R, 1.0 / 2.4) - 0.055) : (12.92 * R);
        G = (G > 0.0031308) ? (1.055 * Math.pow(G, 1.0 / 2.4) - 0.055) : (12.92 * G);
        B = (B > 0.0031308) ? (1.055 * Math.pow(B, 1.0 / 2.4) - 0.055) : (12.92 * B);

        float r = (float) Math.min(Math.max(R, 0), 1);
        float g = (float) Math.min(Math.max(G, 0), 1);
        float b = (float) Math.min(Math.max(B, 0), 1);

        return new float[]{r, g, b};
    }

    public static float[] labGradient(float[] rgb1, float[] rgb2, double t) {
        double[] xyz1 = rgbToXyz(rgb1[0], rgb1[1], rgb1[2]);
        double[] lab1 = xyzToLab(xyz1[0], xyz1[1], xyz1[2]);
        double[] xyz2 = rgbToXyz(rgb2[0], rgb2[1], rgb2[2]);
        double[] lab2 = xyzToLab(xyz2[0], xyz2[1], xyz2[2]);

        double L = lab1[0] + t * (lab2[0] - lab1[0]);
        double a = lab1[1] + t * (lab2[1] - lab1[1]);
        double b = lab1[2] + t * (lab2[2] - lab1[2]);

        double[] xyz = labToXyz(L, a, b);
        return xyzToRgb(xyz[0], xyz[1], xyz[2]);
    }
}
