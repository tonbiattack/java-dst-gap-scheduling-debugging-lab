package lab;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AppointmentScheduler {
    private final List<ZonedDateTime> appointments = new ArrayList<>();

    public ZonedDateTime schedule(LocalDateTime requestedLocalTime, ZoneId zone) {
        ZonedDateTime resolved = requestedLocalTime.atZone(zone);
        appointments.add(resolved);
        return resolved;
    }

    public int appointmentCount() {
        return appointments.size();
    }
}
