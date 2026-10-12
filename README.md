# CinayWave

Ứng dụng Kotlin/Compose và API Express/MongoDB. KKPhim chỉ cung cấp metadata qua adapter; ảnh và HLS tải trực tiếp từ nguồn. Không có phim/video mẫu trong ứng dụng.

## Chạy backend

Yêu cầu Node.js 22+, MongoDB và kết nối tới `https://phimapi.com`.

MongoDB local: chạy dịch vụ MongoDB đã cài, hoặc tạo thư mục dữ liệu và chạy `mongod --dbpath <thư-mục-dữ-liệu>` trong terminal riêng; dùng URI `mongodb://127.0.0.1:27017/cinaywave`. Với Atlas, lấy connection string từ mục Connect → Drivers, thay DB user/password và tên database, rồi điền vào `.env`.

```powershell
cd server
npm ci
Copy-Item .env.example .env
# Điền MONGODB_URI và JWT_SECRET trong .env.
# Tạo secret: node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
npm start
```

`GET /api/health` trả 200 khi MongoDB sẵn sàng, 503 khi đang khởi động. Catalog vẫn hoạt động nếu DB chưa kết nối; các thao tác tài khoản trả lỗi có thể thử lại. Không đưa `.env` lên Git.

## Chạy Android

Mở thư mục này trong Android Studio; dùng JDK 21, SDK 35. Dự án mới dùng AGP 8.9.2, Kotlin 2.1.20, Gradle 8.11.1; namespace/applicationId `com.cinaywave.app`.

```powershell
.\gradlew.bat :app:assembleDebug
```

Emulator dùng mặc định `http://10.0.2.2:3000/api/`. Backend lắng nghe `0.0.0.0`.

Máy thật: dùng IP LAN của máy chạy server, ví dụ `http://192.168.1.10:3000/api/`, cùng mạng Wi-Fi; cho phép cổng 3000 qua firewall. Thêm đúng IP đó vào `app/src/debug/res/xml/debug_network.xml`, rồi build với:

```powershell
.\gradlew.bat :app:assembleDebug -PDEBUG_API_URL=http://192.168.1.10:3000/api/
```

Không dùng localhost trên điện thoại để trỏ máy tính. Debug chỉ cho HTTP tới các host được khai báo; release chặn HTTP. Sau khi deploy backend, đặt URL HTTPS (kèm dấu `/` cuối):

```powershell
.\gradlew.bat :app:assembleRelease -PRELEASE_API_URL=https://YOUR-SERVICE.onrender.com/api/
```

Release cần cấu hình ký APK khi phát hành. URL release mặc định là host `.invalid` để tránh kết nối nhầm dịch vụ.

## Deploy Render

Tạo Web Service từ repository, Root Directory `server`, Build Command `npm ci`, Start Command `npm start`, Health Check `/api/health`. Khai báo `MONGODB_URI` của MongoDB Atlas, secret riêng `JWT_SECRET`, `NODE_ENV=production`, và `TRUST_PROXY_HOPS=1` nếu deployment chỉ có một reverse proxy Render. `PORT` do Render cấp. Cho phép kết nối Atlas từ môi trường Render; tạo DB user có quyền tối thiểu trên database ứng dụng. Không commit secret. Không bật trust proxy cho deployment chưa biết số proxy.

## Hồ sơ và ảnh đại diện

Màn “Tôi” tải hồ sơ từ `GET /api/me`, sửa tên bằng `PATCH /api/me` và lưu tên cùng ảnh qua `PUT /api/me/avatar`. Email chỉ đọc. Android dùng Photo Picker, xem trước ảnh và giữ nội dung chỉnh sửa khi lưu lỗi. Ảnh JPEG, PNG hoặc WebP tối đa 5 MB được kiểm tra cả trên Android và backend; URL lưu trong MongoDB là HTTPS từ storage, không phải URI trên điện thoại.

Đặt `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` trong `server/.env` hoặc Environment của Render, sau đó khởi động lại backend. Lấy các giá trị trong Cloudinary Console; chỉ backend giữ API secret. Upload có chữ ký, chuyển ảnh thành avatar 512×512 và lưu trên Cloudinary để tồn tại sau redeploy. Khi thiếu cấu hình, app báo lỗi upload; vẫn lưu được tên nếu không chọn ảnh mới. Smoke check dùng storage giả lập, chưa xác minh upload trên tài khoản Cloudinary thật.

## Kiểm tra ngắn

### Đăng nhập Google

Google dùng Credential Manager và backend Node.js, không dùng Firebase. Cấu hình OAuth trong cùng Google Cloud project:

- Android OAuth client: package `com.cinaywave.app`, SHA-1 đúng certificate ký bản cài trên điện thoại. Lấy bằng `./gradlew.bat :app:signingReport`; debug, release và Play App Signing có certificate khác nhau.
- Web OAuth client: đặt cùng client ID vào `GOOGLE_WEB_CLIENT_ID` trong `gradle.properties` và `server/.env`. Không dùng Android client ID hoặc client secret ở đây.
- Đặt `DEBUG_API_URL=https://<backend>/api/` trong `gradle.properties`, hoặc truyền `-PDEBUG_API_URL=...`; Google ID token chỉ được gửi qua HTTPS. Bản release dùng `RELEASE_API_URL`.
- Trong `server`, chạy `npm install` để cài `google-auth-library` và cập nhật lockfile, sau đó `npm start`. Môi trường chỉnh sửa hiện tại chặn npm registry nên chưa tạo được lockfile cho dependency mới; cần thực hiện bước này trước khi dùng lại `npm ci` hoặc deploy.

OAuth consent screen cần cho phép tài khoản thử nghiệm nếu project đang ở chế độ Testing. Điện thoại cần Google Play Services. Không cần `google-services.json`. Email trùng tài khoản mật khẩu sẽ yêu cầu đăng nhập tài khoản cũ; backend không tự liên kết email. Đăng xuất xóa phiên ứng dụng và trạng thái Credential Manager, không thu hồi quyền Google.

```powershell
cd server
$env:SMOKE_MONGODB_URI='mongodb://127.0.0.1:27017/cinaywave_smoke'
npm run smoke
```

Smoke check dùng DB riêng có `smoke` trong tên; dọn tài khoản do bài kiểm tra tạo. Auth, các route tài khoản và MongoDB chạy thật; chỉ stub upstream KKPhim. Bài kiểm tra này không xác minh khả năng phát video thực.

Trên thiết bị, kiểm tra: Home → chi tiết → server/tập → phát/tua → Back → đổi tab/tìm kiếm → pause/resume mini player → mở lại → đóng; fullscreen/xoay/PiP; đăng ký → yêu thích → xem một tập → mở lại app → tiếp tục đúng tập/vị trí. Ngắt mạng khi xem, nối lại để kiểm tra hàng đợi đồng bộ. Một ExoPlayer được sở hữu bởi `PlaybackManager`; đổi màn chỉ đổi TextureView, không tạo player thứ hai. Khi app bị đưa vào nền mà không có PiP, video tạm dừng.

Xem [VERIFICATION.md](VERIFICATION.md) cho kết quả thực tế trong workspace này.
