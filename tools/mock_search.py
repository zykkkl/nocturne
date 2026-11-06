"""本地博查 Web Search mock，用于验证夜航联网搜索工具循环。

运行：python tools/mock_search.py
监听：0.0.0.0:8789，模拟器通过 http://10.0.2.2:8789/v1/web-search 访问。
"""
import json
import time
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        payload = json.loads(self.rfile.read(length) or b"{}")
        query = (payload.get("query") or "").strip()
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
