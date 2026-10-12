# Kết quả kiểm tra

## Màn chi tiết theo ảnh mới, review UI và Home phát trực tiếp — 2026-10-11

- Màn chi tiết có ảnh hero bo góc, nút phát, tên phim/chất lượng, thời lượng và số tập theo server, năm phát hành, chip thể loại/diễn viên, giới thiệu mở rộng và card phim liên quan ngang. Số tập/grid tập dùng dữ liệu API; không tạo điểm IMDb hoặc ngày phát hành không có trong nguồn.
- Phần đánh giá/review chỉ có giao diện: chọn sao và nhập bản nháp tại chỗ, trạng thái chưa có đánh giá, nút gửi vô hiệu hóa; không thêm API hay lưu review.
- Luồng Home đã đổi lại theo yêu cầu mới: bấm thẻ phim/banner mở màn chi tiết. Nút Xem ngay và mục Tiếp tục xem vẫn dùng thao tác phát riêng; xem phim từ màn chi tiết qua nút phát hoặc chọn tập.
- Hàng action player thay Âm thanh & phụ đề bằng Toàn màn hình ở cùng vị trí, bỏ nút fullscreen trùng.
- Build cuối `:app:assembleDebug --offline -PENABLE_GOOGLE_AUTH=false` PASS. APK ở `app/build/outputs/apk/debug/app-debug.apk` dùng đăng nhập email. Chưa đối chiếu UI trên thiết bị hoặc xác minh Home → HLS thực trong môi trường này; ADB bị chặn như ghi nhận bên dưới.

## Chi tiết phim, điều khiển player và overlay tập — 2026-10-11

- Màn chi tiết bỏ backdrop/poster/metadata đầu trang; appbar gọn giữ yêu thích, giới thiệu mở rộng/thu gọn và chip diễn viên chỉ từ dữ liệu API. Grid tập dùng các hàng lazy, ba cột trên màn thường, giảm cột trên màn hẹp/font lớn; tổng tập và trạng thái theo server/phim/tập đang phát.
- Player có controls fade, seek ±10 giây và double tap có feedback, khóa/mở khóa, auto-hide 3 giây có điều kiện, seekbar với buffered position thật và panel track/speed thật. Đóng/Back thu về mini player; Back khi overlay mở chỉ đóng overlay.
- Overlay tập nằm trên cùng video session, chia nhóm 30 tập, tự chọn nhóm và cuộn tới tập hiện tại. Card dùng ảnh phim fallback vì DTO/API hiện chỉ trả slug, name, url cho tập; không hiển thị thời lượng hoặc mô tả giả. Đổi tập đi qua PlaybackManager.start để lưu tiến trình cũ.
- PlaybackManager vẫn sở hữu một ExoPlayer, bổ sung bufferedPosition; bỏ thao tác seek khi chưa biết thời lượng. Chọn lại cùng tập tiếp tục player hiện tại mà không prepare lại media.
- `assembleDebug --offline -PENABLE_GOOGLE_AUTH=false` PASS sau thay đổi cuối. APK: `app/build/outputs/apk/debug/app-debug.apk` (đăng nhập email; không đổi cấu hình build).
- `verification/EpisodePresentationCheck.java` PASS với mã Kotlin đã biên dịch: tên tập Việt/Anh/Full, tránh lặp tiền tố; nhóm tập không chồng/lọt và kết thúc đúng tổng số ở các mốc 0, 1, 7, 30, 31, 32, 60, 61, 3001.
- Chưa xác minh UI, tua/track HLS thực, đổi tập nhanh, fullscreen/xoay/PiP và mini player trên thiết bị. ADB thất bại với `Cannot mkdir '\\.android': Permission denied`; kiểm tra build và logic thuần không thay thế kiểm tra phát video thực.

## Giới hạn ảnh đại diện 5 MB — 2026-10-11

- Tăng giới hạn lên 5 × 1024 × 1024 byte ở Android, bộ kiểm tra ảnh backend và body parser của endpoint avatar; cập nhật thông báo Việt/Anh và README.
- Kiểm tra biên backend PASS: ảnh JPEG 4 MB và đúng 5 MB được chấp nhận, vượt 5 MB một byte bị từ chối với HTTP 413.
- Android build: xem `verification/avatar-5mb-build.log`. Cần cài APK mới và khởi động lại backend để áp dụng giới hạn mới.

## Hồ sơ theo ảnh tham chiếu và chuyển ngôn ngữ — 2026-10-11

- Màn hồ sơ dùng phần thông tin căn giữa, avatar tròn có nút sửa, thẻ thống kê màu xanh ngọc và các mục bo góc. Thẻ thống kê dùng số phim yêu thích/đã xem của tài khoản. Các mục mở sửa hồ sơ, lịch sử, giao diện và ngôn ngữ.
- Giữ màn sửa hồ sơ riêng với Photo Picker, preview, kiểm tra tên, email chỉ đọc và lưu qua ViewModel hiện có. Chỉnh lại header, khoảng cách, thẻ form và nút lưu.
- Bottom navigation chỉ hiển thị icon; vẫn giữ mô tả cho trình đọc màn hình.
- Thêm chọn Việt/Anh ở đầu Cài đặt, lưu SharedPreferences và cập nhật resources trong Compose mà không reset navigation hay bản nháp. Có 178 khóa tài nguyên tương ứng ở values và values-en; các chuỗi có tham số dùng resource format.
- Android `assembleDebug --offline -PENABLE_GOOGLE_AUTH=false` PASS; APK ở `app/build/outputs/apk/debug/app-debug.apk`. Bản kiểm tra dùng đăng nhập email vì dependency Google chưa có trong cache. Log: `verification/profile-build-final.log`.
- `lintDebug` chưa chạy được: cache thiếu `com.android.tools.lint:lint-gradle:31.9.2`, mạng bị hạn chế. Log: `verification/profile-build.log`.
- Chưa xác minh trực quan trên thiết bị: ADB không truy cập được thư mục user trong môi trường này. Nội dung phim và thông báo lỗi trả về trực tiếp từ backend giữ ngôn ngữ của nguồn.

## Cập nhật hồ sơ và danh sách tập — 2026-10-11

- Điều chỉnh theo yêu cầu mới: tab Tôi trở lại thẻ thông tin tài khoản và các mục lịch sử, chỉnh sửa hồ sơ, cài đặt, đăng xuất. Form chỉnh sửa và nút camera chỉ xuất hiện sau khi bấm “Chỉnh sửa hồ sơ”, có nút quay lại. Thẻ tài khoản dùng hồ sơ đã lưu, không hiển thị bản nháp chưa lưu.

- Màn Tôi: header, avatar giao mép card, Photo Picker, preview, tên chỉnh sửa, email chỉ đọc, form giữ trong ViewModel, loading/retry, lưu và snackbar. Scaffold đang xử lý padding, bàn phím và mini player.
- Player: nút tập trước/tiếp trong video (kể cả fullscreen) và dưới video, danh sách tập dọc trực tiếp dưới video, chọn server và đánh dấu tập đang xem; tập thiếu URL bị vô hiệu hóa. Chuyển tập dùng PlaybackManager hiện có và lưu tiến trình cũ.
- Backend smoke PASS trên MongoDB riêng: sửa tên, cấm đổi email, avatar MIME/dung lượng/auth, URL remote, cách ly tài khoản, giữ tên/ảnh cũ khi storage lỗi và dọn ảnh cũ. Cloudinary giả lập, chưa upload thực.
- Android assembleDebug offline PASS với `-PENABLE_GOOGLE_AUTH=false`; các dependency Google chưa có trong cache nên bản kiểm tra dùng đăng nhập email. Không thay phiên bản toolchain.
- Chưa kiểm tra giao diện mới trên điện thoại, Photo Picker hoặc next/back với HLS thật; không có thiết bị ADB kết nối. Các kiểm tra emulator bên dưới thuộc bản trước.
- Avatar thật cần ba biến Cloudinary trong `server/.env.example` và khởi động lại backend; xem README.

- Backend: `npm install` hoàn tất, có một `package-lock.json`. Smoke check trên MongoDB 9.0 thật đã PASS: health, validation, auth, profile, refresh rotation/revocation, cách ly tài khoản, yêu thích upsert, tiến trình theo tập, tiếp tục xem, chống retry cũ ghi đè, hoàn tất >95%, xóa và chuẩn hóa metadata.
- Smoke check chỉ giả lập upstream; không đưa fixture vào catalog ứng dụng.
- Đã đọc tài liệu tại https://kkphim.com/api-document. Các lần gọi response trực tiếp KKPhim bằng web tool và Node/PowerShell thất bại vì môi trường không kết nối được nguồn. Response thực và HLS chưa xác minh.
- Android: `:app:assembleDebug` đã BUILD SUCCESSFUL (AGP 8.9.2 / Kotlin 2.1.20 / Gradle 8.11.1 / JDK 21 / SDK 35). APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Emulator API 37: đã cài/chạy APK, đăng ký → quay lại hồ sơ, đăng xuất → đăng nhập, giữ phiên đăng nhập và lựa chọn giao diện sau cập nhật/mở lại app. Đã quan sát trạng thái lỗi nguồn có retry; log AndroidRuntime không có crash trong các bước này.
- Các ảnh chụp và UI dump kiểm tra nằm trong `verification/`. Tài khoản `@example.invalid` trong ảnh là tài khoản kiểm tra, không phải dữ liệu mẫu được nhúng vào app.
- Màn hình 320dp: hồ sơ/email dài xuống dòng, cuộn tới cài đặt/đăng xuất, trạng thái yêu thích/lịch sử rỗng, giữ từ khóa khi đổi tab. Chế độ sáng/tối đã quan sát; phiên đăng nhập và giao diện vẫn còn sau force-stop/mở lại. Đã sửa insets bàn phím và vùng thông báo tìm kiếm có thể cuộn.
- Chưa xác minh bằng media thật: duyệt catalog thành công, ảnh phim, HLS/phụ đề, fullscreen/PiP, mini player liên tục, và tiếp tục sau mở lại với video đang phát. Môi trường chặn kết nối KKPhim; không thay bằng video mẫu. Mã playback dùng một `ExoPlayer.Builder` và session chung, nhưng kiểm tra cấu trúc không thay thế kiểm tra phát thực tế.
- Release/deploy Render chưa chạy; cần URL HTTPS, secret môi trường và cấu hình ký bản phát hành theo README.
