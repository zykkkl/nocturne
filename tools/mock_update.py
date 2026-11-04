#!/usr/bin/env python3
"""本地模拟 GitHub Releases API，用于验证应用内更新（仅开发用）。"""
import http.server
import json
import os
import sys

PORT = 8788
APK = sys.argv[1] if len(sys.argv) > 1 else "app-debug.apk"
APK_PATH = os.path.abspath(APK)


class Handler(http.server.BaseHTTPRequestHandler):
    def _json(self, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/latest":
            self._json({
                "tag_name": "v9.9.9",
                "html_url": "https://example.com/nocturne",
                "body": "本地更新测试：\n- 新的贾维斯动作\n- 修复若干问题",
                "assets": [{
                    "name": "app-release.apk",
                    "size": os.path.getsize(APK_PATH),
                    "browser_download_url": f"http://10.0.2.2:{PORT}/app.apk",
                }],
            })
        elif self.path == "/app.apk":
            with open(APK_PATH, "rb") as f:
                data = f.read()
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, fmt, *args):
        print(fmt % args, flush=True)


if __name__ == "__main__":
    print(f"serving {APK_PATH} on :{PORT}", flush=True)
    http.server.HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
