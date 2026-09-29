package be.dnsbelgium.mercator.pipeline.service;

public interface ItemSourceFactory<T> {

    T create(String moduleName);
}
