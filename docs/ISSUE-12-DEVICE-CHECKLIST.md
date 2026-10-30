# Issue #12 — چک‌لیست تست دستگاه (مالک)

CI جایگزین این تست‌ها نیست. بعد از نصب APK از `main`:

## کد از قبل پوشش می‌دهد (نیاز به تأیید گوشی)
- Audio focus (`handleAudioFocus`)
- هدفون / noisy (`setHandleAudioBecomingNoisy`)
- Wake هنگام پخش (`WAKE_MODE_LOCAL`)
- نوتیف + قفل‌صفحه via MediaSession + `sessionActivity`
- خطای فایل/فرمت → `PlaybackErrorMapper` (پیام فارسی)
- URI خراب → `LocalUriAccess` بدون crash
- حذف از Recent در حین پخش → سرویس در صورت `playWhenReady` زنده می‌ماند

## پخش محلی
- [ ] Import MP3 → پخش
- [ ] Import M4A (در صورت داشتن) → پخش یا پیام خطای واضح
- [ ] Seek و pause/resume
- [ ] پایان فایل → دکمه پخش از اول کار کند
- [ ] صفحه خاموش: پخش ادامه دارد
- [ ] حذف اپ از Recent در حین پخش: صدا + نوتیف ادامه دارد

## سیستم Android
- [ ] نوتیفیکیشن media: play/pause
- [ ] قفل صفحه: کنترل پخش
- [ ] جدا کردن هدفون / BT → pause
- [ ] تماس یا فوکوس صوتی دیگر → pause/duck سیستم

## خطاها
- [ ] URI حذف‌شده / دسترسی لغو‌شده → بنر فارسی، بدون crash
- [ ] قطع اینترنت هنگام استریم → پیام شبکه

## آفلاین
- [ ] دانلود خصوصی → قطع نت → پخش آفلاین

نتیجه را با مدل گوشی + نسخهٔ Android در Issue #12 کامنت کنید.
