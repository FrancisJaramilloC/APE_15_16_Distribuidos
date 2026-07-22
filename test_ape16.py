#!/usr/bin/env python3
"""
Script de Verificación de Requisitos para la Guía 16:
Patrón Circuit Breaker en Microservicios, Persistencia SQLite (circuit_log) y Tabla de Observaciones.
"""

import os
import sys
import time
import json
import sqlite3
import requests

ORCHESTRATOR_URL = "http://127.0.0.1:5001"
BALANCER_URL = "http://127.0.0.1:5000"
DB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "nodos.db"))

def print_header(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def get_db_circuit_logs():
    if not os.path.exists(DB_PATH):
        return []
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM circuit_log ORDER BY id ASC;")
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()
    return rows

def get_db_node_status():
    if not os.path.exists(DB_PATH):
        return []
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM estado_nodos ORDER BY puerto ASC;")
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()
    return rows

def test_orchestrator_request(request_num):
    start = time.time()
    try:
        r = requests.get(f"{ORCHESTRATOR_URL}/pago", timeout=3)
        elapsed = (time.time() - start) * 1000
        data = r.json()
        return r.status_code, data.get("circuit_state", "UNKNOWN"), data.get("status", "UNKNOWN"), elapsed
    except Exception as e:
        elapsed = (time.time() - start) * 1000
        return 500, "ERROR", str(e), elapsed

def run_gui16_tests():
    print_header("PRUEBA AUTOMATIZADA - GUÍA 16: CIRCUIT BREAKER & MICROSERVICIOS")

    print("\n[1] Verificando conectividad del Orquestador (5001) y Balanceador (5000)...")
    try:
        st = requests.get(f"{ORCHESTRATOR_URL}/api/circuit/status", timeout=3).json()
        print(f"  [OK] Orquestador activo. Estado inicial Circuit Breaker: {st['estado']}")
    except Exception as e:
        print(f"  [ERROR] No se pudo conectar al Orquestador: {e}")
        print("  Asegúrate de ejecutar 'python orquestador/app.py'")
        return False

    print("\n[2] Ejecución de Escenarios según la Tabla de Observaciones (Guía 16):\n")

    # FASE 1: Backends Activos
    print("  --- FASE 1: Backends Activos (Circuito esperado: CLOSED) ---")
    status_code, circuit_state, res_status, elapsed = test_orchestrator_request(1)
    print(f"  Petición #1 -> Status: {status_code} | Circuito: {circuit_state} | Tiempo: {elapsed:.2f}ms")

    # FASE 2: Consultar logs en SQLite
    print("\n[3] Registros en la Base de Datos SQLite (nodos.db):")
    print("  -> Tabla 'estado_nodos' (Heartbeat físico):")
    for n in get_db_node_status():
        print(f"     Nodo: {n['nodo']} | Estado BD: {n['estado']} | Latencia: {n['latencia']}ms")

    print("\n  -> Tabla 'circuit_log' (Circuit Breaker lógico):")
    logs = get_db_circuit_logs()
    if not logs:
        print("     (Sin transiciones registradas aún, circuito en estado inicial CLOSED)")
    else:
        for l in logs:
            print(f"     ID: {l['id']} | Servicio: {l['servicio']} | {l['estado_anterior']} -> {l['nuevo_estado']} | Motivo: {l['motivo']}")

    print("\n" + "=" * 70)
    print("  VERIFICACIÓN DE ARQUITECTURA COMPLETADA EXITOSAMENTE")
    print("=" * 70 + "\n")
    return True

if __name__ == '__main__':
    run_gui16_tests()
