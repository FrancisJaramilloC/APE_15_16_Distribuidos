#!/usr/bin/env python3
"""
Script de Verificación para Laboratorio Físico (Cluster Multimáquina conectado vía Switch Ethernet).
Permite enviar peticiones al Orquestador y Balanceador especificando las IPs de la red LAN.
"""

import sys
import argparse
import time
import requests
import json

def main():
    parser = argparse.ArgumentParser(description="Verificador de Cluster Multimáquina en Red LAN / Switch")
    parser.add_argument('--orquestador', type=str, default='http://127.0.0.1:5001', help="URL base del Orquestador (ej: http://192.168.1.10:5001)")
    parser.add_argument('--balanceador', type=str, default='http://127.0.0.1:5000', help="URL base del Balanceador (ej: http://192.168.1.20:5000)")
    parser.add_argument('--requests', type=int, default=10, help="Número de peticiones a enviar")
    args = parser.parse_args()

    orch_url = args.orquestador.rstrip('/')
    bal_url = args.balanceador.rstrip('/')

    print("\n" + "=" * 75)
    print("  VERIFICACIÓN DE CLUSTER MULTIMÁQUINA EN RED (SWITCH ETHERNET / LAN)")
    print("=" * 75)
    print(f"  Microservicio Orquestador : {orch_url}")
    print(f"  Microservicio Balanceador : {bal_url}")
    print("=" * 75 + "\n")

    # 1. Consultar estado del Circuit Breaker
    try:
        cb_res = requests.get(f"{orch_url}/api/circuit/status", timeout=3).json()
        print(f"[+] Conexión con Orquestador OK | Estado Circuit Breaker: {cb_res['estado']}")
    except Exception as e:
        print(f"[-] Error conectando con Orquestador en {orch_url}: {e}")
        return

    # 2. Consultar nodos del cluster registrados en SQLite vía Balanceador
    try:
        nodes_res = requests.get(f"{bal_url}/api/db/nodos", timeout=3).json()
        print(f"\n[+] Nodos registrados en el Cluster (nodos.db):")
        for n in nodes_res:
            print(f"    -> IP/Puerto: {n['nodo']} | Estado BD: {n['estado']} | Latencia: {n['latencia']}ms | Actualizado: {n['ultima_actualizacion']}")
    except Exception as e:
        print(f"[-] Error consultando SQLite en Balanceador {bal_url}: {e}")

    # 3. Enviar peticiones de prueba a través de la red
    print(f"\n[+] Enviando {args.requests} peticiones a través del Orquestador ({orch_url}/pago)...")
    success = 0
    fallback = 0
    errors = 0

    for i in range(1, args.requests + 1):
        start = time.time()
        try:
            r = requests.get(f"{orch_url}/pago", timeout=5)
            elapsed = (time.time() - start) * 1000
            data = r.json()
            st = data.get("status", "DESCONOCIDO")
            circuit_st = data.get("circuit_state", "DESCONOCIDO")

            if st == "SUCCESS":
                success += 1
                backend_info = data.get("respuesta_backend", {})
                port = backend_info.get("port", "?")
                print(f"  Petición #{i:02d} -> HTTP {r.status_code} | Circuito: {circuit_st} | Tiempo: {elapsed:.2f}ms | Procesado por Backend: {port}")
            elif st == "FALLBACK":
                fallback += 1
                print(f"  Petición #{i:02d} -> HTTP {r.status_code} | Circuito: {circuit_st} (FALLBACK INMEDIATO) | Tiempo: {elapsed:.2f}ms")
            else:
                errors += 1
                print(f"  Petición #{i:02d} -> HTTP {r.status_code} | Error: {data.get('error')}")

        except Exception as e:
            elapsed = (time.time() - start) * 1000
            errors += 1
            print(f"  Petición #{i:02d} -> Falló conexión de red: {e} ({elapsed:.2f}ms)")

        time.sleep(0.1)

    print("\n" + "=" * 75)
    print(f"  RESUMEN: Exitosas: {success} | Fallbacks: {fallback} | Errores: {errors}")
    print("=" * 75 + "\n")

if __name__ == '__main__':
    main()
