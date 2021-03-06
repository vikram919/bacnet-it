/*******************************************************************************
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2017 University of Applied Sciences and Arts,
 * Northwestern Switzerland FHNW,
 * Institute of Mobile and Distributed Systems.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.orglicenses.
 *******************************************************************************/
package ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive;

import java.util.Calendar;
import java.util.GregorianCalendar;

import ch.fhnw.bacnetit.samplesandtests.api.encoding.enums.DayOfWeek;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.enums.Month;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;

/**
 * ASHRAE Standard 135-2012 Clause 20.2.12 Encoding of a Date Value p.631<br>
 * The encoding of a date value shall be primitive, with four contents octets
 * <br>
 * Unless otherwise specified (e.g. UTC date), a date value generated by a
 * device<br>
 * shall be a local date. Date values shall be encoded in the contents octets as
 * <br>
 * four binary integers. <br>
 * The first contents octet shall represent the year minus 1900;<br>
 * the second octet shall represent the month, with January = 1; <br>
 * the third octet shall represent the day of the month; <br>
 * and the fourth octet shall represent the day of the week, with Monday = 1.
 * <br>
 * A value of X'FF' = D'255' in any of the four octets shall indicate that the
 * <br>
 * corresponding value is unspecified and shall be considered a wildcard when
 * <br>
 * matching dates. If all four octets = X'FF', the corresponding date may be<br>
 * interpreted as "any" or "don't care."<br>
 * <br>
 * Neither an unspecified date nor a date pattern shall be used in date values
 * <br>
 * that convey actual dates, such as in a TimeSynchronization-Request.<br>
 * <br>
 * The processing of a day of week received in a service that is in the range 1
 * to 7<br>
 * and is inconsistent with the values in the other octets shall be a local
 * matter.<br>
 * <br>
 * A number of special values for the month and day octets have been defined.
 * <br>
 * The following special values shall not be used when conveying an actual date
 * <br>
 * value, such as the Local_Date property of the Device object or in a<br>
 * TimeSynchronization-Request.<br>
 * A value of 13 in the second octet shall indicate odd months.<br>
 * A value of 14 in the second octet shall indicate even months.<br>
 * A value of 32 in the third octet shall indicate the last day of the month.
 * <br>
 * A value of 33 in the third octet shall indicate odd days of the month.<br>
 * A value of 34 in the third octet shall indicate even days of the month.<br>
 * <br>
 * Example: Application-tagged specific date value<br>
 * ASN.1 = Date<br>
 * Value = January 24, 1991 (Day of week = Thursday)<br>
 * Application Tag = Date (Tag Number = 10)<br>
 * Encoded Tag = X'A4'<br>
 * Encoded Data = X'5B011804'<br>
 * <br>
 * Example: Application-tagged date pattern value<br>
 * ASN.1 = Date<br>
 * Value = year = 1991, month is unspecified, day = 24, day of week is
 * unspecified<br>
 * Application Tag = Date (Tag Number = 10)<br>
 * Encoded Tag = X'A4'<br>
 * Encoded Data = X'5BFF18FF'<br>
 */
public class Date extends Primitive {
    private static final long serialVersionUID = -5981590660136837990L;

    public static final byte TYPE_ID = 10;

    private final int year;

    private final Month month;

    private final int day;

    private final DayOfWeek dayOfWeek;

    public Date(int year, final Month month, int day,
            final DayOfWeek dayOfWeek) {
        if (year > 1900) {
            year -= 1900;
        } else if (year == -1) {
            year = 255;
        }
        if (day == -1) {
            day = 255;
        }

        this.year = year;
        this.month = month;
        this.day = day;
        this.dayOfWeek = dayOfWeek;
    }

    public Date() {
        this(new GregorianCalendar());
    }

    public Date(final GregorianCalendar now) {
        this.year = now.get(Calendar.YEAR) - 1900;
        this.month = Month.valueOf((byte) (now.get(Calendar.MONTH) + 1));
        this.day = now.get(Calendar.DATE);
        this.dayOfWeek = DayOfWeek.valueOf(
                (byte) (((now.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1));
    }

    public boolean isYearUnspecified() {
        return year == 255;
    }

    public int getYear() {
        return year;
    }

    public int getCenturyYear() {
        return year + 1900;
    }

    public Month getMonth() {
        return month;
    }

    public boolean isLastDayOfMonth() {
        return day == 32;
    }

    public boolean isDayUnspecified() {
        return day == 255;
    }

    public int getDay() {
        return day;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    //
    // Reading and writing
    //
    public Date(final ByteQueue queue) {
        readTag(queue);
        year = queue.popU1B();
        month = Month.valueOf(queue.pop());
        day = queue.popU1B();
        dayOfWeek = DayOfWeek.valueOf(queue.pop());
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        queue.push(year);
        queue.push(month.getId());
        queue.push((byte) day);
        queue.push(dayOfWeek.getId());
    }

    @Override
    protected long getLength() {
        return 4;
    }

    @Override
    protected byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + day;
        result = PRIME * result
                + ((dayOfWeek == null) ? 0 : dayOfWeek.hashCode());
        result = PRIME * result + ((month == null) ? 0 : month.hashCode());
        result = PRIME * result + year;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Date other = (Date) obj;
        if (day != other.day) {
            return false;
        }
        if (dayOfWeek == null) {
            if (other.dayOfWeek != null) {
                return false;
            }
        } else if (!dayOfWeek.equals(other.dayOfWeek)) {
            return false;
        }
        if (month == null) {
            if (other.month != null) {
                return false;
            }
        } else if (!month.equals(other.month)) {
            return false;
        }
        if (year != other.year) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + month + " " + day + ", " + year;
    }
}
