package com.parca.yedekparca;

import com.parca.yedekparca.service.WorkingHoursValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkingHoursValidatorTest {
    @Test void acceptsClosedDaysAndValidOpenDays() {
        assertDoesNotThrow(() -> WorkingHoursValidator.validate("{\"Pazartesi\":{\"closed\":false,\"open\":\"09:00\",\"close\":\"18:00\"},\"Pazar\":{\"closed\":true}}"));
    }
    @Test void acceptsLegacyDayTimeStrings() {
        assertDoesNotThrow(() -> WorkingHoursValidator.validate("{\"monday\":\"09:00-18:00\"}"));
    }
    @Test void rejectsUnknownDaysAndMalformedTimes() {
        assertThrows(IllegalArgumentException.class, () -> WorkingHoursValidator.validate("{\"Holiday\":{\"closed\":true}}"));
        assertThrows(IllegalArgumentException.class, () -> WorkingHoursValidator.validate("{\"monday\":{\"closed\":false,\"open\":\"9am\",\"close\":\"18:00\"}}"));
    }
    @Test void rejectsClosingBeforeOrAtOpening() {
        assertThrows(IllegalArgumentException.class, () -> WorkingHoursValidator.validate("{\"monday\":{\"closed\":false,\"open\":\"18:00\",\"close\":\"09:00\"}}"));
        assertThrows(IllegalArgumentException.class, () -> WorkingHoursValidator.validate("{\"monday\":{\"closed\":false,\"open\":\"09:00\",\"close\":\"09:00\"}}"));
    }
}
