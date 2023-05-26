package be.dnsbelgium.mercator.common.messaging.idn;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ULabelConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return IDN2008.toUnicode(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData;
    }

}