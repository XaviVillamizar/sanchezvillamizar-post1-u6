CREATE TABLE clientes (
    id BIGINT PRIMARY KEY,
    tipo_cliente VARCHAR(20),
    nit VARCHAR(20)
);

CREATE TABLE productos (
    id BIGINT PRIMARY KEY,
    precio DOUBLE
);

CREATE TABLE inventario (
    producto_id BIGINT PRIMARY KEY,
    stock INT
);

CREATE TABLE facturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    monto DOUBLE,
    pagada BOOLEAN
);

CREATE TABLE pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    subtotal DOUBLE,
    descuento DOUBLE,
    impuesto DOUBLE,
    total DOUBLE,
    fecha TIMESTAMP,
    estado VARCHAR(20)
);

CREATE TABLE detalle_pedido (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT,
    producto_id BIGINT,
    cantidad INT
);