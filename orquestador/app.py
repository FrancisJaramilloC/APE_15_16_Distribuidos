# orquestador/app.py
import sys
import os
import time
import requests
from flask import Flask, jsonify, request

# Importar el DatabaseManager desde el módulo balanceador para compartir nodos.db
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'balanceador')))
from models.database import DatabaseManager
from circuit_breaker import CircuitBreaker, CircuitBreakerOpenException

# Configuración del Microservicio Orquestador
BALANCER_URL = os.getenv('BALANCER_URL', 'http://127.0.0.1:5000')
ORCHESTRATOR_PORT = int(os.getenv('ORCHESTRATOR_PORT', 5001))

app = Flask(__name__)

# Inicializar Administrador de BD y Circuit Breaker
db_manager = DatabaseManager()
circuit_breaker = CircuitBreaker(
    service_name="ServicioOrquestador",
    failure_threshold=3,    # Umbral de 3 fallos consecutivos
    recovery_timeout=10.0,  # 10 segundos de cooldown
    db_manager=db_manager
)

def make_backend_call(path="procesar-pago"):
    """Función envuelta por el Circuit Breaker para comunicarse con el Balanceador."""
    url = f"{BALANCER_URL}/balance/{path}"
    # Timeout corto (1.0s) para detectar fallos rápidos ante caídas de nodos
    resp = requests.get(url, timeout=1.0)
    if resp.status_code != 200:
        raise Exception(f"Backend respondió con status HTTP {resp.status_code}")
    return resp.json()

@app.route('/', methods=['GET', 'POST'])
@app.route('/pago', methods=['GET', 'POST'])
@app.route('/orden', methods=['GET', 'POST'])
def handle_orchestrated_request():
    """Endpoint principal del Orquestador protegido por Circuit Breaker."""
    start_time = time.time()
    try:
        # Llamada protegida por Circuit Breaker
        backend_data = circuit_breaker.call(make_backend_call, path="procesar-pago")
        elapsed = (time.time() - start_time) * 1000

        return jsonify({
            "status": "SUCCESS",
            "microservicio": "Orquestador",
            "circuit_state": circuit_breaker.state,
            "tiempo_respuesta_ms": round(elapsed, 2),
            "respuesta_backend": backend_data
        }), 200

    except CircuitBreakerOpenException:
        # Respuesta Inmediata de Fallback (< 0.01s) cuando el circuito está ABIERTO
        elapsed = (time.time() - start_time) * 1000
        return jsonify({
            "status": "FALLBACK",
            "circuit_state": "OPEN",
            "error": "Circuit Breaker ABIERTO (OPEN)",
            "message": "Servicio de Pagos aislado preventivamente por fallos acumulados. Respuesta alternativa entregada de forma inmediata.",
            "tiempo_respuesta_ms": round(elapsed, 2),
            "fallback_data": {
                "transaccion_id": "FALLBACK-TEMP-999",
                "mensaje": "Su solicitud ha sido encolada de forma segura para procesamiento diferido."
            }
        }), 503

    except Exception as e:
        # Excepción durante estado CLOSED o HALF_OPEN al intentar conectar
        elapsed = (time.time() - start_time) * 1000
        return jsonify({
            "status": "ERROR",
            "circuit_state": circuit_breaker.state,
            "error": str(e),
            "message": "Fallo en la comunicación con el Balanceador/Backend.",
            "tiempo_respuesta_ms": round(elapsed, 2)
        }), 502

@app.route('/api/circuit/status', methods=['GET'])
def get_circuit_status():
    """Retorna el estado actual del Circuit Breaker."""
    return jsonify(circuit_breaker.get_status())

@app.route('/api/circuit/logs', methods=['GET'])
def get_circuit_logs():
    """Retorna los registros de auditoría de la tabla circuit_log de SQLite."""
    return jsonify(db_manager.get_circuit_logs())

if __name__ == '__main__':
    print()
    print("=" * 62)
    print("  Microservicio Orquestador — Circuit Breaker (Guía 16)")
    print("=" * 62)
    print(f"  Base de datos : {db_manager.db_path}")
    print(f"  Orquestador   : http://127.0.0.1:{ORCHESTRATOR_PORT}/")
    print(f"  Balanceador   : {BALANCER_URL}")
    print("=" * 62)
    print()

    app.run(host='0.0.0.0', port=ORCHESTRATOR_PORT, debug=False, threaded=True)
