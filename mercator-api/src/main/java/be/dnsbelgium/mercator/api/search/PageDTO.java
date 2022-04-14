package be.dnsbelgium.mercator.api.search;

import lombok.Data;

import java.util.List;

@Data
public class PageDTO {

    private List<SearchDTO> dtos;
    private long amountOfRecords;
    private int amountOfPages;
    private boolean hasNext;
    private boolean hasPrevious;

}
