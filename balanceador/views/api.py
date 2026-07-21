# views/api.py
from flask import Blueprint, jsonify, request, Response, current_app
from config import Config

api_bp = Blueprint('api', __name__)

@api_bp.route('/api/servers', methods=['GET'])
def get_servers():
    return jsonify([s.to_dict() for s in current_app.balancer.servers])

@api_bp.route('/api/servers', methods=['POST'])
def add_server():
    data = request.json or {}
    address = data.get('address', '').strip()
    weight = float(data.get('weight', 1.0))

    if not address: return jsonify({'error': 'Dirección requerida'}), 400
    parts = address.split(':')
    if len(parts) != 2:
        return jsonify({'error': 'Formato inválido. Usa IP:PUERTO'}), 400
    try:
        port = int(parts[1])
        if not (1 <= port <= 65535): raise ValueError()
    except ValueError:
        return jsonify({'error': 'Puerto inválido (1-65535)'}), 400

    server, msg = current_app.balancer.add_server(address, max(0.1, min(10.0, weight)))
    if not server: return jsonify({'error': msg}), 409
    
    current_app.history.register_server(server)
    return jsonify({'message': msg, 'server': server.to_dict()}), 201

@api_bp.route('/api/servers/<int:server_id>', methods=['DELETE'])
def remove_server(server_id):
    server, msg = current_app.balancer.remove_server(server_id)
    if not server: return jsonify({'error': msg}), 404
    current_app.history.unregister_server(server.name)
    return jsonify({'message': msg})

@api_bp.route('/api/servers/<int:server_id>/health', methods=['POST'])
def force_health(server_id):
    import threading
    for s in current_app.balancer.servers:
        if s.id == server_id:
            threading.Thread(target=s.health_check, daemon=True).start()
            return jsonify({'message': f'Health check iniciado para {s.address}'})
    return jsonify({'error': 'No encontrado'}), 404

@api_bp.route('/api/stats')
def get_stats():
    return jsonify(current_app.balancer.get_stats())

@api_bp.route('/api/db/nodos')
def get_db_nodos():
    return jsonify(current_app.db.get_all_nodes())

@api_bp.route('/api/metrics')
def get_metrics():
    return jsonify(current_app.history.get_chart_data())

@api_bp.route('/api/decisions')
def get_decisions():
    d = list(current_app.balancer.decisions_log)
    d.reverse()
    return jsonify(d[:50])

@api_bp.route('/api/config', methods=['POST'])
def update_config():
    data = request.json or {}
    current_app.balancer.update_config(data.get('ollama_url'), data.get('ollama_model'))
    return jsonify({'status': 'ok', 'ollama_available': current_app.balancer.ollama_available})

@api_bp.route('/api/ollama-models')
def ollama_models():
    import requests as http_requests
    try:
        resp = http_requests.get(f'{Config.OLLAMA_URL}/api/tags', timeout=3)
        if resp.status_code == 200:
            return jsonify({'models': [m['name'] for m in resp.json().get('models', [])]})
    except Exception: pass
    return jsonify({'models': []})

@api_bp.route('/balance/<path:path>', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD'])
def proxy(path):
    chosen, decision = current_app.balancer.choose_server()
    if not chosen:
        return jsonify({'error': 'No hay servidores disponibles', 'decision': decision}), 503

    resp, elapsed, success = chosen.proxy_request(
        method=request.method, path=path, headers=dict(request.headers),
        body=request.get_data(), params=dict(request.args),
        client_ip=request.remote_addr
    )
    decision['success'] = success
    decision['elapsed_ms'] = round(elapsed, 1)

    if not resp:
        return jsonify({'error': f'Backend {chosen.address} no respondió', 'elapsed_ms': round(elapsed, 1)}), 502

    excluded = {'content-encoding', 'content-length', 'transfer-encoding', 'connection'}
    resp_headers = [(k, v) for k, v in resp.headers.items() if k.lower() not in excluded]
    return Response(resp.content, status=resp.status_code, headers=resp_headers)