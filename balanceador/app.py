# app.py
import threading
import time
from flask import Flask
from config import Config
from models import AILoadBalancer, MetricsHistory, DatabaseManager
from views.api import api_bp
from views.dashboard import dashboard_bp

def create_app():
    app = Flask(__name__)
    
    # Inicializar Modelos
    balancer = AILoadBalancer()
    history = MetricsHistory()
    db = DatabaseManager()

    # Inyectar dependencias directamente en la app (acceso mediante current_app)
    app.balancer = balancer
    app.history = history
    app.db = db

    # Pre-registrar nodos backend del cluster (vía variable de entorno BACKEND_NODES o por defecto local)
    import os
    env_nodes = os.getenv('BACKEND_NODES', '127.0.0.1:9001,127.0.0.1:9002').split(',')
    for addr in env_nodes:
        addr = addr.strip()
        if addr:
            srv, _ = balancer.add_server(addr, 1.0)
            if srv: history.register_server(srv)

    # Registrar Blueprints (Vistas)
    app.register_blueprint(dashboard_bp)
    app.register_blueprint(api_bp)

    # ---------------------------------------------------------
    # Hilo de fondo: Heartbeat Checker & Persistencia SQLite
    # ---------------------------------------------------------
    def health_checker_loop():
        while True:
            servers_snapshot = list(balancer.servers)
            threads = []
            for s in servers_snapshot:
                t = threading.Thread(target=s.health_check, daemon=True)
                t.start()
                threads.append(t)
            for t in threads:
                t.join(timeout=Config.HEALTH_TIMEOUT + 0.5)
            
            # Actualizar base de datos SQLite (nodos.db -> estado_nodos)
            for s in servers_snapshot:
                estado_str = 'ACTIVO' if s.status == 'healthy' else 'INACTIVO'
                db.update_node_status(
                    nodo=s.address,
                    puerto=s.port,
                    estado=estado_str,
                    latencia=s.latency if s.status == 'healthy' else 0.0
                )

            history.snapshot(servers_snapshot)
            time.sleep(Config.HEALTH_INTERVAL)

    threading.Thread(target=health_checker_loop, daemon=True).start()

    return app

if __name__ == '__main__':
    print()
    print("=" * 62)
    print("  AI Load Balancer & Heartbeat Failover — Guía 15 APE")
    print("=" * 62)
    print(f"  Base de datos : {DatabaseManager().db_path}")
    print(f"  Dashboard     : http://localhost:{Config.FLASK_PORT}")
    print(f"  Proxy         : http://localhost:{Config.FLASK_PORT}/balance/<ruta>")
    print("=" * 62)
    print()

    app = create_app()
    app.run(host='0.0.0.0', port=Config.FLASK_PORT, debug=False, threaded=True)