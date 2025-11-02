import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import com.lake_team.fistserios.service.NewsApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@CrossOrigin // keep only if your frontend runs on a different origin/port
public class NewsController {

    private final NewsRepository newsRepository;
    private final NewsApiService newsApiService;

    /**
     * Fast read: DB only, no external API calls.
     * Example: GET /news?page=0&size=6
     */
    @GetMapping
    public Page<NewsItem> getPage(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "6") int size) {
        return newsRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(page, size));
    }

    /** Backward compatibility, but prefer GET /news with pagination. */
    @GetMapping("/all")
    public List<NewsItem> getAll() {
        return newsRepository.findAll();
    }

    /** Get a single news item by id. */
    @GetMapping("/{id}")
    public ResponseEntity<NewsItem> getById(@PathVariable Long id) {
        return newsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Slow update: trigger refresh asynchronously so the client is not blocked.
     * Returns 202 Accepted immediately.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh() {
        newsApiService.refreshAsync(); // requires @Async in the service (step 2)
        // Alternative without @Async:
        // CompletableFuture.runAsync(() -> newsApiService.fetchAndSaveTopHeadlines("us"));
        return ResponseEntity.accepted().build();
    }
}
