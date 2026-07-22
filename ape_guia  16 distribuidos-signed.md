## Guía de Actividades Práctico- Experimentales Nro. 015

## 2. Objetivo(s) de la Práctica:

Implementar una arquitectura básica de microservicios en Python, integrando el patrón de tolerancia a fallos Circuit Breaker para proteger la comunicación entre servicios y evitar fallos en cascada, registrando los cambios de estado del circuito en una base de datos SQLite.

- Diseñar e implementar dos microservicios independientes (Servicio Orquestador y Servicio de Pagos) que se comuniquen vía API REST.

- Desarrollar una clase CircuitBreaker en Python que administre los estados Closed (Cerrado), Open (Abierto) y Half-Open (Semi- abierto).

- Simular fallos en el microservicio de Pagos y observar cómo el Circuit Breaker aísla el fallo, devolviendo respuestas alternativas (fallback) de forma inmediata sin saturar el sistema.

- Persistir los cambios de estado del Circuit Breaker en la base de datos SQLite para su posterior análisis y auditoría.


## 3. Materiales y reactivos:

- Equipo de cómputo con privilegios de administrador.

- Terminal de línea de comandos.

- Editor de texto o IDE (VS Code, PyCharm, etc.).

- Base de datos nodos.db creada en la práctica anterior (se añadirá una nueva tabla).

## 4. Recursos

## Hardware:

Procesador: Dual-core 2.0 GHz o superior.

Memoria RAM: Mínimo 4 GB.

## Software:

## Sistema Operativo: Linux, macOS o Windows.

Python 3.8 o superior.

Librerías de Python: http.server, sqlite3, urllib, time, json (librería estándar)..

## 5. Metodología/procedimiento:

*Fig. 1: Arquitectura*

Paso 1: usar el balanceador de carga de la practica 15

Paso 2: Crear 4 microservicios (uno por integrante de grupo) en sprint boot

Paso 3: Armar la arquitectura según la figura 1

Paso 4: Crear el Microservicio Orquestador con Circuit Breaker

Paso 5: Pruebas del sistema completo 10. Abre una quinta terminal. Haz una

petición al Orquestador: curl http://127.0.0.1:<port>/ (Debes recibir un 200 OK. El Orquestador llamó al Balanceador, y este a los backends).


Paso 6: haga pruebas haciendo caer los nodos para ver como se comporta cada uno de las instancias

## 6. Resultados esperados:

- El estudiante evidencia que la caída simultánea de los nodos backend provoca que el Orquestador sufra timeouts, pero el Circuit Breaker corta la comunicación rápidamente tras el umbral de fallos.

- La base de datos SQLite cruza dos datos fundamentales: estado_nodos muestra cuándo el Heartbeat detectó la caída física de los backends, y circuit_log muestra cuándo el Circuit Breaker reaccionó a nivel lógico de red.

- Se comprueba que tras la recuperación de los backends y su verificación por heartbeat, el sistema vuelve a la normalidad de forma autónoma

## Tabla de observaciones

| Escenario Estado | de Backend s | Estado BD: estado_nod os | Estado del Circuito (Esperado) | Tiempo de respuesta Orquestado r (Aprox) | Registro BD: circuit_log |
| --- | --- | --- | --- | --- | --- |
| Backends activos | 2 Activos 9001: | ACTIVO, 9002: ACTIVO | CLOSED | ~0.05s | CLOSED |
| Caen ambos Backends (Peticiones 1-3) | 2 Inactivos | 9001: INACTIVO, 9002: INACTIVO | CLOSED -> (Acumula) | ~1s | - |
| Backends siguen caídos (Petición 4+) | 2 Inactivos | 9001: INACTIVO, 9002: INACTIVO | OPEN | <0.01s | OPEN |
| Pasan 10s (Prueba Half-Open) | 2 Inactivos | 9001: INACTIVO, 9002: INACTIVO | HALF_OPE N -> OPEN | ~1s | HALF_OPE N, OPEN |
| Backends recuperado s + 10s | 2 Activos 9001: | ACTIVO, 9002: ACTIVO | HALF_OPE N -> CLOSED | ~0.05s | HALF_OPE N, CLOSED |


## 7. Preguntas de Control:

- En esta arquitectura integrada, si un solo backend (ej. el puerto 9001) cae pero el otro sigue activo, ¿el Circuit Breaker del Orquestador debería abrirse? Justifica tu respuesta basándote en el comportamiento del Balanceador de carga.

- ¿Qué diferencia hay entre el mecanismo de Heartbeat (práctica anterior) y el Circuit Breaker (práctica actual) en cuanto a la detección de fallos? (Pista: Monitoreo pasivo vs. activo, o detección por uso vs. sondeo).

- Si revisamos la base de datos SQLite justo en el momento que el circuito se abre, ¿habrá diferencia temporal (en segundos) entre el timestamp de estado_nodos (cuando se detectó la caída del backend) y el timestamp de circuit_log? ¿Por qué?

- ¿En qué escenario de la vida real (ej. Netflix) es crítico tener un Fallback (respuesta alternativa) en lugar de solo devolver un error 500 al usuario cuando el Circuit Breaker se abre?

- Si el balanceador de carga muere repentinamente (se cae el proceso del puerto 8080), ¿cómo reaccionará el Circuit Breaker del Orquestador? ¿Podrá el sistema recuperarse automáticamente alguna vez?

## 8. Evaluación

| 9. Criterio | Excelente | Bueno | Bajo |
| --- | --- | --- | --- |
| Funcionamiento (back-end, front-end) se evaluara criterios de Verificacion | Funciona completo (4) | Parcial (2) | No funciona (0) |
| Documento de practica (Enlace a git, metodología usada por el alumno, resultados, preguntas de control, bibliografía, conclusiones, video de explicación) | Funciona completo (3) | Parcial (2) | No funciona (0) |
| Defensa de practica | Todos los puntos (3) | Parcial (1) | No valido (0) |


## 10. Bibliografía

Semana 16 Guaman, Jose. 2026

## Elaboración y Aprobación

| Elaborado por | José Guamán Docente |   |
| --- | --- | --- |
| Aprobado por | Edison L Coronel Romero Director de Carrera |   |
