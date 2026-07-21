# models/metrics_history.py
import threading
from datetime import datetime
from collections import deque
from config import Config

class MetricsHistory:
    """Almacena el historial temporal para los gráficos del dashboard."""
    
    def __init__(self):
        self.timestamps = deque(maxlen=Config.MAX_HISTORY)
        self.requests_per_server = {}
        self.latency_per_server = {}
        self.lock = threading.Lock()

    def register_server(self, server):
        self.requests_per_server[server.name] = deque(maxlen=Config.MAX_HISTORY)
        self.latency_per_server[server.name] = deque(maxlen=Config.MAX_HISTORY)

    def unregister_server(self, name):
        self.requests_per_server.pop(name, None)
        self.latency_per_server.pop(name, None)

    def snapshot(self, servers):
        with self.lock:
            self.timestamps.append(datetime.now().strftime('%H:%M:%S'))
            for s in servers:
                if s.name in self.requests_per_server:
                    self.requests_per_server[s.name].append(s.total_requests)
                    self.latency_per_server[s.name].append(round(s.latency, 1))

    def get_chart_data(self):
        with self.lock:
            return {
                'timestamps': list(self.timestamps),
                'requests': {n: list(d) for n, d in self.requests_per_server.items()},
                'latencies': {n: list(d) for n, d in self.latency_per_server.items()},
            }