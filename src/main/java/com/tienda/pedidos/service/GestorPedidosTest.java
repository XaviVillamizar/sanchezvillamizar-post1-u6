package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GestorPedidosTest {

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
    void stockInsuficienteRechazaElPedido() {
        // El producto 3 solo tiene 2 unidades en inventario
        ResultadoPedido r = gestor.procesarPedido(pedido(1L, new ItemPedido(3L, 10)));
        assertFalse(r.isConfirmado());
        assertTrue(r.getMotivoRechazo().contains("Stock insuficiente"));
    }

    @Test
    void clienteInexistenteSeRechaza() {
        // Un cliente que no existe hace que queryForObject lance EmptyResultDataAccessException
        // en el codigo original, porque no maneja ese caso. Es un defecto del codigo de partida.
        assertThrows(Exception.class,
                () -> gestor.procesarPedido(pedido(999L, new ItemPedido(1L, 1))));
    }

    @Test
    void clienteMorosoDependeDelHorarioDeCorte() {
        ResultadoPedido r = gestor.procesarPedido(pedido(3L, new ItemPedido(1L, 1)));
        if (LocalTime.now().isBefore(LocalTime.of(20, 0))) {
            assertFalse(r.isConfirmado());
            assertTrue(r.getMotivoRechazo().contains("deuda pendiente"));
        } else {
            assertTrue(r.isConfirmado());
        }
    }

    @Test
    void clienteVipConSubtotalAltoRecibeDescuentoDel15() {
        // 2 x 600.000 = 1.200.000 -> VIP > 1.000.000 -> 15%
        ResultadoPedido r = gestor.procesarPedido(pedido(1L, new ItemPedido(2L, 2)));
        assertTrue(r.isConfirmado());
        double esperado = (1_200_000 - 1_200_000 * 0.15) * 1.19;
        assertEquals(esperado, r.getTotal(), 0.01);
    }

    @Test
    void clienteFrecuenteSinHistorialNoRecibeDescuento() {
        // Sin pedidos previos el descuento es 0
        ResultadoPedido r = gestor.procesarPedido(pedido(2L, new ItemPedido(1L, 1)));
        assertTrue(r.isConfirmado());
        assertEquals(100_000 * 1.19, r.getTotal(), 0.01);
    }
}