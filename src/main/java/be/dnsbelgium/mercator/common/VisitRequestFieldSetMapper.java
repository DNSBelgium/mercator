package be.dnsbelgium.mercator.common;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

public class VisitRequestFieldSetMapper implements FieldSetMapper<VisitRequest> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VisitRequestFieldSetMapper.class);

  @Override
  public VisitRequest mapFieldSet(FieldSet fieldSet) throws BindException {

    String visitId = fieldSet.readString("visitId");
    String domainName = fieldSet.readString("domainName");

    if (visitId == null || visitId.isBlank()) {
      logger.error("Missing visitId: {}", fieldSet);
      throw new BindException(fieldSet, "visitId");
    }
    if (domainName == null || domainName.isBlank()) {
      logger.error("Missing domainName: {}", fieldSet);
      throw new BindException(fieldSet, "domainName");
    }

    VisitRequest request = new VisitRequest();
    request.setVisitId(visitId);
    String trailingDotRemoved = domainName.replaceAll("\\.$", "");
    request.setDomainName(trailingDotRemoved);

    try {
      request.a_label();
      request.u_label();
    } catch (Exception e) {
      logger.error("Invalid domainName: {}", request.getDomainName(), e);
      throw new BindException(fieldSet, "domainName");
    }

    long numberOfDots = request.a_label().chars().filter(ch -> ch == '.').count();
    if (numberOfDots != 1) {
      logger.error("Invalid domainName, expected 2nd level domain: {}", request.getDomainName());
      throw new BindException(fieldSet, "domainName");
    }

    return request;
  }
}
