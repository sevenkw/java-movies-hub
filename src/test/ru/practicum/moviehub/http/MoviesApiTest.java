package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        // !!! Реализуйте метод beforeAll
        server = new MoviesServer();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        server.start();
    }

    @AfterAll
    static void afterAll() {
        // !!! Реализуйте метод afterAll
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        // Создание HTTP запроса
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        // Отправка запроса
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        // Проверки
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
    }

    @Test
    void postMovies_whenValidData_returnsCreatedAndAddsToList() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setYear(2024);

        String movieJson = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String responseBody = resp.body().trim();
        assertTrue(responseBody.contains("\"id\""), "Ответ должен содержать ID");
        assertTrue(responseBody.contains("\"title\":\"Test Movie\""), "Ответ должен содержать title");
        assertTrue(responseBody.contains("\"year\":2024"), "Ответ должен содержать year");

        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> respGet = client.send(reqGet,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, respGet.statusCode());

        String listBody = respGet.body().trim();
        assertTrue(listBody.contains("\"title\":\"Test Movie\""), "Фильм должен быть в списке");
        assertTrue(listBody.contains("\"year\":2024"), "Год фильма должен совпадать");
        assertTrue(listBody.contains("\"id\":"), "Фильм должен иметь ID");
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Test Movie For ID");
        movie.setYear(2023);

        String movieJson = gson.toJson(movie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> postResp = client.send(postReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());

        String postResponseBody = postResp.body().trim();
        int movieId = extractIdFromJson(postResponseBody);

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode(), "GET /movies/{id} должен вернуть 200");

        String contentType = getResp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String getResponseBody = getResp.body().trim();
        assertTrue(getResponseBody.contains("\"id\":" + movieId), "Ответ должен содержать правильный ID");
        assertTrue(getResponseBody.contains("\"title\":\"Test Movie For ID\""), "Ответ должен содержать title");
        assertTrue(getResponseBody.contains("\"year\":2023"), "Ответ должен содержать year");
    }

    // Вспомогательный метод для извлечения ID из JSON
    private int extractIdFromJson(String json) {
        String idStr = json.substring(json.indexOf("\"id\":") + 5, json.indexOf(","));
        return Integer.parseInt(idStr.trim());
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/999"))
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode(), "Должен вернуть 404");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Фильм не найден\""), "Должен содержать ошибку, фильм не найден");
    }

    @Test
    void getMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/abc")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(), "Должен вернуть 400");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Некорректный id\""),
                "Должен содержать ошибку 'Некорректный id'");

    }

    @Test
    void deleteMovieById_whenExists_returns204() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Test Movie For Delete on ID");
        movie.setYear(2023);

        String movieJson = gson.toJson(movie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> responsePost = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonElement jsonElement = JsonParser.parseString(responsePost.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        int id = jsonObject.get("id").getAsInt();

        HttpRequest httpRequestDelete = HttpRequest.newBuilder().DELETE().uri(URI.create(BASE + "/movies/" + id)).build();
        HttpResponse<String> responseDelete = client.send(httpRequestDelete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, responseDelete.statusCode());
    }

    @Test
    void deleteMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/999")).DELETE().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Фильм не найден\""), "Должен содержать ошибку, фильм не найден");

    }

    @Test
    void deleteMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/abc")).DELETE().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Некорректный id\""),
                "Должен содержать ошибку 'Некорректный id'");
    }

    @Test
    void getMoviesByYear_whenValidYear_returnsFiltered() throws Exception {
        Movie movie2024 = new Movie();
        movie2024.setTitle("Movie 2024");
        movie2024.setYear(2024);

        Movie movie2023 = new Movie();
        movie2023.setTitle("Movie 2023");
        movie2023.setYear(2023);

        String movieJson = gson.toJson(movie2024);
        String movieJson2023 = gson.toJson(movie2023);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpRequest postReq2023 = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson2023))
                .build();

        HttpResponse<String> response = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode());

        HttpResponse<String> response2023 = client.send(postReq2023, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response2023.statusCode());

        HttpRequest getReq = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=2024")).GET().build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentType = getResponse.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = getResponse.body().trim();
        assertTrue(body.contains("\"title\":\"Movie 2024\""), "Должен быть фильм 2024");
        assertTrue(body.contains("\"year\":2024"), "Должен быть год 2024");

        assertFalse(body.contains("\"title\":\"Movie 2023\""), "Не должно быть фильма 2023");
        assertFalse(body.contains("\"year\":2023"), "Не должно быть года 2023");
    }

    @Test
    void getMoviesByYear_whenInvalidFormat_returns400() throws Exception {
        HttpRequest getReq = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=abc")).GET().build();
        HttpResponse<String> response = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(), "Должен вернуть 400");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Некорректный формат года\""),
                "Должен содержать ошибку 'Некорректный формат года'");

    }

    @Test
    void getMoviesByYear_whenInvalidRange_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=1800")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(), "Должен вернуть 400");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Некорректный год\""),
                "Должен содержать ошибку 'Некорректный год'");
    }

    @Test
    void postMovies_whenEmptyTitle_returns422() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("");
        movie.setYear(2024);

        String movieJson = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(), "Должен вернуть 422");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Ошибка валидации\""),
                "Должен содержать ошибку валидации");
        assertTrue(body.contains("\"details\""),
                "Должен содержать массив деталей");
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("a".repeat(101));
        movie.setYear(2024);

        String jsonMovie = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Ошибка валидации\""));
        assertTrue(body.contains("\"details\""));

    }

    @Test
    void postMovies_whenYearTooEarly_returns422() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Good film");
        movie.setYear(1800);

        String movieJson = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(), "Должен вернуть 422");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Ошибка валидации\""));
        assertTrue(body.contains("\"details\""));

    }

    @Test
    void postMovies_whenWrongContentType_returns415() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Normal Movie");
        movie.setYear(2024);

        String jsonMovie = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, response.statusCode());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.contains("\"error\":\"Unsupported Media Type\""));
    }
}