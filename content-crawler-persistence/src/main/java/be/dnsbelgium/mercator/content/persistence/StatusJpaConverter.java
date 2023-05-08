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
        return attribute.name();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Status.valueOf(dbData);
    }
}
