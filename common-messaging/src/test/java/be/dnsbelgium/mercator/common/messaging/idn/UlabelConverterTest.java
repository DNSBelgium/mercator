package be.dnsbelgium.mercator.common.messaging.idn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UlabelConverterTest {

    private final ULabelConverter converter = new ULabelConverter();

    @Test
    void convertToDatabaseColumn_withNullAttribute_shouldReturnNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_withValidAttribute_shouldReturnUnicodeString() {
        String attribute = "xn--dnsbelgi-01a.be";
        String expectedUnicode = "dnsbelgië.be";
        assertEquals(expectedUnicode, converter.convertToDatabaseColumn(attribute));
    }

    @Test
    void convertToDatabaseColumn_inUnicode_shouldReturnUnicodeString() {
        String attribute = "dnsbelgië.be";
        String expectedUnicode = "dnsbelgië.be";
        assertEquals(expectedUnicode, converter.convertToDatabaseColumn(attribute));
    }

    @Test
    void convertToEntityAttribute_withNullDbData_shouldReturnNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttribute_withValidDbData_shouldReturnUnicodeString() {
        String dbData = "xn--dnsbelgi-01a.be";
        String expectedUnicode = "dnsbelgië.be";
        assertEquals(expectedUnicode, converter.convertToEntityAttribute(dbData));
    }

    @Test
    void convertToEntityAttribute_inUnicode_shouldReturnUnicodeString() {
        String dbData = "dnsbelgië.be";
        String expectedUnicode = "dnsbelgië.be";
        assertEquals(expectedUnicode, converter.convertToEntityAttribute(dbData));
    }

    @Test
    void convertToDatabaseColumn_withInvalidAttribute_shouldThrowIdnException() {
        String attribute = "--xx--.be";
        assertThrows(IdnException.class, () -> converter.convertToDatabaseColumn(attribute));
    }

}