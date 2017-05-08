/*
 * Copyright 2012 The Solmix Project
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.gnu.org/licenses/ 
 * or see the FSF site: http://www.fsf.org. 
 */

package org.solmix.commons.util;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @version 110035
 */
public final class DateUtils
{

    private static final Logger LOG = LoggerFactory.getLogger(DateUtils.class);

    public static final long DAY_MS_TIME = 24 * 60 * 60 * 1000;

    public static String simpleDateFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);

    }

    public static String getFirstDayofMouth(String date, String inPattern, String outPattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(inPattern);
        String result = "";
        try {
            Date d = sdf.parse(date);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            int start = c.getActualMinimum(Calendar.DAY_OF_MONTH);
            c.set(Calendar.DAY_OF_MONTH, start);
            sdf = new SimpleDateFormat(outPattern);
            result = sdf.format(c.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }
    public static Date getMouthStartDay(){
        java.util.Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
  }
  
  public static Date getRelativeDay(String date ,int i)throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d =sdf.parse(date);
        java.util.Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_MONTH, i);
        return c.getTime();
  }

    public static String getEndDayofMouth(String date, String inPattern, String outPattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(inPattern);
        String __return = "";
        try {
            Date d = sdf.parse(date);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            int start = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            c.set(Calendar.DAY_OF_MONTH, start);
            sdf = new SimpleDateFormat(outPattern);
            __return = sdf.format(c.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return __return;
    }

    public static Date getDateFromString(String date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        Date __return = null;
        try {
            __return = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return __return;
    }

    public static String getCurrentDateStr(String pattern) {
        return getDateString(new Date(), pattern);
    }

    public static String getDateString(Date date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        String __return = null;
        try {
            __return = sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return __return;
    }

    public static String getDateString(long dateLong, String pattern) {
        Date date = new Date();
        date.setTime(dateLong);
        return getDateString(date, pattern);
    }

    public static String getDateString(long dateLong) {
        Date date = new Date();
        date.setTime(dateLong);
        return getDateString(date, "yyyy-MM-dd");
    }

    public static String getDateString(String dateLongStr) {
        Date date = new Date();
        date.setTime(Long.valueOf(dateLongStr));
        return getDateString(date, "yyyy-MM-dd");
    }
    

    public static String fastDateFormat(java.util.Date date) {
        Calendar calendar = Calendar.getInstance();
        StringWriter out = new StringWriter();
        calendar.setTime(date);
        out.write(String.valueOf(calendar.get(1)));
        out.write("-");
        out.write(String.valueOf(calendar.get(2) + 1));
        out.write("-");
        out.write(String.valueOf(calendar.get(5)));
        out.write(" ");
        out.write(String.valueOf(calendar.get(11)));
        out.write(":");
        out.write(String.valueOf(calendar.get(12)));
        out.write(":");
        out.write(String.valueOf(calendar.get(13)));
        return out.toString();
    }
    
    public static Date getMinuteAfter(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    public static Date getMinuteBefore(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, -minute);
        return calendar.getTime();
    }
    private static Date parseSimpleDate(String str){
        DateFormat df;

        df  = DateFormat.getDateInstance(DateFormat.SHORT);

        df.setLenient(false);

        try {
            return df.parse(str);
        } catch(ParseException exc){
            return null;
        }
    }

    private static Date parseSimpleTime(String str){
        SimpleDateFormat tf, tf2;

        tf = new SimpleDateFormat("hh:mma");
        tf2  = new SimpleDateFormat("HH:mm");

        tf.setLenient(false);
        tf2.setLenient(false);

        try {
            return tf.parse(str);
        } catch(ParseException exc){
            try {
                return tf2.parse(str);
            } catch(ParseException iexc){
                return null;
            }
        }
    }
    /**
     * Parse times in a 'natural language' fashion.  The following
     * types of times/dates are supported:
     *
     * 'now'               - Returns the 'basetime' that is passed in
     * 'now - 1 day'       - Returns 1 day prior to the base time
     * 'yesterday'         - Returns basetime - 1 day
     * 'tomorrow'          - Returns basetime + 1 day
     * 'monday'            - Returns the previous monday if futureTime is
     *                       set to false, else true
     *
     * 'monday + 12 hours' - Depending on futureTime returns different
     *                       mondays at noon
     * 'march + 4 days'    - 4 days into March
     * '3:00'              - Parse error -- not enough info
     * '3:00pm'            - Future 3 if futureTime is set
     *
     * '12/24/02 9:00pm'   - Absolute time
     * 
     * @param str        String to parse
     * @param baseTime   Basetime to use when calculating relative
     *                   times (i.e. now - 3 days)
     * @param futureTime When there is some ambiguity as to the time,
     *                   default to the time in the future.  I.e
     *                   in the case where someone says, "noon" and
     *                   the time is currently 1:00, does he mean
     *                   the noon that just passed, or the upcoming noon?
     * 
     * @return milliseconds from the epoch representing the parsed time
     */
    public static long parseComplexTime(String str, long baseTime,
                                        boolean futureTime)
        throws ParseException
    {
        Calendar resCal;
        String[] exp;
        String dur;
        int weekday, month, useIdx, offset;

        exp = (String[])StringUtils.split(str, " ");
        if(exp.length < 1){
            throw new ParseException("No time found to parse", 0);
        }

        resCal = Calendar.getInstance();
        resCal.setTime(new Date(baseTime));

        useIdx = 0;

        // Get the base
        if(exp[useIdx].equalsIgnoreCase("now")){
            // Already set resCal to current time
            useIdx++;
        } else if((exp[useIdx].equalsIgnoreCase("yesterday"))){
            resCal.add(Calendar.DAY_OF_YEAR, -1);
            useIdx++;
        } else if((exp[useIdx].equalsIgnoreCase("tomorrow"))){
            resCal.add(Calendar.DAY_OF_YEAR, 1);
            useIdx++;
        } else if((weekday = getWeekDay(exp[useIdx])) != -1){
            int curDay;

            curDay = resCal.get(Calendar.DAY_OF_WEEK);
            if(futureTime){
                if(weekday < curDay)
                    weekday += 7;
            } else {
                if(weekday > curDay)
                    weekday -= 7;
            }
            
            resCal.add(Calendar.DAY_OF_WEEK, weekday - curDay);
            useIdx++;
        } else if((month = getMonth(exp[useIdx])) != -1){
            int curMonth;

            curMonth = resCal.get(Calendar.MONTH);
            if(futureTime){
                if(month < curMonth)
                    month += 7;
            } else {
                if(month > curMonth)
                    month -= 7;
            }
            
            resCal.add(Calendar.MONTH, month - curMonth);
            useIdx++;
        } else {
            Date newDate = null, newTime = null;

            newDate = parseSimpleDate(exp[useIdx]);
            if(newDate == null){
                newTime = parseSimpleTime(exp[useIdx]);
                if(newTime != null){
                    useIdx++;
                }
            } else {
                useIdx++;
                if(useIdx != exp.length){
                    newTime = parseSimpleTime(exp[useIdx]);
                    if(newTime != null){
                        useIdx++;
                    }
                }
            }

            if(newDate == null &&  newTime == null){
                throw new ParseException("Invalid date/time specified", 0);
            }

            if(newDate != null){
                resCal.setTime(newDate);
            }

            if(newTime != null){
                Calendar tmpCal;
                long newMillis;
                long curTimeMillis;

                tmpCal = Calendar.getInstance();
                tmpCal.setTime(newTime);

                resCal.set(Calendar.SECOND, 0);
                newMillis =
                    tmpCal.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 +
                    tmpCal.get(Calendar.MINUTE) * 60 * 1000;

                curTimeMillis = 
                    resCal.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 +
                    resCal.get(Calendar.MINUTE) * 60 * 1000;

                // If the user explicitly set a date, the time is absolute
                // else, it's relative to our current time
                if(newDate == null){
                    if(futureTime){
                        if(curTimeMillis > newMillis)
                            newMillis += 24 * 60 * 60 * 1000;
                    } else {
                        if(curTimeMillis < newMillis)
                            newMillis -= 24 * 60 * 60 * 1000;
                    }
                    resCal.add(Calendar.SECOND, 
                               (int)((newMillis - curTimeMillis) / 1000));
                } else {
                    resCal.add(Calendar.SECOND, (int)(newMillis / 1000));
                }
            }
        }

        // Check for offsets from the base time
        if(useIdx == exp.length)
            return resCal.getTime().getTime();

        if(useIdx != exp.length - 3 ||
           (exp[useIdx].equals("+") == false &&
            exp[useIdx].equals("-") == false))
        {
            throw new ParseException("Invalid time offset specified, '" +
                                     exp[useIdx] + "'", 0);
        } 

        try {
            offset = Integer.parseInt(exp[useIdx + 1]);
        } catch(NumberFormatException exc){
            throw new ParseException("Error parsing offset value: " +
                                     exp[useIdx + 1] + " is not a number", 0);
        }

        if(exp[useIdx].equals("-"))
            offset = offset * -1;

        dur = exp[useIdx + 2];

        if(dur.regionMatches(true, 0, "seconds", 0, dur.length()))
            resCal.add(Calendar.SECOND, offset);
        else if(dur.regionMatches(true, 0, "minutes", 0, dur.length()))
            resCal.add(Calendar.MINUTE, offset);
        else if(dur.regionMatches(true, 0, "hours", 0, dur.length()))
            resCal.add(Calendar.HOUR, offset);
        else if(dur.regionMatches(true, 0, "days", 0, dur.length()))
            resCal.add(Calendar.DAY_OF_YEAR, offset);
        else if(dur.regionMatches(true, 0, "weeks", 0, dur.length()))
            resCal.add(Calendar.WEEK_OF_YEAR, offset);
        else if(dur.regionMatches(true, 0, "months", 0, dur.length()))
            resCal.add(Calendar.MONTH, offset);
        else if(dur.regionMatches(true, 0, "years", 0, dur.length()))
            resCal.add(Calendar.YEAR, offset);
        else
            throw new ParseException("Invalid offset duration '" + dur + "'",
                                     0);

        return resCal.getTime().getTime();
    }

    private static int getWeekDay(String str){
        if(str.length() < 3)
            return -1;

        if(str.regionMatches(true, 0, "sunday", 0, str.length()))
            return Calendar.SUNDAY;
        else if(str.regionMatches(true, 0, "monday", 0, str.length()))
            return Calendar.MONDAY;
        else if(str.regionMatches(true, 0, "tuesday", 0, str.length()))
            return Calendar.TUESDAY;
        else if(str.regionMatches(true, 0, "wednesday", 0, str.length()))
            return Calendar.WEDNESDAY;
        else if(str.regionMatches(true, 0, "thursday", 0, str.length()))
            return Calendar.THURSDAY;
        else if(str.regionMatches(true, 0, "friday", 0, str.length()))
            return Calendar.FRIDAY;
        else if(str.regionMatches(true, 0, "saturday", 0, str.length()))
            return Calendar.SATURDAY;
        else
            return -1;
    }

    private static int getMonth(String str){
        if(str.length() < 3)
            return -1;

        if(str.regionMatches(true, 0, "january", 0, str.length()))
            return Calendar.JANUARY;
        else if(str.regionMatches(true, 0, "february", 0, str.length()))
            return Calendar.FEBRUARY;
        else if(str.regionMatches(true, 0, "march", 0, str.length()))
            return Calendar.MARCH;
        else if(str.regionMatches(true, 0, "april", 0, str.length()))
            return Calendar.APRIL;
        else if(str.regionMatches(true, 0, "may", 0, str.length()))
            return Calendar.MAY;
        else if(str.regionMatches(true, 0, "june", 0, str.length()))
            return Calendar.JUNE;
        else if(str.regionMatches(true, 0, "july", 0, str.length()))
            return Calendar.JULY;
        else if(str.regionMatches(true, 0, "august", 0, str.length()))
            return Calendar.AUGUST;
        else if(str.regionMatches(true, 0, "september", 0, str.length()))
            return Calendar.SEPTEMBER;
        else if(str.regionMatches(true, 0, "october", 0, str.length()))
            return Calendar.OCTOBER;
        else if(str.regionMatches(true, 0, "november", 0, str.length()))
            return Calendar.NOVEMBER;
        else if(str.regionMatches(true, 0, "december", 0, str.length()))
            return Calendar.DECEMBER;
        else
            return -1;
    }
    
    public static void main(String args[]) {
        System.out.println(DateUtils.getFirstDayofMouth("201105", "yyyyMM", "yyyyMMdd"));
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        String textDate = "2013-04-01T02:27:05";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(DateUtils.getCurrentDateStr("yyyy-MM-dd HH:mm:ss"));
        System.out.println(new Date());
        try {
            System.out.println(sdf.parse(textDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
