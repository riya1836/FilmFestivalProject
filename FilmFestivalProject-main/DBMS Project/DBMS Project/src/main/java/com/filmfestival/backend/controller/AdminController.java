package com.filmfestival.backend.controller;

import com.filmfestival.backend.model.Film;
import com.filmfestival.backend.repository.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private FilmRepository filmRepository;

    @PostMapping("/films")
    public Film createFilm(@RequestBody Film film) {
        return filmRepository.save(film);
    }
}
