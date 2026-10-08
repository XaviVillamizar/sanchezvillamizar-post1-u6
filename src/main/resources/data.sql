INSERT INTO clientes (id, tipo_cliente, nit) VALUES (1, 'VIP', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (2, 'FRECUENTE', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (3, 'MOROSO', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (4, 'ESTANDAR', '900123456');

INSERT INTO productos (id, precio) VALUES (1, 100000);
INSERT INTO productos (id, precio) VALUES (2, 600000);
INSERT INTO productos (id, precio) VALUES (3, 5000);

INSERT INTO inventario (producto_id, stock) VALUES (1, 500);
INSERT INTO inventario (producto_id, stock) VALUES (2, 50);
INSERT INTO inventario (producto_id, stock) VALUES (3, 2);

INSERT INTO facturas (cliente_id, monto, pagada) VALUES (3, 250000, false);