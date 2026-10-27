package com.ofimarck.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class CarritoDTO {

    private List<CarritoItemDTO> items;
    private Integer totalItems;
    private BigDecimal total;

    public CarritoDTO() {
    }

    public CarritoDTO(List<CarritoItemDTO> items, Integer totalItems, BigDecimal total) {
        this.items = items;
        this.totalItems = totalItems;
        this.total = total;
    }

    public List<CarritoItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CarritoItemDTO> items) {
        this.items = items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}