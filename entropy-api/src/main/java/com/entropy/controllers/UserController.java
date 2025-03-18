package com.entropy.controllers;

import com.entropy.model.Role;
import com.entropy.model.Users;
import com.entropy.model.dto.user.CreateUserDTO;
import com.entropy.model.dto.user.UpdateUserDTO;
import com.entropy.model.dto.user.UserResponseDTO;
import com.entropy.repository.UsersRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UsersRepository usersRepository;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserDTO dto) {
        if (usersRepository.existsByLogin(dto.login())) {
            throw new RuntimeException("Login já está em uso!");
        }

        Users newUser = new Users();
        newUser.setUsername(dto.username());
        newUser.setLogin(dto.login());
        newUser.setRole(Role.USER);
        newUser.setPassword(new BCryptPasswordEncoder().encode(dto.password()));
        newUser.setScore(0);

        usersRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseDTO(newUser));
    }


    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id){
        Optional<Users> usuario = usersRepository.findById(id);

        if(usuario.isPresent()){
            return ResponseEntity.ok(responseDTO(usuario.get()));
        }else{
            throw new RuntimeException("Usuario nao encontrado");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        Optional<Users> user = usersRepository.findById(id);

        if(user.isPresent()){
            user.get().setUsername(dto.username());
            user.get().setPassword(dto.password());


            usersRepository.save(user.get());
            return ResponseEntity.ok(responseDTO(user.get()));
        }else{
            throw new RuntimeException("Usuario nao encontrado");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!usersRepository.existsById(id)) {
            throw new RuntimeException("Usuário não encontrado");
        }

        usersRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> listAll() {
        List<Users> usuarios = usersRepository.findAll();
        List<UserResponseDTO> resposta = usuarios.stream().map(this::responseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    private UserResponseDTO responseDTO(Users user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getLogin(),
                user.getScore()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> ex(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}