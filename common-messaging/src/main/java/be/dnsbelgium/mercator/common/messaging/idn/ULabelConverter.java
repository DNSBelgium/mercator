package be.dnsbelgium.mercator.common.messaging.idn;

import com.ibm.icu.text.IDNA;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static com.ibm.icu.text.IDNA.NONTRANSITIONAL_TO_UNICODE;

@Converter
public class ULabelConverter implements AttributeConverter<String, String> {
    public final static IDNA IDNA_INSTANCE = com.ibm.icu.text.IDNA.getUTS46Instance(NONTRANSITIONAL_TO_UNICODE);

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return toUnicode(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return toUnicode(dbData);
    }

    private String toUnicode(String input) {
        StringBuilder destination = new StringBuilder();
        IDNA.Info info = new IDNA.Info();
        var unicode = IDNA_INSTANCE.nameToUnicode(input, destination, info);
        if (info.hasErrors()) {
            var msg = String.format("Could not convert [%s] to UNICODE: %s", input, info.getErrors());
            throw new IdnException(msg);
        }
        return unicode.toString();
    }
}