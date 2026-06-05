.PHONY: frontend backend ai

frontend:
	cd frontend && npm run dev -- --port 5173

backend:
	cd backend && ./mvnw spring-boot:run

ai:
	cd ai-service && .venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
