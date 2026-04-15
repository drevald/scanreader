import uuid
from pathlib import Path


class StorageManager:

    def __init__(self, base_path: str = "./data"):
        self.base = Path(base_path)
        (self.base / "uploads").mkdir(parents=True, exist_ok=True)
        (self.base / "blocks").mkdir(parents=True, exist_ok=True)

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
