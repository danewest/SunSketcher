package com.wkuxr.eclipsetotality.location;

//this is being translated from JS to Java; Source is https://gml.noaa.gov/grad/solcalc/sunrise.html (I have a backup of the JS if site goes down for some reason)

import java.util.TimeZone;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Sunset {
    public static long calcSun(double lat, double lon){
        int timezoneDiff = -timeDiff();
        int[] date = getDate();

        int JD = calcJD(date);

        //daylight saving time boolean, assumed to be true for this test since it's being tested during daylight saving time so doesn't matter. also doesn't matter for actual eclipse because both October 14 and April 8 are in daylight saving time; false would be 0 though
        int daySaving = 60;

        double newjd = findRecentSunset(JD, lat, lon);
        double newtime = calcSunsetUTC(newjd, lat, lon) - (60 * timezoneDiff) + daySaving;
        if (newtime > 1440){
            newtime -= 1440;
            newjd += 1.0;
        }
        if (newtime < 0){
            newtime += 1440;
            newjd -= 1.0;
        }
        long sunset = timeUnixMilDate(newtime, newjd);

        return sunset;
    }

    static int[] getDate(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime now = LocalDateTime.now();
        String date = dtf.format(now);
        String[] dateArr = date.split("/");
        int[] dateVals = new int[3];
        for(int i = 0; i < 3; i++){
            dateVals[i] = Integer.parseInt(dateArr[i]);
        }
        return dateVals;
    }

    static int calcJD(int[] date){
        if(date[1] <= 2){
            date[0] -= 1;
            date[1] += 12;
        }

        int A = (int)Math.floor(date[0]/100);
        int B = 2 - A + (int)Math.floor(A/4);

        int JD = (int)(Math.floor(365.25 * (date[0] + 4716)) + Math.floor(30.6001 * (date[1] + 1)) + date[2] + B - 1524.5);

        return JD;
    }

    static double calcTimeJulianCent(double jd){
        return (jd - 2451545.0) / 36525.0;
    }


    static double calcObliquityCorrection(double t){
        double e0 = calcMeanObliquityOfEcliptic(t);

        double omega = 125.04 - 1934.136 * t;
        return e0 + 0.00256 * Math.cos((Math.PI / 180) * omega);
    }

    static double calcMeanObliquityOfEcliptic(double t){
        double seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * (0.001813)));
        return 23.0 + (26.0 + (seconds / 60.0)) / 60.0;
    }

    static double calcSunApparentLong(double t){
        double o = calcSunTrueLong(t);
        double omega = 125.04 - 1934.136 * t;
        return o - 0.00569 - 0.00478 * Math.sin((Math.PI / 180) * omega);
    }

    static double calcSunTrueLong(double t){
        double l0 = calcGeomMeanLongSun(t);
        double c = calcSunEqOfCenter(t);
        return l0 + c;
    }

    static double calcGeomMeanLongSun(double t){
        double l0 = 280.46646 + t * (36000.76983 + 0.0003032 * t);
        while(l0 > 360.0){
            l0 -= 360.0;
        }
        while(l0 < 0.0){
            l0 += 360.0;
        }
        return l0;
    }

    static double calcSunEqOfCenter(double t){
        double m =  calcGeomMeanAnomalySun(t);
        double mrad = m * (Math.PI / 180);
        double sinm = Math.sin(mrad);
        double sin2m = Math.sin(mrad * 2);
        double sin3m = Math.sin(mrad * 3);
        return sinm * (1.914602 - t * (0.000014 * t)) + sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289;
    }

    static double calcGeomMeanAnomalySun(double t){
        return 357.52911 + t * (35999.05029 - 0.0001537 * t);
    }

    static double calcSunDeclination(double t){
        double e = calcObliquityCorrection(t);
        double lambda = calcSunApparentLong(t);
        double sint = Math.sin((Math.PI / 180) * e) * Math.sin((Math.PI / 180) * lambda);
        return Math.toDegrees(Math.asin(sint));
    }

    static double calcEquationOfTime(double t){
        double epsilon = calcObliquityCorrection(t);
        double l0 = calcGeomMeanLongSun(t);
        double e = calcEccentricityEarthOrbit(t);
        double m = calcGeomMeanAnomalySun(t);

        double y = Math.tan(Math.toRadians(epsilon) / 2.0);
        y *= y;

        double sin2l0 = Math.sin(2.0 * Math.toRadians(l0));
        double sinm = Math.sin(Math.toRadians(m));
        double cos2l0 = Math.cos(2.0 * Math.toRadians(l0));
        double sin4l0 = Math.sin(4.0 * Math.toRadians(l0));
        double sin2m = Math.sin(2.0 * Math.toRadians(m));

        return Math.toDegrees(y * sin2l0 - 2.0 * e * sinm + 4.0 * e * y * sinm * cos2l0 - 0.5 * y * y * sin4l0 - 1.25 * e * e * sin2m) * 4.0;
    }

    static double calcEccentricityEarthOrbit(double t){
        return 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
    }


    static double calcSolNoonUTC(double t, double longitude){
        double tnoon = calcTimeJulianCent(calcJDFromJulianCent(t) + longitude / 360.0);
        double eqTime = calcEquationOfTime(tnoon);
        double solNoonUTC = 720 + (longitude * 4) - eqTime;

        double newt = calcTimeJulianCent(calcJDFromJulianCent(t) - 0.5 + solNoonUTC / 1440.0);

        eqTime = calcEquationOfTime(newt);
        solNoonUTC = 720 + (longitude * 4) - eqTime;

        return solNoonUTC;
    }


    static double calcJDFromJulianCent(double t){
        return t * 36525.0 + 2451545.0;
    }

    static double calcSunsetUTC(double JD, double latitude, double longitude){
        double t = calcTimeJulianCent(JD);

        double noonmin = calcSolNoonUTC(t, longitude);
        double tnoon = calcTimeJulianCent(JD + noonmin / 1440.0);

        double eqTime = calcEquationOfTime(tnoon);
        double solarDec = calcSunDeclination(tnoon);
        double hourAngle = calcHourAngleSunset(latitude, solarDec);

        double delta = longitude - Math.toRadians(hourAngle);
        double timeDiff = 4 * delta;
        double timeUTC = 720 + timeDiff - eqTime;

        double newt = calcTimeJulianCent(calcJDFromJulianCent(t) + timeUTC / 1440.0);
        eqTime = calcEquationOfTime(newt);
        solarDec = calcSunDeclination(newt);
        hourAngle = calcHourAngleSunset(latitude, solarDec);

        delta = longitude - Math.toDegrees(hourAngle);
        timeDiff = 4 * delta;
        timeUTC = 720 + timeDiff - eqTime;

        return timeUTC;
    }

    static double calcHourAngleSunset(double lat, double solarDec){
        double latRad = Math.toRadians(lat);
        double sdRad = Math.toRadians(solarDec);

        double HA = (Math.acos(Math.cos(Math.toRadians(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad)));

        return -HA;
    }


    static double findRecentSunset(double jd, double latitude, double longitude){
        double julianday = jd;
        return julianday;
    }

    static long timeUnixMilDate(double minutes, double jd){
        double floatHour = minutes / 60.0;
        double hour = Math.floor(floatHour);
        double floatMinute = 60.0 * (floatHour - hour);
        double minute = Math.floor(floatMinute);
        double floatSec = 60.0 * (floatMinute - minute);
        double second = Math.floor(floatSec + 0.5);

        long timeUnix = convertTimes(new double[]{hour, minute, second});

        return timeUnix;
    }

    //the calcSun function uses CDT as its base timezone, so we need to separately determine the difference in hours between CDT and the user's timezone
    static long convertTimes(double[] start){

        //we don't need to worry about standard timezones, since the actual eclipse is on 4/8, during daylight savings
        int timeDiff = 0;
        switch(TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT)){
            case "HST-10:00":
                timeDiff = -6;
                break;
            case "AKDT-8:00":
                timeDiff = -4;
                break;
            case "PDT-7:00":
                timeDiff = -3;
                break;
            case "MDT-6:00":
                timeDiff = -2;
                break;
            case "CDT-5:00":
                timeDiff = -1;
                break;
            case "EDT-4:00":
                timeDiff = 0;
                break;
            default:
                timeDiff = -1;
        }

        //get current unix time, mod to get current unix date, add calculated time as unix time to get unix time of sunset
        long current = System.currentTimeMillis() / 1000;
        long timeUnix = (current - (current % (60 * 60 * 24))) + (((long)start[0] + timeDiff) * 3600L) + ((long)start[1] * 60L) + (long)start[2];

        return timeUnix;
    }


    static int timeDiff(){
        int hours;
        switch(TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT)){
            case "HST-10:00" -> hours = -5;
            case "AKDT-8:00" -> hours = -3;
            case "PDT-7:00" -> hours = -2;
            case "MDT-6:00" -> hours = -1;
            case "CDT-5:00" -> hours = 0;
            case "EDT-4:00" -> hours = 1;
            default -> hours = 0;
        }
        return hours;
    }
}