# models/server.py
import threading
import time
import random
import requests as http_requests
from collections import deque
from config import Config

class BackendServer:
    """Representa un backend real. Mide latencia y hace health checks."""
    
    def __init__(self, server_id, address, weight=1.0):
        self.id = server_id
        self.address = address
        self.weight = weight
        self.lock = threading.Lock()

        self.status = 'unknown'
        self.latency = 0
        self.estimated_cpu = 0
        self.estimated_ram = 0

        self.active_connections = 0
        self.total_requests = 0
        self.successful = 0
        self.failed = 0

        self._latency_samples = deque(maxlen=10)
        self._consecutive_failures = 0
        self._created_at = time.time()

    @property
    def url(self):
        return f'http://{self.address}'

    @property
    def name(self):
        return f'Node-{self.id}'

    @property
    def port(self):
        try:
            return int(self.address.split(':')[-1])
        except Exception:
            return 9000 + self.id

    def health_check(self):
        """Hace ping real al backend para medir latencia y estado."""
        start = time.time()
        try:
            resp = http_requests.get(f'{self.url}/health', timeout=Config.HEALTH_TIMEOUT)
            elapsed = (time.time() - start) * 1000
            with self.lock:
                self._latency_samples.append(elapsed)
                self.latency = sum(self._latency_samples) / len(self._latency_samples)
                self._consecutive_failures = 0

                if resp.status_code == 200:
                    try:
                        data = resp.json()
                        self.estimated_cpu = float(data.get('cpu', self.estimated_cpu))
                        self.estimated_ram = float(data.get('ram', self.estimated_ram))
                    except (ValueError, TypeError):
                        self._estimate_from_latency()
                    self.status = 'healthy'
                else:
                    self._estimate_from_latency()
                    self.status = 'degraded'
        except Exception:
            with self.lock:
                self._consecutive_failures += 1
                self._handle_failure()

    def _estimate_from_latency(self):
        lat = self.latency if self.latency > 0 else 50
        self.estimated_cpu = min(95, max(5, lat * 1.2 + random.uniform(-5, 10)))
        self.estimated_ram = min(95, max(15, 30 + self.active_connections * 3 + random.uniform(-3, 5)))

    def _handle_failure(self):
        self.status = 'down'
        self.latency = 9999
        self.estimated_cpu = 0
        self.estimated_ram = 0

    def proxy_request(self, method, path, headers, body, params, client_ip):
        """Reenvía la petición real al backend."""
        with self.lock:
            self.active_connections += 1
            self.total_requests += 1

        url = f'{self.url}/{path}'
        fwd_headers = {k: v for k, v in headers.items() if k.lower() not in ('host', 'content-length')}
        fwd_headers['X-Forwarded-For'] = client_ip
        fwd_headers['X-Balanced-By'] = 'ai-load-balancer'

        start = time.time()
        try:
            resp = http_requests.request(
                method=method, url=url, headers=fwd_headers,
                data=body, params=params,
                timeout=Config.PROXY_TIMEOUT, allow_redirects=False
            )
            elapsed = (time.time() - start) * 1000
            with self.lock:
                self.active_connections -= 1
                self.successful += 1
                self._latency_samples.append(elapsed)
                self.latency = sum(self._latency_samples) / len(self._latency_samples)
                self._consecutive_failures = 0
                if self.status != 'healthy': self.status = 'healthy'
            return resp, elapsed, True
        except Exception:
            elapsed = (time.time() - start) * 1000
            with self.lock:
                self.active_connections -= 1
                self.failed += 1
                self._consecutive_failures += 1
                if self._consecutive_failures >= 2: self.status = 'degraded'
            return None, elapsed, False

    def to_dict(self):
        with self.lock:
            return {
                'id': self.id, 'address': self.address, 'name': self.name,
                'weight': self.weight, 'status': self.status,
                'latency': round(self.latency, 1),
                'cpu': round(self.estimated_cpu, 1), 'ram': round(self.estimated_ram, 1),
                'active_connections': self.active_connections,
                'total_requests': self.total_requests,
                'successful': self.successful, 'failed': self.failed,
                'success_rate': round(self.successful / max(1, self.total_requests) * 100, 1),
                'uptime_seconds': round(time.time() - self._created_at),
            }