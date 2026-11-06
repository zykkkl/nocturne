"""本地搜索 mock，用于验证夜航联网搜索工具循环。

支持博查格式 /v1/web-search 与 DeepSeek 格式 /v1/tools/search。
运行：python tools/mock_search.py
监听：0.0.0.0:8789，模拟器通过 http://10.0.2.2:8789 访问。
"""
import json
import time
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        payload = json.loads(self.rfile.read(length) or b"{}")
        query = (payload.get("query") or "").strip()
        if self.path.startswith("/v1/tools/search"):
            results = [
                {
                    "title": f"夜航 DeepSeek 搜索测试：{query or '未命名查询'}",
                    "url": "https://github.com/zykkkl/nocturne/releases",
                    "snippet": "DeepSeek 搜索 mock 返回的摘要，用于验证引用展示。",
                    "summary": "这是本地 mock 的 DeepSeek Search 结果，不代表真实网页内容。",
                    "site_name": "GitHub",
                    "date": "2026-11-06",
                }
            ]
            body = json.dumps({"results": results}, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        value = [
            {
                "name": f"夜航搜索测试来源：{query or '未命名查询'}",
                "url": "https://github.com/zykkkl/nocturne/releases",
                "snippet": "夜航是一个本地优先的 AI 学习应用，支持学习航线、追问和教材拆解。",
                "summary": "这条结果是本地 mock 返回的，用于验证引用展示，不代表真实网页内容。",
                "siteName": "GitHub",
                "datePublished": "2026-11-06",
            }
        ]
        body = json.dumps(
            {
                "code": 200,
                "log_id": f"mock-{int(time.time())}",
                "msg": None,
                "data": {"webPages": {"value": value}},
            },
            ensure_ascii=False,
        ).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        pass


if __name__ == "__main__":
    HTTPServer(("0.0.0.0", 8789), Handler).serve_forever()
