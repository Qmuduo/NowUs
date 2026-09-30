FROM python:3.13-slim AS base
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /srv/nowus
COPY requirements.txt ./backend/requirements.txt
RUN pip install --no-cache-dir -r backend/requirements.txt

FROM base AS runtime
COPY app ./backend/app
COPY migrations ./backend/migrations
USER 10001:10001
EXPOSE 8000
CMD ["uvicorn", "backend.app.main:app", "--host", "0.0.0.0", "--port", "8000"]

FROM base AS test
COPY app ./backend/app
COPY migrations ./backend/migrations
COPY tests ./backend/tests
CMD ["python", "-m", "pytest", "backend/tests", "-q"]
