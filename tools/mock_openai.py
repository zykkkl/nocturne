"""本地 OpenAI 兼容 mock，用于验证夜航的工具循环。

运行：python tools/mock_openai.py
监听：0.0.0.0:8787，模拟器通过 http://10.0.2.2:8787/v1 访问。
每次请求追加写入 tools/mock_requests.jsonl，便于检查对话历史。
"""
import json
import re
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

LOG = "tools/mock_requests.jsonl"


def redact_images(value):
    """日志里不要把 base64 图片整段写进去。"""
    if isinstance(value, list):
        return [
            {"type": item.get("type"), "image_url": {"url": "[image]"}}
            if isinstance(item, dict) and item.get("type") == "image_url"
            else redact_images(item)
            for item in value
        ]
    if isinstance(value, dict):
        return {key: redact_images(item) for key, item in value.items()}
    return value


def tool_call(name, args, call_id):
    return {
        "id": call_id,
        "type": "function",
        "function": {"name": name, "arguments": json.dumps(args, ensure_ascii=False)},
    }


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass

    def do_GET(self):
        body = json.dumps({"object": "list", "data": [{"id": "mock-tutor", "object": "model"}]})
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body.encode("utf-8"))))
        self.end_headers()
        self.wfile.write(body.encode("utf-8"))

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        payload = json.loads(self.rfile.read(length) or b"{}")
        with open(LOG, "a", encoding="utf-8") as log:
            log.write(json.dumps(redact_images(payload), ensure_ascii=False) + "\n")

        messages = payload.get("messages", [])
        last = messages[-1] if messages else {}

        system_text = "\n".join(
            str(message.get("content", ""))
            for message in messages
            if message.get("role") == "system"
        )
        # 教材拆解：识别到教材分析器的系统提示词时，返回严格 JSON。
        if "教材结构分析器" in system_text:
            user_text = str(last.get("content", ""))
            title = "教材片段"
            source = "第 1 页"
            for line in user_text.splitlines():
                if line.startswith("章节："):
                    title = line.removeprefix("章节：").strip() or title
                if line.startswith("来源："):
                    source = line.removeprefix("来源：").strip() or source
            short_title = re.sub(r"^第.{1,3}章\s*", "", title).strip() or title
            short_title = re.sub(r"^\d+(\.\d+)*\s*", "", short_title).strip() or short_title
            short_title = short_title[:12]
            points = [
                {
                    "title": f"{short_title}原理",
                    "note": "先弄清楚这个章节在讲什么，以及它和前后内容的关系；能用自己的话复述，再往下走。",
                    "source_ref": source,
                },
                {
                    "title": f"{short_title}方法",
                    "note": "把章节里的步骤拆成可执行的动作，知道每一步为什么这么做，而不只是记住结论。",
                    "source_ref": source,
                },
                {
                    "title": f"{short_title}例子",
                    "note": "用一个正面例子和一个反例检验理解；能解释反例为什么不符合条件，才算真的掌握。",
                    "source_ref": source,
                },
            ]
            message = {
                "role": "assistant",
                "content": json.dumps(
                    {
                        "title": title,
                        "summary": f"这一段围绕「{title}」展开。",
                        "points": points,
                    },
                    ensure_ascii=False,
                ),
            }
            response = {
                "id": "chatcmpl-mock-textbook",
                "object": "chat.completion",
                "created": int(time.time()),
                "model": "mock-tutor",
                "choices": [
                    {
                        "index": 0,
                        "message": message,
                        "finish_reason": "stop",
                    }
                ],
                "usage": {"prompt_tokens": 10, "completion_tokens": 10, "total_tokens": 20},
            }
            body = json.dumps(response, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        # 追问模块没有 tools，返回一段苏格拉底式引导，便于本地联调。
        if not payload.get("tools"):
            content = last.get("content")
            has_image = isinstance(content, list) and any(
                isinstance(part, dict) and part.get("type") == "image_url"
                for part in content
            )
            message = {
                "role": "assistant",
                "content": (
                    "我看到你发来的图片了。先不急着算——你觉得题目里哪一个条件最关键？"
                    if has_image
                    else "先别急着要答案。你还记得题目里最先出现的那个条件吗？"
                    "如果把它圈出来，你觉得下一步该先处理谁？"
                ),
            }
            response = {
                "id": "chatcmpl-mock-ask",
                "object": "chat.completion",
                "created": int(time.time()),
                "model": "mock-tutor",
                "choices": [
                    {
                        "index": 0,
                        "message": message,
                        "finish_reason": "stop",
                    }
                ],
                "usage": {"prompt_tokens": 10, "completion_tokens": 10, "total_tokens": 20},
            }
            body = json.dumps(response, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        assistant_tools = {
            call.get("function", {}).get("name")
            for message in messages
            if message.get("role") == "assistant"
            for call in message.get("tool_calls", [])
        }

        message = {"role": "assistant", "content": None}
        if last.get("role") == "tool":
            message["content"] = (
                "记下了。先问你一个简单问题：变量在你看来，是用来做什么的？"
            )
        elif "save_plan" not in assistant_tools:
            message["tool_calls"] = [
                tool_call(
                    "save_plan",
                    {
                        "goal": "Python 入门",
                        "level": "零基础，但接触过电脑",
                        "plan": "先建立变量与类型的感觉，再进入条件与循环，最后写一个小工具。",
                        "kps": [
                            {"title": "变量与赋值", "note": "变量是给数据起名字"},
                            {"title": "条件分支", "note": "让程序做选择"},
                            {"title": "循环", "note": "重复做一件事"},
                        ],
                    },
                    "call_plan_1",
                )
            ]
        elif last.get("role") == "user" and "update_mastery" not in assistant_tools:
            message["tool_calls"] = [
                tool_call(
                    "update_mastery",
                    {
                        "kp_title": "变量与赋值",
                        "mastery": 0.55,
                        "evidence": "学生说变量像贴标签的盒子",
                    },
                    "call_mastery_1",
                ),
                tool_call(
                    "schedule_review",
                    {"kp_title": "变量与赋值", "days": 3},
                    "call_review_1",
                ),
            ]
        else:
            message["content"] = "你的理解基本对。现在试着用 Python 写一行：把 42 赋给变量 answer。"

        response = {
            "id": "chatcmpl-mock",
            "object": "chat.completion",
            "created": int(time.time()),
            "model": "mock-tutor",
            "choices": [
                {
                    "index": 0,
                    "message": message,
                    "finish_reason": "tool_calls" if message.get("tool_calls") else "stop",
                }
            ],
            "usage": {"prompt_tokens": 10, "completion_tokens": 10, "total_tokens": 20},
        }
        body = json.dumps(response, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    HTTPServer(("0.0.0.0", 8787), Handler).serve_forever()
