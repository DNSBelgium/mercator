package be.dnsbelgium.mercator.content.persistence;

import be.dnsbelgium.mercator.content.dto.Status;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class StatusJpaConverter implements AttributeConverter<Status, String> {
    @Override
    public String convertToDatabaseColumn(Status attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getStatus();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (Status value : Status.values()) {
            if (value.getStatus().equals(dbData)) {
                return value;
            }
        }
        throw new RuntimeException("status from db not defined in code.");
    }
}
