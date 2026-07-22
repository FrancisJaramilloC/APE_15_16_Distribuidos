# Guía de Actividades Práctico-Experimentales (APE 15 y 16)
## Arquitectura de Microservicios Distribuidos en Python: Balanceador de Carga con Heartbeat Failover, Patrón Circuit Breaker y Persistencia en SQLite (`nodos.db`)

Este proyecto implementa una arquitectura completa de microservicios distribuidos en Python que integra tolerancia a fallos tanto a nivel físico (**Heartbeat Failover**) como a nivel lógico (**Circuit Breaker**), registrando todos los cambios de estado operacional en una base de datos local **SQLite (`nodos.db`)**.

---

## 📐 Arquitectura del Sistema

```mermaid
graph TD
    Client["📱 Cliente HTTP / cURL"] -->|Petición /pago| Orch["⚡ Microservicio Orquestador (:5001)"]
    Orch -->|CircuitBreaker Protection| CB["🛡️ Circuit Breaker (CLOSED/OPEN/HALF-OPEN)"]
    CB -->|Si CLOSED / HALF-OPEN| LB["⚖️ Balanceador de Carga Proxy (:5000)"]
    CB -->|Si OPEN (< 0.01s)| FB["⚠️ Respuesta Fallback Inmediata"]
    LB -->|Least-Connections / IA| B1["🟢 Backend Node 1 (:9001)"]
    LB -->|Least-Connections / IA| B2["🟢 Backend Node 2 (:9002)"]
    
    HB["❤️ Hilo Monitor Heartbeat (< 3s)"] -->|GET /health| B1
    HB -->|GET /health| B2
    HB -->|Actualiza estado| DB[("🗄️ SQLite nodos.db")]
    CB -->|Registra transiciones| DB
```

---

## 🎯 Objetivos de la Práctica

### Guía 15 (Balanceador & Heartbeat)
- **Servidores Backend Simulados**: Nodos independientes en Python procesando tráfico HTTP y respondiendo a sondeos `/health`.
- **Proxy Inverso / Balanceador de Carga**: Distribución dinámica de tráfico (MVC en `balanceador/`).
- **Heartbeat & Failover Automático**: Hilo de monitorización sondeando cada 2s (timeout 1.5s). Detecta caídas en **< 3 segundos** y actualiza el estado en SQLite (`nodos.db` -> tabla `estado_nodos`).

### Guía 16 (Circuit Breaker & Microservicios)
- **Microservicio Orquestador**: Microservicio en puerto `5001` que expone endpoints REST (`/pago`, `/orden`) y delega el procesamiento de forma segura.
- **Patrón Circuit Breaker**: Clase `CircuitBreaker` administrando 3 estados:
  - **`CLOSED` (Cerrado)**: Operación normal. Si ocurren 3 fallos consecutivos, transiciona a `OPEN`.
  - **`OPEN` (Abierto)**: Corta la comunicación de inmediato (< 0.01s), devolviendo una respuesta de **fallback** sin saturar la red. Permanece en `OPEN` durante el tiempo de enfriamiento (cooldown de 10s).
  - **`HALF_OPEN` (Semi-Abierto)**: Permite 1 petición de prueba tras 10s. Si tiene éxito vuelve a `CLOSED`; si falla, regresa a `OPEN`.
- **Persistencia y Auditoría**: Registro de cada cambio de estado del circuito en la tabla `circuit_log` de SQLite (`nodos.db`).

---

## 🛠️ Requisitos e Instalación

### Pasos de Instalación

1. **Clonar/Ingresar al repositorio**:
   ```bash
   cd APE_15_16_Distribuidos
   ```

2. **Crear y activar el entorno virtual Python**:
   - En **Linux / macOS (Bash / Zsh)**:
     ```bash
     python3 -m venv venv
     source venv/bin/activate
     ```
   - En **Linux (Fish Shell)**:
     ```fish
     source venv/bin/activate.fish
     ```
   - En **Windows (CMD / PowerShell)**:
     ```cmd
     python -m venv venv
     venv\Scripts\activate
     ```

3. **Instalar dependencias**:
   ```bash
   pip install -r requirements.txt
   ```

---

## 📁 Estructura del Proyecto Organizada

```
APE_15_16_Distribuidos/
├── .gitignore                   # Exclusiones de Git (venv/, __pycache__/, nodos.db, etc.)
├── requirements.txt             # Dependencias del proyecto (Flask, requests)
├── README.md                    # Documentación completa del proyecto (Guías 15 y 16)
├── backend_server.py            # Simulador de Servidores Backend HTTP (puertos 9001, 9002)
├── test_ape15.py                # Pruebas automatizadas Guía 15 (Heartbeat & Failover)
├── test_ape16.py                # Pruebas automatizadas Guía 16 (Circuit Breaker & Fallback)
├── ape_guia  15 distribuidos-signed.md # Especificación Guía APE 15
├── ape_guia  16 distribuidos-signed.md # Especificación Guía APE 16
├── nodos.db                     # Base de datos SQLite (generada automáticamente)
├── balanceador/                 # Microservicio Balanceador de Carga (Arquitectura MVC)
│   ├── app.py                   # Aplicación Flask (:5000) e hilo de Heartbeat
│   ├── config.py                # Configuración de timeouts e intervalos
│   ├── models/
│   │   ├── __init__.py          # Exportaciones del paquete
│   │   ├── database.py          # Administrador SQLite (estado_nodos y circuit_log)
│   │   ├── server.py            # BackendServer (Health check y proxy)
   │   ├── balancer.py          # Algoritmos de balanceo (Least-connections / IA)
│   │   └── metrics_history.py   # Historial en memoria para Dashboard
│   └── views/
│       ├── api.py               # Endpoints REST (/api/servers, /api/db/nodos, /api/db/circuit)
│       ├── dashboard.py         # Controlador de vistas web
│       └── templates/
│           └── index.html       # Dashboard interactivo con monitoreo SQLite en tiempo real
└── orquestador/                 # Microservicio Orquestador (Guía 16)
    ├── app.py                   # Aplicación Flask (:5001) protegida por Circuit Breaker
    └── circuit_breaker.py      # Lógica del Patrón Circuit Breaker (CLOSED / OPEN / HALF_OPEN)
```

---

## 🗄️ Modelo de Datos SQLite (`nodos.db`)

La base de datos `nodos.db` administra dos tablas clave para el cruce de datos físicos y lógicos:

### 1. Tabla `estado_nodos` (Heartbeat Físico - Guía 15)
```sql
CREATE TABLE IF NOT EXISTS estado_nodos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nodo TEXT NOT NULL UNIQUE,          -- Ejemplo: "127.0.0.1:9001"
    puerto INTEGER NOT NULL,            -- Ejemplo: 9001
    estado TEXT NOT NULL,               -- "ACTIVO" o "INACTIVO"
    latencia REAL DEFAULT 0.0,          -- Latencia en ms
    ultima_actualizacion TEXT NOT NULL  -- YYYY-MM-DD HH:MM:SS
);
```

### 2. Tabla `circuit_log` (Auditoría Lógica de Red - Guía 16)
```sql
CREATE TABLE IF NOT EXISTS circuit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    servicio TEXT NOT NULL,             -- Ejemplo: "ServicioOrquestador"
    estado_anterior TEXT NOT NULL,      -- "CLOSED", "OPEN", "HALF_OPEN"
    nuevo_estado TEXT NOT NULL,         -- "CLOSED", "OPEN", "HALF_OPEN"
    motivo TEXT,                        -- Descripción del evento
    timestamp TEXT NOT NULL             -- YYYY-MM-DD HH:MM:SS
);
```

---

## 🚀 Guía de Ejecución Paso a Paso

### 1. Iniciar Servidores Backend (Terminal 1)
```bash
python3 backend_server.py --all
```

### 2. Iniciar Balanceador de Carga (Terminal 2)
```bash
python3 balanceador/app.py
```

### 3. Iniciar Microservicio Orquestador (Terminal 3)
```bash
python3 orquestador/app.py
```

### 4. Abrir Dashboard Web
Accede a [http://localhost:5000](http://localhost:5000) para monitorear en tiempo real:
- Servidores Backend activos/inactivos.
- Persistencia física `estado_nodos`.
- Auditoría lógica `circuit_log`.

---

## 📊 Tabla de Observaciones (Resultados Esperados Guía 16)

| Escenario | Estado de Backends | Estado BD: `estado_nodos` | Estado del Circuito (Esperado) | Tiempo de respuesta Orquestador | Registro BD: `circuit_log` |
| --- | --- | --- | --- | --- | --- |
| **Backends activos** | 9001: ACTIVO, 9002: ACTIVO | 9001: ACTIVO, 9002: ACTIVO | **`CLOSED`** | ~0.05s | `CLOSED` |
| **Caen ambos Backends (Peticiones 1-3)** | 9001: Inactivo, 9002: Inactivo | 9001: INACTIVO, 9002: INACTIVO | **`CLOSED` -> Acumula 3 fallos** | ~1s | - |
| **Backends siguen caídos (Petición 4+)** | 9001: Inactivo, 9002: Inactivo | 9001: INACTIVO, 9002: INACTIVO | **`OPEN`** | **< 0.01s** (Fallback) | `OPEN` |
| **Pasan 10s (Prueba Half-Open)** | 9001: Inactivo, 9002: Inactivo | 9001: INACTIVO, 9002: INACTIVO | **`HALF_OPEN` -> `OPEN`** | ~1s | `HALF_OPEN`, `OPEN` |
| **Backends recuperados + 10s** | 9001: ACTIVO, 9002: ACTIVO | 9001: ACTIVO, 9002: ACTIVO | **`HALF_OPEN` -> `CLOSED`** | ~0.05s | `HALF_OPEN`, `CLOSED` |

---

## ❓ Preguntas de Control Resueltas (Guía 16)

1. **¿Si un solo backend (ej. 9001) cae pero el otro sigue activo, el Circuit Breaker del Orquestador debería abrirse?**
   - **Respuesta**: **No**. Debido a que el Balanceador de Carga ejecuta failover automático a nivel de proxy, al caer el puerto 9001 el balanceador redirige inmediatamente el 100% de las peticiones hacia el nodo activo (puerto 9002). El Orquestador continúa recibiendo respuestas HTTP 200 exitosas, por lo que el Circuit Breaker permanece en estado **`CLOSED`**. El circuito solo debe abrirse cuando **todos los nodos del pool caen simultáneamente** y el balanceador no puede resolver ninguna petición.

2. **¿Qué diferencia hay entre el mecanismo de Heartbeat y el Circuit Breaker en cuanto a la detección de fallos?**
   - **Respuesta**: El **Heartbeat** es un mecanismo de *monitoreo activo por sondeo periódico* (sondea `/health` cada 2s en segundo plano), mientras que el **Circuit Breaker** es un patrón de *monitoreo pasivo en línea de tráfico* que reacciona en tiempo real según el éxito o fracaso de las peticiones realizadas por los usuarios.

3. **Al abrirse el circuito, ¿habrá diferencia temporal entre el timestamp de `estado_nodos` y el de `circuit_log`?**
   - **Respuesta**: **Sí**. `circuit_log` registra de inmediato la reacción lógica de red al alcanzar el umbral de fallos en peticiones reales, mientras que `estado_nodos` depende de la llegada del siguiente pulso de sondeo del hilo Heartbeat.

4. **¿En qué escenario de la vida real (ej. Netflix) es crítico tener un Fallback en lugar de un error 500?**
   - **Respuesta**: En la carga del catálogo o recomendaciones personalizadas. Si el microservicio de recomendaciones falla o se satura, devolver un Fallback (ej. lista estática de tendencias o populares) permite que el usuario continúe usando la plataforma normalmente sin ver una pantalla de error.

5. **Si el balanceador de carga muere repentinamente (puerto 5000), ¿cómo reaccionará el Circuit Breaker del Orquestador?**
   - **Respuesta**: El Orquestador acumulará inmediatamente fallos de conexión HTTP y el Circuit Breaker se **ABRIRÁ (`OPEN`)** tras 3 intentos. El sistema entregará la respuesta de Fallback en < 0.01s. Cuando el proceso del balanceador vuelva a levantarse, el circuito pasará a `HALF_OPEN` tras el cooldown de 10s y se recuperará autónomamente a `CLOSED`.
