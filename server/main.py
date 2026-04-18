import logging
import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI, File, Form, UploadFile, HTTPException, Request
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from rpc.router import handle_rpc
from rpc.handlers import _pipeline
from storage.manager import StorageManager

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Pre-load EasyOCR models in a background thread so the server starts
    # accepting connections immediately, but the first /rpc request won't
    # have to wait for model loading (which could exceed Caddy's timeout).
    def _warmup():
        logger.info("Warming up processing pipeline (EasyOCR model load)…")
        _pipeline.warmup()
        logger.info("Pipeline warmup complete.")

    t = threading.Thread(target=_warmup, daemon=True, name="pipeline-warmup")
    t.start()
    yield


app = FastAPI(title="ScanReader API", version="0.1.0", lifespan=lifespan)
storage = StorageManager()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/rpc")
async def rpc_endpoint(request: Request) -> dict:
    """JSON-RPC 2.0 endpoint for all control messages."""
    body = await request.json()
    return await handle_rpc(body, storage)


@app.post("/pages/{book_id}/{page_num}")
async def upload_page(
    book_id: str,
    page_num: int,
    file: UploadFile = File(...),
    name: str = Form(default=None),
) -> dict:
    """Receive a raw page image and persist it."""
    data = await file.read()
    image_id = await storage.save_page_image(book_id, page_num, data)
    await storage.save_page_persistent(book_id, name or book_id, data)
    return {"image_id": image_id}


@app.get("/api/pages")
async def list_pages() -> list:
    """Return all persisted pages with their processing versions."""
    return storage.list_pages_data()


@app.get("/api/pages/{page_id}/image")
async def get_persistent_page_image(page_id: str) -> FileResponse:
    """Serve the original image for a persisted page."""
    path = storage.pages_dir / page_id / "image.jpg"
    if not path.exists():
        raise HTTPException(status_code=404, detail="Page not found")
    return FileResponse(path, media_type="image/jpeg")


@app.put("/api/pages/{page_id}/language")
async def set_page_language(page_id: str, request: Request) -> dict:
    body = await request.json()
    storage.set_page_language(page_id, body.get("language"))
    return {"ok": True}


@app.delete("/api/pages/{page_id}/results/{ts}")
async def delete_page_result(page_id: str, ts: int) -> dict:
    storage.delete_page_result(page_id, ts)
    return {"ok": True}


@app.delete("/api/pages/{page_id}")
async def delete_page(page_id: str) -> dict:
    storage.delete_page(page_id)
    return {"ok": True}


@app.get("/images/{book_id}/{page_num}/{block_id}")
async def get_block_image(book_id: str, page_num: int, block_id: str) -> FileResponse:
    """Serve an extracted picture block back to the client."""
    path = storage.get_block_image_path(book_id, page_num, block_id)
    if not path.exists():
        raise HTTPException(status_code=404, detail="Block image not found")
    return FileResponse(path, media_type="image/jpeg")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
