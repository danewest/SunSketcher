package com.wkuxr.eclipsetotality.Location;

//this is being translated from JS to Java; Source is https://gml.noaa.gov/grad/solcalc/sunrise.html (I have a backup of the JS if site goes down for some reason)

import java.util.TimeZone;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Sunset {
    public long calcSun(double lat, double lon){
        int timezoneDiff = 5 - timeDiff();
        int[] date = getDate();
        
        //TODO: rename to month, using the same variable names as NOAA for now to make it easy to translate
        int indexRS = date[1];
        
        int JD = calcJD(date);
        String dow = calcDayOfWeek(JD);
        int doy = calcDayOfYear(date);
        double T = calcTimeJulianCent(JD);
        
        double alpha = calcSunRtAscension(T);
        double theta = calcSunDeclination(T);
        double Etime = calcEquationOfTime(T);
        
        double eqTime = Etime;
        double solarDec = theta;
        
        var riseTimeGMT = calcSunriseUTC(JD, lat, lon);
        var setTimeGMT = calcSunsetUTC(JD, lat, lon);
        
        //daylight saving time boolean, assumed to be true for this test since it's being tested during daylight saving time so doesn't matter. also doesn't matter for actual eclipse because both October 14 and April 8 are in daylight saving time; false would be 0 though
        int daySaving = 60;
        
        var solNoonGMT = calcSolNoonUTC(T, lon);
        var solNoonLST = solNoonGMT - (60 * timezoneDiff) + daySaving;
        
        var solnStr = timeString(solNoonLST);
        var utcSolnStr = timeString(solNoonGMT);
        
        var tsnoon = calcTimeeJulianCent(calcJDFromJulianCent(T) - 0.5 + solNoonGMT / 1440.0);
        
        eqTime = calcEquationOfTime(tsnoon);
        solarDec = calcSunDeclination(tsnoon);
        
        //copied stuff from JS, fix later
        if ( ((lat > 66.4) && ((doy < 83) || (doy > 263))) || ((lat < -66.4) && (doy > 79) && (doy < 267))){
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
            riseSetForm["sunset"].value = timeStringAMPMDate(newtime, newjd);
            riseSetForm["utcsunset"].value = "prior sunset";
            riseSetForm["solnoon"].value = "N/A";
            riseSetForm["utcsolnoon"].value = "";
        }

        
        return 0;
    }
    
    //the calcSun function uses CDT as its base timezone, so we need to separately determine the difference in hours between CDT and the user's timezone
    int timeDiff(){
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
    
    int[] getDate(){
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
    
    int calcJD(int[] date){
        if(date[1] <= 2){
            date[0] -= 1;
            date[1] += 12;
        }
        
        int A = (int)Math.floor(date[0]/100);
        int B = 2 - A + (int)Math.floor(A/4);
        
        int JD = (int)(Math.floor(365.25 * (date[0] + 4716)) + Math.floor(30.6001 * (date[1] + 1)) + date[2] + B - 1524.5);
        
        return JD;
    }
    
    String calcDayOfWeek(int jd){
        int A = (int)(jd + 1.5) % 7;
        return switch(A){
            case 0 -> "Sunday";
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            default -> "Saturday";
        };
    }
    
    int calcDayOfYear(int[] date){
        int k = (isLeapYear(date[0]) ? 1 : 2);
        return (int)(Math.floor((275 * date[1]) / 9) - k * Math.floor((date[1] + 9) / 12) + date[2] - 30);
    }
    
    boolean isLeapYear(int year){
        return ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0);
    }
    
    double calcTimeJulianCent(double jd){
        return (jd - 2451545.0) / 36525.0;
    }
    
    double calcSunRtAscension(double t){
        double e = calcObliquityCorrection(t);
        double lambda = calcSunApparentLong(t);
        
        double tananum = (Math.cos((e * Math.PI) / 180) * Math.sin((lambda * Math.PI) / 180));
        double tanadenom = (Math.cos((lambda * Math.PI) / 180));
        return (180 / Math.PI) * Math.atan2(tananum, tanadenom);
    }
    
    double calcObliquityCorrection(double t){
        double e0 = calcMeanObliquityOfEcliptic(t);
        
        double omega = 125.04 - 1934.136 * t;
        return e0 + 0.00256 * Math.cos((Math.PI / 180) * omega);
    }
    
    double calcMeanObliquityOfEcliptic(double t){
        double seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * (0.001813)));
        return 23.0 + (26.0 + (seconds / 60.0)) / 60.0;
    }
    
    double calcSunApparentLong(double t){
        double o = calcSunTrueLong(t);
        double omega = 125.04 - 1934.136 * t;
        return o - 0.00569 - 0.00478 * Math.sin((Math.PI / 180) * omega);
    }
    
    double calcSunTrueLong(double t){
        double l0 = calcGeomMeanLongSun(t);
        double c = calcSunEqOfCenter(t);
        return l0 + c;
    }
    
    double calcGeomMeanLongSun(double t){
        double l0 = 280.46646 + t * (36000.76983 + 0.0003032 * t);
        while(l0 > 360.0){
            l0 -= 360.0;
        }
        while(l0 < 0.0){
            l0 += 360.0;
        }
        return l0;
    }
    
    double calcSunEqOfCenter(double t){
        double m =  calcGeomMeanAnomalySun(t);
        double mrad = m * (Math.PI / 180);
        double sinm = Math.sin(mrad);
        double sin2m = Math.sin(mrad * 2);
        double sin3m = Math.sin(mrad * 3);
        return sinm * (1.914602 - t * (0.000014 * t)) + sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289;
    }
    
    double calcGeomMeanAnomalySun(double t){
        return 357.52911 + t * (35999.05029 - 0.0001537 * t);
    }
    
    double calcSunDeclination(double t){
        double e = calcObliquityCorrection(t);
        double lambda = calcSunApparentLong(t);
        double sint = Math.sin((Math.PI / 180) * e) * Math.sin((Math.PI / 180) * lambda);
        return Math.toDegrees(Math.asin(sint));
    }
    
    double calcEquationOfTime(double t){
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
    
    double calcEccentricityEarthOrbit(double t){
        return 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
    }
    
    double calcSunriseUTC(double JD, double latitude, double longitude){
        double t = calcTimeJulianCent(JD);
        
        //get time of solar noon for more accurate calculation than start of Julian day
        var noonmin = calcSolNoonUTC(t, longitude);
        double tnoon = calcTimeJulianCent(JD + noonmin / 1440.0);
        
        //approximate sunrise
        var eqTime = calcEquationOfTime(tnoon);
        var solarDec = calcSunDeclination(tnoon);
        var hourAngle = calcHourAngleSunrise(latitude, solarDec);
        
        var delta = longitude - Math.toDegrees(hourAngle);
        var timeDiff = 4 * delta; //minutes
        var timeUTC = 720 + timeDiff - eqTime; //minutes
        
        var newt = calcTimeJulianCent(calcJDFromJulianCent(t) + timeUTC / 1440.0);
        eqTime = calcEquationOfTime(newt);
        solarDec = calcSunDeclination(newt);
        hourAngle = calcHourAngleSunrise(latitude, solarDec);
        delta = longitude - Math.toDegrees(hourAngle);
        timeDiff = 4 * delta;
        timeUTC = 720 + timeDiff - eqTime;
        
        return timeUTC;
    }
    
    double calcSolNoonUTC(double t, double longitude){
        var tnoon = calcTimeJulianCent(calcJDFromJulianCent(t) + longitude / 360.0);
        var eqTime = calcEquationOfTime(tnoon);
        var solNoonUTC = 720 + (longitude * 4) - eqTime;
        
        var newt = calcTimeJulianCent(calcJDFromJulianCent(t) - 0.5 + solNoonUTC / 1440.0);
        
        eqTime = calcEquationOfTime(newt);
        solNoonUTC = 720 + (longitude * 4) - eqTime;
        
        return solNoonUTC;
    }
    
    double calcHourAngleSunrise(double lat, double solarDec){
        var latRad = Math.toRadians(lat);
        var sdRad = Math.toRadians(solarDec);
        
        var HA = (Math.acos(Math.cos(Math.toRadians(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad)));
        
        return HA;
    }
    
    double calcJDFromJulianCent(double t){
        return t * 36525.0 + 2451545.0;
    }
    
    double calcSunsetUTC(double JD, double latitude, double longitude){
        var t = calcTimeJulianCent(JD);
        
        var noonmin = calcSolNoonUTC(t, longitude);
        var tnoon = calcTimeJulianCent(JD + noonmin / 1440.0);
        
        var eqTime = calcEquationOfTime(tnoon);
        var solarDec = calcSunDeclination(tnoon);
        var hourAngle = calcHourAngleSunset(latitude, solarDec);
        
        var delta = longitude - Math.toRadians(hourAngle);
        var timeDiff = 4 * delta;
        var timeUTC = 720 + timeDiff - eqTime;
        
        var newt = calcTimeJulianCent(calcJDFromJulianCent(t) + timeUTC / 1440.0);
        eqTime = calcEquationOfTime(newt);
        solarDec = calcSunDeclination(newt);
        hourAngle = calcHourAngleSunset(latitude, solarDec);
        
        delta = longitude - Math.toDegrees(hourAngle);
        timeDiff = 4 * delta;
        timeUTC = 720 + timeDiff - eqTime;
        
        return timeUTC;
    }
    
    double calcHourAngleSunset(double lat, double solarDec){
        var latRad = Math.toRadians(lat);
        var sdRad = Math.toRadians(solarDec);
        
        var HA = (Math.acos(Math.cos(Math.toRadians(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad)));
        
        return -HA;
    }
}
