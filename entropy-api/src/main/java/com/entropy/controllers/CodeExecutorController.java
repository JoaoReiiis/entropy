package com.entropy.controllers;

import com.entropy.model.Language;
import com.entropy.model.Problem;
import com.entropy.model.Users;
import com.entropy.model.dto.submission.CodeExecutionRequest;
import com.entropy.model.dto.submission.CodeExecutionResult;
import com.entropy.model.dto.submission.SubmissionResponseDTO;
import com.entropy.repository.ProblemRepository;
import com.entropy.repository.UsersRepository;
import com.entropy.service.CodeExecutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/execute")
@RequiredArgsConstructor
public class CodeExecutorController {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ProblemRepository problemRepository;
    @Autowired
    private CodeExecutorService codeExecutorService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<SubmissionResponseDTO> executeCode(
            @RequestParam("file") MultipartFile file,
            @RequestParam("language") Language language,
            @RequestParam("problemId") Long problemId,
            @RequestParam("userId") Long userId) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdm= authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new RuntimeException("Problema não encontrado"));

            String code = new String(file.getBytes(), StandardCharsets.UTF_8);

            CodeExecutionRequest request = new CodeExecutionRequest(
                    user,
                    problem,
                    code,
                    language,
                    file.getOriginalFilename()
            );

            CodeExecutionResult result = codeExecutorService.executeCodeWithTests(request);

            return ResponseEntity.ok(
                    new SubmissionResponseDTO(
                            problem.getId(),
                            problem.getTitle(),
                            isAdm? result.testResults() : null,
                            user.getUsername(),
                            result.updatedUserScore(),
                            result.problemSolved(),
                            result.submissionTime(),
                            result.errorMessage()
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new SubmissionResponseDTO(
                            null, null, null, null, 0, false, null, e.getMessage()
                    )
            );
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> ex(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
