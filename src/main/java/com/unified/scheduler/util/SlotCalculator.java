package com.unified.scheduler.util;

import com.unified.scheduler.domain.Dealership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class SlotCalculator {

    public static final int GRID_INTERVAL_MINUTES = 30;

    /**
     * Verifies that the requested start time aligns with the 30-minute discrete grid.
     */
    public boolean isOnGrid(OffsetDateTime time) {
        if (time == null) {
            return false;
        }
        int minute = time.getMinute();
        return (minute % GRID_INTERVAL_MINUTES == 0) && time.getSecond() == 0 && time.getNano() == 0;
    }

    /**
     * Checks if the service time window [startTime, endTime) falls strictly within dealership operating hours.
     */
    public boolean isWithinOperatingHours(Dealership dealership, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (dealership == null || startTime == null || endTime == null) {
            return false;
        }
        ZoneId zone = ZoneId.of(dealership.getTimeZone() != null ? dealership.getTimeZone() : "UTC");
        LocalTime startLocal = startTime.atZoneSameInstant(zone).toLocalTime();
        LocalTime endLocal = endTime.atZoneSameInstant(zone).toLocalTime();

        LocalTime openTime = dealership.getOperatingOpenTime();
        LocalTime closeTime = dealership.getOperatingCloseTime();

        if (endTime.isBefore(startTime)) {
            return false;
        }

        if (closeTime.isAfter(openTime)) {
            return !startLocal.isBefore(openTime) && !endLocal.isAfter(closeTime);
        } else {
            // Support for edge-case overnight operating hours (e.g. 20:00 to 04:00)
            return !startLocal.isBefore(openTime) || !endLocal.isAfter(closeTime);
        }
    }

    /**
     * Generates all candidate discrete start times for a dealership on a given date.
     */
    public List<OffsetDateTime> generateCandidateStartTimes(Dealership dealership, LocalDate date, int durationMinutes) {
        List<OffsetDateTime> candidateStarts = new ArrayList<>();
        ZoneId zone = ZoneId.of(dealership.getTimeZone() != null ? dealership.getTimeZone() : "UTC");

        LocalTime current = dealership.getOperatingOpenTime();
        LocalTime closeTime = dealership.getOperatingCloseTime();

        while (true) {
            LocalTime potentialEnd = current.plusMinutes(durationMinutes);
            if (potentialEnd.isAfter(closeTime)) {
                break;
            }
            OffsetDateTime slotStart = date.atTime(current).atZone(zone).toOffsetDateTime();
            candidateStarts.add(slotStart);
            current = current.plusMinutes(GRID_INTERVAL_MINUTES);
            if (current.isAfter(closeTime) || current.equals(closeTime)) {
                break;
            }
        }
        return candidateStarts;
    }

    /**
     * Determines if two half-open intervals [s1, e1) and [s2, e2) overlap.
     */
    public boolean overlaps(OffsetDateTime s1, OffsetDateTime e1, OffsetDateTime s2, OffsetDateTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}
