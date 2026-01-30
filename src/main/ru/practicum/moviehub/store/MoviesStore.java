package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoviesStore {

    private final Map<Integer, Movie> storage;
    private int currentId = 1;

    public MoviesStore() {
        this.storage = new HashMap<>();
    }

    public Map<Integer, Movie> getStorageMove() {
        return storage;
    }

    private int generateId() {
        return currentId++;
    }

    public Movie addMovie(Movie movie) {
        int id = generateId();
        Movie movieWithId = new Movie(id, movie.getTitle(), movie.getYear());
        storage.put(id, movieWithId);
        return movieWithId;
    }

    public Movie getMovieById(int id) {
        return storage.get(id);
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(storage.values());
    }

    public boolean deleteMovie(int id) {
        return storage.remove(id) != null;
    }

    public void clear() {
        storage.clear();
        currentId = 1;
    }

    public List<Movie> getMoviesByYear(int year) {
        return storage.values().stream().filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
    }

}