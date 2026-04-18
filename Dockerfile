FROM python:3.12-slim

WORKDIR /app

# system deps for OpenCV + Tesseract OCR (CPU-compatible, no AVX2 needed)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 libgomp1 \
    tesseract-ocr \
    tesseract-ocr-rus \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# copy document_layout source (editable install)
COPY document_layout/ /deps/document_layout/
# bake the commit SHA so the server can report which version of the pipeline was used
# .git is excluded from the build context (.dockerignore), so the commit is passed as a build arg
ARG PIPELINE_COMMIT=unknown
RUN echo "$PIPELINE_COMMIT" > /deps/document_layout/.image-commit

# install server dependencies (includes -e /deps/document_layout)
COPY scanreader/server/requirements.txt ./server/requirements.txt
RUN pip install --no-cache-dir -r server/requirements.txt

# copy server source
COPY scanreader/server/ ./server/

WORKDIR /app/server

EXPOSE 8001

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
