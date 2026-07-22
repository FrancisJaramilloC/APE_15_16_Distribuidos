# orquestador/circuit_breaker.py
import time
import threading
from datetime import datetime

class CircuitBreakerOpenException(Exception):
    """Excepción lanzada cuando el Circuit Breaker está en estado OPEN."""
    def __init__(self, message="El circuito está ABIERTO (OPEN). Petición bloqueada para evitar fallos en cascada."):
        super().__init__(message)

class CircuitBreaker:
    """
    Implementación del Patrón Circuit Breaker.
    Administra los estados:
      - CLOSED    : Operación normal, monitorea fallos.
      - OPEN      : Circuito abierto por exceso de fallos, bloquea peticiones de inmediato (< 0.01s).
      - HALF_OPEN : Estado de prueba tras transcurrir recovery_timeout.
    """
    
    def __init__(self, service_name="ServicioOrquestador", failure_threshold=3, recovery_timeout=10.0, db_manager=None):
        self.service_name = service_name
        self.failure_threshold = failure_threshold  # Umbral de fallos consecutivos (3)
        self.recovery_timeout = recovery_timeout     # Tiempo de enfriamiento en segundos (10s)
        self.db_manager = db_manager

        self.state = "CLOSED"
        self.failure_count = 0
        self.success_count = 0
        self.total_calls = 0
        self.total_blocked = 0
        self.last_state_change = time.time()
        self.lock = threading.Lock()

    def _transition_to(self, new_state, reason=""):
        old_state = self.state
        if old_state != new_state:
            self.state = new_state
            self.last_state_change = time.time()
            if self.state == "CLOSED":
                self.failure_count = 0
            
            # Persistir cambio de estado en SQLite (tabla circuit_log)
            if self.db_manager:
                try:
                    self.db_manager.log_circuit_transition(
                        servicio=self.service_name,
                        estado_anterior=old_state,
                        nuevo_estado=new_state,
                        motivo=reason
                    )
                except Exception as e:
                    print(f"[CircuitBreaker DB Error] {e}")

    def can_execute(self):
        """Verifica si la petición puede ejecutarse según el estado del circuito."""
        with self.lock:
            if self.state == "OPEN":
                elapsed = time.time() - self.last_state_change
                if elapsed >= self.recovery_timeout:
                    self._transition_to("HALF_OPEN", f"Pasaron {elapsed:.1f}s en OPEN. Probando recuperación (HALF_OPEN).")
                    return True
                else:
                    self.total_blocked += 1
                    return False
            return True

    def record_success(self):
        """Registra una ejecución exitosa."""
        with self.lock:
            self.success_count += 1
            if self.state == "HALF_OPEN":
                self._transition_to("CLOSED", "Prueba en HALF_OPEN exitosa. Circuito restablecido a CLOSED.")
            else:
                self.failure_count = 0

    def record_failure(self, error_msg=""):
        """Registra un fallo en la llamada al backend."""
        with self.lock:
            self.failure_count += 1
            if self.state == "HALF_OPEN":
                self._transition_to("OPEN", f"Prueba en HALF_OPEN falló ({error_msg}). Circuito regresa a OPEN.")
            elif self.state == "CLOSED" and self.failure_count >= self.failure_threshold:
                self._transition_to("OPEN", f"Se alcanzó el umbral de {self.failure_threshold} fallos consecutivos. Circuito ABIERTO.")

    def call(self, func, *args, **kwargs):
        """
        Ejecuta la función decorada o envuelta respetando las reglas del Circuit Breaker.
        """
        with self.lock:
            self.total_calls += 1

        if not self.can_execute():
            raise CircuitBreakerOpenException()

        try:
            result = func(*args, **kwargs)
            self.record_success()
            return result
        except Exception as e:
            self.record_failure(str(e))
            raise e

    def get_status(self):
        with self.lock:
            elapsed = time.time() - self.last_state_change
            time_remaining = max(0.0, self.recovery_timeout - elapsed) if self.state == "OPEN" else 0.0
            return {
                "servicio": self.service_name,
                "estado": self.state,
                "fallos_consecutivos": self.failure_count,
                "umbral_fallos": self.failure_threshold,
                "tiempo_cooldown_seg": self.recovery_timeout,
                "tiempo_restante_cooldown_seg": round(time_remaining, 1),
                "total_llamadas": self.total_calls,
                "total_bloqueadas": self.total_blocked,
                "ultima_transicion": datetime.fromtimestamp(self.last_state_change).strftime('%Y-%m-%d %H:%M:%S')
            }
