from storage.manager import StorageManager
from processing.pipeline import ProcessingPipeline

_pipeline = ProcessingPipeline()


async def handle_process_page(params: dict, storage: StorageManager) -> dict:
    """
    params: { book_id, page_num, image_id }
    returns: PageData (see model/PageData.kt for the expected shape)
    """
    book_id: str = params["book_id"]
    page_num: int = params["page_num"]
    image_id: str = params["image_id"]

    image_bytes = await storage.get_page_image(image_id)
    return await _pipeline.process(image_bytes, book_id, page_num)
