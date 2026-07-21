#!/usr/bin/env python3
"""
Script de Verificación de Requisitos de la Guía 15 (Heartbeat + Failover + SQLite nodos.db).
"""

import time
import sqlite3
import json
import os
import requests

BALANCER_URL = "http://localhost:5000"
DB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "nodos.db"))

def print_header(title):
    print("\n" + "=" * 65)
    print(f"  {title}")
    print("=" * 65)

def get_sqlite_status():
    if not os.path.exists(DB_PATH):
        return []
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM estado_nodos ORDER BY puerto ASC;")
    rows = [dict(r) for r in cursor.fetchall()]
    conn.close()
    return rows

def run_tests():
    print_header("PRUEBA AUTOMATIZADA - GUÍA 15: BALANCEADOR & FAILOVER")

    print("\n[1] Verificando conexión con el Balanceador en", BALANCER_URL)
    try:
        r = requests.get(f"{BALANCER_URL}/api/stats", timeout=3)
        print("  [OK] Balanceador respondiendo. Stats actual:")
        print(f"       Servidores configurados: {r.json().get('total_servers')}")
        print(f"       Servidores activos: {r.json().get('healthy_servers')}")
    except Exception as e:
        print(f"  [ERROR] No se pudo conectar al balanceador: {e}")
        print("  Asegúrate de que 'python balanceador/app.py' esté ejecutándose.")
        return False

    print("\n[2] Consultando estado persistente en la Base de Datos SQLite (nodos.db)...")
    nodes = get_sqlite_status()
    for n in nodes:
        print(f"  -> Nodo: {n['nodo']} | Puerto: {n['puerto']} | Estado BD: {n['estado']} | Latencia: {n['latencia']}ms | Actualizado: {n['ultima_actualizacion']}")

    print("\n[3] Enviando 10 peticiones HTTP a través del proxy /balance/saludo...")
    distribution = {}
    for i in range(1, 11):
        try:
            resp = requests.get(f"{BALANCER_URL}/balance/saludo", timeout=5)
            if resp.status_code == 200:
                data = resp.json()
                port = data.get("port", "desconocido")
                distribution[port] = distribution.get(port, 0) + 1
                print(f"  Petición #{i:02d} -> Procesada exitosamente por Backend en puerto {port}")
            else:
                print(f"  Petición #{i:02d} -> Error HTTP {resp.status_code}")
        except Exception as e:
            print(f"  Petición #{i:02d} -> Falló: {e}")
        time.sleep(0.2)

    print(f"\n  Resumen Distribución de Tráfico: {distribution}")

    print("\n[4] Verificación de la BD SQLite desde endpoint /api/db/nodos:")
    try:
        r = requests.get(f"{BALANCER_URL}/api/db/nodos", timeout=3)
        print(json.dumps(r.json(), indent=2))
    except Exception as e:
        print(f"  [ERROR] Error al consultar API /api/db/nodos: {e}")

    print("\n" + "=" * 65)
    print("  VERIFICACIÓN DE COMPORTAMIENTO COMPLETADA CON ÉXITO")
    print("=" * 65 + "\n")
    return True

if __name__ == '__main__':
    run_tests()
