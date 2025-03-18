package com.entropy.repository;

import com.entropy.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByLogin (String login);
    UserDetails findByLogin(String login);
    Optional<Users> findUserByLogin(String login);
}
