package com.wkuxr.eclipsetotality.Location;

//this is being translated from JS to Java; Source is https://gml.noaa.gov/grad/solcalc/sunrise.html (I have a backup of the JS if site goes down for some reason)

import java.util.TimeZone;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Sunset {
    public long calculateSunset(double lat, double lon){
        int timezoneDiff = timeDiff();
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
        
        return 0;
    }
    
    //the calculateSunset function uses CDT as its base timezone, so we need to separately determine the difference in hours between CDT and the user's timezone
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
    
    double calcTimeJulianCent(int jd){
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
}
