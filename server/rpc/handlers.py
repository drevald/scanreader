from storage.manager import StorageManager
from processing.pipeline import ProcessingPipeline

_pipeline = ProcessingPipeline()

PROBE_LANGUAGES = ['ru', 'en']


def _detect_language(text: str) -> str | None:
    if not text or len(text.strip()) < 20:
        return None
    try:
        from langdetect import detect, DetectorFactory
        DetectorFactory.seed = 0
        return detect(text)  # returns ISO codes like 'ru', 'en', 'zh-cn'
    except Exception:
        return None


async def handle_process_page(params: dict, storage: StorageManager) -> dict:
    """
    params: { book_id, page_num }
    Loads the page image from persistent storage, runs the pipeline, saves the result.
    returns: PageData (see model/PageData.kt for the expected shape)
    """
    book_id: str = params["book_id"]
    page_num: int = params["page_num"]

    meta = storage.get_page_meta(book_id)
    lang_override: str | None = meta.get("language")

    if lang_override:
        languages = [lang_override] if lang_override == 'en' else [lang_override, 'en']
    else:
        languages = PROBE_LANGUAGES

    image_bytes = await storage.get_persistent_page_image(book_id)
    result = await _pipeline.process(image_bytes, book_id, page_num, languages=languages)

    # Auto-detect and persist language on first processing (no override set)
    if not lang_override:
        all_text = " ".join(
            b.get('text') or '' for b in result.get('blocks', [])
        )
        detected = _detect_language(all_text)
        if detected:
            storage.set_page_language(book_id, detected)
            result['language'] = detected
    else:
        result['language'] = lang_override

    await storage.save_page_result(book_id, result)
    return result
