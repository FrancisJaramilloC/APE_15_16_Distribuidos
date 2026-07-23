# frontend/app.py
"""
Servidor Web Flask — Dashboard Interactivo de Monitoreo.
Proporciona la interfaz de usuario para enviar solicitudes al Orquestador,
monitorear las métricas de peticiones en tiempo real y consultar las tablas SQLite (estado_nodos y circuit_log).
"""

import os
import sqlite3
import time
import requests
from flask import Flask, render_template, jsonify, request

app = Flask(__name__)

# Configuración de URLs de los servicios mediante variables de entorno o valores por defecto
ORCHESTRATOR_URL = os.getenv('ORCHESTRATOR_URL', 'http://127.0.0.1:8080')
BALANCEADOR_URL = os.getenv('BALANCEADOR_URL', 'http://127.0.0.1:8000')

# Ruta absoluta al archivo compartido nodos.db en la raíz del proyecto
DB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'nodos.db'))

def query_sqlite(query, params=()):
    """
    Función de utilidad para consultar la base de datos SQLite nodos.db
    y retornar los resultados como diccionarios de Python.
    """
    if not os.path.exists(DB_PATH):
        return []
    try:
        conn = sqlite3.connect(DB_PATH, timeout=5)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        cursor.execute(query, params)
        rows = [dict(r) for r in cursor.fetchall()]
        conn.close()
        return rows
    except Exception as e:
        print(f"[SQLite Query Error] {e}")
        return []

@app.route('/')
def index():
    """Ruta principal que sirve la plantilla web HTML del Dashboard."""
    return render_template('index.html')

@app.route('/api/orquestar', methods=['POST'])
def proxy_orquestar():
    """
    Endpoint Proxy invocado por la web al presionar 'Enviar 1 Petición'.
    Reenvía la solicitud HTTP GET hacia el Microservicio Orquestador (:8080/pago)
    y mide la latencia total de ida y vuelta.
    """
    start = time.time()
    try:
        url = f"{ORCHESTRATOR_URL}/pago"
        resp = requests.get(url, timeout=4)
        elapsed = (time.time() - start) * 1000
        
        try:
            data = resp.json()
        except Exception:
            data = {"raw": resp.text}
        
        return jsonify({
            "http_code": resp.status_code,
            "elapsed_ms": round(elapsed, 2),
            "data": data
        })
    except Exception as e:
        elapsed = (time.time() - start) * 1000
        return jsonify({
            "http_code": 503,
            "elapsed_ms": round(elapsed, 2),
            "error": str(e)
        }), 503

@app.route('/api/circuit-status')
def get_circuit_status():
    """Consulta el estado en tiempo real del Circuit Breaker desde el Orquestador."""
    try:
        resp = requests.get(f"{ORCHESTRATOR_URL}/api/circuit/status", timeout=2)
        if resp.status_code == 200:
            return jsonify(resp.json())
    except Exception:
        pass
    
    return jsonify({
        "servicio": "ServicioOrquestador",
        "estado": "DESCONOCIDO",
        "fallosConsecutivos": 0,
        "tiempoRestanteCooldownSegundos": 0
    })

@app.route('/api/nodos-db')
def get_nodos_db():
    """Consulta los registros del monitoreo de Heartbeat en la tabla estado_nodos."""
    rows = query_sqlite("SELECT * FROM estado_nodos ORDER BY puerto ASC;")
    return jsonify(rows)

@app.route('/api/circuit-db')
def get_circuit_db():
    """Consulta los registros de auditoría de transiciones en la tabla circuit_log."""
    rows = query_sqlite("SELECT * FROM circuit_log ORDER BY id DESC LIMIT 50;")
    return jsonify(rows)

if __name__ == '__main__':
    port = int(os.getenv('FRONTEND_PORT', 5000))
    print()
    print("=" * 65)
    print("  Dashboard Web Interactivo — Microservicios Distribuidos")
    print("=" * 65)
    print(f"  Base de datos : {DB_PATH}")
    print(f"  Orquestador   : {ORCHESTRATOR_URL}")
    print(f"  Balanceador   : {BALANCEADOR_URL}")
    print(f"  Frontend Web  : http://localhost:{port}")
    print("=" * 65)
    print()
    app.run(host='0.0.0.0', port=port, debug=False, threaded=True)
