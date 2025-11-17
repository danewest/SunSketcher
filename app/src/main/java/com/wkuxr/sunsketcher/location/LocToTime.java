package com.wkuxr.sunsketcher.location;

public class LocToTime {
//Java Solar Eclipse Calculator
//
// This code is being released under the terms of the GNU General Public
// License (http://www.gnu.org/copyleft/gpl.html).
// The source code this file is based on was created by
// chris@obyrne.com  and  fred.espenak@nasa.gov
// If you would like to use or modify this code, send them,
// as well as travis.peden194@topper.wku.edu, an email of inquiry.
//
// Code obtained from http://eclipse.gsfc.nasa.gov/JSEX/JSEX-index.html
//

/*
Java Solar Eclipse Explorer
Java Version 1 by Travis Peden - 2022.
Javascript Version 1 by Chris O'Byrne and Fred Espenak - 2007.
(based on "Eclipse Calculator" by Chris O'Byrne and Stephen McCann - 2003)

This program (LocToTime.java) is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/

//
// Observer constants -
// (0) North Latitude (radians)
// (1) West Longitude (radians)
// (2) Altitude (metres)
// (3) West time zone (hours)
// (4) rho sin O'
// (5) rho cos O'
// (6) index into the elements array for the eclipse in question
//
// Note that correcting for refraction will involve creating a "virtual" altitude
// for each contact, and hence a different value of rho and O' for each contact!
//

/*public static void main(String[] args){
    //test main to see if everything works properly
    //double lat = 32 + 50.8/60;
    //double lon = -96 + (-0.8/60);
    double lat = 41.62517;
    double lon = -2.24238;
    double alt = 1012;
    //double lat = 42.31943;
    //double lon = 0.28997;
    //double alt = 734;
    long[] data = calculatefor(lat, lon, alt);

    if(data.length == 1) System.out.println(data[0]);
    if(data[0] == Long.MAX_VALUE) System.out.println("Location is outside of path of totality/annularity.");
    else System.out.println("Start of total: " + data[0] + "\nEnd of total:   " + data[1]);
    System.out.println("Start of day:   " + getdate(mid));
}*/

    static double[] obsvconst = new double[7];

    //TODO: Aug. 12, 2026
    public static double[] elements = {2461265.24104, 18.0, -3.0, 3.0, 75.4, 75.4,    //Date, hour of greatest eclipse, delta T
            0.47551399,  0.51892489, -0.00007730, -0.00000804,                        //x
            0.77118301, -0.23016800, -0.00012460,  0.00000377,                        //y
           14.79666996, -0.01206500, -0.00000300,                                     //d
           88.74778748, 15.00308990,  0.00000000,                                     //mu
            0.53795499,  0.00009390, -0.00001210,                                     //l1
           -0.00814200,  0.00009350, -0.00001210,                                     //l2
            0.00461410,  0.00459110};                                                 //tan f1, tan f2

    //updated elements provided by Fred Espenak
//TODO: Apr. 8, 2024
/*public static double[] elements = {2460409.262841, 18.0, -4.0, 4.0, 69.2, 69.2,     //Date, hour of greatest eclipse, delta T
   -0.3182485,    0.5117099,  0.0000326, -0.0000084,                                //x
    0.2197639,    0.2709581, -0.0000594, -0.0000047,                                //y
    7.5861838,    0.0148444, -0.0000017,                                            //d
   89.591230,    15.004082,  -8.380e-07,                                            //mu
    0.5358323,    0.0000618, -1.276e-05,                                            //l1
   -0.0102736,    0.0000615, -1.269e-05,                                            //l2
    0.0046683,    0.0046450};*/                                                       //tan f1, tan f2


//TODO: Oct. 14, 2023
/*public static double[] elements = {2460232.250470, 18.0, -4.0, 4.0, 69.1, 69.1,   //Date, hour of greatest eclipse, delta T
    0.1696573,   0.4585517,  0.0000278, -0.0000054,                                 //x
    0.3348613,  -0.2413663,  0.0000241,  0.0000030,                                 //y
   -8.2441736,  -0.0148882,  0.0000016,                                             //d
   93.501741,   15.003529,  -0.000002,                                              //mu
    0.5643306,  -0.0000891, -0.0000103,                                             //l1
    0.0180827,  -0.0000886, -0.0000103,                                             //l2
    0.0046882,   0.0046648};*/                                                      //tan f1, tan f2

//TODO: Aug. 21, 2017 (for testing the calculator without spoofing, as this eclipse passes the lab)
/*public static double[] elements = {2457987.268521, 18.0, -4.0, 4.0, 70.3, 70.3,
   -0.1295710,   0.5406426, -2.940e-05, -8.100e-06,
    0.4854160,  -0.1416400, -9.050e-05,  2.050e-06,
   11.8669596,  -0.0136220, -2.000e-06,
   89.2454300,  15.0039368,  0.000e-00,
    0.5420930,   0.0001241, -1.180e-05,
   -0.0040250,   0.0001234, -1.170e-05,
    0.0046222,   0.0045992};*/

//
// Eclipse circumstances
//  (0) Event type (C1=-2, C2=-1, Mid=0, C3=1, C4=2)
//  (1) t
// -- time-only dependent circumstances (and their per-hour derivatives) follow --
//  (2) x
//  (3) y
//  (4) d
//  (5) sin d
//  (6) cos d
//  (7) mu
//  (8) l1
//  (9) l2
// (10) dx
// (11) dy
// (12) dd
// (13) dmu
// (14) dl1
// (15) dl2
// -- time and location dependent circumstances follow --
// (16) h
// (17) sin h
// (18) cos h
// (19) xi
// (20) eta
// (21) zeta
// (22) dxi
// (23) deta
// (24) u
// (25) v
// (26) a
// (27) b
// (28) l1'
// (29) l2'
// (30) n^2
// -- observational circumstances follow --
// (31) p
// (32) alt
// (33) q
// (34) v
// (35) azi
// (36) m (mid eclipse only) or limb correction applied (where available!)
// (37) magnitude (mid eclipse only)
// (38) moon/sun (mid eclipse only)
// (39) calculated local event type for a transparent earth (mid eclipse only)
//      (0 = none, 1 = partial, 2 = annular, 3 = total)
// (40) event visibility
//      (0 = above horizon, 1 = below horizon, 2 = sunrise, 3 = sunset, 4 = below horizon, disregard)
//

    static double[] c2 = new double[41];
    static double[] mid = new double[41];
    static double[] c3 = new double[41];

    // Populate the circumstances array with the time-only dependent circumstances (x, y, d, m, ...)
    static double[] timedependent(double[] circumstances) {
        double type;
        int index;
        double t;
        double ans;

        t = circumstances[1];
        index = (int) obsvconst[6];
        // Calculate x
        ans = elements[9 + index] * t + elements[8 + index];
        ans = ans * t + elements[7 + index];
        ans = ans * t + elements[6 + index];
        circumstances[2] = ans;
        // Calculate dx
        ans = 3.0 * elements[9 + index] * t + 2.0 * elements[8 + index];
        ans = ans * t + elements[7 + index];
        circumstances[10] = ans;
        // Calculate y
        ans = elements[13 + index] * t + elements[12 + index];
        ans = ans * t + elements[11 + index];
        ans = ans * t + elements[10 + index];
        circumstances[3] = ans;
        // Calculate dy
        ans = 3.0 * elements[13 + index] * t + 2.0 * elements[12 + index];
        ans = ans * t + elements[11 + index];
        circumstances[11] = ans;
        // Calculate d
        ans = elements[16 + index] * t + elements[15 + index];
        ans = ans * t + elements[14 + index];
        ans = ans * Math.PI / 180.0;
        circumstances[4] = ans;
        // sin d and cos d
        circumstances[5] = Math.sin(ans);
        circumstances[6] = Math.cos(ans);
        // Calculate dd
        ans = 2.0 * elements[16 + index] * t + elements[15 + index];
        ans = ans * Math.PI / 180.0;
        circumstances[12] = ans;
        // Calculate m
        ans = elements[19 + index] * t + elements[18 + index];
        ans = ans * t + elements[17 + index];
        if (ans >= 360.0) {
            ans = ans - 360.0;
        }
        ans = ans * Math.PI / 180.0;
        circumstances[7] = ans;
        // Calculate dm
        ans = 2.0 * elements[19 + index] * t + elements[18 + index];
        ans = ans * Math.PI / 180.0;
        circumstances[13] = ans;
        // Calculate l1 and dl1
        type = circumstances[0];
        if ((type == -2) || (type == 0) || (type == 2)) {
            ans = elements[22 + index] * t + elements[21 + index];
            ans = ans * t + elements[20 + index];
            circumstances[8] = ans;
            circumstances[14] = 2.0 * elements[22 + index] * t + elements[21 + index];
        }
        // Calculate l2 and dl2
        if ((type == -1) || (type == 0) || (type == 1)) {
            ans = elements[25 + index] * t + elements[24 + index];
            ans = ans * t + elements[23 + index];
            circumstances[9] = ans;
            circumstances[15] = 2.0 * elements[25 + index] * t + elements[24 + index];
        }
        return circumstances;
    }

    // Populate the circumstances array with the time and location dependent circumstances
    static double[] timelocdependent(double[] circumstances) {
        //var ans;
        int index;
        double type;

        timedependent(circumstances);
        index = (int) obsvconst[6];
        // Calculate h, sin h, cos h
        circumstances[16] = circumstances[7] - obsvconst[1] - (elements[index + 5] / 13713.44);
        circumstances[17] = Math.sin(circumstances[16]);
        circumstances[18] = Math.cos(circumstances[16]);
        // Calculate xi
        circumstances[19] = obsvconst[5] * circumstances[17];
        // Calculate eta
        circumstances[20] = obsvconst[4] * circumstances[6] - obsvconst[5] * circumstances[18] * circumstances[5];
        // Calculate zeta
        circumstances[21] = obsvconst[4] * circumstances[5] + obsvconst[5] * circumstances[18] * circumstances[6];
        // Calculate dxi
        circumstances[22] = circumstances[13] * obsvconst[5] * circumstances[18];
        // Calculate deta
        circumstances[23] = circumstances[13] * circumstances[19] * circumstances[5] - circumstances[21] * circumstances[12];
        // Calculate u
        circumstances[24] = circumstances[2] - circumstances[19];
        // Calculate v
        circumstances[25] = circumstances[3] - circumstances[20];
        // Calculate a
        circumstances[26] = circumstances[10] - circumstances[22];
        // Calculate b
        circumstances[27] = circumstances[11] - circumstances[23];
        // Calculate l1'
        type = circumstances[0];
        if ((type == -2) || (type == 0) || (type == 2)) {
            circumstances[28] = circumstances[8] - circumstances[21] * elements[26 + index];
        }
        // Calculate l2'
        if ((type == -1) || (type == 0) || (type == 1)) {
            circumstances[29] = circumstances[9] - circumstances[21] * elements[27 + index];
        }
        // Calculate n^2
        circumstances[30] = circumstances[26] * circumstances[26] + circumstances[27] * circumstances[27];
        return circumstances;
    }

    // Iterate on C2 or C3
    static double[] c2c3iterate(double[] circumstances) {
        double sign;
        int iter;
        double tmp;
        double n;

        timelocdependent(circumstances);
        if (circumstances[0] < 0) {
            sign = -1.0;
        } else {
            sign = 1.0;
        }
        if (mid[29] < 0.0) {
            sign = -sign;
        }
        tmp = 1.0;
        iter = 0;
        while (((tmp > 0.000001) || (tmp < -0.000001)) && (iter < 50)) {
            n = Math.sqrt(circumstances[30]);
            tmp = circumstances[26] * circumstances[25] - circumstances[24] * circumstances[27];
            tmp = tmp / n / circumstances[29];
            tmp = sign * Math.sqrt(1.0 - tmp * tmp) * circumstances[29] / n;
            tmp = (circumstances[24] * circumstances[26] + circumstances[25] * circumstances[27]) / circumstances[30] - tmp;
            circumstances[1] = circumstances[1] - tmp;
            timelocdependent(circumstances);
            iter++;
        }
        return circumstances;
    }

    // Get C2 and C3 data
    //   Entry conditions -
    //   1. The mid array must be populated
    //   2. There must be either a total or annular eclipse at the location!
    static void getc2c3() {
        double tmp;
        double n;

        n = Math.sqrt(mid[30]);
        tmp = mid[26] * mid[25] - mid[24] * mid[27];
        tmp = tmp / n / mid[29];
        tmp = Math.sqrt(1.0 - tmp * tmp) * mid[29] / n;
        c2[0] = -1;
        c3[0] = 1;
        if (mid[29] < 0.0) {
            c2[1] = mid[1] + tmp;
            c3[1] = mid[1] - tmp;
        } else {
            c2[1] = mid[1] - tmp;
            c3[1] = mid[1] + tmp;
        }
        c2c3iterate(c2);
        c2c3iterate(c3);
    }

    // Get the observational circumstances
    static void observational(double[] circumstances) {
        double contacttype;
        double coslat;
        double sinlat;

        // We are looking at an "external" contact UNLESS this is a total eclipse AND we are looking at
        // c2 or c3, in which case it is an INTERNAL contact! Note that if we are looking at mid eclipse,
        // then we may not have determined the type of eclipse (mid[39]) just yet!
        if (circumstances[0] == 0) {
            contacttype = 1.0;
        } else {
            if ((mid[39] == 3) && ((circumstances[0] == -1) || (circumstances[0] == 1))) {
                contacttype = -1.0;
            } else {
                contacttype = 1.0;
            }
        }
        // Calculate p
        circumstances[31] = Math.atan2(contacttype * circumstances[24], contacttype * circumstances[25]);
        // Calculate alt
        sinlat = Math.sin(obsvconst[0]);
        coslat = Math.cos(obsvconst[0]);
        circumstances[32] = Math.asin(circumstances[5] * sinlat + circumstances[6] * coslat * circumstances[18]);
        // Calculate q
        circumstances[33] = Math.asin(coslat * circumstances[17] / Math.cos(circumstances[32]));
        if (circumstances[20] < 0.0) {
            circumstances[33] = Math.PI - circumstances[33];
        }
        // Calculate v
        circumstances[34] = circumstances[31] - circumstances[33];
        // Calculate azi
        circumstances[35] = Math.atan2(-1.0 * circumstances[17] * circumstances[6], circumstances[5] * coslat - circumstances[18] * sinlat * circumstances[6]);
        // Calculate visibility
        if (circumstances[32] > -0.00524) {
            circumstances[40] = 0;
        } else {
            circumstances[40] = 1;
        }
    }

    // Get the observational circumstances for mid eclipse
    static void midobservational() {
        observational(mid);
        // Calculate m, magnitude and moon/sun
        mid[36] = Math.sqrt(mid[24] * mid[24] + mid[25] * mid[25]);
        mid[37] = (mid[28] - mid[36]) / (mid[28] + mid[29]);
        mid[38] = (mid[28] - mid[29]) / (mid[28] + mid[29]);
    }

    // Calculate mid eclipse
    static void getmid() {
        int iter;
        double tmp;

        mid[0] = 0;
        mid[1] = 0.0;
        iter = 0;
        tmp = 1.0;
        timelocdependent(mid);

        while (((tmp > 0.000001) || (tmp < -0.000001)) && (iter < 50)) {
            tmp = (mid[24] * mid[26] + mid[25] * mid[27]) / mid[30];
            mid[1] = mid[1] - tmp;
            iter++;
            timelocdependent(mid);
        }
    }

    static void getall() {
        getmid();
        midobservational();
        if (mid[37] > 0.0) {
            if ((mid[36] < mid[29]) || (mid[36] < -mid[29])) {
                getc2c3();
                if (mid[29] < 0.0) {
                    mid[39] = 3; // Total eclipse
                } else {
                    mid[39] = 2; // Annular eclipse
                }
                observational(c2);
                observational(c3);

                c2[36] = 999.9; //these can be used for limb correction if available, currently unusued
                c3[36] = 999.9;
            } else {
                mid[39] = 1; // Partial eclipse
            }
        } else {
            mid[39] = 0; // No eclipse
        }
    }

    // Read the data that's in the form, and populate the obsvconst array
    static void calcObsv(double lat, double lon, double alt) {
        double tmp;

        //latitude
        obsvconst[0] = lat * Math.PI / 180.0;

        //longitude
        obsvconst[1] = lon * Math.PI / 180.0 * -1;

        //altitude
        obsvconst[2] = alt;

        //timezone is always 0 for these calculations, will convert after
        obsvconst[3] = 0;

        // Get the observer's geocentric position
        tmp = Math.atan(0.99664719 * Math.tan(obsvconst[0]));
        obsvconst[4] = 0.99664719 * Math.sin(tmp) + (obsvconst[2] / 6378140.0) * Math.sin(obsvconst[0]);
        obsvconst[5] = Math.cos(tmp) + (obsvconst[2] / 6378140.0 * Math.cos(obsvconst[0]));

        //the original code had a list of all besellian elements for a large number of eclipses, but this app currently only needs the elements for the April 8 2024 eclipse, so we set it to 0
        obsvconst[6] = 0;

    }

    // Get the UTC 00:00 timestamp for the day of the eclipse
    static long getdate(double[] circumstances) {
        int index = (int) obsvconst[6];
        // Calculate the JD for noon (TDT) the day before the day that contains T0
        double jd = Math.floor(elements[index] - (elements[1 + index] / 24.0)) + 0.5;
        return (long) (86400.0 * (jd - 2440587.5)) * 1000;
    }

    //
    // Get the local time of an event
    static long gettime(double[] circumstances) {
        int index;

        index = (int) obsvconst[6];
        double t = circumstances[1] + elements[1 + index] - obsvconst[3] - (elements[4 + index] - 0.5) / 3600.0;
        if (t < 0.0) {
            t = t + 24.0;
        }
        if (t >= 24.0) {
            t = t - 24.0;
        }

        return (long) Math.floor(t * 3600 * 1000);
    }

    //given a latitude, longitude, and altitude, calculate the times of contact 2 and 3 of a total/annular eclipse.
    //If the location is outside of the eclipse path, returns Long.MAX_VALUE which can be handled by calling methods.
    public static long[] calculatefor(double lat, double lon, double alt) {
        long[] info = new long[2];

        calcObsv(lat, lon, alt);
        //calcObsv(25.122, -104.2252, alt);

        getall();

        if (mid[39] > 1) {
            info[0] = gettime(c2) + getdate(mid);
            info[1] = gettime(c3) + getdate(mid);
        } else return new long[]{Long.MAX_VALUE};
        return info;
    }
}