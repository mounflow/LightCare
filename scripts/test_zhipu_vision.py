# -*- coding: utf-8 -*-
"""
智谱 GLM-4V 图像识别测试脚本
用途：验证你的智谱 API Key 能否正确识别食物图片 + 估算卡路里。

用法：
    1. 把你的 API Key 填到下面 API_KEY
    2. 准备一张食物图片，路径填到 IMAGE_PATH
    3. 运行：python test_zhipu_vision.py

智谱视觉模型：
    - glm-4v         （基础版，按量计费，有免费额度）
    - glm-4v-plus    （增强版，多图/更高清，推荐）
    - glm-4.5v       （最新版，能力最强，需开通）
先试 glm-4v-plus，不行降 glm-4v。

接口文档：https://open.bigmodel.cn/dev/api/visual/glm-4v
"""

import base64
import json
import sys
import os
import requests

# Windows 终端默认 GBK，强制 UTF-8 输出，避免打印中文/符号时崩溃
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

# ============ 你只需要改这三行 ============
# API Key 从环境变量读取，避免明文泄露。设置方法（Windows）：
#   set ZHIPU_API_KEY=你的key
API_KEY = os.environ.get("ZHIPU_API_KEY", "")                    # 形如 xxxxxx.xxxxxx
IMAGE_PATH = r"D:\AI_Practice\LightCare\scripts\test_food.jpg"   # 改成你的测试图片路径
MODEL = "glm-4.5v"                             # Coding Plan 套餐内的视觉模型
# =========================================

API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

PROMPT = (
    "请识别这张图片里的食物。用 JSON 格式回答，字段：\n"
    "{\n"
    '  "items": [{"name": "食物中文名", "kcal": 估算热量整数, "confidence": 0-1}],\n'
    '  "total_kcal": 总热量整数\n'
    "}\n"
    "只返回 JSON，不要任何解释。"
)


def image_to_base64(path: str) -> str:
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


def main():
    if "在这里粘贴" in API_KEY:
        print("✗ 请先在脚本顶部填写 API_KEY")
        sys.exit(1)

    try:
        b64 = image_to_base64(IMAGE_PATH)
    except FileNotFoundError:
        print(f"✗ 找不到图片：{IMAGE_PATH}")
        print("  请准备一张食物图片，把路径填到 IMAGE_PATH")
        sys.exit(1)

    print(f"图片已读取：{IMAGE_PATH}")
    print(f"base64 长度：{len(b64)} 字符")
    print(f"模型：{MODEL}")
    print("-" * 50)

    payload = {
        "model": MODEL,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "image_url",
                     "image_url": {"url": f"data:image/jpeg;base64,{b64}"}},
                    {"type": "text", "text": PROMPT},
                ],
            }
        ],
        "temperature": 0.1,
    }

    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
    }

    try:
        resp = requests.post(API_URL, headers=headers, json=payload, timeout=60)
    except requests.RequestException as e:
        print(f"✗ 网络请求失败：{e}")
        sys.exit(1)

    print(f"HTTP 状态码：{resp.status_code}")
    print("-" * 50)

    if resp.status_code != 200:
        print("✗ 请求失败，返回内容：")
        print(resp.text)
        sys.exit(1)

    data = resp.json()
    content = data["choices"][0]["message"]["content"]

    print("✓ 识别成功！模型返回：")
    print(content)
    print("-" * 50)

    # 尝试解析 JSON
    try:
        # 模型可能包了 ```json ``` 代码块，剥掉
        clean = content.strip().strip("`")
        if clean.startswith("json"):
            clean = clean[4:].strip()
        result = json.loads(clean)
        print("解析后的结构化结果：")
        print(json.dumps(result, ensure_ascii=False, indent=2))
        print("-" * 50)
        print(f"识别到 {len(result.get('items', []))} 项，总热量约 {result.get('total_kcal')} kcal")
    except (json.JSONDecodeError, KeyError) as e:
        print(f"（返回内容不是标准 JSON，无法结构化解析：{e}）")

    # token 用量
    usage = data.get("usage")
    if usage:
        print("-" * 50)
        print(f"token 用量：{usage}")


if __name__ == "__main__":
    main()
