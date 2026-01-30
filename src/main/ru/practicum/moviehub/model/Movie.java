package ru.practicum.moviehub.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Movie {
    private int id;
    private String title;
    private int year;
    private static final int THIS_YEAR = LocalDate.now().getYear();

    public Movie(int id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public Movie() {

    }

    public int getThisYear() {
        return THIS_YEAR;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id == movie.id && year == movie.year && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, year);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year=" + year +
                '}';
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            errors.add("Название фильма не может быть пустым");
        } else if (title.length() > 100) {
            errors.add("Название не должно превышать 100 символов");
        }

        if (year < 1888 || year > (THIS_YEAR + 1)) {
            errors.add("Введите корректный год выхода фильма");
        }
        return errors;
    }
}