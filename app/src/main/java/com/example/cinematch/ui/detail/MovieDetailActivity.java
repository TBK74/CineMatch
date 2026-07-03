package com.example.cinematch.ui.detail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cinematch.R;
import com.example.cinematch.adapters.CastAdapter;
import com.example.cinematch.adapters.ReviewAdapter;
import com.example.cinematch.database.MovieCacheDAO;
import com.example.cinematch.firebase.FirestoreManager;
import com.example.cinematch.models.Cast;
import com.example.cinematch.models.Movie;
import com.example.cinematch.models.Rating;
import com.example.cinematch.models.Report;
import com.example.cinematch.models.Review;
import com.example.cinematch.models.Video;
import com.example.cinematch.models.WatchlistItem;
import com.example.cinematch.network.ApiClient;
import com.example.cinematch.network.response.CreditsResponse;
import com.example.cinematch.network.response.MovieResponse;
import com.example.cinematch.network.response.VideoListResponse;
import com.example.cinematch.utils.Constants;
import com.example.cinematch.utils.SharedPrefManager;
import com.example.cinematch.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIE_ID = "extra_movie_id";

    private ImageView imgBackdrop;
    private TextView tvTitle, tvMeta, tvOverview;
    private Button btnWatchlist, btnSubmitReview, btnSaveRating, btnTrailer;
    private RecyclerView rvCast, rvReviews, rvSimilar;
    private com.example.cinematch.adapters.MovieAdapter similarAdapter;
    private EditText edtReviewContent;
    private RatingBar ratingBar;
    private String trailerYoutubeKey; // null nếu chưa tìm được trailer nào

    private CastAdapter castAdapter;
    private ReviewAdapter reviewAdapter;

    private MovieCacheDAO cacheDAO;
    private FirestoreManager firestoreManager;
    private SharedPrefManager prefManager;

    private int movieId;
    private Movie currentMovie;
    private boolean isInWatchlist = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        movieId = getIntent().getIntExtra(EXTRA_MOVIE_ID, -1);
        if (movieId == -1) {
            finish();
            return;
        }

        cacheDAO = new MovieCacheDAO(this);
        firestoreManager = new FirestoreManager();
        prefManager = new SharedPrefManager(this);

        bindViews();
        setupRecyclerViews();

        loadMovieDetail();
        loadCredits();
        loadVideos();
        loadReviews();
        loadSimilarMovies();
        checkWatchlistStatus();
        loadExistingRating();

        btnWatchlist.setOnClickListener(v -> toggleWatchlist());
        btnSubmitReview.setOnClickListener(v -> submitReview());
        btnSaveRating.setOnClickListener(v -> saveRating());
        btnTrailer.setOnClickListener(v -> openTrailerOnYoutube());
    }

    private void bindViews() {
        imgBackdrop = findViewById(R.id.imgBackdrop);
        tvTitle = findViewById(R.id.tvTitle);
        tvMeta = findViewById(R.id.tvMeta);
        tvOverview = findViewById(R.id.tvOverview);
        btnWatchlist = findViewById(R.id.btnWatchlist);
        btnTrailer = findViewById(R.id.btnTrailer);
        rvCast = findViewById(R.id.rvCast);
        rvSimilar = findViewById(R.id.rvSimilar);
        rvReviews = findViewById(R.id.rvReviews);
        edtReviewContent = findViewById(R.id.edtReviewContent);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        btnSaveRating = findViewById(R.id.btnSaveRating);
        ratingBar = findViewById(R.id.ratingBar);
    }

    private void setupRecyclerViews() {
        rvCast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        castAdapter = new CastAdapter(this, new ArrayList<>());
        rvCast.setAdapter(castAdapter);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(this, new ArrayList<>(), this::showReportDialog);
        rvReviews.setAdapter(reviewAdapter);

        rvSimilar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        similarAdapter = new com.example.cinematch.adapters.MovieAdapter(this, new ArrayList<>());
        rvSimilar.setAdapter(similarAdapter);
    }

    // ================= Load chi tiết phim: network trước, SQLite cache khi mất mạng =================
    private void loadMovieDetail() {
        ApiClient.getTmdbApi().getMovieDetail(movieId, Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE)
                .enqueue(new Callback<Movie>() {
                    @Override
                    public void onResponse(Call<Movie> call, Response<Movie> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentMovie = response.body();
                            bindMovieData(currentMovie);
                            // Cache lại để xem offline sau này -> chạy nền, không block UI
                            ThreadUtils.runInBackground(() -> cacheDAO.cacheMovie(currentMovie));
                        } else {
                            loadFromCacheFallback();
                        }
                    }

                    @Override
                    public void onFailure(Call<Movie> call, Throwable t) {
                        // Mất mạng -> thử lấy từ cache SQLite
                        loadFromCacheFallback();
                    }
                });
    }

    private void loadFromCacheFallback() {
        ThreadUtils.runInBackground(() -> cacheDAO.getCachedMovie(movieId), movie -> {
            if (movie != null) {
                currentMovie = movie;
                bindMovieData(movie);
                Toast.makeText(this, "Đang xem bản offline (đã cache trước đó)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không có kết nối và chưa có cache cho phim này", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindMovieData(Movie movie) {
        tvTitle.setText(movie.getTitle());
        tvOverview.setText(movie.getOverview());
        tvMeta.setText(movie.getReleaseDate() + "  •  ⭐ " + movie.getVoteAverage());
        Glide.with(this).load(movie.getFullBackdropUrl())
                .placeholder(R.drawable.placeholder_poster).into(imgBackdrop);
    }

    private void loadCredits() {
        ApiClient.getTmdbApi().getMovieCredits(movieId, Constants.TMDB_API_KEY)
                .enqueue(new Callback<CreditsResponse>() {
                    @Override
                    public void onResponse(Call<CreditsResponse> call, Response<CreditsResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getCast() != null) {
                            List<Cast> topCast = response.body().getCast();
                            castAdapter.updateData(topCast.subList(0, Math.min(10, topCast.size())));
                        }
                    }

                    @Override
                    public void onFailure(Call<CreditsResponse> call, Throwable t) { }
                });
    }

    // Lấy key trailer từ TMDB /videos. Thay vì nhúng WebView (hay lỗi 135/152 do YouTube chặn
    // phát trong WebView thiếu Play Services đầy đủ), chỉ hiện 1 nút "TRAILER" -> bấm vào mở
    // thẳng app YouTube/trình duyệt để phát, giống cách Letterboxd làm.
    // Nhiều phim không có bản trailer tiếng Việt trên TMDB nên thử vi-VN trước,
    // nếu rỗng thì fallback sang en-US.
    private void loadVideos() {
        fetchVideos(Constants.TMDB_LANGUAGE, true);
    }

    private void fetchVideos(String language, boolean allowFallback) {
        ApiClient.getTmdbApi().getMovieVideos(movieId, Constants.TMDB_API_KEY, language)
                .enqueue(new Callback<VideoListResponse>() {
                    @Override
                    public void onResponse(Call<VideoListResponse> call, Response<VideoListResponse> response) {
                        List<Video> results = (response.isSuccessful() && response.body() != null)
                                ? response.body().getResults() : null;

                        Video chosen = pickBestTrailer(results);
                        if (chosen != null) {
                            trailerYoutubeKey = chosen.getKey();
                            btnTrailer.setVisibility(android.view.View.VISIBLE);
                        } else if (allowFallback) {
                            fetchVideos("en-US", false); // thử lại bằng tiếng Anh
                        } else {
                            btnTrailer.setVisibility(android.view.View.GONE); // không có trailer nào cả
                        }
                    }

                    @Override
                    public void onFailure(Call<VideoListResponse> call, Throwable t) {
                        if (allowFallback) {
                            fetchVideos("en-US", false);
                        } else {
                            btnTrailer.setVisibility(android.view.View.GONE);
                        }
                    }
                });
    }

    // Mở app YouTube nếu có cài, không thì tự fallback sang trình duyệt (hành vi mặc định của Intent)
    private void openTrailerOnYoutube() {
        if (trailerYoutubeKey == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=" + trailerYoutubeKey));
        startActivity(intent);
    }

    // Ưu tiên Trailer chính thức, không có thì lấy Teaser, cuối cùng lấy video YouTube bất kỳ
    private Video pickBestTrailer(List<Video> results) {
        if (results == null || results.isEmpty()) return null;

        for (Video v : results) if (v.isYoutubeTrailer()) return v;
        for (Video v : results) if (v.isYoutubeVideo() && "Teaser".equalsIgnoreCase(v.getType())) return v;
        for (Video v : results) if (v.isYoutubeVideo()) return v;
        return null;
    }

    // Phim tương tự -> gợi ý thêm phim cùng gu ngay tại trang chi tiết (giống Related Films của Letterboxd)
    private void loadSimilarMovies() {
        ApiClient.getTmdbApi().getSimilarMovies(movieId, Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE, 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            similarAdapter.updateData(response.body().getResults());
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) { }
                });
    }

    private void loadReviews() {
        firestoreManager.getReviewsForMovie(movieId, new FirestoreManager.ListCallback<Review>() {
            @Override
            public void onSuccess(List<Review> list) {
                reviewAdapter.updateData(list);
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void checkWatchlistStatus() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        firestoreManager.isInWatchlist(uid, movieId, result -> {
            isInWatchlist = result;
            updateWatchlistButtonText();
        });
    }

    private void updateWatchlistButtonText() {
        btnWatchlist.setText(isInWatchlist ? R.string.remove_from_watchlist : R.string.add_to_watchlist);
    }

    private void toggleWatchlist() {
        String uid = prefManager.getUid();
        if (uid == null || currentMovie == null) return;

        if (isInWatchlist) {
            firestoreManager.removeFromWatchlist(uid, movieId, new FirestoreManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    isInWatchlist = false;
                    updateWatchlistButtonText();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MovieDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            WatchlistItem item = new WatchlistItem(uid, movieId, currentMovie.getTitle(), currentMovie.getPosterPath());
            firestoreManager.addToWatchlist(item, new FirestoreManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    isInWatchlist = true;
                    updateWatchlistButtonText();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MovieDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Gửi review dạng text -> KHÔNG còn phụ thuộc vào rating nữa (2 thao tác độc lập)
    private void submitReview() {
        String uid = prefManager.getUid();
        String content = edtReviewContent.getText().toString().trim();

        if (uid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Vui lòng nhập nội dung đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreManager.getUserProfile(uid, new FirestoreManager.UserCallback() {
            @Override
            public void onSuccess(com.example.cinematch.models.User user) {
                Review review = new Review(uid, user.getDisplayName(), movieId, content);
                firestoreManager.addReview(review, new FirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        edtReviewContent.setText("");
                        loadReviews();
                        Toast.makeText(MovieDetailActivity.this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(MovieDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) { }
        });
    }

    // Lưu điểm rating -> LUÔN bấm được ngay cả khi không viết review, vì đây là 2 thao tác tách biệt.
    // Đây là chỗ user hay bị "không đánh giá được" trước đây do rating bị gộp chung với review text.
    private void saveRating() {
        String uid = prefManager.getUid();
        float stars = ratingBar.getRating();

        if (uid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stars <= 0) {
            Toast.makeText(this, "Vui lòng chọn số sao trước khi lưu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentMovie == null) {
            Toast.makeText(this, "Đang tải dữ liệu phim, thử lại sau giây lát", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> genreIds = new ArrayList<>();
        if (currentMovie.getGenres() != null) {
            for (com.example.cinematch.models.Genre g : currentMovie.getGenres()) genreIds.add(g.getId());
        }

        // Thang 5 sao * 2 = thang điểm 10, dùng genreIds từ TMDB detail để RecommendationEngine tính weight
        Rating rating = new Rating(uid, movieId, currentMovie.getTitle(),
                currentMovie.getPosterPath(), genreIds, stars * 2);

        firestoreManager.addOrUpdateRating(rating, new FirestoreManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MovieDetailActivity.this,
                        "Đã lưu đánh giá " + stars + " sao", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MovieDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Nếu user đã rate phim này trước đó, tự hiện lại số sao khi mở lại trang chi tiết
    private void loadExistingRating() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        firestoreManager.getRating(uid, movieId, new FirestoreManager.RatingCallback() {
            @Override
            public void onSuccess(Rating rating) {
                if (rating != null) ratingBar.setRating((float) (rating.getScore() / 2.0));
            }

            @Override
            public void onError(String message) { }
        });
    }

    // User bấm "Báo cáo" trên 1 review -> hiện dialog nhập lý do -> tạo document Report
    private void showReportDialog(Review review) {
        String uid = prefManager.getUid();
        if (uid == null) return;

        EditText edtReason = new EditText(this);
        edtReason.setHint(getString(R.string.report_reason_hint));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.report))
                .setView(edtReason)
                .setPositiveButton(getString(R.string.submit), (dialog, which) -> {
                    String reason = edtReason.getText().toString().trim();
                    if (TextUtils.isEmpty(reason)) return;

                    Report report = new Report(review.getReviewId(), uid, reason);
                    firestoreManager.reportReview(report, new FirestoreManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MovieDetailActivity.this, "Đã gửi báo cáo", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MovieDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
