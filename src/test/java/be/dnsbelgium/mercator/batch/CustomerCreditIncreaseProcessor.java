package be.dnsbelgium.mercator.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;

public class CustomerCreditIncreaseProcessor implements ItemProcessor<CustomerCredit, CustomerCredit> {

  public static final BigDecimal FIXED_AMOUNT = new BigDecimal("5");

  @Nullable
  @Override
  public CustomerCredit process(CustomerCredit item) throws Exception {
    return item.increaseCreditBy(FIXED_AMOUNT);
  }

}
