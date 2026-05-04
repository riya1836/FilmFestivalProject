package com.filmfestival.backend.controller;

import com.filmfestival.backend.service.DashboardQueryService;
import com.filmfestival.backend.model.Venue;
import com.filmfestival.backend.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    @Autowired
    private DashboardQueryService dashboardQueryService;

    @Autowired
    private VenueRepository venueRepository;

    // --- Venues (JPA based) ---
    @GetMapping("/venues")
    public List<Venue> getAllVenues() {
        return venueRepository.findAll();
    }

    @PostMapping("/venues")
    public Venue createVenue(@RequestBody Venue venue) {
        return venueRepository.save(venue);
    }

    @PutMapping("/venues/{id}")
    public ResponseEntity<Venue> updateVenue(@PathVariable Integer id, @RequestBody Venue venueDetails) {
        return venueRepository.findById(id)
                .map(venue -> {
                    venue.setName(venueDetails.getName());
                    venue.setLocation(venueDetails.getLocation());
                    venue.setCapacity(venueDetails.getCapacity());
                    return ResponseEntity.ok(venueRepository.save(venue));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/venues/{id}")
    public ResponseEntity<?> deleteVenue(@PathVariable Integer id) {
        return venueRepository.findById(id)
                .map(venue -> {
                    venueRepository.delete(venue);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Complex Queries (JdbcTemplate based) ---
    @GetMapping("/tickets")
    public List<Map<String, Object>> getAllTickets() {
        return dashboardQueryService.getAllTickets();
    }

    @GetMapping("/screenings")
    public List<Map<String, Object>> getAllScreenings() {
        return dashboardQueryService.getAllScreenings();
    }

    @GetMapping("/attendees")
    public List<Map<String, Object>> getAllAttendees() {
        return dashboardQueryService.getAllAttendees();
    }

    @GetMapping("/jury-assignments")
    public List<Map<String, Object>> getJuryAssignments() {
        return dashboardQueryService.getJuryAssignments();
    }

    @GetMapping("/award-eligible")
    public List<Map<String, Object>> getAwardEligibleFilms() {
        return dashboardQueryService.getAwardEligibleFilms();
    }

    @GetMapping("/awards")
    public List<Map<String, Object>> getAllAwards() {
        return dashboardQueryService.getAllAwards();
    }

    @GetMapping("/filmcrew")
    public List<Map<String, Object>> getAllFilmCrew() {
        return dashboardQueryService.getAllFilmCrew();
    }
}
