package com.filmfestival.backend.controller;

import com.filmfestival.backend.model.Film;
import com.filmfestival.backend.repository.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    @Autowired
    private FilmRepository filmRepository;

    @GetMapping("/films")
    public List<Film> getAllFilms() {
        return filmRepository.findAll();
    }
}
