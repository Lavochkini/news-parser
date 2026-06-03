package com.lake_team.fistserios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lake_team.fistserios.controller.rest.ManualNewsController;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManualNewsController")
class ManualNewsControllerTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ManualNewsController controller;

    private ManualNewsController.ManualArticleRequest req(String title, String url) {
        return new ManualNewsController.ManualArticleRequest(
                title, "description", "full content",
                url, null, "TestSource", "Politics", null
        );
    }

    // ── addOne ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Успішне додавання → HTTP 200 з id та title")
    void addOne_success_returns200() {
        when(newsRepository.findByTitle("New Unique Title")).thenReturn(Optional.empty());
        NewsItem saved = NewsItem.builder().id("abc123").title("New Unique Title").build();
        when(newsRepository.save(any())).thenReturn(saved);

        var response = controller.addOne(req("New Unique Title", "https://example.com/unique"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsEntry("title", "New Unique Title");
    }

    @Test
    @DisplayName("Дублікат за заголовком → HTTP 409")
    void addOne_duplicateTitle_returns409() {
        NewsItem existing = NewsItem.builder().id("existing").title("Existing Title").build();
        when(newsRepository.findByTitle("Existing Title")).thenReturn(Optional.of(existing));

        var response = controller.addOne(req("Existing Title", null));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).containsKey("duplicate");
        assertThat(response.getBody()).containsKey("error");
        // Стаття не збережена
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Дублікат за URL (DuplicateKeyException) → HTTP 409")
    void addOne_duplicateUrl_returns409() {
        when(newsRepository.findByTitle(any())).thenReturn(Optional.empty());
        when(newsRepository.save(any())).thenThrow(new DuplicateKeyException("URL duplicate"));

        var response = controller.addOne(req("Unique Title", "https://example.com/exists"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("Порожній заголовок → HTTP 400")
    void addOne_blankTitle_returns400() {
        var response = controller.addOne(req("", null));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(newsRepository, never()).findByTitle(any());
        verify(newsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Null заголовок → HTTP 400")
    void addOne_nullTitle_returns400() {
        var response = controller.addOne(req(null, null));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Заголовок з пробілами навколо → trim перед перевіркою")
    void addOne_titleWithSpaces_trimmedBeforeCheck() {
        when(newsRepository.findByTitle("Trimmed Title")).thenReturn(Optional.empty());
        NewsItem saved = NewsItem.builder().id("id1").title("  Trimmed Title  ").build();
        when(newsRepository.save(any())).thenReturn(saved);

        var response = controller.addOne(req("  Trimmed Title  ", null));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(newsRepository).findByTitle("Trimmed Title");
    }

    @Test
    @DisplayName("Успішне додавання → repository.save() викликано рівно 1 раз")
    void addOne_success_savesExactlyOnce() {
        when(newsRepository.findByTitle(any())).thenReturn(Optional.empty());
        NewsItem saved = NewsItem.builder().id("id99").title("Article").build();
        when(newsRepository.save(any())).thenReturn(saved);

        controller.addOne(req("Article", null));

        verify(newsRepository, times(1)).save(any());
    }
}
