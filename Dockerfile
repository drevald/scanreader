FROM python:3.12-slim

WORKDIR /app

# install dependencies first (layer cache)
COPY server/requirements.txt ./server/requirements.txt
RUN pip install --no-cache-dir -r server/requirements.txt

# copy server source
COPY server/ ./server/

WORKDIR /app/server

EXPOSE 8001

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
