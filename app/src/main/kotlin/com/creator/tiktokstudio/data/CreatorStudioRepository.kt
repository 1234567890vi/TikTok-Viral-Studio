package com.creator.tiktokstudio.data

object CreatorStudioRepository {

    val goldenPostingTimes: List<PostingTimeSlot> = listOf(
        PostingTimeSlot(
            id = "slot_thu_night",
            dayArabic = "الخميس (ذروة الأسبوع)",
            timeSlotArabic = "08:00 م - 11:30 م",
            scorePercentage = 99,
            recommendedNiche = "ترفيه، كوميديا، فلوقات، قصص",
            strategyTip = "أعلى معدل مشاهدات أسبوعي في الوطن العربي، استخدم سكريبت مشوق يبدأ بسؤال صادم."
        ),
        PostingTimeSlot(
            id = "slot_fri_afternoon",
            dayArabic = "الجمعة",
            timeSlotArabic = "02:00 م - 05:30 م",
            scorePercentage = 96,
            recommendedNiche = "يوميات، تسوق، تكنولوجيا، بودكاست",
            strategyTip = "وقت تجمع العائلة والاسترخاء بعد الصلاة، تكون شاشات الهواتف في أعلى نشاط."
        ),
        PostingTimeSlot(
            id = "slot_sat_evening",
            dayArabic = "السبت",
            timeSlotArabic = "07:30 م - 10:30 م",
            scorePercentage = 93,
            recommendedNiche = "تطوير ذات، تعليم، مشاريع، استثمار",
            strategyTip = "الجمهور يبحث عن محتوى تحفيزي لبداية الأسبوع الجديد، ركز على حلول سريعة."
        ),
        PostingTimeSlot(
            id = "slot_mon_lunch",
            dayArabic = "الاثنين",
            timeSlotArabic = "01:00 م - 03:00 م",
            scorePercentage = 88,
            recommendedNiche = "أخبار سريعة، ميمز، معلومات خاطفة",
            strategyTip = "فترة استراحة العمل والدراسة، الفيديوهات الأقل من 20 ثانية تحقق أعلى اكتمال (Watch Time)."
        ),
        PostingTimeSlot(
            id = "slot_wed_prime",
            dayArabic = "الأربعاء",
            timeSlotArabic = "06:30 م - 09:30 م",
            scorePercentage = 91,
            recommendedNiche = "تقنية، تطبيقات، شروحات، تجارب منتجات",
            strategyTip = "منتصف الأسبوع وقت ممتاز لتقديم قيمة تعليمية أو مراجعة منتجات مع كول تو أكشن قوي."
        )
    )

    val hashtagCategories: Map<String, List<HashtagItem>> = mapOf(
        "تقنية وذكاء اصطناعي" to listOf(
            HashtagItem("#ذكاء_اصطناعي", "3.4B+", "ترند صاعد 🔥", "tech"),
            HashtagItem("#تطبيقات_أندرويد", "1.8B+", "تفاعل عالي ⚡", "tech"),
            HashtagItem("#تقنية_اليوم", "2.1B+", "انتشار سريع 🚀", "tech"),
            HashtagItem("#شروحات_مفيدة", "4.6B+", "ثابت في FYP 📈", "tech"),
            HashtagItem("#تيك_توك_تقنية", "950M+", "مجتمع نشط 💡", "tech"),
            HashtagItem("#ai_tools", "5.2B+", "عالمي 🌐", "tech")
        ),
        "كوميديا وترفيه" to listOf(
            HashtagItem("#ضحك_فلة", "11.2B+", "فيرال متصدر 🔥", "comedy"),
            HashtagItem("#مقالب_مضحكة", "8.9B+", "إكسبلور مضمون 🎭", "comedy"),
            HashtagItem("#fyp_arab", "24.5B+", "خوارزمية نشطة ⚡", "comedy"),
            HashtagItem("#ترند_الموسم", "6.7B+", "ترند لحظي 🚀", "comedy"),
            HashtagItem("#شعب_الصيني_ماله_حل", "18.3B+", "ترند خليجي 🇸🇦", "comedy")
        ),
        "تعليم وتطوير ذات" to listOf(
            HashtagItem("#تعلم_على_التيك_توك", "9.4B+", "مدعوم رسمياً 🎓", "education"),
            HashtagItem("#تطوير_الشخصية", "3.1B+", "احتفاظ عالي 📚", "education"),
            HashtagItem("#نصائح_بزنس", "2.8B+", "جمهور نخبوي 💼", "education"),
            HashtagItem("#كتب_أنصح_بها", "1.5B+", "مشاركة سريعة 📖", "education")
        ),
        "تجارة وريادة أعمال" to listOf(
            HashtagItem("#التجارة_الالكترونية", "4.2B+", "عائد مرتفع 💰", "business"),
            HashtagItem("#الدروب_شيبينج", "2.1B+", "ترند مستمر 📦", "business"),
            HashtagItem("#صناع_المحتوى", "3.6B+", "أرباح ومشاهدات 🎯", "business"),
            HashtagItem("#الاستثمار_الذكي", "1.9B+", "جمهور مستهدف 💎", "business")
        ),
        "طبخ ووصفات" to listOf(
            HashtagItem("#وصفات_سهلة", "14.1B+", "حفظ فيديو متكرر 🍲", "food"),
            HashtagItem("#طبخات_تيك_توك", "9.8B+", "تفاعل بالتعليقات 👨‍🍳", "food"),
            HashtagItem("#حلا_سريع", "7.3B+", "إكسبلور فوري 🍰", "food")
        )
    )

    val videoIdeaCards: List<VideoIdeaCard> = listOf(
        VideoIdeaCard(
            id = "idea_1",
            categoryArabic = "تقنية وتطبيقات",
            hook = "تطبيق أندرويد سرّي بيغير طريقة استخدامك للهاتف للأبد، ومستحيل ترجع تحذفه!",
            cameraMovement = "لقطة ثابتة للشاشة مع حركة يد سريعة وزوم حركي (Crash Zoom) على زر الميزة.",
            executionSecret = "لا تذكر اسم التطبيق إلا في الثانية 15، واعرض النتيجة المذهلة في أول 3 ثوانٍ.",
            audioVibe = "موسيقى تيك توك غامضة وسريعة الإيقاع (Mystery Beat)"
        ),
        VideoIdeaCard(
            id = "idea_2",
            categoryArabic = "صناع المحتوى والربح",
            hook = "لو عندك 0 متابع على تيك توك، طبق هذه الحيلة اليوم وشوف كيف بتوصل 10K بأسبوع!",
            cameraMovement = "تصوير ذاتي من زاوية منخفضة قليلاً مع إضاءة نيون خلفية ونظرة مباشرة لعدسة الكاميرا.",
            executionSecret = "اكتب رقم الحيلة على الشاشة بخط بارز واطلب منهم التوقف وأخذ لقطة شاشة (Screenshot Trap).",
            audioVibe = "صوت ترند تحفيزي ذو بيز قوي (Phonk Motivation)"
        ),
        VideoIdeaCard(
            id = "idea_3",
            categoryArabic = "تطوير الذات والإنتاجية",
            hook = "الخطأ الكارثي اللي يدمر يومك أول ما تصحى من النوم بدون ما تحس!",
            cameraMovement = "حركة كاميرا تتبعية (Tracking Shot) وأنت تمشي نحو الكاميرا بثقة وتتكلم بدون توقف.",
            executionSecret = "قسّم الفيديو إلى 3 نقاط سريعة واجعل النقطة الثالثة مفاجئة وغير متوقعة إطلاقاً.",
            audioVibe = "مؤثر صوتي للساعة مع دقات قلب متسارعة"
        ),
        VideoIdeaCard(
            id = "idea_4",
            categoryArabic = "تجارة رقمية ومال",
            hook = "كيف تسوي 100 دولار يومياً من تيك توك بدون ما تظهر بوجهك؟ الدليل الكامل!",
            cameraMovement = "تسجيل شاشة الحاسوب أو الهاتف بجودة 4K مع زوم رقمي ناعم وإبراز الأرقام باللون الأخضر.",
            executionSecret = "أظهر لوحة التحكم والأرباح كإثبات اجتماعي (Proof) في أول ثانيتين.",
            audioVibe = "موسيقى ناعمة وإيجابية للمشاريع الناشئة (Corporate Upbeat)"
        )
    )

    fun styleBioText(input: String): List<Pair<String, String>> {
        val text = if (input.isBlank()) "Creator VIP" else input

        return listOf(
            "عريض فخم (Bold Sans)" to convertFont(text, 'A', 'Z', 0x1D5D4, 'a', 'z', 0x1D5EE),
            "خط سيريف كلاسيكي (Serif Bold)" to convertFont(text, 'A', 'Z', 0x1D400, 'a', 'z', 0x1D41A),
            "حروف دائرية (Circled Aesthetic)" to convertFont(text, 'A', 'Z', 0x24B6, 'a', 'z', 0x24D0),
            "خط قوطي أسطوري (Fraktur VIP)" to convertFont(text, 'A', 'Z', 0x1D56C, 'a', 'z', 0x1D586),
            "أحادي المسافة (Monospace Tech)" to convertFont(text, 'A', 'Z', 0x1D670, 'a', 'z', 0x1D68A),
            "أقواس نيون [ TikTok ✨ ]" to "【 $text 】✨",
            "نجمي إبداعي (Starry VIP)" to "★彡 $text 彡★",
            "زخرفة أجنحة الملاك (Wings)" to "꧁༺ $text ༻꧂"
        )
    }

    private fun convertFont(input: String, minU: Char, maxU: Char, baseU: Int, minL: Char, maxL: Char, baseL: Int): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in minU..maxU -> {
                    val codePoint = baseU + (ch - minU)
                    sb.append(String(Character.toChars(codePoint)))
                }
                in minL..maxL -> {
                    val codePoint = baseL + (ch - minL)
                    sb.append(String(Character.toChars(codePoint)))
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun calculateEarnings(views: Long, rpm: Double): Pair<Double, Double> {
        val thousands = views.toDouble() / 1000.0
        val baseEarnings = thousands * rpm
        val maxPotential = baseEarnings * 1.35 // With creator bonuses & live tips
        return Pair(baseEarnings, maxPotential)
    }

    fun generateVipScript(topic: String, category: String): ViralScript {
        val cleanTopic = if (topic.isBlank()) "زيادة المتابعين والأرباح" else topic
        return when (category) {
            "تقنية وشروحات" -> ViralScript(
                title = cleanTopic,
                category = category,
                hook0To3Sec = "لا تشتري أي جهاز أو تنزل تطبيق قبل ما تشوف هذه الميزة الخفية في $cleanTopic!",
                coreValue = "في إعدادات التطبيق الكثير يغفل عن خيار بسيط يضاعف السرعة بنسبة 200%، كل اللي عليك تسويه هو تفعيل هذا الخيار وتغيير هذا الإعداد كما هو موضح على الشاشة الآن.",
                callToAction = "جربها الآن واكتب لي في التعليقات إذا اشتغلت معك، ولا تنسَ متابعة الحساب للشروحات اليومية!",
                cinematographyTip = "ابدأ بلقطة مقربة (Extreme Close-Up) مع وميض خفيف (Flash Transition) ومؤثر نقر صوتي حاد."
            )
            "تجارة ومال" -> ViralScript(
                title = cleanTopic,
                category = category,
                hook0To3Sec = "لو كنت مكانك وعمري 20 سنة، هذا أول مشروع ببدأ فيه في $cleanTopic برأس مال صفر!",
                coreValue = "أغلب الناس تعتقد أنك تحتاج آلاف الدولارات، بينما الواقع أنك تحتاج فقط هاتف ذكي وإنترنت، والخطوة الأولى تبدأ من منصة تيك توك نفسها عبر استغلال خوارزمية الفيديوهات القصيرة.",
                callToAction = "احفظ الفيديو للرجوع إليه وقت التطبيق، وأرسله لشخص ودك تبدأ معاه المشروع!",
                cinematographyTip = "حركة كاميرا بطيئة وسلسة، ارتدِ ملابس ذات طابع رسمي غير متكلف واستخدم إضاءة هادئة ودافئة."
            )
            else -> ViralScript(
                title = cleanTopic,
                category = category,
                hook0To3Sec = "وقف السحب ثانيتين بس! هذا السر اللي ما راح يخبرك عنه أي صانع محتوى في $cleanTopic...",
                coreValue = "السبب الحقيقي وراء انفجار الفيديوهات وتصدرها صفحة الـ FYP ليس جودة الكاميرا، بل هو معدل الاحتفاظ في أول 5 ثوانٍ، وهذه الطريقة اللي تقدر تطبقها فوراً في مجالك.",
                callToAction = "شاركنا في التعليقات كم أعلى عدد مشاهدات وصل له حسابك، وتابعني عشان تتصدر الترند القادم!",
                cinematographyTip = "زوم سريع مع موسيقى ذات إيقاع متصاعد (Beat Drop) وتغيير زاوية الكاميرا كل 3 ثوانٍ لمنع الملل."
            )
        }
    }

    fun analyzeAccount(username: String, followers: Long, views: Long, likes: Long, comments: Long): AccountAnalysisReport {
        val cleanName = if (username.isBlank()) "مبدع_تيك_توك" else username.removePrefix("@")
        val totalInteractions = likes + (comments * 2) // Comments hold double weight in TikTok algorithm
        val engagementRate = if (views > 0) {
            ((totalInteractions.toDouble() / views.toDouble()) * 100.0).coerceIn(0.1, 35.0)
        } else {
            5.2
        }

        val grade = when {
            engagementRate >= 9.0 -> "A+ (فيرال استثنائي)"
            engagementRate >= 6.0 -> "A (تفاعل ممتاز جداً)"
            engagementRate >= 3.5 -> "B (جيد - جاهز للتطوير)"
            else -> "C (يحتاج تحسين الخاطف الأولي)"
        }

        val viralityStatus = when {
            engagementRate >= 6.0 -> "الحساب في حالة ترشيح عالية لخوارزمية FYP العالمية"
            else -> "الحساب يعاني من انخفاض معدل المشاهدة الكاملة (Watch-time Drop)"
        }

        val findings = listOf(
            "معدل التفاعل الفعلي لحساب @$cleanName يبلغ ${String.format("%.2f", engagementRate)}%.",
            "نسبة التعليقات إلى الإعجابات تشير إلى ${if (comments > likes * 0.05) "ارتباط قوي جداً مع الجمهور" else "حاجة لتشجيع النقاش في نهاية الفيديو"}.",
            "متوسط المشاهدات مقابل المتابعين يمثل ${String.format("%.1f", (views.toDouble() / (if (followers > 0) followers else 1).toDouble()) * 100)}% من القاعدة الجماهيرية."
        )

        val recommendations = listOf(
            "تقليل مدة الفيديو إلى 15-25 ثانية مع تسريع وتيرة الإلقاء بنسبة 10% لرفع نسبة المشاهدة الكاملة (Retention Rate).",
            "استخدام سؤال جدلي أو جملة صادمة في أول ثانيتين لمنع السحب السريع (Swipe Rate < 25%).",
            "النشر في أوقات الذروة المحددة داخل تبويب 'أوقات النشر الذهبية' بالاستوديو.",
            "إضافة نص بارز ملون في أعلى الفيديو لأن 45% من المستخدمين يشاهدون بدون صوت في الثواني الأولى.",
            "الرد على أول 10 تعليقات بفيديو تفاعلي خلال أول ساعتين من نشر المقطع."
        )

        return AccountAnalysisReport(
            username = cleanName,
            followers = followers,
            averageViews = views,
            engagementRatePercentage = engagementRate,
            algorithmGrade = grade,
            viralityStatusArabic = viralityStatus,
            keyFindings = findings,
            actionRecommendations = recommendations
        )
    }
}
