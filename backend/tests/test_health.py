from fastapi.testclient import TestClient

from backend.app.main import create_app


def test_health_endpoint_reports_api_ready():
    with TestClient(create_app()) as client:
        response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
