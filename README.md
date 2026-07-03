# CineMatch — Android Native (Java)

## Bước setup trước khi build

1. **TMDB API key**: đăng ký tại https://www.themoviedb.org/settings/api, dán vào
   `app/src/main/java/com/example/cinematch/utils/Constants.java` (`TMDB_API_KEY`).
2. **Firebase**: tạo project trên Firebase Console, add Android app với package
   `com.example.cinematch`, tải file `google-services.json` thật, **ghi đè**
   file placeholder tại `app/google-services.json`.
   - Bật **Authentication** > Email/Password.
   - Bật **Firestore Database** (start in test mode để demo nhanh, sau siết lại Rules).
   - Bật **Storage** (start in test mode) — cần cho tính năng đổi ảnh đại diện.
3. Mở project bằng Android Studio (Giraffe trở lên), để Gradle tự sync.
4. Icon app đang dùng adaptive-icon vector đơn giản — có thể thay bằng
   Image Asset Studio (`res > New > Image Asset`) nếu muốn icon đẹp hơn.

## Mapping 12 kiến thức môn học -> nơi áp dụng trong code

| # | Kiến thức | Vị trí |
|---|---|---|
| 1 | Activity & Lifecycle | Tất cả class trong `ui/` extends `AppCompatActivity`, `SplashActivity` dùng lifecycle để check session |
| 2 | Intent (explicit) | `MovieAdapter`/`WatchlistAdapter` -> `MovieDetailActivity` qua `EXTRA_MOVIE_ID`; `ProfileFragment` -> `ModeratorDashboardActivity` |
| 3 | Fragment + Bottom Navigation | `MainActivity` + `HomeFragment/SearchFragment/WatchlistFragment/ProfileFragment` |
| 4 | View & Layout | `ConstraintLayout` (item_movie), `RecyclerView` khắp nơi, custom item layouts |
| 5 | SharedPreferences | `utils/SharedPrefManager.java` — lưu uid/role/session, dark mode |
| 6 | SQLite | `database/DBHelper.java` + `MovieCacheDAO.java` — cache phim + lịch sử tìm kiếm |
| 7 | Internal Storage | Poster cache qua Glide disk cache (mặc định); `movie_cache` SQLite lưu metadata offline |
| 8 | Multi-threading (Handler) | `utils/ThreadUtils.java` — wrapper ExecutorService + Handler cho thao tác SQLite nền |
| 9 | Firebase Authentication | `firebase/AuthManager.java` |
| 10 | Firebase Firestore | `firebase/FirestoreManager.java` — ratings/reviews/watchlist/reports |
| 11 | Firebase Storage | `firebase/StorageManager.java` — upload ảnh đại diện (`avatars/{uid}.jpg`), dùng ở `EditProfileActivity` |
| 12 | Retrofit + OkHttp + Gson | `network/ApiClient.java`, `network/TmdbApiService.java` |

## Điểm nhấn thuyết trình

- **Recommendation Engine** (`recommendation/RecommendationEngine.java`): tính
  genre weight từ lịch sử rating Firestore, gọi `/discover/movie`, loại phim đã
  xem, sort theo `genre_match_score` kết hợp `vote_average`.
- **Phân quyền 3 role**: Guest (chưa login, chỉ xem), User (role mặc định khi
  đăng ký), Moderator (set thủ công field `role` trong Firestore Console thành
  `"moderator"` cho tài khoản demo).
- **Offline cache**: `MovieDetailActivity` tự cache phim vào SQLite mỗi lần
  xem thành công; khi request TMDB thất bại (mất mạng), tự fallback đọc từ
  cache.
- **Moderation flow**: user báo cáo review -> `reportCount` tăng qua
  `FieldValue.increment()` -> `ModeratorDashboardActivity` query review có
  `reportCount > 0` để duyệt/ẩn.

## Còn thiếu / có thể mở rộng thêm nếu còn thời gian

- Firebase Storage chưa dùng (đề bài ghi "nếu cần" — hiện tại poster lấy thẳng từ TMDB).
- Có thể thêm Room thay SQLite thuần nếu muốn code gọn hơn (nhưng đề yêu cầu SQLite nên giữ nguyên).
- Filter Search hiện lọc năm/điểm ở client-side sau khi gọi `/search/movie`
  (TMDB search endpoint không hỗ trợ filter genre/year server-side).

## Cập nhật sau phản hồi thực tế chạy thử (tham khảo Letterboxd)

**Bug đã sửa:**
- `Watchlist` báo `FAILED_PRECONDITION`: do `getWatchlist()`/`getReviewsForMovie()`/
  `getReportedReviews()` kết hợp `whereEqualTo()` + `orderBy()`/`whereGreaterThan()`
  trên Firestore — Firestore bắt buộc phải tạo composite index thủ công cho tổ hợp
  này. Đã bỏ `orderBy()` ở tầng Firestore và **sort lại ở client** (Java `List.sort`)
  để tránh phải cấu hình gì thêm trên Firebase Console.
- Trailer bị trống trắng: do chỉ gọi `/videos` với `language=vi-VN`, nhiều phim không
  có bản dịch tiếng Việt nên trả về rỗng. Đã thêm fallback: thử `vi-VN` trước, không
  có thì thử `en-US`, ưu tiên Trailer > Teaser > video YouTube bất kỳ; nếu vẫn không
  có video nào thì ẩn hẳn khung WebView thay vì để trống trắng.

**Tính năng thêm (tham khảo UI Letterboxd):**
- **Mục thể loại ở Home**: tự động dựng 4 hàng theo thể loại (dùng `/genre/movie/list`
  + `/discover/movie`) ngay dưới hàng "Phổ biến".
- **Phim tương tự** ở Movie Detail: dùng endpoint `/movie/{id}/similar`, giống mục
  "Related Films" của Letterboxd.
- **Trang Cá nhân** được thiết kế lại: avatar tròn (chữ cái đầu tên), 3 thẻ thống kê
  (Đã đánh giá / Watchlist / Điểm trung bình), dòng "Thể loại bạn đánh giá cao nhất",
  và dải phim đã đánh giá gần đây (tái dùng `MovieAdapter`). Để làm được việc này,
  model `Rating` được thêm 2 field `movieTitle`/`posterPath` (denormalize) để không
  phải gọi lại TMDB cho từng phim đã rate.

**Gợi ý mở rộng thêm nếu còn thời gian** (theo hướng Letterboxd nhưng nằm ngoài phạm
vi đồ án 1 tháng nên chưa làm): Diary (nhật ký xem phim theo ngày), Lists (danh sách
phim tự tạo), Activity Feed (theo dõi bạn bè), rating dạng nửa sao trên thang 5.

## Cập nhật lần 2 (fix bug thực tế trên máy thật + hoàn thiện các phần trước đó ghi "hướng mở rộng")

**Bug đã sửa:**
- **Không đánh giá được nếu không viết review**: trước đây `Rating` chỉ được lưu bên
  trong luồng thành công của `submitReview()`, mà `submitReview()` bắt buộc phải có
  nội dung review mới chạy tiếp → nếu user chỉ kéo thanh sao mà không gõ chữ thì
  rating không được lưu. Đã tách hoàn toàn 2 thao tác: nút **"Lưu đánh giá"** riêng
  cạnh `RatingBar` (độc lập, không cần viết review), và nút **"Gửi"** chỉ lo phần
  review text. Mở lại trang chi tiết phim đã rate trước đó sẽ tự hiện lại số sao
  (`getRating()` trong `FirestoreManager`).
- **Profile "Đã đánh giá"/"Watchlist" không đổi**: hệ quả trực tiếp của bug trên —
  không có `Rating` nào được lưu nên số liệu luôn là 0. Sau khi tách nút "Lưu đánh
  giá", số liệu sẽ cập nhật đúng mỗi khi quay lại tab Cá nhân (`onResume()`).
- **Search bị đè chữ điểm đánh giá trên màn hình lớn (6.7 inch)**: do `item_movie.xml`
  dùng width cố định 120dp (thiết kế cho cuộn ngang ở Home), khi nhét vào
  `GridLayoutManager(3)` ở màn Search thì width cố định không khớp với độ rộng cột
  thực tế trên từng máy, gây chồng lấn. Đã tách riêng `item_movie_grid.xml`
  (width `match_parent` + tỉ lệ khung hình `2:3` cố định) chỉ dùng cho grid, không
  đụng tới layout cuộn ngang cũ.
- **Trailer báo lỗi 135, không phát được trong app, luôn phải mở sang YouTube**: do
  nạp thẳng URL `youtube.com/embed/...` vào WebView (dễ bị chặn bởi
  referrer/X-Frame-Options khi WebView không có "trang chủ" hợp lệ) và không set
  `WebViewClient` (mọi link, kể cả nút "Watch on YouTube" khi lỗi, bị đẩy ra app
  YouTube ngoài thay vì xử lý trong WebView). Đã bọc video trong 1 trang HTML tối
  giản chứa `<iframe>` rồi nạp bằng `loadDataWithBaseURL(base="youtube.com", ...)`,
  đồng thời bật `domStorageEnabled`, `mediaPlaybackRequiresUserGesture(false)`, và
  set `WebViewClient`/`WebChromeClient` để video phát trực tiếp trong app.

**Hoàn thiện phần trước đây ghi "hướng mở rộng":**
- **Chỉnh sửa hồ sơ** (`EditProfileActivity`): đổi tên hiển thị, giới tính (Spinner),
  tuổi, số điện thoại. Email đăng nhập hiển thị read-only kèm ghi chú lý do (đổi
  email Firebase Auth cần re-authenticate, ngoài phạm vi UI đơn giản của đồ án).
- **Đổi ảnh đại diện từ bộ sưu tập ảnh**: dùng
  `ActivityResultContracts.GetContent()` mở Photo Picker hệ thống (không cần xin
  quyền runtime), upload lên **Firebase Storage** qua `StorageManager.java`
  (hoàn thiện luôn kiến thức #11 trong bảng mapping phía trên), lưu `avatarUrl`
  vào Firestore, hiển thị bằng `Glide` với `.circleCrop()`.
- Trang **Cá nhân** giờ hiển thị avatar ảnh thật (thay vì chữ cái đại diện) và dòng
  thông tin giới tính/tuổi/sđt nếu đã cập nhật.
