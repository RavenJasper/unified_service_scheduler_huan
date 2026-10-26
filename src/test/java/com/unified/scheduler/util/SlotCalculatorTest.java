package com.unified.scheduler.util;

import com.unified.scheduler.domain.Dealership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlotCalculatorTest {

    private SlotCalculator slotCalculator;
    private Dealership dealership;

    @BeforeEach
    void setUp() {
        slotCalculator = new SlotCalculator();
        dealership = new Dealership(
            1L,
            "DLR-TEST",
            "Test Dealership",
            "123 Auto Way",
            "Metroville",
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
            "UTC"
        );
    }

    @Test
    @DisplayName("shouldReturnTrue_whenTimeIsOn30MinuteGrid")
    void shouldReturnTrue_whenTimeIsOn30MinuteGrid() {
        OffsetDateTime onGrid1 = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime onGrid2 = OffsetDateTime.of(2026, 11, 1, 9, 30, 0, 0, ZoneOffset.UTC);

        assertThat(slotCalculator.isOnGrid(onGrid1)).isTrue();
        assertThat(slotCalculator.isOnGrid(onGrid2)).isTrue();
    }

    @Test
    @DisplayName("shouldReturnFalse_whenTimeIsOffGrid")
    void shouldReturnFalse_whenTimeIsOffGrid() {
        OffsetDateTime offGridMinute = OffsetDateTime.of(2026, 11, 1, 9, 15, 0, 0, ZoneOffset.UTC);
        OffsetDateTime offGridSecond = OffsetDateTime.of(2026, 11, 1, 9, 0, 5, 0, ZoneOffset.UTC);

        assertThat(slotCalculator.isOnGrid(offGridMinute)).isFalse();
        assertThat(slotCalculator.isOnGrid(offGridSecond)).isFalse();
        assertThat(slotCalculator.isOnGrid(null)).isFalse();
    }

    @Test
    @DisplayName("shouldReturnTrue_whenSlotIsWithinDealershipOperatingHours")
    void shouldReturnTrue_whenSlotIsWithinDealershipOperatingHours() {
        OffsetDateTime start = OffsetDateTime.of(2026, 11, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        assertThat(slotCalculator.isWithinOperatingHours(dealership, start, end)).isTrue();
    }

    @Test
    @DisplayName("shouldReturnFalse_whenSlotStartsBeforeOpeningOrEndsAfterClosing")
    void shouldReturnFalse_whenSlotStartsBeforeOpeningOrEndsAfterClosing() {
        OffsetDateTime earlyStart = OffsetDateTime.of(2026, 11, 1, 7, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime earlyEnd = OffsetDateTime.of(2026, 11, 1, 8, 30, 0, 0, ZoneOffset.UTC);

        OffsetDateTime lateStart = OffsetDateTime.of(2026, 11, 1, 17, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime lateEnd = OffsetDateTime.of(2026, 11, 1, 18, 30, 0, 0, ZoneOffset.UTC);

        assertThat(slotCalculator.isWithinOperatingHours(dealership, earlyStart, earlyEnd)).isFalse();
        assertThat(slotCalculator.isWithinOperatingHours(dealership, lateStart, lateEnd)).isFalse();
    }

    @Test
    @DisplayName("shouldGenerateCorrectCandidateStartTimes_forFullWorkingDay")
    void shouldGenerateCorrectCandidateStartTimes_forFullWorkingDay() {
        LocalDate date = LocalDate.of(2026, 11, 1);

        List<OffsetDateTime> slots = slotCalculator.generateCandidateStartTimes(dealership, date, 60);

        assertThat(slots).hasSize(19);
        assertThat(slots.getFirst().toLocalTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.getLast().toLocalTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    @DisplayName("shouldDetectIntervalOverlapsCorrectly")
    void shouldDetectIntervalOverlapsCorrectly() {
        OffsetDateTime t0900 = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t0930 = OffsetDateTime.of(2026, 11, 1, 9, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t1000 = OffsetDateTime.of(2026, 11, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t1030 = OffsetDateTime.of(2026, 11, 1, 10, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t1100 = OffsetDateTime.of(2026, 11, 1, 11, 0, 0, 0, ZoneOffset.UTC);

        assertThat(slotCalculator.overlaps(t0900, t1000, t0930, t1030)).isTrue();
        assertThat(slotCalculator.overlaps(t0900, t1000, t1000, t1100)).isFalse();
        assertThat(slotCalculator.overlaps(t1000, t1100, t0900, t0930)).isFalse();
    }
}
