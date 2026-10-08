# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software.
Un único proyecto Spring Boot (`pedidos-service`) con dos partes: diagnóstico y
refactorización de un antipatrón combinado (God Object y Spaghetti Code) en
`GestorPedidos`, y diagnóstico y corrección de un segundo antipatrón (Golden
Hammer) introducido al agregar tres campañas de descuento.

## Cómo ejecutar

```
mvn spring-boot:run
mvn test
```

La base de datos es H2 en memoria. `schema.sql` y `data.sql` se cargan al arrancar.
La URL usa `MODE=LEGACY` para que `CALL IDENTITY()` funcione con H2 2.x, ya que el
código de partida lo usa.

## Estructura del proyecto

```
src/main/java/com/tienda/pedidos/
├── PedidosServiceApplication.java
├── dto/          PedidoRequest, ItemPedido, ResultadoPedido
├── validacion/   ContextoPedido, ValidadorPedido, ValidadorStock, ValidadorCliente
├── descuento/    EstrategiaDescuento, DescuentoVip, DescuentoFrecuente,
│                 DescuentoEstandar, SelectorEstrategiaDescuento,
│                 DescuentoBlackFriday, DescuentoCorporativo, DescuentoVolumen,
│                 CalculadorDescuentoFinal
└── service/      GestorPedidos (orquestador), PedidoRepository,
                  NotificacionPedidoService, EmailService, EmailServiceConsola
```

## Decisiones de diseño

### Parte 1 — Diagnóstico de `GestorPedidos`

**Antipatrones identificados:** God Object y Spaghetti Code combinados.

**Evidencia del código original** (`GestorPedidos.procesarPedido`, líneas 28-134 de
un archivo de 135 líneas):

| Responsabilidad | Líneas |
|---|---|
| Validación de stock, con SQL embebido | 31-44 |
| Validación de cliente y mora, con excepción por horario | 46-65 |
| Cálculo de subtotal, una consulta SQL por ítem | 67-73 |
| Cálculo de descuento por tipo de cliente | 75-93 |
| Cálculo de impuesto y total | 95-96 |
| Persistencia directa por JDBC (inserts, update de inventario) | 98-113 |
| Construcción del correo y notificación | 115-130 |

- **God Object:** un solo método de más de 100 líneas concentra seis
  responsabilidades distintas (validación, cálculo de precio, descuento,
  persistencia, notificación y registro). La clase tiene, por tanto, al menos seis
  razones para cambiar, lo que viola el principio de responsabilidad única.
- **Spaghetti Code, anidamiento:** la validación de mora anida tres condiciones
  (`if tipoCliente == MOROSO` → `if deudaPendiente > 0` → `if ahora.isBefore(20:00)`,
  líneas 52-64). El descuento anida dos niveles (`if VIP` → `if subtotal > ...`,
  líneas 77-84) dentro de una cadena `if / else if` por tipo de cliente.
- **Mezcla de niveles de abstracción:** en la misma secuencia de líneas conviven SQL
  (líneas 37-38, 99-101, 110-111), reglas de negocio (líneas 77-92), formato de
  texto del correo (líneas 116-123) y manejo de excepciones (líneas 125-130).
- **Costo de cambio:** agregar un nuevo tipo de cliente con reglas de descuento
  propias obliga a editar el bloque 77-93 dentro de `procesarPedido`, y a releer
  las más de 100 líneas del método para asegurar que nada se rompe.
- **Defecto adicional:** `queryForObject` lanza `EmptyResultDataAccessException`
  cuando el cliente no existe, por lo que la rama `tipoCliente == null` (línea 49)
  nunca se alcanza. El test de línea base lo documenta. La refactorización lo
  conserva a propósito, porque el objetivo es mantener el comportamiento observable.

**Patrones aplicados:**

| Responsabilidad | Patrón / clase |
|---|---|
| Secuencia de validaciones | Chain of Responsibility: `ValidadorStock` → `ValidadorCliente` |
| Descuento por tipo de cliente | Strategy: `EstrategiaDescuento` + `SelectorEstrategiaDescuento` |
| Persistencia | `PedidoRepository` |
| Notificación | `NotificacionPedidoService` |
| Coordinación | `GestorPedidos` como orquestador delgado |

**Por qué Chain of Responsibility para las validaciones:** tienen dependencia real
de orden y corte anticipado. Si el stock falla, no tiene sentido consultar la mora
del cliente. **Alternativa descartada:** una lista de `Predicate<ContextoPedido>`
evaluada en bloque. Esa opción evalúa todos los predicados aunque el primero ya
haya fallado, y ningún validador puede decidir no delegar al siguiente.

**Por qué Strategy para el descuento y no otro eslabón de la cadena:** las reglas de
descuento no dependen de un orden entre sí ni necesitan cortar el flujo. Siempre se
aplica exactamente una regla, según el tipo de cliente. Modelarlas como cadena
exigiría un mecanismo artificial para que solo un eslabón module el descuento. Un
mapa de selección directa (Strategy más un selector simple) lo resuelve con menos
indirección y sin condicionales.

### Parte 2 — Diagnóstico de las campañas de descuento

**Antipatrón identificado:** Golden Hammer.

**Evidencia:** se agregaron `PromocionBlackFriday`, `PromocionCorporativo` y
`PromocionVolumen` como eslabones de la cadena de validación, porque "los eslabones
ya sabían cómo conectarse entre sí".

- **No tienen dependencia de orden.** Ejecutar `PromocionVolumen` antes que
  `PromocionCorporativo` no cambia el resultado, porque cada una solo propone un
  porcentaje y se queda con el mayor. En `ValidadorStock` y `ValidadorCliente` sí
  hay orden real.
- **Nunca rechazan nada.** Una clase que extiende `ValidadorPedido`, cuyo contrato
  es "decidir si el pedido continúa o se rechaza", ahora contiene eslabones que solo
  escriben en un campo compartido (`descuentoCampana`). El nombre y el contrato
  mienten sobre lo que hace la clase.
- **Estado mutable compartido.** Los eslabones escriben en `descuentoCampana` y la
  regla "el mayor gana" vive escondida en `aplicarDescuentoCampana`. Si dos campañas
  debieran sumarse en vez de competir, la cadena no lo expresa sin ambigüedad.
- **Origen de la decisión.** La cadena se reutilizó porque "ya funcionó" en la
  Parte 1, no porque el problema tuviera su forma.

**Corrección aplicada:** las tres campañas pasan a ser `EstrategiaDescuento`
(`DescuentoBlackFriday`, `DescuentoCorporativo`, `DescuentoVolumen`), igual que
`DescuentoVip` y `DescuentoFrecuente`. `CalculadorDescuentoFinal` combina el
descuento por tipo de cliente con el mejor de las campañas activas, tomando el
mayor, que es la misma regla de negocio que antes.

**Alternativa descartada:** mantener las campañas como eslabones de la cadena. Es la
causa del antipatrón diagnosticado.

**Código eliminado, no comentado:** `PromocionBlackFriday`, `PromocionCorporativo`,
`PromocionVolumen` y el campo `descuentoCampana` se borraron del código. Comentarlos
"por si acaso" crearía un Lava Flow, porque nadie se atrevería a borrarlos después.
La referencia histórica queda en el historial de Git.

### Comparación antes / después

Los casos de prueba producen el mismo resultado con el diseño original, con el
refactor de la Parte 1, con los eslabones Golden Hammer y con la versión final
(los tests no se modificaron entre versiones):

| Caso | Total esperado |
|---|---|
| Stock insuficiente (producto 3, 10 unidades) | Rechazado: "Stock insuficiente" |
| Cliente inexistente | Excepción (comportamiento del original, conservado) |
| Moroso | Rechazado antes de las 20:00, confirmado después |
| VIP, subtotal 1.200.000, sin campañas | 1.200.000 × 0,85 × 1,19 = 1.213.800 |
| FRECUENTE sin historial | 100.000 × 1,19 = 119.000 |
| Black Friday activo, cliente 4, 1 × 100.000 | 100.000 × 0,75 × 1,19 = 89.250 |
| Corporativo (Black Friday inactivo), NIT | 100.000 × 0,90 × 1,19 = 107.100 |
| Volumen (Black Friday inactivo), 25 unidades | 2.500.000 × 0,88 × 1,19 = 2.618.000 |

## Herramientas utilizadas
- Java 17, Spring Boot, Spring JDBC, Maven, H2 Database, JUnit 5
- VS Code, Git, GitHub

## Conclusiones
Esta práctica mostró que el mismo patrón puede ser la solución correcta en un caso
y un antipatrón en otro. Chain of Responsibility fue adecuada para las validaciones
porque hay orden real y corte anticipado, pero usarla para las campañas solo porque
"ya funcionó" fue Golden Hammer. También quedó claro que el diagnóstico debe apoyarse
en evidencia concreta del código, como líneas, niveles de anidamiento y
responsabilidades mezcladas, y no en nombrar un antipatrón. Lo más difícil fue
reconocer el segundo antipatrón, porque el código funcionaba y compilaba, y el
problema era de diseño y no funcional. Por último, mantener tests de línea base
permitió refactorizar con confianza y comprobar que el comportamiento no cambió.