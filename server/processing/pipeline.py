"""
Processing pipeline — integrates document_layout (ocr_reflow) for:
  1. Deskew / skew correction
  2. Layout analysis  (doclayout_yolo / YOLOv10)
  3. OCR              (DocTR detection + recognition, CPU)

Returns structured PageData matching the Android + web client models.
"""

import logging
from pathlib import Path

logger = logging.getLogger(__name__)

def _read_pipeline_commit() -> str:
    try:
        return Path('/deps/document_layout/.image-commit').read_text().strip()
    except Exception:
        return 'unknown'

PIPELINE_COMMIT = _read_pipeline_commit()


class ProcessingPipeline:

    def warmup(self, languages: list[str] | None = None) -> None:
        """Pre-load EasyOCR models. Runs synchronously; call from a background thread."""
        try:
            from ocr_reflow import warmup_readers
            warmup_readers(languages or ['ru', 'en'])
        except ImportError as e:
            logger.warning(f"Cannot warm up pipeline (ocr_reflow not installed): {e}")
        except Exception as e:
            logger.error(f"Pipeline warmup failed: {e}", exc_info=True)

    async def process(self, image_bytes: bytes, book_id: str, page_num: int,
                      languages: list[str] | None = None) -> dict:
        """
        Args:
            image_bytes: raw JPEG/PNG page image
            book_id:     book identifier
            page_num:    0-based page number
            languages:   EasyOCR language codes, e.g. ['ru', 'en']

        Returns:
            dict matching PageData (see client/android model and web client)
        """
        import asyncio
        return await asyncio.get_event_loop().run_in_executor(
            None, self._process_sync, image_bytes, book_id, page_num, languages
        )

    def _process_sync(self, image_bytes: bytes, book_id: str, page_num: int,
                      languages: list[str] | None = None) -> dict:
        try:
            from ocr_reflow import extract_page_data
        except ImportError as e:
            logger.error(f"ocr_reflow not installed: {e}")
            return self._fallback(image_bytes, book_id, page_num)

        try:
            result = extract_page_data(
                image_source=image_bytes,
                page_num=page_num,
                book_id=book_id,
                languages=languages,
            )
            result['pipelineCommit'] = PIPELINE_COMMIT
            return result
        except Exception as e:
            logger.error(f"extract_page_data failed: {e}", exc_info=True)
            return self._fallback(image_bytes, book_id, page_num)

    @staticmethod
    def _fallback(image_bytes: bytes, book_id: str, page_num: int) -> dict:
        """Minimal stub returned when the real pipeline fails."""
        import io
        from PIL import Image
        img = Image.open(io.BytesIO(image_bytes))
        w, h = img.size
        return {
            "bookId": book_id,
            "pageNum": page_num,
            "width": w,
            "height": h,
            "blocks": [{
                "id": "b0",
                "type": "TEXT",
                "bbox": {"x": 0, "y": 0, "width": w, "height": h},
                "text": "[Pipeline unavailable]",
                "words": [],
                "imageUrl": None,
            }],
        }
