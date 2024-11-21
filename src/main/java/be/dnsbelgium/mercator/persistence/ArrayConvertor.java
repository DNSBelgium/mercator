package be.dnsbelgium.mercator.persistence;

import lombok.SneakyThrows;
import org.duckdb.DuckDBArray;
import org.springframework.core.convert.converter.Converter;

import java.util.Arrays;
import java.util.List;

public class ArrayConvertor implements Converter<DuckDBArray, List<String>> {

  @SneakyThrows
  @Override
  public List<String> convert(DuckDBArray array) {
    return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();

  }

  @Override
  public <U> Converter<DuckDBArray, U> andThen(Converter<? super List<String>, ? extends U> after) {
    return Converter.super.andThen(after);
  }
}