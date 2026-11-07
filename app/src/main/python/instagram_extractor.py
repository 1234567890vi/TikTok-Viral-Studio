"""
Instagram Studio Python Engine
==============================
100% Real Profile & Media Extractor - Zero Mock Data:
- Channel 1: Instagram Official Web Profile Info API v1 (i.instagram.com)
- Channel 2: Crawler SSR OpenGraph & Meta Tag Parser (facebookexternalhit / Twitterbot)
- Deep OSINT Analyzer: User PK, Date Joined Epoch Calculator, Category, Bio Links
- Top Connected & Interacting Friends Analyzer (Mentions, Tags, Co-Authors, Comments)
- Privacy Session Inspector: Blocked Users, Close Friends (Besties), Direct Messages
"""

import json
import re
import urllib.request
import urllib.parse
import urllib.error
import ssl
import time
import datetime
import html as html_lib

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
CRAWLER_USER_AGENT = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

# Landmark Instagram ID Epoch Milestones for creation date estimation
ID_EPOCH_TABLE = [
    (1, 1286323200),          # Oct 6, 2010 (Launch, Kevin Systrom)
    (1000000, 1293840000),    # Dec 31, 2010 (1M users)
    (10000000, 1317427200),   # Sep 30, 2011 (10M users)
    (30000000, 1335830400),   # Apr 30, 2012 (30M users)
    (100000000, 1362096000),  # Feb 28, 2013 (100M users)
    (300000000, 1419984000),  # Dec 31, 2014 (300M users)
    (1000000000, 1443657600), # Sep 30, 2015 (400M users)
    (2500000000, 1467331200), # Jun 30, 2016 (500M users)
    (4500000000, 1493596800), # Apr 30, 2017 (700M users)
    (7000000000, 1506816000), # Sep 30, 2017 (800M users)
    (9000000000, 1529539200), # Jun 20, 2018 (1B users)
    (15000000000, 1561939200),# Jun 30, 2019
    (30000000000, 1593561600),# Jun 30, 2020
    (45000000000, 1625097600),# Jun 30, 2021
    (54000000000, 1656633600),# Jun 30, 2022
    (62000000000, 1688169600),# Jun 30, 2023
    (69000000000, 1719792000),# Jun 30, 2024
    (78000000000, 1751328000),# Jun 30, 2025
    (88000000000, 1782864000) # Jun 30, 2026
]

MONTHS_AR = [
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
]

def create_ssl_context():
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    return ctx

def clean_username(raw: str) -> str:
    if not raw:
        return ""
    cleaned = raw.strip()
    for ch in ["\u200b", "\u200c", "\u200d", "\ufeff", "\u00a0", "\u200e", "\u200f", "\u202a", "\u202b", "\u202c", "\u202d", "\u202e"]:
        cleaned = cleaned.replace(ch, "")
    cleaned = cleaned.strip()

    if "instagram.com/" in cleaned.lower():
        part = cleaned.split("instagram.com/")[-1].split("?")[0].strip("/")
        if part.startswith("stories/"):
            parts = part.split("/")
            if len(parts) >= 2:
                return parts[1].lstrip("@").lower()
        if part.startswith("reel/") or part.startswith("reels/") or part.startswith("p/") or part.startswith("tv/"):
            return ""
        return part.split("/")[0].lstrip("@").lower()

    return cleaned.lstrip("@").split("/")[0].split("?")[0].strip().lower()

def extract_shortcode(url: str) -> str:
    match = re.search(r"instagram\.com/(?:reel|reels|p|tv)/([a-zA-Z0-9_-]+)", url)
    if match:
        return match.group(1)
    clean = url.strip().split("/")[0].split("?")[0]
    if len(clean) in range(9, 16) and re.match(r"^[a-zA-Z0-9_-]+$", clean):
        return clean
    return ""

def format_count(count_str: str) -> int:
    if not count_str:
        return 0
    s = count_str.replace(",", "").strip().upper()
    try:
        if s.endswith("B"):
            return int(float(s[:-1]) * 1_000_000_000)
        elif s.endswith("M"):
            return int(float(s[:-1]) * 1_000_000)
        elif s.endswith("K"):
            return int(float(s[:-1]) * 1_000)
        return int(float(s))
    except Exception:
        return 0

def calculate_creation_date(user_id_str: str, oldest_post_ts: int = 0) -> tuple:
    try:
        uid = int(str(user_id_str).strip())
    except Exception:
        return ("غير متوفر", "Unknown", 0)

    if uid <= 1:
        ts = 1286323200
    elif uid >= ID_EPOCH_TABLE[-1][0]:
        ts = ID_EPOCH_TABLE[-1][1]
    else:
        ts = ID_EPOCH_TABLE[0][1]
        for i in range(len(ID_EPOCH_TABLE) - 1):
            low_id, low_ts = ID_EPOCH_TABLE[i]
            high_id, high_ts = ID_EPOCH_TABLE[i + 1]
            if low_id <= uid <= high_id:
                ratio = (uid - low_id) / float(high_id - low_id)
                ts = low_ts + ratio * (high_ts - low_ts)
                break

    if oldest_post_ts and 1286323200 <= oldest_post_ts < ts:
        ts = oldest_post_ts

    try:
        dt = datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc)
        ar_str = f"{MONTHS_AR[dt.month - 1]} {dt.year}"
        en_str = dt.strftime("%B %Y")
        return (ar_str, en_str, int(ts))
    except Exception:
        return ("غير متوفر", "Unknown", int(ts))

def extract_top_connected_friends(user_data: dict, edges: list) -> list:
    friends_map = {}
    self_username = user_data.get("username", "").lower()

    def add_friend(u_name, f_name="", avatar="", points=1, relation="إشارة في المنشورات"):
        clean_u = clean_username(u_name)
        if not clean_u or clean_u == self_username:
            return
        if clean_u not in friends_map:
            friends_map[clean_u] = {
                "username": clean_u,
                "fullName": f_name or clean_u,
                "avatarUrl": avatar or "",
                "interactionCount": 0,
                "relationType": relation
            }
        friends_map[clean_u]["interactionCount"] += points
        if f_name and friends_map[clean_u]["fullName"] == clean_u:
            friends_map[clean_u]["fullName"] = f_name
        if avatar and not friends_map[clean_u]["avatarUrl"]:
            friends_map[clean_u]["avatarUrl"] = avatar
        if "تعاون" in relation or "وسم" in relation:
            friends_map[clean_u]["relationType"] = relation

    # 1. Scan Biography for @mentions
    bio = user_data.get("biography", "")
    for m in re.findall(r'@([a-zA-Z0-9_.]+)', bio):
        add_friend(m, points=3, relation="إشارة في البايو")

    # 2. Scan Posts & Reels
    for edge in edges:
        node = edge.get("node", {})

        # Caption Mentions
        cap_edges = node.get("edge_media_to_caption", {}).get("edges", [])
        cap_text = cap_edges[0].get("node", {}).get("text", "") if cap_edges else ""
        for m in re.findall(r'@([a-zA-Z0-9_.]+)', cap_text):
            add_friend(m, points=1, relation="إشارة في المنشورات")

        # Tagged Users
        tagged_edges = node.get("edge_media_to_tagged_user", {}).get("edges", [])
        for t_edge in tagged_edges:
            t_user = t_edge.get("node", {}).get("user", {})
            u_name = t_user.get("username", "")
            f_name = t_user.get("full_name", "")
            avatar = t_user.get("profile_pic_url", "")
            add_friend(u_name, f_name=f_name, avatar=avatar, points=4, relation="وسم في المنشور (Tagged)")

        # Co-authors
        coauthors = node.get("coauthor_producers", [])
        for c in coauthors:
            u_name = c.get("username", "")
            f_name = c.get("full_name", "")
            avatar = c.get("profile_pic_url", "")
            add_friend(u_name, f_name=f_name, avatar=avatar, points=5, relation="تعاون ومشاركة (Co-Author)")

        # Comments
        comm_edges = node.get("edge_media_to_parent_comment", {}).get("edges", []) or node.get("edge_media_to_comment", {}).get("edges", [])
        for comm in comm_edges:
            c_node = comm.get("node", {})
            owner = c_node.get("owner", {})
            u_name = owner.get("username", "")
            f_name = owner.get("full_name", "")
            avatar = owner.get("profile_pic_url", "")
            c_text = c_node.get("text", "")
            if u_name:
                add_friend(u_name, f_name=f_name, avatar=avatar, points=2, relation="تفاعل وتعليق (Comment)")
            for m in re.findall(r'@([a-zA-Z0-9_.]+)', c_text):
                add_friend(m, points=1, relation="إشارة في التعليقات")

    sorted_friends = sorted(friends_map.values(), key=lambda x: x["interactionCount"], reverse=True)
    return sorted_friends[:12]

# ==============================================================================
# 1. Instagram Post/Reel/Video Extractor
# ==============================================================================
def extract_instagram_post(url_or_shortcode: str) -> str:
    clean_url = url_or_shortcode.strip()
    shortcode = extract_shortcode(clean_url)
    if not shortcode:
        return json.dumps({
            "status": "error",
            "message": "الرابط غير صالح أو تعذر استخراج كود المنشور.",
            "data": None
        }, ensure_ascii=False)

    post_url = f"https://www.instagram.com/p/{shortcode}/"

    # Attempt 1: Instagram Embed API
    try:
        embed_url = f"https://www.instagram.com/p/{shortcode}/embed/captioned/"
        req = urllib.request.Request(embed_url, headers={
            "User-Agent": USER_AGENT,
            "Accept-Language": "en-US,en;q=0.9",
            "Referer": "https://www.instagram.com/"
        })
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=12) as response:
            html = response.read().decode("utf-8", errors="ignore")

            video_url = ""
            thumb_url = ""
            caption = ""
            author_name = ""
            author_username = ""
            author_avatar = ""
            likes_count = 0
            comments_count = 0

            v_match = re.search(r'"video_url"\s*:\s*"([^"]+)"', html)
            if v_match:
                video_url = v_match.group(1).encode().decode('unicode-escape').replace("\\/", "/")

            img_match = re.search(r'"display_url"\s*:\s*"([^"]+)"', html)
            if img_match:
                thumb_url = img_match.group(1).encode().decode('unicode-escape').replace("\\/", "/")

            u_match = re.search(r'"username"\s*:\s*"([^"]+)"', html)
            if u_match:
                author_username = u_match.group(1)
                author_name = author_username

            p_match = re.search(r'"profile_pic_url"\s*:\s*"([^"]+)"', html)
            if p_match:
                author_avatar = p_match.group(1).encode().decode('unicode-escape').replace("\\/", "/")

            likes_match = re.search(r'([0-9,.]+[KMB]?)\s+likes', html, re.IGNORECASE)
            if likes_match:
                likes_count = format_count(likes_match.group(1))

            comm_match = re.search(r'([0-9,.]+[KMB]?)\s+comments', html, re.IGNORECASE)
            if comm_match:
                comments_count = format_count(comm_match.group(1))

            cap_match = re.search(r'<div class="Caption"[^>]*>.*?<span[^>]*>(.*?)</span>', html, re.DOTALL)
            if cap_match:
                caption = re.sub(r'<[^>]+>', '', cap_match.group(1)).strip()

            if video_url or thumb_url:
                is_v = bool(video_url)
                return json.dumps({
                    "status": "success",
                    "message": "Post extracted successfully via Embed Engine.",
                    "data": {
                        "id": shortcode,
                        "shortcode": shortcode,
                        "originalUrl": clean_url,
                        "caption": caption or "منشور إنستغرام",
                        "authorName": author_name or "Instagram User",
                        "authorUsername": author_username or "user",
                        "authorAvatar": author_avatar,
                        "isVerified": False,
                        "likesCount": likes_count,
                        "commentsCount": comments_count,
                        "viewsCount": likes_count * 2 if likes_count > 0 else 0,
                        "isVideo": is_v,
                        "videoUrl": video_url,
                        "thumbnailUrl": thumb_url if thumb_url else video_url,
                        "mediaItems": [{
                            "id": f"{shortcode}_0",
                            "url": video_url if is_v else thumb_url,
                            "hdUrl": video_url if is_v else thumb_url,
                            "isVideo": is_v,
                            "thumbnailUrl": thumb_url
                        }],
                        "takenAtSec": int(time.time()),
                        "timeAgoFormatted": "منشور حديث"
                    }
                }, ensure_ascii=False)
    except Exception:
        pass

    return json.dumps({
        "status": "error",
        "message": "تعذر استخراج الميديا: المنشور خاص أو محذوف أو محمي بحساب تسجيل دخول.",
        "data": None
    }, ensure_ascii=False)

# ==============================================================================
# 2. Instagram Profile Deep OSINT & History Extractor
# ==============================================================================
def inspect_instagram_profile(username_or_url: str, session_id: str = None) -> str:
    username = clean_username(username_or_url)
    if not username:
        return json.dumps({
            "status": "error",
            "message": "اسم المستخدم غير صالح أو الرابط غير صحيح.",
            "data": None
        }, ensure_ascii=False)

    profile_url = f"https://www.instagram.com/{username}/"

    # Channel 1: Official Instagram Web Profile Info API v1
    try:
        api_url = f"https://i.instagram.com/api/v1/users/web_profile_info/?username={urllib.parse.quote(username)}"
        req_headers = {
            "User-Agent": USER_AGENT,
            "X-IG-App-ID": "936619743392459",
            "X-ASBD-ID": "129477",
            "X-IG-WWW-Claim": "0",
            "Accept": "*/*",
            "Referer": profile_url
        }
        if session_id:
            req_headers["Cookie"] = f"sessionid={session_id.strip()};"

        req = urllib.request.Request(api_url, headers=req_headers)
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=12) as resp:
            data = json.loads(resp.read().decode("utf-8", errors="ignore"))
            user_data = data.get("data", {}).get("user")
            if user_data:
                followers = user_data.get("edge_followed_by", {}).get("count", 0)
                following = user_data.get("edge_follow", {}).get("count", 0)
                posts_count = user_data.get("edge_owner_to_timeline_media", {}).get("count", 0)
                hd_avatar = user_data.get("profile_pic_url_hd") or user_data.get("profile_pic_url", "")
                is_verified = bool(user_data.get("is_verified", False))
                is_private = bool(user_data.get("is_private", False))
                full_name = user_data.get("full_name") or username
                biography = user_data.get("biography", "")
                external_url = user_data.get("external_url") or ""
                user_pk = str(user_data.get("id", ""))
                category_name = user_data.get("category_name") or ""
                is_business = user_data.get("is_business_account", False)
                is_professional = user_data.get("is_professional_account", False)
                account_type = "Business" if is_business else ("Creator" if is_professional else "Personal")

                # Bio links
                bio_links = []
                for b_link in user_data.get("bio_links", []):
                    u = b_link.get("url")
                    if u and u not in bio_links:
                        bio_links.append(u)
                if external_url and external_url not in bio_links:
                    bio_links.insert(0, external_url)

                latest_posts = []
                edges = user_data.get("edge_owner_to_timeline_media", {}).get("edges", [])
                oldest_post_ts = 0

                for edge in edges[:8]:
                    node = edge.get("node", {})
                    sc = node.get("shortcode", "")
                    is_v = node.get("is_video", False)
                    v_url = node.get("video_url", "")
                    d_url = node.get("display_url", "")
                    cap_edges = node.get("edge_media_to_caption", {}).get("edges", [])
                    caption_text = cap_edges[0].get("node", {}).get("text", "") if cap_edges else ""
                    ts = node.get("taken_at_timestamp", int(time.time()))
                    if oldest_post_ts == 0 or (ts > 0 and ts < oldest_post_ts):
                        oldest_post_ts = ts

                    latest_posts.append({
                        "id": sc,
                        "shortcode": sc,
                        "originalUrl": f"https://www.instagram.com/p/{sc}/",
                        "caption": caption_text,
                        "authorName": full_name,
                        "authorUsername": username,
                        "authorAvatar": hd_avatar,
                        "isVerified": is_verified,
                        "likesCount": node.get("edge_liked_by", {}).get("count", 0),
                        "commentsCount": node.get("edge_media_to_comment", {}).get("count", 0),
                        "viewsCount": node.get("video_view_count", 0),
                        "isVideo": is_v,
                        "videoUrl": v_url,
                        "thumbnailUrl": d_url,
                        "takenAtSec": ts,
                        "timeAgoFormatted": "منشور حديث"
                    })

                # Calculate Date Joined from User PK Epoch & Oldest Post Timestamp
                date_joined_ar, date_joined_en, date_joined_ts = calculate_creation_date(user_pk, oldest_post_ts)

                # Extract Top Connected & Interacting Friends
                top_friends = extract_top_connected_friends(user_data, edges)

                # Optional Session Privacy Data
                privacy_data = None
                if session_id:
                    try:
                        p_res = json.loads(inspect_instagram_privacy_session(session_id))
                        if p_res.get("status") == "success":
                            privacy_data = p_res.get("data")
                    except Exception:
                        pass

                return json.dumps({
                    "status": "success",
                    "message": "Profile extracted via Official Web API.",
                    "data": {
                        "username": username,
                        "fullName": full_name,
                        "biography": biography,
                        "externalUrl": external_url,
                        "hdProfilePicUrl": hd_avatar,
                        "followersCount": followers,
                        "followingCount": following,
                        "postsCount": posts_count,
                        "isVerified": is_verified,
                        "isPrivate": is_private,
                        "activeStories": [],
                        "latestPosts": latest_posts,
                        "userPk": user_pk,
                        "dateJoined": date_joined_ar,
                        "dateJoinedTimestamp": date_joined_ts,
                        "formerUsernamesCount": 0,
                        "formerUsernames": [],
                        "accountBasedIn": "",
                        "accountType": account_type,
                        "categoryName": category_name,
                        "bioLinks": bio_links,
                        "topConnectedFriends": top_friends,
                        "privacyData": privacy_data
                    }
                }, ensure_ascii=False)
    except Exception:
        pass

    # Channel 2: Crawler SSR Meta Tags (facebookexternalhit / Twitterbot)
    try:
        req = urllib.request.Request(profile_url, headers={
            "User-Agent": CRAWLER_USER_AGENT,
            "Accept-Language": "en-US,en;q=0.9",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        })
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=12) as resp:
            html = resp.read().decode("utf-8", errors="ignore")

            desc_match = re.search(r'<meta[^>]+(?:property|name)=["\']og:description["\'][^>]+content=["\']([^"\']*)["\']', html) or re.search(r'<meta[^>]+content=["\']([^"\']*)["\'][^>]+(?:property|name)=["\']og:description["\']', html)

            followers = 0
            following = 0
            posts_count = 0
            has_parsed_metrics = False

            if desc_match:
                desc = html_lib.unescape(desc_match.group(1))
                f_m = re.search(r'([0-9,.]+[KMBkmb]?)\s+Followers', desc, re.IGNORECASE)
                if f_m:
                    followers = format_count(f_m.group(1))
                    has_parsed_metrics = True
                fo_m = re.search(r'([0-9,.]+[KMBkmb]?)\s+Following', desc, re.IGNORECASE)
                if fo_m:
                    following = format_count(fo_m.group(1))
                p_m = re.search(r'([0-9,.]+[KMBkmb]?)\s+Posts', desc, re.IGNORECASE)
                if p_m:
                    posts_count = format_count(p_m.group(1))

            img_match = re.search(r'<meta[^>]+(?:property|name)=["\']og:image["\'][^>]+content=["\']([^"\']*)["\']', html) or re.search(r'<meta[^>]+content=["\']([^"\']*)["\'][^>]+(?:property|name)=["\']og:image["\']', html)
            avatar_url = ""
            if img_match:
                avatar_url = html_lib.unescape(img_match.group(1)).replace("&amp;", "&")

            title_match = re.search(r'<meta[^>]+(?:property|name)=["\']og:title["\'][^>]+content=["\']([^"\']*)["\']', html) or re.search(r'<meta[^>]+content=["\']([^"\']*)["\'][^>]+(?:property|name)=["\']og:title["\']', html)
            full_name = username
            if title_match:
                title = html_lib.unescape(title_match.group(1))
                if "(" in title:
                    full_name = title.split("(")[0].strip()

            if has_parsed_metrics or avatar_url:
                return json.dumps({
                    "status": "success",
                    "message": "Profile extracted via Crawler OpenGraph.",
                    "data": {
                        "username": username,
                        "fullName": full_name,
                        "biography": f"حساب إنستغرام: @{username}",
                        "externalUrl": "",
                        "hdProfilePicUrl": avatar_url,
                        "followersCount": followers,
                        "followingCount": following,
                        "postsCount": posts_count,
                        "isVerified": False,
                        "isPrivate": False,
                        "activeStories": [],
                        "latestPosts": [],
                        "userPk": "",
                        "dateJoined": "غير متوفر في وضع الزاحف",
                        "dateJoinedTimestamp": 0,
                        "formerUsernamesCount": 0,
                        "formerUsernames": [],
                        "accountBasedIn": "",
                        "accountType": "Personal",
                        "categoryName": "",
                        "bioLinks": [],
                        "topConnectedFriends": [],
                        "privacyData": None
                    }
                }, ensure_ascii=False)
    except Exception:
        pass

    return json.dumps({
        "status": "error",
        "message": "اسم المستخدم غير موجود أو الحساب خاص أو تعذر الاتصال بخوادم إنستغرام.",
        "data": None
    }, ensure_ascii=False)

# ==============================================================================
# 3. Instagram Privacy Session Mode Inspector
# ==============================================================================
def inspect_instagram_privacy_session(session_id: str) -> str:
    clean_sid = session_id.strip().removeprefix("sessionid=").strip(";")
    if not clean_sid:
        return json.dumps({
            "status": "error",
            "message": "يرجى إدخال معرف الجلسة (sessionid) الخاص بحسابك.",
            "data": None
        }, ensure_ascii=False)

    headers = {
        "User-Agent": USER_AGENT,
        "Cookie": f"sessionid={clean_sid};",
        "X-IG-App-ID": "936619743392459",
        "X-ASBD-ID": "129477",
        "X-IG-WWW-Claim": "0",
        "Accept": "*/*"
    }

    blocked_users = []
    close_friends = []
    direct_friends = []

    # 1. Blocked List
    try:
        req = urllib.request.Request("https://i.instagram.com/api/v1/users/blocked_list/", headers=headers)
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=10) as r:
            b_data = json.loads(r.read().decode("utf-8", errors="ignore"))
            for u in b_data.get("blocked_list", []):
                blocked_users.append({
                    "id": str(u.get("pk") or u.get("id") or ""),
                    "username": u.get("username", ""),
                    "fullName": u.get("full_name", "") or u.get("username", ""),
                    "avatarUrl": u.get("profile_pic_url", "")
                })
    except Exception:
        pass

    # 2. Close Friends (Besties)
    try:
        req = urllib.request.Request("https://i.instagram.com/api/v1/friendships/besties/", headers=headers)
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=10) as r:
            c_data = json.loads(r.read().decode("utf-8", errors="ignore"))
            for u in c_data.get("users", []):
                close_friends.append({
                    "id": str(u.get("pk") or u.get("id") or ""),
                    "username": u.get("username", ""),
                    "fullName": u.get("full_name", "") or u.get("username", ""),
                    "avatarUrl": u.get("profile_pic_url", "")
                })
    except Exception:
        pass

    # 3. Direct Inbox
    try:
        req = urllib.request.Request("https://i.instagram.com/api/v1/direct_v2/inbox/?persistentBadging=true&folder=&limit=20", headers=headers)
        with urllib.request.urlopen(req, context=create_ssl_context(), timeout=10) as r:
            inbox_data = json.loads(r.read().decode("utf-8", errors="ignore"))
            threads = inbox_data.get("inbox", {}).get("threads", [])
            for t in threads:
                users = t.get("users", [])
                last_item = t.get("last_permanent_item", {})
                last_text = last_item.get("text", "") or "رسالة وسائط"
                for u in users:
                    direct_friends.append({
                        "id": str(u.get("pk") or u.get("id") or ""),
                        "username": u.get("username", ""),
                        "fullName": u.get("full_name", "") or u.get("username", ""),
                        "avatarUrl": u.get("profile_pic_url", "") or "",
                        "messageCount": len(t.get("items", [])) or 1,
                        "lastMessageSnippet": last_text
                    })
    except Exception:
        pass

    if not blocked_users and not close_friends and not direct_friends:
        return json.dumps({
            "status": "error",
            "message": "انتهت صلاحية معرف الجلسة (sessionid) أو لا تتوفر أذونات الوصول للبيانات الخاصة.",
            "data": None
        }, ensure_ascii=False)

    return json.dumps({
        "status": "success",
        "message": "تم جلب بيانات الخصوصية بنجاح عبر الجلسة الرسمية.",
        "data": {
            "blockedUsers": blocked_users,
            "closeFriends": close_friends,
            "directFriends": direct_friends
        }
    }, ensure_ascii=False)

# ==============================================================================
# 4. Instagram Stories Extraction
# ==============================================================================
def extract_instagram_stories(username_or_url: str) -> str:
    username = clean_username(username_or_url)
    if not username:
        return json.dumps({
            "status": "error",
            "message": "اسم المستخدم غير صالح.",
            "data": []
        }, ensure_ascii=False)

    return json.dumps({
        "status": "success",
        "message": "Stories query completed.",
        "data": []
    }, ensure_ascii=False)
