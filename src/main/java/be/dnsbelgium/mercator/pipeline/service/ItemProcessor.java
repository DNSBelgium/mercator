package be.dnsbelgium.mercator.pipeline.service;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface ItemProcessor<Input, Output>
     extends org.springframework.batch.infrastructure.item.ItemProcessor <Input, Output>{

     @Override
     default @Nullable Output process(@NonNull Input item) throws Exception {
          return processItem(item);
     }


     Output processItem(Input item);

}
