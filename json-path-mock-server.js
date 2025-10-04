const http = require('http');
const url = require('url');

const complexData = {
    "service": "ChatBot Platform",
    "version": "1.0.0",
    "timestamp": "2025-01-04T12:52:49Z",
    "server": {
        "name": "orchestrator-service",
        "port": 8092,
        "status": "running",
        "stats": {
            "memory_usage": "256MB",
            "cpu_usage": "15%",
            "uptime": "2h 30m",
            "requests_count": 1247
        }
    },
    "endpoints": [
        "/api/v1/chat/message",
        "/api/v1/scenarios/execute",
        "/api/v1/nlu/analyze"
    ],
    "users": [
        {
            "id": 1,
            "name": "Олександр Петренко",
            "email": "alex@example.com",
            "profile": {
                "avatar": "avatar1.jpg",
                "settings": {
                    "theme": "dark",
                    "language": "uk",
                    "notifications": true
                }
            },
            "permissions": ["read", "write", "admin"]
        },
        {
            "id": 2,
            "name": "Марія Іваненко",
            "email": "maria@example.com",
            "profile": {
                "avatar": "avatar2.jpg",
                "settings": {
                    "theme": "light",
                    "language": "en",
                    "notifications": false
                }
            },
            "permissions": ["read", "write"]
        }
    ],
    "data": {
        "analytics": {
            "total_sessions": 15420,
            "active_users": 342,
            "reports": [
                {
                    "id": "daily_report",
                    "date": "2025-01-04",
                    "metrics": {
                        "sessions": 1247,
                        "messages": 8934,
                        "conversion_rate": "23.5%",
                        "avg_response_time": "1.2s"
                    }
                },
                {
                    "id": "weekly_report", 
                    "date": "2025-01-01",
                    "metrics": {
                        "sessions": 8765,
                        "messages": 45231,
                        "conversion_rate": "28.1%",
                        "avg_response_time": "0.9s"
                    }
                }
            ]
        },
        "config": {
            "max_session_duration": 3600,
            "supported_languages": ["uk", "en", "ru"],
            "features": {
                "nlu_enabled": true,
                "llm_integration": true,
                "voice_support": false
            }
        }
    }
};

const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    
    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    
    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }
    
    res.setHeader('Content-Type', 'application/json');
    
    if (parsedUrl.pathname === '/api/complex-data' && req.method === 'GET') {
        res.writeHead(200);
        res.end(JSON.stringify(complexData, null, 2));
    } else if (parsedUrl.pathname === '/health' && req.method === 'GET') {
        res.writeHead(200);
        res.end(JSON.stringify({"status": "ok", "service": "json-path-mock-server"}));
    } else {
        res.writeHead(404);
        res.end(JSON.stringify({"error": "Not found"}));
    }
});

const PORT = 8181;
server.listen(PORT, () => {
    console.log(`🚀 JSON Path Mock Server running on http://localhost:${PORT}`);
    console.log(`📊 Test endpoint: http://localhost:${PORT}/api/complex-data`);
});
