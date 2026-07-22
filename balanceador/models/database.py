# models/database.py
import sqlite3
import os
import threading
from datetime import datetime

# Archivo de base de datos en la raíz del proyecto para compatibilidad con Guía 15 y 16
DB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'nodos.db'))

class DatabaseManager:
    """Administra la conexión y persistencia en la base de datos SQLite (nodos.db)."""
    
    def __init__(self, db_path=DB_PATH):
        self.db_path = db_path
        self.lock = threading.Lock()
        self.init_db()

    def get_connection(self):
        conn = sqlite3.connect(self.db_path, timeout=10)
        conn.row_factory = sqlite3.Row
        return conn

    def init_db(self):
        """Crea las tablas estado_nodos y circuit_log si no existen."""
        with self.lock:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                # Tabla de monitorización física Heartbeat (Guía 15)
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS estado_nodos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nodo TEXT NOT NULL UNIQUE,
                        puerto INTEGER NOT NULL,
                        estado TEXT NOT NULL,
                        latencia REAL DEFAULT 0.0,
                        ultima_actualizacion TEXT NOT NULL
                    );
                ''')
                # Tabla de auditoría del Circuit Breaker (Guía 16)
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS circuit_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        servicio TEXT NOT NULL,
                        estado_anterior TEXT NOT NULL,
                        nuevo_estado TEXT NOT NULL,
                        motivo TEXT,
                        timestamp TEXT NOT NULL
                    );
                ''')
                conn.commit()

    def update_node_status(self, nodo, puerto, estado, latencia=0.0):
        """Inserta o actualiza el estado de un nodo backend (ACTIVO / INACTIVO)."""
        now_str = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        with self.lock:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('''
                    INSERT INTO estado_nodos (nodo, puerto, estado, latencia, ultima_actualizacion)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(nodo) DO UPDATE SET
                        puerto = excluded.puerto,
                        estado = excluded.estado,
                        latencia = excluded.latencia,
                        ultima_actualizacion = excluded.ultima_actualizacion;
                ''', (nodo, puerto, estado, round(latencia, 2), now_str))
                conn.commit()

    def get_all_nodes(self):
        """Retorna todos los registros de la tabla estado_nodos."""
        with self.lock:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('SELECT * FROM estado_nodos ORDER BY puerto ASC;')
                rows = cursor.fetchall()
                return [dict(row) for row in rows]

    def log_circuit_transition(self, servicio, estado_anterior, nuevo_estado, motivo=""):
        """Registra un cambio de estado del Circuit Breaker en la tabla circuit_log."""
        now_str = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        with self.lock:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('''
                    INSERT INTO circuit_log (servicio, estado_anterior, nuevo_estado, motivo, timestamp)
                    VALUES (?, ?, ?, ?, ?);
                ''', (servicio, estado_anterior, nuevo_estado, motivo, now_str))
                conn.commit()

    def get_circuit_logs(self, limit=50):
        """Retorna el historial de cambios de estado del Circuit Breaker."""
        with self.lock:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('SELECT * FROM circuit_log ORDER BY id DESC LIMIT ?;', (limit,))
                rows = cursor.fetchall()
                return [dict(row) for row in rows]
