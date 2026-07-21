# config.py
import os

class Config:
    OLLAMA_URL = os.getenv('OLLAMA_URL', 'http://localhost:11434')
    OLLAMA_MODEL = os.getenv('OLLAMA_MODEL', 'llama3.2')
    HEALTH_INTERVAL = 2       # Segundos entre health checks (garantiza detección < 3s)
    HEALTH_TIMEOUT = 1.5      # Timeout del health check
    PROXY_TIMEOUT = 10        # Timeout al reenviar tráfico
    MAX_HISTORY = 60          # Puntos máximos en gráficos
    FLASK_PORT = int(os.getenv('FLASK_PORT', 5000))