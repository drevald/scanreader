"""
Processing pipeline stub.

Real implementation will:
  1. Pre-process: deskew, denoise, normalise contrast  (Pillow / OpenCV)
  2. Layout analysis: identify text blocks, pictures, headings, tables  (external model)
  3. OCR: recognise text per block / word  (external model)
  4. Return structured PageData matching the Android model
"""

import io
from PIL import Image


class ProcessingPipeline:

    async def process(self, image_bytes: bytes, book_id: str, page_num: int) -> dict:
        img = Image.open(io.BytesIO(image_bytes))
        width, height = img.size

        # --- stub result ------------------------------------------------
        return {
            "bookId": book_id,
            "pageNum": page_num,
            "width": width,
            "height": height,
            "blocks": [
                {
                    "id": "b1",
                    "type": "TEXT",
                    "bbox": {"x": 50, "y": 100, "width": width - 100, "height": 200},
                    "text": f"[Stub OCR text — page {page_num + 1}]",
                    "words": [],
                    "imageUrl": None,
                }
            ],
        }
        # ----------------------------------------------------------------
        # TODO: replace stub with real pipeline steps
