package com.ofimarck.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Carrito {

    private List<CarritoItem> items = new ArrayList<>();

    public Carrito() {
    }

    public List<CarritoItem> getItems() {
        return items;
    }

    public void setItems(List<CarritoItem> items) {
        this.items = items;
    }

    public void addItem(CarritoItem item) {
        for (CarritoItem existing : items) {
            if (existing.getIdProducto().equals(item.getIdProducto())) {
                existing.setCantidad(existing.getCantidad() + item.getCantidad());
                existing.recalcularSubtotal();
                return;
            }
        }
        items.add(item);
    }

    public void removeItem(Integer idProducto) {
        items.removeIf(item -> item.getIdProducto().equals(idProducto));
    }

    public void updateQuantity(Integer idProducto, Integer nuevaCantidad) {
        for (CarritoItem item : items) {
            if (item.getIdProducto().equals(idProducto)) {
                item.setCantidad(nuevaCantidad);
                item.recalcularSubtotal();
                return;
            }
        }
    }

    public Integer getTotalItems() {
        return items.stream().mapToInt(CarritoItem::getCantidad).sum();
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(CarritoItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}