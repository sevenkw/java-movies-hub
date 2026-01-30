package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final Gson gson = new Gson();
    private final MoviesStore moviesStore = new MoviesStore();


    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && "/movies".equals(path)) {
            String query = exchange.getRequestURI().getQuery();

            if (query != null && query.startsWith("year=")) {

                try {
                    int year = Integer.parseInt(query.split("=")[1].trim());

                    if (year < 1888 || year > LocalDate.now().plusYears(1).getYear()) {
                        sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный год")));
                        return;
                    }

                    List<Movie> filtered = moviesStore.getMoviesByYear(year);
                    String json = gson.toJson(filtered);
                    sendJson(exchange, 200, json);

                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный формат года")));
                }

            } else {

                List<Movie> movies = moviesStore.getAllMovies();
                String json = gson.toJson(movies);
                sendJson(exchange, 200, json);
            }

        } else if ("POST".equals(method) && "/movies".equals(path)) {

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

            if (!"application/json".equals(contentType)) {
                sendJson(exchange, 415, gson.toJson(new ErrorResponse("Unsupported Media Type")));
                return;
            }

            try {
                byte[] body = exchange.getRequestBody().readAllBytes();
                String jsonBody = new String(body, StandardCharsets.UTF_8);
                Movie movie = gson.fromJson(jsonBody, Movie.class);

                List<String> errors = movie.validate();
                if (!errors.isEmpty()) {
                    sendJson(exchange, 422, gson.toJson(new ErrorResponse("Ошибка валидации", errors)));
                    return;
                }

                Movie addedMovie = moviesStore.addMovie(movie);
                String json = gson.toJson(addedMovie);
                sendJson(exchange, 201, json);

            } catch (Exception e) {
                sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный JSON")));
            }
        } else if ("GET".equals(method) && path.startsWith("/movies/")) {
            String[] pathParts = path.split("/");
            if (pathParts.length == 3) {
                try {
                    int id = Integer.parseInt(pathParts[2]);
                    Movie movie = moviesStore.getMovieById(id);

                    if (movie != null) {
                        String json = gson.toJson(movie);
                        sendJson(exchange, 200, json);
                    } else {
                        sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
                    }

                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный id")));
                }
            } else {
                sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный путь")));
            }
        } else if ("DELETE".equals(method) && path.startsWith("/movies/")) {
            String[] pathParts = path.split("/");
            if (pathParts.length == 3) {
                try {
                    int id = Integer.parseInt(pathParts[2]);
                    boolean deleted = moviesStore.deleteMovie(id);

                    if (deleted) {
                        sendNoContent(exchange);
                    } else {
                        sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
                    }

                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный id")));
                }
            } else {
                sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный путь")));
            }
        }
    }

}

