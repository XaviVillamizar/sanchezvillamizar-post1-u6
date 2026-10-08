package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CampanasDescuentoTest {

    @Autowired
    private GestorPedidos gestor;

    private PedidoRequest pedido(Long clienteId, ItemPedido... items) {
        PedidoRequest r = new PedidoRequest();
        r.setClienteId(clienteId);
        r.setClienteEmail("cliente" + clienteId + "@correo.com");
        r.setItems(List.of(items));
        return r;
    }

    @Test
    void blackFridayAplica25PorCiento() {
        // Cliente estandar (4), 1 x 100.000, campana activa -> 25%
        ResultadoPedido r = gestor.procesarPedido(pedido(4L, new ItemPedido(1L, 1)));
        assertTrue(r.isConfirmado());
        assertEquals(100_000 * 0.75 * 1.19, r.getTotal(), 0.01);
    }

    @Test
    void volumenNoSuperaAlBlackFridayCuandoEstaActivo() {
        // 25 unidades x 100.000 = 2.500.000; Black Friday (25%) gana sobre volumen (12%)
        ResultadoPedido r = gestor.procesarPedido(pedido(4L, new ItemPedido(1L, 25)));
        assertTrue(r.isConfirmado());
        assertEquals(2_500_000 * 0.75 * 1.19, r.getTotal(), 0.01);
    }

    @Test
    void clienteVipConSubtotalAltoEligeElMayorDescuento() {
        // VIP 15% vs Black Friday 25% -> gana 25%
        ResultadoPedido r = gestor.procesarPedido(pedido(1L, new ItemPedido(2L, 2)));
        assertTrue(r.isConfirmado());
        assertEquals(1_200_000 * 0.75 * 1.19, r.getTotal(), 0.01);
    }
}