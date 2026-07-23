# frontend/app.py
import os
import sqlite3
import time
import requests
from flask import Flask, render_template, jsonify, request

app = Flask(__name__)

ORCHESTRATOR_URL = os.getenv('ORCHESTRATOR_URL', 'http://127.0.0.1:8080')
BALANCEADOR_URL = os.getenv('BALANCEADOR_URL', 'http://127.0.0.1:8000')
DB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'nodos.db'))

def query_sqlite(query, params=()):
    if not os.path.exists(DB_PATH):
        return []
    try:
        conn = sqlite3.connect(DB_PATH, timeout=10)
        conn.execute("PRAGMA journal_mode=WAL;")
        conn.execute("PRAGMA busy_timeout=5000;")
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
    return render_template('index.html')

@app.route('/api/orquestar', methods=['POST'])
def proxy_orquestar():
    """Realiza una petición al Microservicio Orquestador."""
    start = time.time()
    try:
        # Petición HTTP al Orquestador
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
    """Consulta el estado en tiempo real del Circuit Breaker."""
    try:
        resp = requests.get(f"{ORCHESTRATOR_URL}/api/circuit/status", timeout=2)
        if resp.status_code == 200:
            return jsonify(resp.json())
    except Exception:
        pass
    
    # Si el servicio no responde, consultar SQLite o fallback
    return jsonify({
        "servicio": "ServicioOrquestador",
        "estado": "DESCONOCIDO",
        "fallosConsecutivos": 0,
        "tiempoRestanteCooldownSegundos": 0
    })

@app.route('/api/nodos-db')
def get_nodos_db():
    """Consulta los nodos sondeados del Balanceador (vía HTTP REST o SQLite)."""
    try:
        resp = requests.get(f"{BALANCEADOR_URL}/api/db/nodos", timeout=2)
        if resp.status_code == 200:
            return jsonify(resp.json())
    except Exception:
        pass
    rows = query_sqlite("SELECT * FROM estado_nodos ORDER BY puerto ASC;")
    return jsonify(rows)

@app.route('/api/circuit-db')
def get_circuit_db():
    """Consulta la auditoría del Circuit Breaker (vía HTTP REST u Orquestador/SQLite)."""
    try:
        resp = requests.get(f"{ORCHESTRATOR_URL}/api/circuit/logs", timeout=2)
        if resp.status_code == 200:
            return jsonify(resp.json())
    except Exception:
        pass
    try:
        resp = requests.get(f"{BALANCEADOR_URL}/api/db/circuit", timeout=2)
        if resp.status_code == 200:
            return jsonify(resp.json())
    except Exception:
        pass
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
