<div dir="rtl" align="right">

# 💰 محفظتي (Mahfazati) — Expense Tracker Backend API

![Java](https://img.shields.io/badge/Java-23-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-Offline--First_Sync-007ACC?style=for-the-badge)

الخادم السحابي (RESTful Backend API) لتطبيق **"محفظتي"** لإدارة المصاريف الشخصية والمحافظ المالية المتعددة، مبني باستخدام **Java Spring Boot** وقاعدة بيانات **PostgreSQL**، ومصمم خصيصاً لدعم معمارية **Offline-First (العمل بدون إنترنت أولاً)** والمزامنة السحابية الذكية ثنائية الاتجاه مع تطبيق الهاتف (**Flutter**).

---

## 📌 نظرة عامة على المشروع (Project Overview)

يهدف هذا النظام إلى تزويد تطبيق **"محفظتي"** ببنية تحتية سحابية آمنة، سريعة، وقابلة للتوسع (Scalable). يتيح التطبيق للمستخدمين تسجيل مصروفاتهم وإيراداتهم محلياً على الهاتف (`sqflite`) بسرعة فائقة دون الحاجة لاتصال دائم بالإنترنت، وعند توفر الاتصال يقوم هذا الخادم بمزامنة البيانات تلقائياً بين الهاتف والسحابة باستخدام خوارزمية **الأحدث يفوز (Last-Write-Wins)** ونظام **الحذف الناعم (Soft Delete)**، مع توفير إحصائيات مالية دقيقة وتقارير رسومية.

---

## ✨ المميزات الهندسية والتقنية (Key Features)

### 1. 🔐 نظام مصادقة وحماية متقدم (Authentication & Security)
* **حماية Stateless JWT:** تأمين جميع المسارات باستخدام `Spring Security` وتذاكر مرور `JWT` مشفرة بخوارزمية `HS256`.
* **التجديد الانزلاقي التلقائي للجلسة (Sliding Session):** تجديد صلاحية التوكن تلقائياً مع كل عملية مزامنة ناجحة لضمان عدم إزعاج المستخدم بطلب تسجيل الدخول المتكرر في بيئة `Offline-First`.
* **تسجيل الدخول عبر Google (OAuth2 ID Token Verification):** دعم إنشاء الحسابات وتسجيل الدخول الفوري عبر حسابات جوجل الرسمية.
* **نظام استعادة كلمة المرور (OTP Password Reset):** توليد رمز تحقق رقمي مؤقت (صالح لمدة 15 دقيقة) لإعادة تعيين كلمة المرور بأمان.
* **تشفير كلمات المرور:** استخدام خوارزمية `BCryptPasswordEncoder` لتشفير كلمات المرور قبل تخزينها.
* **الحماية من ثغرات (IDOR):** التحقق الصارم في كل طلب من أن المحفظة أو المعاملة المطلوبة مملوكة فعلياً للمستخدم صاحب التوكن.

### 2. 🔄 محرك مزامنة سحابية ثنائي الاتجاه (Bi-Directional Sync Engine)
* **الرفع والتحميل الذكي (Upstream & Downstream Sync):** نقطة اتصال موحدة (`/api/sync`) تستقبل كافة التعديلات المحلية التي أجراها المستخدم بدون إنترنت وترسل له التعديلات السحابية التي فاتته منذ آخر طابع زمني (`lastSyncTime`).
* **حل التعارضات تلقائياً (Last-Write-Wins):** مقارنة الطوابع الزمنية (`updatedAt`) بين نسخة الهاتف ونسخة السيرفر لاعتماد النسخة الأحدث دائماً دون فقدان أو تشويه البيانات.
* **دعم تعدد الأجهزة والاستعادة السحابية:** إمكانية تسجيل الدخول من هاتف جديد واسترجاع كافة المحافظ والمعاملات تلقائياً في ثوانٍ.

### 3. 💼 إدارة المحافظ المتعددة والمعاملات المالية (Multi-Wallet & Transactions)
* **تعدد المحافظ:** إمكانية إنشاء عدد غير محدود من المحافظ المستقلة (مثل: *المحفظة الشخصية، مصاريف العمل، مصاريف المشروع*) والتنقل بينها.
* **إدارة شاملة للمعاملات:** إضافة وتعديل وحذف الإيرادات والمصروفات مع تصنيفها حسب الفئات وإرفاق الملاحظات والتواريخ.
* **الحذف الجماعي السريع (Batch Delete):** تحديد وحذف مجموعة من المعاملات دفعة واحدة باستعلام قاعدة بيانات واحد.
* **البحث النصي والفلترة الزمنية:** البحث بالكلمات المفتاحية في الفئات والملاحظات مع فلترة المعاملات بين تاريخين محددين (`startDate` و `endDate`).

### 4. 📊 الإحصائيات والتقارير المالية (Analytics & Reports)
* حساب إجمالي الإيرادات، وإجمالي المصروفات، والرصيد الصافي لكل محفظة لحظياً من قاعدة البيانات.
* تجميع المصروفات حسب الفئات (`GROUP BY Category`) مرتبة تنازلياً لتغذية المخططات الدائرية (Pie Charts) وتحديد الفئات الأكثر استهلاكاً للميزانية.

### 5. ⚡ تحسينات الأداء وقاعدة البيانات (PostgreSQL Optimization & Maintenance)
* **الفهرسة المركبة (Composite B-Tree Indexes):** فهارس مخصصة على المفاتيح الأجنبية وحقول التواريخ (`wallet_id`, `user_id`, `transaction_date`, `updated_at`) لضمان استعلامات فائقة السرعة (`Index Scan`).
* **تجميع الدفعات (Hibernate JDBC Batching):** تفعيل `batch_size=30` لتسريع إدخال وتحديث المعاملات المتعددة أثناء المزامنة.
* **الحذف الناعم وروبوت التنظيف المجدول (`Soft Delete & Cron Job`):** الاحتفاظ بالعناصر المحذوفة بعلامة `is_deleted = true` لضمان مزامنة الحذف بين الأجهزة، مع وجود روبوت صيانة أسبوعي (`@Scheduled` كل يوم جمعة الساعة 03:00 فجراً) يقوم بالحذف النهائي (`Hard Delete`) للسجلات المحذوفة التي مضى عليها أكثر من 30 يوماً لمنع تضخم قاعدة البيانات.

---

## 🏗️ الهيكلة المعمارية للمشروع (Project Structure)

يتبع المشروع المعمارية الطبقية النظيفة (**Layered Architecture**):

```text
com.omar.expense_tracker_backend
├── 📂 config          # إعدادات الحماية (Spring Security)، التشفير، وتفعيل الجدولة والتدقيق (JPA Auditing)
├── 📂 controllers     # بوابات الـ REST API (Auth, Wallet, Transaction, Sync, User)
├── 📂 dtos            # كائنات نقل البيانات والتحقق من المدخلات (@Valid, Jackson Annotations)
├── 📂 exceptions      # المعالج الشامل للأخطاء (@RestControllerAdvice) لتحويل الاستثناءات إلى ردود واضحة
├── 📂 models          # كيانات قاعدة البيانات (Entities: User, Wallet, Transaction) مع الفهارس والحذف الناعم
├── 📂 repositories    # واجهات التواصل مع PostgreSQL باستعلامات JPQL و Native SQL المحسنة
├── 📂 responses       # الغلاف الموحد لجميع ردود الخادم (ApiResponse<T>)
├── 📂 security        # إدارة وتوليد وفحص تذاكر JWT (JwtUtil, JwtAuthenticationFilter, CustomUserDetails)
├── 📂 services        # منطق الأعمال، خوارزمية المزامنة (SyncService)، وروبوت التنظيف (MaintenanceService)
└── 📄 ExpenseTrackerBackendApplication.java