#!/usr/bin/env python3
"""
Simulador de Servidores Backend para Guía 15 (y 16).
Desarrollado con la librería estándar de Python (http.server, json, threading).
"""

import sys
import json
import time
import random
import argparse
from http.server import HTTPServer, BaseHTTPRequestHandler
from threading import Thread

class BackendRequestHandler(BaseHTTPRequestHandler):
    is_faulty = False  # Permite simular fallos dinámicamente

    def log_message(self, format, *args):
        # Reducir verbosidad en consola durante pruebas pesadas
        pass

    def do_GET(self):
        if self.is_faulty and self.path == '/health':
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ERROR", "error": "Nodo en falla simulada"}).encode('utf-8'))
            return

        if self.path == '/health':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            response = {
                "status": "OK",
                "port": self.server.server_address[1],
                "node": f"Node-{self.server.server_address[1]}",
                "cpu": round(random.uniform(10.0, 45.0), 1),
                "ram": round(random.uniform(25.0, 60.0), 1),
                "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
            }
            self.wfile.write(json.dumps(response).encode('utf-8'))
            return

        if self.path == '/toggle-fault':
            BackendRequestHandler.is_faulty = not BackendRequestHandler.is_faulty
            state = "FALLA" if BackendRequestHandler.is_faulty else "NORMAL"
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"message": f"Estado cambiado a {state}"}).encode('utf-8'))
            return

        # Respuesta general proxy
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('X-Backend-Port', str(self.server.server_address[1]))
        self.end_headers()
        response = {
            "status": "OK",
            "message": f"Respuesta procesada exitosamente por Backend en puerto {self.server.server_address[1]}",
            "path": self.path,
            "port": self.server.server_address[1],
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
        }
        self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))

    def do_POST(self):
        self.do_GET()

def run_server(port):
    server_address = ('0.0.0.0', port)
    httpd = HTTPServer(server_address, BackendRequestHandler)
    print(f"[*] Servidor Backend iniciado en http://127.0.0.1:{port}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print(f"\n[-] Deteniendo Servidor Backend en puerto {port}...")
        httpd.server_close()

def main():
    parser = argparse.ArgumentParser(description="Simulador Backend HTTP para Balanceador de Carga")
    parser.add_argument('--port', type=int, default=9001, help="Puerto TCP para el servidor (default: 9001)")
    parser.add_argument('--all', action='store_true', help="Ejecutar instancias 9001 y 9002 simultáneamente")
    args = parser.parse_args()

    if args.all:
        print("=== Iniciando grupo de Servidores Backend (9001 y 9002) ===")
        t1 = Thread(target=run_server, args=(9001,), daemon=True)
        t2 = Thread(target=run_server, args=(9002,), daemon=True)
        t1.start()
        t2.start()
        print("Presiona Ctrl+C para finalizar todos los nodos backend.")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\n[-] Servidores detenidos.")
    else:
        run_server(args.port)

if __name__ == '__main__':
    main()
