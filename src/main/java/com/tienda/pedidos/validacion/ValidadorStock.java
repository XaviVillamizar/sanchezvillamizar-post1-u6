package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ValidadorStock extends ValidadorPedido {

    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        var items = contexto.getRequest().getItems();
        if (items == null || items.isEmpty()) {
            contexto.rechazar("El pedido no contiene items");
            return;
        }
        for (var item : items) {
            Integer stock = jdbcTemplate.queryForObject(
                    "SELECT stock FROM inventario WHERE producto_id = ?", Integer.class, item.getProductoId());
            if (stock == null || stock < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente: producto " + item.getProductoId());
                return;
            }
        }
    }
}