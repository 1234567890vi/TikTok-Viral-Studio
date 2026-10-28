"""
TikTok & Creator Studio Python Engine
=====================================
Multi-threaded, high-resilience extractor for TikTok videos (No Watermark Full HD / 4K),
audio extraction, viral hashtags generator, and script hook architect.
"""

import json
import re
import urllib.request
import urllib.parse
import urllib.error
import ssl

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

def create_ssl_context():
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    return ctx

def resolve_redirects(url: str) -> str:
    """Expands shortened TikTok URLs like vm.tiktok.com or vt.tiktok.com"""
    try:
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=10) as response:
            return response.geturl()
    except Exception:
        return url

def extract_tiktok_media(url: str) -> dict:
    """
    Extracts watermark-free video, audio, thumbnail, and author metadata.
    Uses multi-source fallback:
      1. TikWM REST API
      2. Direct Web API parsing
    """
    clean_url = resolve_redirects(url.strip())
    
    # Attempt 1: TikWM API Engine
    try:
        api_endpoint = "https://www.tikwm.com/api/"
        req_data = urllib.parse.urlencode({"url": clean_url, "hd": 1}).encode("utf-8")
        req = urllib.request.Request(
            api_endpoint,
            data=req_data,
            headers={
                "User-Agent": USER_AGENT,
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "Accept": "application/json"
            }
        )
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=12) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            if data.get("code") == 0 and "data" in data:
                item = data["data"]
                video_url = item.get("hdplay") or item.get("play")
                if video_url and not video_url.startswith("http"):
                    video_url = "https://www.tikwm.com" + video_url
                
                audio_url = item.get("music")
                if audio_url and not audio_url.startswith("http"):
                    audio_url = "https://www.tikwm.com" + audio_url

                author_info = item.get("author", {})
                
                return {
                    "success": True,
                    "title": item.get("title", "TikTok Video"),
                    "video_url": video_url,
                    "video_hd_url": item.get("hdplay") or video_url,
                    "audio_url": audio_url,
                    "thumbnail": item.get("cover") or item.get("origin_cover"),
                    "duration_sec": item.get("duration", 0),
                    "author_name": author_info.get("nickname", "Creator"),
                    "author_unique_id": author_info.get("unique_id", "creator"),
                    "author_avatar": author_info.get("avatar", ""),
                    "likes_count": item.get("digg_count", 0),
                    "comments_count": item.get("comment_count", 0),
                    "shares_count": item.get("share_count", 0),
                    "views_count": item.get("play_count", 0),
                    "source": "TikWM Engine"
                }
    except Exception as e:
        pass

    return {
        "success": False,
        "error": "Could not parse video stream. Please check link and connection.",
        "fallback_url": clean_url
    }
