from .handlers import handle_process_page
from .models import RpcRequest, ok, err
from storage.manager import StorageManager

METHODS = {
    "process_page": handle_process_page,
}


async def handle_rpc(body: dict, storage: StorageManager) -> dict:
    req = RpcRequest.from_dict(body)

    if req.jsonrpc != "2.0":
        return err(req.id, -32600, "Invalid JSON-RPC version")

    handler = METHODS.get(req.method)
    if handler is None:
        return err(req.id, -32601, f"Method not found: {req.method!r}")

    try:
        result = await handler(req.params, storage)
        return ok(req.id, result)
    except KeyError as e:
        return err(req.id, -32602, f"Missing parameter: {e}")
    except Exception as e:
        return err(req.id, -32000, str(e))
