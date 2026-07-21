# Guía de Actividades Práctico-Experimentales Nro. 015
## Balanceador de Carga con Mecanismo de Heartbeat, Failover Automático y Persistencia en SQLite

Este proyecto implementa una arquitectura distribuida en Python compuesta por servidores backend simulados, un **Proxy Inverso / Balanceador de Carga** dinámico con arquitectura **MVC**, un hilo de monitorización en tiempo real por **Heartbeat (latido)** y persistencia transaccional del estado del pool en una base de datos **SQLite (`nodos.db`)**.

---

## 🎯 Objetivos de la Práctica

1. **Servidores Backend Simulados**: Desarrollar nodos backend en Python capaces de procesar peticiones HTTP y responder a señales de salud (`GET /health`).
2. **Balanceador de Carga (Proxy Inverso)**: Implementar un proxy inverso que distribuya el tráfico entrante de manera equitativa o inteligente entre los nodos disponibles.
3. **Hilo de Monitorización Heartbeat**: Crear un hilo en segundo plano que sondee la disponibilidad de cada servidor backend a intervalos regulares (intervalo de 2s, timeout de 1.5s) y actualice el estado (`ACTIVO` / `INACTIVO`) en la tabla `estado_nodos` de SQLite (`nodos.db`) en **menos de 3 segundos**.
4. **Conmutación por Error (Failover Automático)**: Demostrar empíricamente que ante la caída de una instancia backend, el balanceador redirige inmediatamente todo el tráfico al nodo sobreviviente sin interrumpir el servicio.
5. **Reintegración Autónoma**: Verificar que al recuperar la instancia caída, el hilo de Heartbeat detecta su restablecimiento, actualiza la base de datos a `ACTIVO` y reincorpora el nodo al pool de tráfico.

---

## 🛠️ Requisitos e Instalación

### Requisitos Previos
- **Python**: Version 3.8 o superior.
- **Git** (opcional, para control de versiones).

### Pasos de Instalación

1. **Clonar el repositorio o situarse en el directorio del proyecto**:
   ```bash
   cd APE_15_16_Distribuidos
   ```

2. **Crear y activar el entorno virtual Python**:
   - En **Linux / macOS**:
     ```bash
     python3 -m venv venv
     source venv/bin/activate
     ```
   - En **Windows (CMD / PowerShell)**:
     ```cmd
     python -m venv venv
     venv\Scripts\activate
     ```

3. **Instalar las dependencias necesarias**:
   ```bash
   pip install -r requirements.txt
   ```

---

## 📁 Estructura del Proyecto

```
APE_15_16_Distribuidos/
├── .gitignore                   # Exclusiones de Git (venv/, __pycache__/, nodos.db, etc.)
├── requirements.txt             # Dependencias del proyecto (Flask, requests)
├── backend_server.py            # Simulador de Servidores Backend HTTP (puertos 9001, 9002)
├── test_ape15.py                # Script de pruebas automatizadas y verificación de failover
├── ape_guia  15 distribuidos-signed.md # Guía oficial APE 15
├── ape_guia  16 distribuidos-signed.md # Guía oficial APE 16 (próxima fase)
├── nodos.db                     # Base de datos SQLite (generada automáticamente en runtime)
└── balanceador/                 # Código del Balanceador de Carga (Arquitectura MVC)
    ├── app.py                   # Punto de entrada Flask e hilo Heartbeat
    ├── config.py                # Configuración de timeouts e intervalos
    ├── requirements.txt         # Copia de dependencias para el módulo balanceador
    ├── models/
    │   ├── __init__.py          # Exportación de modelos
    │   ├── database.py          # Administrador de persistencia SQLite (nodos.db)
    │   ├── server.py            # Modelo BackendServer (Health check y proxying)
    │   ├── balancer.py          # Lógica AILoadBalancer (Least-connections / IA)
    │   └── metrics_history.py   # Historial en memoria para gráficos del Dashboard
    └── views/
        ├── api.py               # API REST (/api/servers, /api/db/nodos, /balance/<ruta>)
        ├── dashboard.py         # Vista principal del Dashboard web
        └── templates/
            └── index.html       # Interfaz de usuario interactiva con HTML/CSS/JS
```

---

## 🗄️ Modelo de Datos y Persistencia SQLite (`nodos.db`)

El sistema utiliza la base de datos `nodos.db` ubicada en la raíz del proyecto para persistir el estado operativo de cada servidor backend. La tabla principal `estado_nodos` se crea automáticamente al iniciar el aplicativo con el siguiente esquema SQL:

```sql
CREATE TABLE IF NOT EXISTS estado_nodos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nodo TEXT NOT NULL UNIQUE,          -- Ejemplo: "127.0.0.1:9001"
    puerto INTEGER NOT NULL,            -- Ejemplo: 9001
    estado TEXT NOT NULL,               -- "ACTIVO" o "INACTIVO"
    latencia REAL DEFAULT 0.0,          -- Latencia medida en ms
    ultima_actualizacion TEXT NOT NULL  -- Formato YYYY-MM-DD HH:MM:SS
);
```

### Consultar el Estado desde Terminal
Puedes inspeccionar la base de datos SQLite directamente con la consola interactiva de SQLite3 o mediante `sqlite3`:

```bash
sqlite3 nodos.db "SELECT * FROM estado_nodos;"
```

**Ejemplo de resultado:**
| id | nodo | puerto | estado | latencia | ultima_actualizacion |
|---|---|---|---|---|---|
| 1 | 127.0.0.1:9001 | 9001 | ACTIVO | 6.45 | 2026-07-21 11:14:05 |
| 2 | 127.0.0.1:9002 | 9002 | INACTIVO | 0.0 | 2026-07-21 11:14:07 |

---

## 🚀 Guía de Ejecución y Pruebas

### 1. Iniciar los Servidores Backend Simulados
Abre una terminal con el entorno virtual activado y ejecuta el grupo de servidores backend (en los puertos **9001** y **9002**):

```bash
python3 backend_server.py --all
```

*Salida esperada:*
```text
=== Iniciando grupo de Servidores Backend (9001 y 9002) ===
[*] Servidor Backend iniciado en http://127.0.0.1:9001
[*] Servidor Backend iniciado en http://127.0.0.1:9002
```

### 2. Iniciar el Balanceador de Carga
En una segunda terminal con el entorno virtual activado, ejecuta el balanceador:

```bash
python3 balanceador/app.py
```

*Salida esperada:*
```text
==============================================================
  AI Load Balancer & Heartbeat Failover — Guía 15 APE
==============================================================
  Base de datos : /ruta/al/proyecto/nodos.db
  Dashboard     : http://localhost:5000
  Proxy         : http://localhost:5000/balance/<ruta>
==============================================================
```

### 3. Acceder al Dashboard Web
Abre tu navegador de preferencia en:
👉 [http://localhost:5000](http://localhost:5000)

En la interfaz podrás visualizar:
- **KPIs en tiempo real**: Solicitudes totales, RPS, Latencia Promedio, Servidores Activos.
- **Tabla de Persistencia SQLite**: Refleja dinámicamente la tabla `estado_nodos` de `nodos.db`.
- **Log de Decisiones de Tráfico**: Historial de cada proxy request atendido.
- **Gráficos interactivos**: Distribución de carga y latencias.

---

## 🔬 Verificación Empírica del Failover (Conmutación por Error)

Se puede verificar el cumplimiento de los resultados esperados utilizando el script de prueba automatizada o manualmente:

### Prueba Automatizada
Ejecuta en una tercera terminal:

```bash
python3 test_ape15.py
```

### Prueba Manual de Caída y Recuperación (Failover)

1. **Verificación Inicial (Ambos Nodos Activos)**:
   Realiza peticiones a través del proxy:
   ```bash
   curl http://localhost:5000/balance/saludo
   ```
   *Resultado*: Las respuestas alternan exitosamente entre el backend 9001 y 9002.

2. **Simular Caída del Backend 9001**:
   Detén el proceso del puerto 9001 (o cancela `backend_server.py`).
   - En **máximo 2 a 3 segundos**, el hilo de Heartbeat detecta la falla.
   - Se actualiza automáticamente la base de datos `nodos.db` marcando el puerto 9001 como `INACTIVO`.
   - El balanceador conmuta todo el tráfico inmediatamente hacia `127.0.0.1:9002` sin arrojar errores al cliente HTTP.

3. **Recuperación del Nodo 9001**:
   Vuelve a iniciar el servidor backend.
   - En **menos de 3 segundos**, el hilo de Heartbeat valida la respuesta 200 en `http://127.0.0.1:9001/health`.
   - La base de datos `nodos.db` se actualiza a `ACTIVO`.
   - El nodo se reintegra automáticamente al pool de balanceo.

---

## 📌 Conclusión y Base para la Guía 16

El desarrollo realizado en la **Guía 15** deja establecida una base sólida, modular y escalable. Toda la lógica de monitoreo **Heartbeat** y persistencia en **SQLite (`nodos.db`)** servirá de soporte para la implementación del patrón **Circuit Breaker** en la **Guía 16**, agregando tablas adicionales (`circuit_log`) y el Microservicio Orquestador.
