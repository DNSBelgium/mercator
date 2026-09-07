package be.dnsbelgium.mercator.pipeline.service;

public interface ItemWriter<Output> {

  void write(Output t);

  void flush();

  int writtenItems();
}
