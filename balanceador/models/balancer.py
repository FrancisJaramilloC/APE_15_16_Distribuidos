# models/balancer.py
import threading
import requests as http_requests
from datetime import datetime, timedelta
from collections import deque
from config import Config
from models.server import BackendServer  # <-- MOVIDO ARRIBA

class AILoadBalancer:
    """Lógica de balanceo: elección con IA y fallback a Least-Connections."""
    
    def __init__(self):
        self.servers = []
        self._next_id = 1
        self.ollama_url = Config.OLLAMA_URL
        self.ollama_model = Config.OLLAMA_MODEL
        self.ollama_available = False
        self.decisions_log = deque(maxlen=300)
        self.total_requests = 0
        self.lock = threading.Lock()
        self._check_ollama()

    def _check_ollama(self):
        try:
            resp = http_requests.get(f'{self.ollama_url}/api/tags', timeout=3)
            self.ollama_available = resp.status_code == 200
            if self.ollama_available:
                models = resp.json().get('models', [])
                model_names = [m.get('name', '') for m in models]
                if not any(self.ollama_model in mn for mn in model_names):
                    if models:
                        self.ollama_model = models[0].get('name', Config.OLLAMA_MODEL)
        except Exception:
            self.ollama_available = False

    def add_server(self, address, weight=1.0):
        with self.lock:
            for s in self.servers:
                if s.address == address:
                    return None, f'Ya existe un servidor con {address}'
            server = BackendServer(self._next_id, address, weight)
            self._next_id += 1
            self.servers.append(server)
            return server, f'Servidor {address} agregado'

    def remove_server(self, server_id):
        with self.lock:
            for i, s in enumerate(self.servers):
                if s.id == server_id:
                    removed = self.servers.pop(i)
                    return removed, f'Servidor {removed.address} eliminado'
            return None, 'Servidor no encontrado'

    def _build_prompt(self, metrics):
        prompt = "Eres un balanceador de carga. Elige el MEJOR servidor para la proxima solicitud.\n\nMetricas:\n"
        for m in metrics:
            prompt += (f"- {m['name']} ({m['address']}): CPU={m['cpu']}%, "
                       f"RAM={m['ram']}%, Latencia={m['latency']}ms, "
                       f"Conexiones={m['active_connections']}, Estado={m['status']}\n")
        prompt += "\nResponde SOLO con el nombre exacto del servidor (ej: Node-1).\n"
        return prompt

    def _ai_decision(self, available):
        metrics = [s.to_dict() for s in available]
        try:
            resp = http_requests.post(
                f'{self.ollama_url}/api/generate',
                json={'model': self.ollama_model, 'prompt': self._build_prompt(metrics),
                      'stream': False, 'options': {'temperature': 0.1, 'num_predict': 20}},
                timeout=8
            )
            if resp.status_code == 200:
                text = resp.json().get('response', '').strip()
                for s in available:
                    if s.name in text: return s, text
                return None, text
        except Exception as e:
            return None, f"Error IA: {str(e)[:50]}"
        return None, "Sin respuesta"

    def _score_decision(self, available):
        best, best_score = None, float('inf')
        for s in available:
            with s.lock:
                score = (s.active_connections * 3 + s.estimated_cpu * 0.6 + s.latency * 0.4) / max(0.1, s.weight)
            if score < best_score:
                best_score, best = score, s
        return (best, f"Score={best_score:.1f}") if best else (available[0], "Único disponible")

    def choose_server(self):
        with self.lock:
            self.total_requests += 1
            snapshot = list(self.servers)

        healthy = [s for s in snapshot if s.status == 'healthy']
        usable = healthy or [s for s in snapshot if s.status != 'down'] or snapshot
        
        if not usable:
            return None, {'method': 'none', 'reason': 'Sin servidores', 'chosen_server': 'none', 'chosen_address': 'none'}

        if self.ollama_available and healthy:
            result, reason = self._ai_decision(healthy)
            if result:
                decision = self._log_decision('ai', result, reason)
                return result, decision
            chosen, reason = self._score_decision(usable)
            return chosen, self._log_decision('fallback', chosen, f"IA no resolvió. {reason}")

        chosen, reason = self._score_decision(usable)
        method = "least-connections" if not self.ollama_available else "fallback-no-healthy"
        return chosen, self._log_decision(method, chosen, reason)

    def _log_decision(self, method, chosen, reason):
        decision = {
            'timestamp': datetime.now().isoformat(), 'method': method,
            'chosen_server': chosen.name if chosen else 'none',
            'chosen_address': chosen.address if chosen else 'none',
            'reason': reason[:150], 'request_num': self.total_requests
        }
        self.decisions_log.append(decision)
        return decision

    def update_config(self, ollama_url=None, ollama_model=None):
        if ollama_url: self.ollama_url = ollama_url
        if ollama_model: self.ollama_model = ollama_model
        self._check_ollama()

    def get_stats(self):
        srvs = list(self.servers)
        total = sum(s.total_requests for s in srvs)
        successful = sum(s.successful for s in srvs)
        active = [s for s in srvs if s.status != 'down']
        recent = [d for d in self.decisions_log if datetime.fromisoformat(d['timestamp']) > datetime.now() - timedelta(seconds=10)]
        
        methods = {}
        for d in self.decisions_log:
            methods[d['method']] = methods.get(d['method'], 0) + 1

        return {
            'total_requests': total, 'successful': successful, 'failed': sum(s.failed for s in srvs),
            'success_rate': round(successful / max(1, total) * 100, 1),
            'healthy_servers': sum(1 for s in srvs if s.status == 'healthy'),
            'degraded_servers': sum(1 for s in srvs if s.status == 'degraded'),
            'down_servers': sum(1 for s in srvs if s.status == 'down'),
            'total_servers': len(srvs),
            'avg_latency': round((sum(s.latency for s in active) / len(active)) if active else 0, 1),
            'rps': round(len(recent) / 10.0, 1),
            'ollama_available': self.ollama_available,
            'ollama_model': self.ollama_model,
            'methods_distribution': methods,
        }