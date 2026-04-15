from fastapi import FastAPI, File, UploadFile, HTTPException, Request
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from rpc.router import handle_rpc
from storage.manager import StorageManager

app = FastAPI(title="ScanReader API", version="0.1.0")
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
async def upload_page(book_id: str, page_num: int, file: UploadFile = File(...)) -> dict:
    """Receive a raw page image from the client."""
    data = await file.read()
    image_id = await storage.save_page_image(book_id, page_num, data)
    return {"image_id": image_id}


@app.get("/images/{book_id}/{page_num}/{block_id}")
async def get_block_image(book_id: str, page_num: int, block_id: str) -> FileResponse:
    """Serve an extracted picture block back to the client."""
    path = storage.get_block_image_path(book_id, page_num, block_id)
    if not path.exists():
        raise HTTPException(status_code=404, detail="Block image not found")
    return FileResponse(path, media_type="image/jpeg")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
