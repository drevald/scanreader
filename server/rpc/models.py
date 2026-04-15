from dataclasses import dataclass, field
from typing import Any


@dataclass
class RpcRequest:
    method: str
    params: dict
    id: int | str | None
    jsonrpc: str = "2.0"

    @classmethod
    def from_dict(cls, data: dict) -> "RpcRequest":
        return cls(
            method=data.get("method", ""),
            params=data.get("params", {}),
            id=data.get("id"),
            jsonrpc=data.get("jsonrpc", "2.0"),
        )


def ok(id: Any, result: Any) -> dict:
    return {"jsonrpc": "2.0", "id": id, "result": result}


def err(id: Any, code: int, message: str) -> dict:
    return {"jsonrpc": "2.0", "id": id, "error": {"code": code, "message": message}}
