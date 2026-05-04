package com.filmfestival.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashboardQueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getAllTickets() {
        String sql = "SELECT t.ticket_id, t.screening_id, t.attendee_id, t.seat_number, t.booking_time, " +
                     "s.screening_date, s.start_time, s.ticket_price, a.name AS attendee_name " +
                     "FROM ticket t " +
                     "JOIN screening s ON t.screening_id = s.screening_id " +
                     "JOIN attendee a ON t.attendee_id = a.attendee_id " +
                     "ORDER BY t.booking_time DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAllScreenings() {
        String sql = "SELECT s.*, f.title as film_title, v.name as venue_name " +
                     "FROM screening s " +
                     "JOIN film f ON s.film_id = f.film_id " +
                     "JOIN venue v ON s.venue_id = v.venue_id " +
                     "ORDER BY s.screening_date DESC, s.start_time DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAllAttendees() {
        String sql = "SELECT * FROM attendee ORDER BY registration_date DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getJuryAssignments() {
        String sql = "SELECT ja.id, ja.jury_id, ja.film_id, ja.assigned_at, " +
                     "f.title AS film_title, u.name AS jury_name " +
                     "FROM jury_assignment ja " +
                     "JOIN film f ON ja.film_id = f.film_id " +
                     "JOIN jury j ON ja.jury_id = j.jury_id " +
                     "JOIN users u ON j.user_id = u.user_id " +
                     "ORDER BY ja.assigned_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAwardEligibleFilms() {
        // Just fetching all films for now as basic implementation
        // Advanced logic can be added later
        String sql = "SELECT * FROM film ORDER BY rating DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAllAwards() {
        String sql = "SELECT * FROM award ORDER BY award_name ASC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAllFilmCrew() {
        String sql = "SELECT fc.id, fc.film_id, fc.crew_id, fc.role, " +
                     "f.title as film_title, c.name as crew_name " +
                     "FROM film_crew fc " +
                     "JOIN film f ON fc.film_id = f.film_id " +
                     "JOIN crew c ON fc.crew_id = c.crew_id";
        return jdbcTemplate.queryForList(sql);
    }
}
