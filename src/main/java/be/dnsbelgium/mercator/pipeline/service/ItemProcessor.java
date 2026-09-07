package be.dnsbelgium.mercator.pipeline.service;

public interface ItemProcessor<Input, Output> {

     Output processItem(Input item);

}
