## Guía de Actividades Práctico- Experimentales Nro. 015

## 2. Objetivo(s) de la Práctica:

Diseñar e implementar un balanceador de carga básico en Python que utilice un mecanismo de heartbeat (latido) para monitorizar la salud de los servidores backend y ejecute procesos de failover (conmutación por error) de forma automática, registrando el estado de los nodos en una base de datos local SQLite.

- Desarrollar servidores backend simulados en Python capaces de responder a peticiones HTTP y a señales de heartbeat.

- Implementar un balanceador de carga (Proxy inverso) que distribuya las peticiones entre los nodos activos.

- Programar un hilo de monitorización (heartbeat) que verifique la disponibilidad de los nodos periódicamente y actualice su estado (ACTIVO/INACTIVO) en una base de datos SQLite.

- Comprobar de forma empírica la funcionalidad de failover al simular la caída de un servidor backend, observando cómo el tráfico se redirige a los nodos restantes.


## 3. Materiales y reactivos:

- Equipo de cómputo con privilegios de administrador (opcional, solo si se requiere abrir puertos específicos en el firewall).

- Terminal de línea de comandos.

- Editor de texto o IDE (VS Code, PyCharm, etc.).

## 4. Recursos

## Hardware:

Procesador: Dual-core 2.0 GHz o superior.

Memoria RAM: Mínimo 4 GB.

Conexión de red local (los nodos se ejecutarán en localhost).

## Software:

## Sistema Operativo: Linux, macOS o Windows.

Python 3.8 o superior.

Librerías de Python: http.server, sqlite3, urllib, threading (todas incluidas en la librería estándar de Python, no requiere pip install).

## 5. Metodología/procedimiento:

Paso 2: instalar dependencias.

Paso 3: Probar el funcionamiento

Paso 4: agregar la base de datos para agregar las IP


## 6. Resultados esperados:

El balanceador reparte las cargas entre los dos nodos mientras ambos estén activos.

- Al detener un nodo, el hilo heartbeat detecta la caída en un máximo de 3 segundos, actualiza la base de datos SQLite y el balanceador deja de enviar tráfico al nodo caído (Failover automático).

- Al recuperar el nodo, el heartbeat lo reintegra al pool de nodos activos.

- El estudiante comprueba que el estado persistente en SQLite coincide con la realidad operativa del sistema en memoria


