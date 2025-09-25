#!/usr/bin/env python3
import http.server
import socketserver
import json

class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        if 'scenarios' in self.path:
            response = {
                "scenarios": [
                    {"id": "greeting-001", "name": "Приветствие пользователя", "category": "greeting", "is_active": True},
                    {"id": "help-001", "name": "Помощь пользователю", "category": "support", "is_active": True}
                ],
                "count": 2
            }
        elif 'dialogs' in self.path:
            response = {
                "dialogs": [
                    {"session_id": "session-001", "status": "completed", "message_count": 8, "last_message": "Спасибо!"},
                    {"session_id": "session-002", "status": "active", "message_count": 3, "last_message": "Тестируем..."}
                ]
            }
        else:
            response = {"status": "ok"}
        
        self.wfile.write(json.dumps(response).encode())
    
    def do_POST(self):
        self.do_GET()
    
    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', '*')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()

with socketserver.TCPServer(("", 8099), Handler) as httpd:
    print("🚀 Ultra Simple Mock Server running on port 8099")
    httpd.serve_forever()
