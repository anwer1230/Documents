"""
╔══════════════════════════════════════════════════════════════════════════╗
║   محلّل دولة المجموعة على تيليجرام — Analysis Pipeline متكامل            ║
║   يجمع: تحليل المحتوى + اللهجة + العملة + DC + OSINT + Groq AI            ║
╚══════════════════════════════════════════════════════════════════════════╝
"""

import os
import re
import json
import time
import base64
import asyncio
import logging
import requests
import threading
from datetime import datetime
from collections import Counter

logger = logging.getLogger(__name__)

# ══════════════════════════════════════════════════════════════════════════
#  إعدادات — مفتاح Groq
# ══════════════════════════════════════════════════════════════════════════
GROQ_API_KEY_DEFAULT = os.environ.get("GROQ_API_KEY", "").strip()
GROQ_API_KEY = str(os.environ.get('GROQ_API_KEY') or GROQ_API_KEY_DEFAULT).strip()
os.environ['GROQ_API_KEY'] = GROQ_API_KEY

# ── خريطة مراكز بيانات تيليجرام ──
DC_LOCATIONS = {
    1: {'city': 'ميامي',        'country': 'الولايات المتحدة', 'code': 'US'},
    2: {'city': 'أمستردام',     'country': 'هولندا',          'code': 'NL'},
    3: {'city': 'ميامي',        'country': 'الولايات المتحدة', 'code': 'US'},
    4: {'city': 'أمستردام',     'country': 'هولندا',          'code': 'NL'},
    5: {'city': 'سنغافورة',     'country': 'سنغافورة',        'code': 'SG'},
}

# ── مؤشرات الدول/المناطق حسب اللهجة والعملة والرموز ──
COUNTRY_INDICATORS = {
    'السعودية': {
        'dialect': ['ابشر', 'مايهمك', 'وش', 'ايش', 'كيفك', 'زين', 'عساك', 'تسلم', 'ياطويل العمر', 'أبشر', 'حياك', 'فالك', 'ارحب', 'لاهنت'],
        'currency': ['ريال', 'SAR', '﷼', 'هللة', 'ريال سعودي'],
        'cities':   ['الرياض', 'جدة', 'مكة', 'المدينة', 'الدمام', 'الخبر', 'أبها', 'الطائف', 'تبوك', 'جازان', 'حائل', 'نجران', 'القصيم', 'بريدة'],
        'phone':    ['+966', '966', '05'],
        'domains':  ['.sa', '.com.sa', '.edu.sa', '.gov.sa'],
    },
    'الإمارات': {
        'dialect': ['شو', 'زين', 'الحين', 'يعطيك العافية', 'شحالك', 'أبا', 'وايد', 'سير', 'طرشلي', 'فديتك'],
        'currency': ['درهم', 'AED', 'فلس', 'درهم إماراتي'],
        'cities':   ['دبي', 'أبوظبي', 'الشارقة', 'عجمان', 'رأس الخيمة', 'الفجيرة', 'العين', 'أم القيوين'],
        'phone':    ['+971', '971', '050', '052', '054', '055', '056'],
        'domains':  ['.ae', '.com.ae'],
    },
    'مصر': {
        'dialect': ['ازاي', 'إيه', 'عامل ايه', 'خلاص', 'كده', 'دلوقتي', 'أوي', 'معلش', 'بص', 'يعني', 'حبيبي', 'تمام كده', 'باشا'],
        'currency': ['جنيه', 'EGP', 'قرش', 'جنيه مصري'],
        'cities':   ['القاهرة', 'الإسكندرية', 'الجيزة', 'طنطا', 'المنصورة', 'أسيوط', 'أسوان', 'الزقازيق', 'بورسعيد', 'السويس'],
        'phone':    ['+20', '20', '010', '011', '012', '015'],
        'domains':  ['.eg', '.com.eg'],
    },
    'الكويت': {
        'dialect': ['شلونك', 'زين', 'اي', 'چذي', 'وايد', 'شكو ماكو', 'ابشري', 'قواك الله', 'يا هلا'],
        'currency': ['دينار', 'KWD', 'فلس', 'دينار كويتي'],
        'cities':   ['الكويت', 'الجهراء', 'حولي', 'الفروانية', 'الأحمدي', 'مبارك الكبير'],
        'phone':    ['+965', '965'],
        'domains':  ['.kw', '.com.kw'],
    },
    'قطر': {
        'dialect': ['شلونك', 'زين', 'وايد', 'أكيد', 'يا طويل العمر', 'مرحبا ومسهلا'],
        'currency': ['ريال قطري', 'QAR'],
        'cities':   ['الدوحة', 'الوكرة', 'الخور', 'الريان', 'الشمال'],
        'phone':    ['+974', '974'],
        'domains':  ['.qa', '.com.qa'],
    },
    'البحرين': {
        'dialect': ['شلونك', 'زين', 'وايد', 'اي', 'خوش', 'شخبارك'],
        'currency': ['دينار بحريني', 'BHD'],
        'cities':   ['المنامة', 'المحرق', 'الرفاع', 'سترة', 'مدينة عيسى', 'مدينة حمد'],
        'phone':    ['+973', '973'],
        'domains':  ['.bh'],
    },
    'عُمان': {
        'dialect': ['شحالك', 'زين', 'وايد', 'مو', 'تو', 'عاد', 'اخبارك', 'ماشي باس'],
        'currency': ['ريال عماني', 'OMR', 'بيسة'],
        'cities':   ['مسقط', 'صلالة', 'صحار', 'نزوى', 'صور', 'البريمي'],
        'phone':    ['+968', '968'],
        'domains':  ['.om'],
    },
    'الأردن': {
        'dialect': ['شو', 'كيفك', 'زلمة', 'منيح', 'شو أخبارك', 'يخزي', 'هيك', 'يسعد مساك', 'هسا'],
        'currency': ['دينار أردني', 'JOD'],
        'cities':   ['عمان', 'الزرقاء', 'إربد', 'العقبة', 'المفرق', 'جرش', 'مادبا'],
        'phone':    ['+962', '962', '077', '078', '079'],
        'domains':  ['.jo', '.com.jo'],
    },
    'العراق': {
        'dialect': ['شلونك', 'شكو ماكو', 'هواية', 'زين', 'شبيها', 'چان', 'فدوه', 'عيني', 'هسه'],
        'currency': ['دينار عراقي', 'IQD'],
        'cities':   ['بغداد', 'البصرة', 'الموصل', 'أربيل', 'النجف', 'كربلاء', 'السليمانية', 'بابل'],
        'phone':    ['+964', '964', '077', '078', '079'],
        'domains':  ['.iq'],
    },
    'سوريا': {
        'dialect': ['شو', 'كيفك', 'منيح', 'هلق', 'لك', 'ياعمي', 'شلونك', 'حبيبي', 'تقبرني'],
        'currency': ['ليرة سورية', 'SYP'],
        'cities':   ['دمشق', 'حلب', 'حمص', 'حماة', 'اللاذقية', 'دير الزور', 'طرطوس'],
        'phone':    ['+963', '963'],
        'domains':  ['.sy'],
    },
    'لبنان': {
        'dialect': ['شو', 'كيفك', 'هيدا', 'هيدي', 'منيح', 'كتير', 'لك', 'تيكير', 'ميرسي'],
        'currency': ['ليرة لبنانية', 'LBP'],
        'cities':   ['بيروت', 'طرابلس', 'صيدا', 'صور', 'زحلة', 'جونية', 'جبيل'],
        'phone':    ['+961', '961'],
        'domains':  ['.lb'],
    },
    'فلسطين': {
        'dialect': ['شو', 'كيفك', 'منيح', 'هلق', 'زي', 'لك', 'وين', 'يسعدك', 'هيك'],
        'currency': ['شيكل', 'ILS', 'دينار أردني'],
        'cities':   ['القدس', 'غزة', 'رام الله', 'نابلس', 'الخليل', 'بيت لحم', 'جنين'],
        'phone':    ['+970', '970', '+972'],
        'domains':  ['.ps'],
    },
    'اليمن': {
        'dialect': ['كيف حالك', 'زين', 'شو', 'ايش', 'عساك', 'طيب', 'وانت', 'ياخبير', 'حياك الله'],
        'currency': ['ريال يمني', 'YER'],
        'cities':   ['صنعاء', 'عدن', 'تعز', 'الحديدة', 'إب', 'حضرموت', 'المكلا'],
        'phone':    ['+967', '967'],
        'domains':  ['.ye'],
    },
    'المغرب': {
        'dialect': ['كيفاش', 'بزاف', 'مزيان', 'دابا', 'واخا', 'غير', 'صافي', 'لاباس', 'تبارك الله'],
        'currency': ['درهم مغربي', 'MAD'],
        'cities':   ['الدار البيضاء', 'الرباط', 'فاس', 'مراكش', 'طنجة', 'أكادير', 'مكناس'],
        'phone':    ['+212', '212'],
        'domains':  ['.ma'],
    },
    'الجزائر': {
        'dialect': ['كيفاش', 'بزاف', 'مليح', 'واش', 'راك', 'خلاص', 'لاباس', 'صحا'],
        'currency': ['دينار جزائري', 'DZD'],
        'cities':   ['الجزائر', 'وهران', 'قسنطينة', 'عنابة', 'سطيف', 'باتنة'],
        'phone':    ['+213', '213'],
        'domains':  ['.dz'],
    },
    'تونس': {
        'dialect': ['شنوة', 'برشة', 'باهي', 'كيفاش', 'ياخي', 'علاش', 'شنحوالك', 'يعيشك'],
        'currency': ['دينار تونسي', 'TND'],
        'cities':   ['تونس', 'صفاقس', 'سوسة', 'بنزرت', 'القيروان', 'قابس', 'المنستير'],
        'phone':    ['+216', '216'],
        'domains':  ['.tn'],
    },
    'ليبيا': {
        'dialect': ['شحالك', 'زي', 'وايد', 'وين', 'شنو', 'شنجوك', 'تي شنو', 'باهي'],
        'currency': ['دينار ليبي', 'LYD'],
        'cities':   ['طرابلس', 'بنغازي', 'مصراتة', 'الزاوية', 'سبها', 'البيضاء'],
        'phone':    ['+218', '218'],
        'domains':  ['.ly'],
    },
    'السودان': {
        'dialect': ['كيفنك', 'زول', 'شديد', 'قسما', 'بالله', 'يا زول', 'حبابك', 'ان شاء الله طيب'],
        'currency': ['جنيه سوداني', 'SDG'],
        'cities':   ['الخرطوم', 'أم درمان', 'بورتسودان', 'كسلا', 'الأبيض', 'ود مدني'],
        'phone':    ['+249', '249'],
        'domains':  ['.sd'],
    },
    'تركيا': {
        'dialect': ['merhaba', 'nasılsın', 'tamam', 'evet', 'hayır', 'teşekkürler'],
        'currency': ['ليرة تركية', 'TRY', 'TL'],
        'cities':   ['إسطنبول', 'أنقرة', 'إزمير', 'أنطاليا', 'بورصة'],
        'phone':    ['+90', '90'],
        'domains':  ['.tr', '.com.tr'],
    },
    'إيران': {
        'dialect': ['سلام', 'ممنون', 'خوبی', 'بله', 'نه', 'تشکر'],
        'currency': ['ريال إيراني', 'IRR', 'تومان'],
        'cities':   ['طهران', 'أصفهان', 'مشهد', 'شيراز', 'تبريز'],
        'phone':    ['+98', '98'],
        'domains':  ['.ir'],
    },
}


# ══════════════════════════════════════════════════════════════════════════
#  الجزء الأول: جمع البيانات الوصفية (Metadata)
# ══════════════════════════════════════════════════════════════════════════

async def _collect_metadata(client, entity):
    """جمع كل البيانات الوصفية المتاحة من المجموعة أو القناة."""
    meta = {
        'id':                getattr(entity, 'id', None),
        'title':             getattr(entity, 'title', None) or getattr(entity, 'first_name', ''),
        'username':          getattr(entity, 'username', None),
        'participants_count': getattr(entity, 'participants_count', 0) or 0,
        'about':             getattr(entity, 'about', '') or '',
        'dc_id':             None,
        'created_date':      None,
        'is_megagroup':      bool(getattr(entity, 'megagroup', False)),
        'is_broadcast':      bool(getattr(entity, 'broadcast', False)),
        'is_verified':       bool(getattr(entity, 'verified', False)),
        'is_restricted':     bool(getattr(entity, 'restricted', False)),
        'language_hint':     None,
    }

    # محاولة الحصول على الوصف الكامل
    try:
        from telethon.tl.functions.channels import GetFullChannelRequest
        full = await client(GetFullChannelRequest(channel=entity))
        if full and full.full_chat:
            meta['about'] = full.full_chat.about or meta['about']
            meta['participants_count'] = full.full_chat.participants_count or meta['participants_count']
    except Exception as e:
        logger.debug(f"GetFullChannelRequest failed: {e}")

    # استخراج DC من الصورة
    try:
        if hasattr(entity, 'photo') and entity.photo:
            meta['dc_id'] = getattr(entity.photo, 'dc_id', None)
    except Exception:
        pass

    # تاريخ الإنشاء التقريبي = تاريخ أول رسالة
    try:
        first_msg = await client.get_messages(entity, limit=1, reverse=True)
        if first_msg and first_msg[0] and first_msg[0].date:
            meta['created_date'] = first_msg[0].date.isoformat()
    except Exception as e:
        logger.debug(f"First message fetch failed: {e}")

    return meta


# ══════════════════════════════════════════════════════════════════════════
#  الجزء الثاني: تحليل المحتوى محلياً (بدون AI)
# ══════════════════════════════════════════════════════════════════════════

def _analyze_content_locally(texts):
    """
    تحليل محلي سريع: اللهجة + العملة + المدن + أرقام الهواتف + النطاقات.
    يُعيد قاموس بأعلى الدول ترجيحاً مع الأدلة.
    """
    full = "\n".join(texts).lower()
    scores = Counter()
    evidences = {}

    for country, ind in COUNTRY_INDICATORS.items():
        ev = []

        # اللهجة
        for kw in ind['dialect']:
            if kw.lower() in full:
                scores[country] += 3
                ev.append(f"لهجة: {kw}")
                break

        # العملة
        for cur in ind['currency']:
            if cur.lower() in full:
                scores[country] += 5
                ev.append(f"عملة: {cur}")
                break

        # المدن
        for city in ind['cities']:
            if city in full:
                scores[country] += 4
                ev.append(f"مدينة: {city}")

        # أكواد الهاتف
        for ph in ind['phone']:
            if ph in full:
                scores[country] += 6
                ev.append(f"هاتف: {ph}")
                break

        # النطاقات
        for dm in ind['domains']:
            if dm in full:
                scores[country] += 3
                ev.append(f"نطاق: {dm}")
                break

        if ev:
            evidences[country] = ev[:6]

    # روابط واتساب/تيليجرام بأرقام دولية
    intl_phones = re.findall(r'\+(\d{1,3})[\s\-]?\d', full)
    for code in intl_phones:
        for country, ind in COUNTRY_INDICATORS.items():
            if f"+{code}" in ind['phone'] or code in [p.replace('+', '') for p in ind['phone']]:
                scores[country] += 2
                if country in evidences:
                    evidences[country].append(f"رقم دولي: +{code}")
                else:
                    evidences[country] = [f"رقم دولي: +{code}"]
                break

    if not scores:
        return {'country': None, 'confidence': 'منخفضة', 'score': 0, 'evidences': []}

    top_country, top_score = scores.most_common(1)[0]
    total = sum(scores.values())
    confidence = 'عالية' if top_score / max(total, 1) > 0.5 else \
                 'متوسطة' if top_score / max(total, 1) > 0.3 else 'منخفضة'

    return {
        'country':    top_country,
        'confidence': confidence,
        'score':      top_score,
        'all_scores': dict(scores.most_common(5)),
        'evidences':  evidences.get(top_country, []),
    }


# ══════════════════════════════════════════════════════════════════════════
#  الجزء الثالث: تحليل المحتوى بالذكاء الاصطناعي (Groq)
# ══════════════════════════════════════════════════════════════════════════

def _analyze_content_with_groq(texts, sample_size=60):
    """تحليل متقدم باستخدام Llama-3.3-70b عبر Groq بالمفتاح الثابت المدمج."""
    api_key = GROQ_API_KEY or _GROQ_DEFAULT_KEY

    try:
        from groq import Groq
    except ImportError:
        logger.warning("مكتبة groq غير مثبتة")
        return {'country': None, 'error': 'مكتبة groq غير متوفرة'}

    # عيّنة من الرسائل
    sample = "\n---\n".join(texts[:sample_size])
    sample = sample[:5000]  # حد أمان

    prompt = f"""أنت محلّل خبير في تحديد الدولة الجغرافية لمجموعات وقنوات تيليجرام.

حلّل الرسائل التالية واستنتج الدولة/المنطقة الأرجح للمجموعة بناءً على:
- اللهجة والكلمات العامية
- العملة والأسعار المذكورة
- المدن والأماكن والجامعات
- الأرقام والبادئات الدولية
- الأحداث والمناسبات والعادات

═══════ الرسائل ═══════
{sample}
══════════════════════

أجب بصيغة JSON صريحة فقط (بدون أي علامات markdown أو كتل إضافية):
{{
  "country": "اسم الدولة بالعربية (مثال: السعودية)",
  "country_code": "رمز ISO ثنائي (مثال: SA)",
  "region": "المنطقة (الخليج العربي / بلاد الشام / شمال إفريقيا / ...)",
  "confidence": "عالية",
  "language": "العربية",
  "dialect": "اللهجة المكتشفة",
  "indicators": ["مؤشر 1", "مؤشر 2", "مؤشر 3"],
  "reasoning": "شرح موجز ودقيق لسبب تحديد هذه الدولة"
}}"""

    try:
        client = Groq(api_key=api_key)
        resp = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": "أنت محلّل جغرافي دقيق ولغوي خبير بتيليجرام. تُجيب بصيغة JSON صالحة فقط."},
                {"role": "user", "content": prompt}
            ],
            max_tokens=600,
            temperature=0.2,
        )
        raw = resp.choices[0].message.content.strip()
        m = re.search(r'\{.*\}', raw, re.DOTALL)
        if m:
            return json.loads(m.group())
        return {'country': None, 'error': 'فشل استخراج JSON', 'raw': raw}
    except Exception as e:
        logger.error(f"Groq analysis error: {e}")
        return {'country': None, 'error': str(e)}


# ══════════════════════════════════════════════════════════════════════════
#  الجزء الرابع: OSINT خارجي (TGStat + خرائط DC)
# ══════════════════════════════════════════════════════════════════════════

def _query_tgstat(username, tgstat_token=None):
    """استعلام TGStat (اختياري — يحتاج توكن)."""
    if not username or not tgstat_token:
        return None
    try:
        r = requests.get(
            "https://api.tgstat.ru/channels/get",
            params={'token': tgstat_token, 'username': username.lstrip('@')},
            timeout=10,
        )
        if r.status_code == 200:
            data = r.json().get('response', {})
            return {
                'country':  data.get('country'),
                'language': data.get('language'),
                'category': data.get('category'),
            }
    except Exception as e:
        logger.debug(f"TGStat query failed: {e}")
    return None


def _dc_to_location(dc_id):
    """تحويل رقم DC إلى موقع جغرافي."""
    if not dc_id:
        return {'city': 'غير معروف', 'country': 'غير معروف', 'code': None}
    return DC_LOCATIONS.get(int(dc_id), {'city': 'غير معروف', 'country': 'غير معروف', 'code': None})


# ══════════════════════════════════════════════════════════════════════════
#  فحص القناة/المجموعة عبر واجهة الويب لتيليجرام (Fallback قوي وسريع)
# ══════════════════════════════════════════════════════════════════════════

def _fetch_telegram_web_preview(group_link):
    """
    يجلب معلومات المجموعة أو القناة مع أحدث الرسائل العامة من t.me أو t.me/s/
    يعمل بدون جلسة تليثون أو كـ Fallback سريع.
    """
    clean = group_link.strip()
    if 't.me/' in clean:
        clean = clean.split('t.me/')[-1].split('?')[0].strip('/')
    clean = clean.lstrip('@')

    meta = {
        'title': clean,
        'username': clean if not clean.startswith('+') else None,
        'about': '',
        'participants_count': 0,
        'dc_id': None,
        'messages': []
    }

    try:
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'}
        # محاولة قراءة صفحة المعاينة العامة للقناة t.me/s/username
        url = f"https://t.me/s/{clean}" if not clean.startswith('+') else f"https://t.me/{clean}"
        r = requests.get(url, headers=headers, timeout=8)
        if r.status_code == 200:
            html = r.text

            # العنوان
            m_title = re.search(r'<meta property="og:title" content="([^"]+)">', html) or \
                      re.search(r'<div class="tgme_page_title"[^>]*><span[^>]*>(.*?)</span>', html, re.DOTALL)
            if m_title:
                meta['title'] = re.sub(r'<[^>]+>', '', m_title.group(1)).strip()

            # الوصف
            m_desc = re.search(r'<meta property="og:description" content="([^"]+)">', html) or \
                     re.search(r'<div class="tgme_page_description"[^>]*>(.*?)</div>', html, re.DOTALL)
            if m_desc:
                meta['about'] = re.sub(r'<[^>]+>', '', m_desc.group(1)).strip()

            # عدد الأعضاء
            m_extra = re.search(r'([0-9\s,\.KkMm]+)\s*(subscribers|members|مشترك|عضو)', html, re.IGNORECASE)
            if m_extra:
                raw_num = m_extra.group(1).replace(' ', '').replace(',', '')
                if 'k' in raw_num.lower():
                    meta['participants_count'] = int(float(raw_num.lower().replace('k', '')) * 1000)
                elif 'm' in raw_num.lower():
                    meta['participants_count'] = int(float(raw_num.lower().replace('m', '')) * 1000000)
                else:
                    try:
                        meta['participants_count'] = int(raw_num)
                    except Exception:
                        pass

            # استخراج الرسائل العامة من صفحة المعاينة
            msgs = re.findall(r'<div class="tgme_widget_message_text[^"]*"[^>]*>(.*?)</div>', html, re.DOTALL)
            for msg_html in msgs:
                clean_msg = re.sub(r'<[^>]+>', ' ', msg_html).strip()
                if len(clean_msg) > 3:
                    meta['messages'].append(clean_msg)

    except Exception as e:
        logger.debug(f"Web preview fetch failed for {clean}: {e}")

    return meta


# ══════════════════════════════════════════════════════════════════════════
#  الدالة الرئيسية: التنسيق بين كل المراحل
# ══════════════════════════════════════════════════════════════════════════

async def analyze_group_country_async(client, group_link, use_ai=True, use_tgstat=False, tgstat_token=None):
    """
    تحليل شامل لدولة المجموعة من رابطها.
    يُعيد تقريراً كاملاً يجمع كل الطرق: (بيانات وصفية + لهجة + عملة + مدن + DC + Groq AI).
    """
    report = {
        'success':       False,
        'group_link':    group_link,
        'analyzed_at':   datetime.now().isoformat(),
        'metadata':      {},
        'content_local': {},
        'content_ai':    {},
        'tgstat':        None,
        'dc_location':   {},
        'final_country': None,
        'confidence':    'منخفضة',
    }

    entity = None
    texts = []

    # ── 1. جلب الكيان عبر عميل Telethon إن توفر ──
    clean = group_link.strip()
    if 't.me/' in clean:
        clean = clean.split('t.me/')[-1].split('?')[0].rstrip('/')
    clean = clean.lstrip('@')

    if client:
        try:
            entity = await client.get_entity(clean)
        except Exception as e:
            logger.debug(f"client.get_entity failed for {clean}: {e}")
            try:
                from telethon.tl.functions.contacts import ResolveUsernameRequest
                r = await client(ResolveUsernameRequest(username=clean))
                entity = (r.chats[0] if r.chats else (r.users[0] if r.users else None))
            except Exception:
                pass

    # ── 2. جمع البيانات الوصفية والرسائل ──
    if entity and client:
        try:
            report['metadata'] = await _collect_metadata(client, entity)
            report['dc_location'] = _dc_to_location(report['metadata'].get('dc_id'))
        except Exception as e:
            logger.warning(f"Metadata collection failed: {e}")

        try:
            async for msg in client.iter_messages(entity, limit=120):
                if msg.text and len(msg.text.strip()) > 3:
                    texts.append(msg.text.strip())
                if len(texts) >= 120:
                    break
        except Exception as e:
            logger.warning(f"Message fetch failed via Telethon: {e}")

    # إذا لم نحصل على رسائل كافية أو تعذر الوصول بالعميل، نستخدم الـ Web Preview
    if len(texts) < 5:
        web_info = _fetch_telegram_web_preview(group_link)
        if not report['metadata'] or not report['metadata'].get('title'):
            report['metadata'] = {
                'id': None,
                'title': web_info['title'],
                'username': web_info['username'],
                'participants_count': web_info['participants_count'],
                'about': web_info['about'],
                'dc_id': None,
                'is_megagroup': True
            }
        elif web_info['about'] and not report['metadata'].get('about'):
            report['metadata']['about'] = web_info['about']

        # إضافة الرسائل المستخرجة من الويب
        for m in web_info.get('messages', []):
            if m not in texts:
                texts.append(m)

        # إضافة نص الوصف والعنوان للتحليل
        if report['metadata'].get('title'):
            texts.append(f"عنوان القناة: {report['metadata']['title']}")
        if report['metadata'].get('about'):
            texts.append(f"وصف القناة: {report['metadata']['about']}")

    if not texts and not report['metadata'].get('title'):
        report['error'] = 'تعذر الوصول إلى معلومات المجموعة أو القناة'
        return report

    # ── 3. التحليل المحلي (لهجة، عملة، مدن، أرقام) ──
    try:
        report['content_local'] = _analyze_content_locally(texts)
    except Exception as e:
        logger.warning(f"Local analysis failed: {e}")

    # ── 4. التحليل بالذكاء الاصطناعي (Groq Llama-3.3-70b) ──
    if use_ai:
        try:
            ai_texts = []
            if report['metadata'].get('title'):
                ai_texts.append(f"عنوان القناة/المجموعة: {report['metadata']['title']}")
            if report['metadata'].get('about'):
                ai_texts.append(f"وصف القناة/المجموعة: {report['metadata']['about']}")
            ai_texts.extend(texts)
            report['content_ai'] = _analyze_content_with_groq(ai_texts)
        except Exception as e:
            logger.warning(f"AI analysis failed: {e}")
            report['content_ai'] = {'error': str(e)}

    # ── 5. TGStat (اختياري) ──
    if use_tgstat and report['metadata'].get('username'):
        try:
            report['tgstat'] = _query_tgstat(report['metadata']['username'], tgstat_token)
        except Exception as e:
            logger.debug(f"TGStat failed: {e}")

    # ── 6. دمج النتائج لاختيار الدولة النهائية وترجيح الثقة ──
    votes = Counter()
    evidence = []

    ai_country = report.get('content_ai', {}).get('country')
    if ai_country:
        votes[ai_country] += 4
        evidence.append(f"Groq AI: {ai_country}")
        if report.get('content_ai', {}).get('dialect'):
            evidence.append(f"لهجة: {report['content_ai']['dialect']}")

    local_country = report.get('content_local', {}).get('country')
    if local_country:
        votes[local_country] += 3
        evidence.append(f"تحليل محلي: {local_country}")
        for ev in report.get('content_local', {}).get('evidences', [])[:3]:
            evidence.append(ev)

    tg_country = (report.get('tgstat') or {}).get('country')
    if tg_country:
        votes[tg_country] += 2
        evidence.append(f"TGStat: {tg_country}")

    if votes:
        final_country, final_score = votes.most_common(1)[0]
        report['final_country'] = final_country

        ai_conf = report.get('content_ai', {}).get('confidence', 'منخفضة')
        loc_conf = report.get('content_local', {}).get('confidence', 'منخفضة')
        report['confidence'] = ai_conf if ai_conf == 'عالية' else (loc_conf if loc_conf in ['عالية', 'متوسطة'] else 'متوسطة')
    else:
        report['final_country'] = 'غير محدد بدقة'
        report['confidence'] = 'منخفضة'

    report['evidence_summary'] = evidence
    report['success'] = True
    return report


def analyze_group_country_sync(client_manager, group_link, use_ai=True):
    """
    نسخة متزامنة للاستدعاء من مسارات Flask.
    تتعامل مع عميل تيليجرام أو وضع الـ Fallback بسلاسة تامة.
    """
    try:
        # إذا كان عميل Telethon متاحاً ويعمل، ننفذ الكوروتين داخل حلقته
        if client_manager and getattr(client_manager, 'client', None):
            try:
                return client_manager.run_coroutine(
                    analyze_group_country_async(client_manager.client, group_link, use_ai=use_ai)
                )
            except Exception as cm_err:
                logger.warning(f"client_manager.run_coroutine failed: {cm_err}, using async fallback")

        # Fallback متزامن مستقل (يعمل حتى بدون Telethon مستمر)
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(
                analyze_group_country_async(None, group_link, use_ai=use_ai)
            )
        finally:
            loop.close()

    except Exception as e:
        logger.error(f"analyze_group_country_sync error: {e}")
        return {'success': False, 'error': str(e)}


# ══════════════════════════════════════════════════════════════════════════
#  Routes جاهزة للدمج في Flask
# ══════════════════════════════════════════════════════════════════════════

def register_group_country_routes(app, socketio, USERS, USERS_LOCK, PREDEFINED_USERS, telegram_manager):
    """
    تسجيل مسارات Flask لتحليل دولة المجموعة تلقائياً.
    """
    from flask import request, jsonify, session

    @app.route("/api/analyze_group_country", methods=["POST"])
    def api_analyze_group_country():
        """تحليل دولة المجموعة أو القناة من رابطها أو اسمها."""
        try:
            user_id = session.get('user_id', 'user_1')
            data = request.get_json() or {}
            group_link = (data.get('group_link') or data.get('link') or '').strip()
            use_ai = bool(data.get('use_ai', True))

            if not group_link:
                return jsonify({'success': False, 'error': 'يجب إرسال رابط المجموعة أو معرف القناة'})

            cm = None
            with USERS_LOCK:
                if user_id in USERS:
                    cm = USERS[user_id].get('client_manager')
                if not cm:
                    for uid, udata in USERS.items():
                        mgr = udata.get('client_manager')
                        if mgr:
                            cm = mgr
                            break

            # بث تحديث فوري للواجهة
            try:
                socketio.emit('log_update', {
                    'message': f"🔍 جارٍ تحليل دولة المجموعة: {group_link}"
                }, to=user_id)
            except Exception:
                pass

            result = analyze_group_country_sync(cm, group_link, use_ai=use_ai)

            # بث النتيجة
            if result.get('success'):
                try:
                    country = result.get('final_country', 'غير محدد')
                    conf = result.get('confidence', 'متوسطة')
                    socketio.emit('log_update', {
                        'message': f"✅ الدولة المكتشفة: {country} (درجة الثقة: {conf})"
                    }, to=user_id)
                    socketio.emit('group_country_result', result, to=user_id)
                except Exception:
                    pass

            return jsonify(result)
        except Exception as e:
            logger.error(f"api_analyze_group_country error: {e}")
            return jsonify({'success': False, 'error': str(e)}), 500

    @app.route("/api/analyze_groups_batch", methods=["POST"])
    def api_analyze_groups_batch():
        """تحليل مجموعة من الروابط دفعة واحدة."""
        try:
            user_id = session.get('user_id', 'user_1')
            data = request.get_json() or {}
            links = data.get('links', [])
            use_ai = bool(data.get('use_ai', True))

            if not isinstance(links, list) or not links:
                return jsonify({'success': False, 'error': 'أرسل قائمة بروابط المجموعات'})

            cm = None
            with USERS_LOCK:
                if user_id in USERS:
                    cm = USERS[user_id].get('client_manager')

            results = []
            total = len(links)

            def _worker():
                for i, link in enumerate(links, 1):
                    try:
                        socketio.emit('log_update', {
                            'message': f"🔍 [{i}/{total}] تحليل: {link}"
                        }, to=user_id)
                        r = analyze_group_country_sync(cm, link, use_ai=use_ai)
                        r['index'] = i
                        r['total'] = total
                        results.append(r)
                        socketio.emit('group_country_batch_progress', {
                            'index': i, 'total': total,
                            'link': link,
                            'country': r.get('final_country'),
                            'confidence': r.get('confidence'),
                        }, to=user_id)
                    except Exception as e:
                        results.append({'link': link, 'success': False, 'error': str(e)})

                socketio.emit('group_country_batch_done', {
                    'results': results, 'total': total
                }, to=user_id)

            threading.Thread(target=_worker, daemon=True).start()
            return jsonify({'success': True,
                            'message': f'بدأ تحليل {total} مجموعة في الخلفية',
                            'total': total})
        except Exception as e:
            return jsonify({'success': False, 'error': str(e)}), 500

    logger.info("✅ تم تسجيل مسارات تحليل دولة المجموعة بنجاح")
    return True
