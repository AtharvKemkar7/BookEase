package com.bookease.availability;

import java.time.LocalTime;

final class TimeRanges {

    private TimeRanges() {
    }

    static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    static boolean contains(LocalTime outerStart, LocalTime outerEnd, LocalTime innerStart, LocalTime innerEnd) {
        return !innerStart.isBefore(outerStart) && !innerEnd.isAfter(outerEnd);
    }
}
