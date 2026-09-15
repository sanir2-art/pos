package com.example.utils

import java.text.DecimalFormat
import java.util.Calendar
import java.util.TimeZone

object PersianUtils {

    /**
     * Converts English digits (0-9) to Persian digits (۰-۹)
     */
    fun toPersianDigits(input: String): String {
        val persianChars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianChars[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Format number with comma separation and Persian digits, appending currency (تومان)
     */
    fun formatCurrency(amount: Double, currency: String = "تومان"): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount.toLong())
        return "${toPersianDigits(formatted)} $currency"
    }

    fun formatNumber(number: Long): String {
        val formatter = DecimalFormat("#,###")
        return toPersianDigits(formatter.format(number))
    }

    fun formatNumber(number: Int): String {
        val formatter = DecimalFormat("#,###")
        return toPersianDigits(formatter.format(number))
    }

    /**
     * Converts a timestamp in milliseconds to a Persian Jalali date string (e.g. ۱۴۰۳/۰۶/۲۵ - ۱۴:۳۰)
     */
    fun formatToPersianDateTime(timestamp: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        calendar.timeInMillis = timestamp

        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        val timeStr = String.format("%02d:%02d", hour, minute)

        return toPersianDigits("${jalali[0]}/${String.format("%02d", jalali[1])}/${String.format("%02d", jalali[2])} - $timeStr")
    }

    fun formatToPersianDate(timestamp: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        calendar.timeInMillis = timestamp

        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        return toPersianDigits("${jalali[0]}/${String.format("%02d", jalali[1])}/${String.format("%02d", jalali[2])}")
    }

    fun getCurrentPersianDate(): String {
        return formatToPersianDate(System.currentTimeMillis())
    }

    /**
     * Accurate algorithm for Gregorian to Solar Hijri (Jalali) conversion
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): IntArray {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400

        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            // leap and after Feb
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..10) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i
                break
            }
            jDayNo -= jDaysInMonth[i]
            if (i == 10) jm = 11
        }

        val jd = jDayNo + 1
        return intArrayOf(jy, jm + 1, jd)
    }
}
