# Cấu hình xác thực CinayWave

## Backend

Trong `server`, chạy `npm ci` khi có mạng để cài Nodemailer 10.0.10, rồi cấu hình `server/.env` theo `.env.example`:

- Giữ `MONGODB_URI`, `JWT_SECRET` (ít nhất 32 ký tự), `PORT` hiện tại.
- Điền `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM` bằng thông tin dịch vụ email thật. Với Gmail, dùng app password khi tài khoản cho phép.
- Cổng 587 dùng STARTTLS (`SMTP_SECURE=false`); cổng 465 dùng TLS (`SMTP_SECURE=true`).
- Điền `GOOGLE_WEB_CLIENT_ID` bằng OAuth client loại **Web application**.
- Khởi động lại server bằng `npm start` hoặc phiên `npm run dev` đang dùng.

Đăng ký trả HTTP 202 và yêu cầu OTP; tài khoản chỉ được cấp phiên sau khi xác nhận. Tài khoản email cũ chưa có `emailVerified` cũng cần xác nhận bằng mật khẩu hiện tại. Các tài khoản, yêu thích và lịch sử được giữ lại. Thiếu SMTP hoặc thư viện email sẽ trả lỗi 503, không báo đã gửi mã.

## Android và Google Cloud

Tạo OAuth consent screen trong Google Cloud; thêm test users nếu ứng dụng ở trạng thái Testing. Trong cùng project:

1. Tạo client **Android** cho package `com.cinaywave.app` và SHA-1 của debug keystore.
2. Tạo client Android riêng cho SHA-1 release; nếu phát hành Google Play, dùng SHA-1 của App signing certificate, không chỉ upload key.
3. Lấy SHA bằng `gradlew.bat :app:signingReport` hoặc `keytool -list -v -keystore <keystore>` cho release. Không đặt mật khẩu keystore vào source.
4. Đặt `GOOGLE_WEB_CLIENT_ID=<Web-client-ID>.apps.googleusercontent.com` trong Gradle user properties hoặc truyền `-PGOOGLE_WEB_CLIENT_ID=...`. Giá trị này phải giống backend; không dùng Android-client-ID, không đưa client secret vào app.
5. Build mặc định bằng `gradlew.bat :app:assembleDebug`. Google sử dụng Credential Manager, không dùng SDK Google Sign-In cũ. Credentials 1.5.0 giữ tương thích toolchain/compileSdk 35 hiện tại.

APK kiểm tra được ký bằng `.android/debug.keystore`, SHA-1: `C2:62:13:46:67:37:FF:F6:5D:10:FA:5F:61:81:89:24:7C:F1:3B:4C`. Khi dùng keystore khác trong Android Studio, lấy lại fingerprint bằng signingReport.

Điện thoại thật dùng `DEBUG_API_URL=http://192.168.1.93:3000/api/`, cùng Wi-Fi với máy tính. Khi IP đổi, cập nhật URL và domain trong `debug_network.xml`. Release dùng `RELEASE_API_URL=https://<backend>/api/`.

Backend xác minh RS256 bằng khóa công khai Google, kiểm tra audience/issuer/expiry/email verification; lưu Google `sub`. Email trùng tài khoản mật khẩu bị từ chối, không tự gộp tài khoản.

## Kiểm tra trong môi trường hiện tại

- `npm run smoke` với `SMOKE_MONGODB_URI=mongodb://127.0.0.1:27017/cinaywave_smoke`: đã qua OTP, Google token, session, yêu thích và lịch sử; email/upstream/account provider được thay thế có kiểm soát trong script kiểm tra.
- Build kiểm tra UI/email: `gradlew.bat :app:assembleDebug -PENABLE_GOOGLE_AUTH=false`. APK này **không có thư viện Google**; nút Google báo đúng hạn chế, không giả lập đăng nhập.
- Build đầy đủ bị chặn vì chưa có cache ba thư viện Google và môi trường không tải được dependency mới. SMTP thật cũng cần `npm ci` trên máy có mạng.
- Chưa kiểm tra giao diện, OTP/email thật, Google account picker, fullscreen/mini player/PiP trên thiết bị: ADB không có thiết bị kết nối. Ảnh cũ trong `verification` không xác nhận giao diện mới.
- Không tìm thấy source Flutter/thiết kế CineWave cũ; màn chi tiết làm theo bố cục trong `prompt.txt`.

Tài liệu đối chiếu: [Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), [Google ID token](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token).
