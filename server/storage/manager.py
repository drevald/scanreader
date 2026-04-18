import json
import os
import shutil
import time
import uuid
from pathlib import Path


class StorageManager:

    def __init__(self, base_path: str = "./data"):
        self.base = Path(base_path)
        self.pages_dir = Path(os.environ.get("PAGES_DIR", str(self.base / "pages")))

        (self.base / "uploads").mkdir(parents=True, exist_ok=True)
        (self.base / "blocks").mkdir(parents=True, exist_ok=True)
        self.pages_dir.mkdir(parents=True, exist_ok=True)

    # ── temp upload storage (used during single process cycle) ────────────────

    async def save_page_image(self, book_id: str, page_num: int, data: bytes) -> str:
        image_id = str(uuid.uuid4())
        path = self.base / "uploads" / f"{image_id}.jpg"
        path.write_bytes(data)
        return image_id

    async def get_page_image(self, image_id: str) -> bytes:
        path = self.base / "uploads" / f"{image_id}.jpg"
        if not path.exists():
            raise FileNotFoundError(f"Image {image_id} not found")
        return path.read_bytes()

    def get_block_image_path(self, book_id: str, page_num: int, block_id: str) -> Path:
        return self.base / "blocks" / book_id / str(page_num) / f"{block_id}.jpg"

    # ── persistent page storage ───────────────────────────────────────────────

    async def save_page_persistent(self, page_id: str, name: str, image_bytes: bytes):
        """Save page image and metadata. Skips image/meta if page already exists."""
        page_dir = self.pages_dir / page_id
        page_dir.mkdir(parents=True, exist_ok=True)

        image_path = page_dir / "image.jpg"
        if not image_path.exists():
            image_path.write_bytes(image_bytes)
            meta = {"id": page_id, "name": name, "created_at": time.time(), "language": None}
            (page_dir / "meta.json").write_text(json.dumps(meta))

    def get_page_meta(self, page_id: str) -> dict:
        meta_file = self.pages_dir / page_id / "meta.json"
        if not meta_file.exists():
            return {}
        return json.loads(meta_file.read_text())

    def set_page_language(self, page_id: str, language: str | None):
        meta_file = self.pages_dir / page_id / "meta.json"
        if not meta_file.exists():
            return
        meta = json.loads(meta_file.read_text())
        meta["language"] = language
        meta_file.write_text(json.dumps(meta))

    async def get_persistent_page_image(self, page_id: str) -> bytes:
        path = self.pages_dir / page_id / "image.jpg"
        if not path.exists():
            raise FileNotFoundError(f"Persistent image for page {page_id!r} not found")
        return path.read_bytes()

    async def save_page_result(self, page_id: str, result: dict):
        results_dir = self.pages_dir / page_id / "results"
        results_dir.mkdir(parents=True, exist_ok=True)
        ts = int(time.time() * 1000)
        (results_dir / f"{ts}.json").write_text(json.dumps(result))

    def delete_page_result(self, page_id: str, ts: int):
        path = self.pages_dir / page_id / "results" / f"{ts}.json"
        if path.exists():
            path.unlink()

    def delete_page(self, page_id: str):
        page_dir = self.pages_dir / page_id
        if page_dir.exists():
            shutil.rmtree(page_dir)

    def list_pages_data(self) -> list:
        pages = []
        for page_dir in sorted(self.pages_dir.iterdir(), key=lambda p: p.stat().st_ctime):
            if not page_dir.is_dir():
                continue
            meta_file = page_dir / "meta.json"
            if not meta_file.exists():
                continue
            meta = json.loads(meta_file.read_text())

            versions = []
            results_dir = page_dir / "results"
            if results_dir.exists():
                for rf in sorted(results_dir.iterdir(), key=lambda p: p.name):
                    try:
                        versions.append({
                            "ts": int(rf.stem),
                            "result": json.loads(rf.read_text()),
                        })
                    except Exception:
                        pass

            pages.append({
                "id": meta["id"],
                "name": meta["name"],
                "imageUrl": f"/api/pages/{meta['id']}/image",
                "language": meta.get("language"),
                "versions": versions,
            })
        return pages
