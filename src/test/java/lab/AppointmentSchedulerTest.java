package lab;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class AppointmentSchedulerTest {
    public static void main(String[] args) {
        rejectsNonexistentLocalTimeInsteadOfShiftingIt();
        acceptsNormalLocalTimeAndStoresOneAppointment();
        System.out.println("PASS: all tests");
    }

    static void rejectsNonexistentLocalTimeInsteadOfShiftingIt() {
        AppointmentScheduler scheduler = new AppointmentScheduler();
        LocalDateTime requested = LocalDateTime.of(2024, 3, 31, 2, 30);
        ZoneId zone = ZoneId.of("Europe/Berlin");

        try {
            ZonedDateTime actual = scheduler.schedule(requested, zone);
            throw new AssertionError("夏時間の欠落時刻は拒否する expected=IllegalArgumentException actual="
                    + actual + " storedAppointments=" + scheduler.appointmentCount());
        } catch (IllegalArgumentException expected) {
            assertEquals(0, scheduler.appointmentCount(), "拒否した時刻は予定として保存しない");
        }
    }

    static void acceptsNormalLocalTimeAndStoresOneAppointment() {
        AppointmentScheduler scheduler = new AppointmentScheduler();

        ZonedDateTime actual = scheduler.schedule(
                LocalDateTime.of(2024, 3, 30, 2, 30), ZoneId.of("Europe/Berlin"));

        assertEquals(LocalDateTime.of(2024, 3, 30, 2, 30), actual.toLocalDateTime(),
                "通常日のローカル時刻は変更しない");
        assertEquals(1, scheduler.appointmentCount(), "通常日の予定は1件保存する");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
