package com.filmfestival.backend;

import com.filmfestival.backend.model.User;
import com.filmfestival.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

@SpringBootApplication
public class FilmFestivalApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilmFestivalApplication.class, args);
    }

    @Bean
    public CommandLineRunner updateAdminPassword(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Optional<User> admin = userRepository.findByEmail("admin@filmfestival.com");
            if (admin.isPresent()) {
                User user = admin.get();
                // Check if it's the old legacy hash (length 64 means SHA256 base64 usually, BCrypt is 60)
                if (user.getPassword() != null && user.getPassword().length() != 60) {
                    user.setPassword(passwordEncoder.encode("admin123"));
                    userRepository.save(user);
                    System.out.println("✅ Successfully updated admin@filmfestival.com password to BCrypt!");
                }
            }
        };
    }
}
