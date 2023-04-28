package be.dnsbelgium.mercator.content.domain;

import be.dnsbelgium.mercator.content.dto.Status;
import be.dnsbelgium.mercator.content.persistence.StatusJpaConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusJpaConverterTest {

    private StatusJpaConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new StatusJpaConverter();
    }

    @Test
    public void testConvertToDatabaseColumn() {
        Status status = Status.Ok;
        String dbData = converter.convertToDatabaseColumn(status);
        System.out.println("status: " + status);
        System.out.println("dbData" + dbData);
        assertEquals("Ok", dbData);
    }

    @Test
    public void testConvertToEntityAttribute() {
        String dbData = "Ok";
        Status status = converter.convertToEntityAttribute(dbData);
        System.out.println("dbData; " + dbData);
        System.out.println("status" + status);
        assertEquals(Status.Ok, status);
    }
}