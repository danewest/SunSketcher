package com.wkuxr.eclipsetotality.Location;

public class Besselian {
    double[] calculate(double t1){
        double At = 74;
        double t = t1 - 18 + At/3600;
        double[][] e = {{-.3182440,0.5117116,0.0000326,-0.0000084},{0.2197640,0.2709589,-0.0000595,-0.0000047},{7.5862002,0.0148440,-0.0000020,0.0000000},{89.591217,15.004080,0,0}};

        double x = e[0][0] + e[0][1] * t + e[0][2] * Math.pow(t,2) + e[0][3] * Math.pow(t,3);
        double y = e[1][0] + e[1][1] * t + e[1][2] * Math.pow(t,2) + e[1][3] * Math.pow(t,3);
        double d = e[2][0] + e[2][1] * t + e[2][2] * Math.pow(t,2) + e[2][3] * Math.pow(t,3);
        double u = e[3][0] + e[3][1] * t + e[3][2] * Math.pow(t,2) + e[3][3] * Math.pow(t,3);

        double f = 1 / 298.257223563;
        double b = 1 - f;
        double sin_d = Math.sin(d * Math.PI / 180);
        double cos_d = Math.cos(d * Math.PI / 180);
        double y0_r = y * cos_d;
        double z0_r = -y * sin_d;
        double y1_r = y0_r + sin_d;
        double z1_r = z0_r + cos_d;
        double v = y1_r - y0_r;
        double w = z1_r - z0_r;
        double a_quadratic = Math.pow(v,2) + Math.pow(b,2) * Math.pow(w,2);
        double b_quadratic = 2 * z0_r * w * Math.pow(b,2) + 2 * y0_r * v;
        double c_quadratic = Math.pow(z0_r,2) * Math.pow(b,2) + Math.pow(y0_r,2) + Math.pow(b,2) * (Math.pow(x,2) - 1);
        double p = (-b_quadratic + Math.sqrt(Math.pow(b_quadratic,2) - 4 * a_quadratic * c_quadratic)) / (2 * a_quadratic);
        double y_i = y0_r + v * p;
        double z_i = z0_r + w * p;
        double dist_axis = Math.sqrt(Math.pow(x,2) + Math.pow(z_i,2));
        double lat = Math.atan(y_i / dist_axis * (Math.pow(1 - f,2))) * 180 / Math.PI;
        double lon = Math.atan2(x, z_i) * 180 / Math.PI - u + 1.002738 * At / 240;

        return new double[]{lat,lon};
    }
}
