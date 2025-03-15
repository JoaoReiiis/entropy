package com.entropy.controllers;

import com.entropy.model.Problem;
import com.entropy.model.ProblemTestCase;
import com.entropy.model.dto.problem.CreateProblemDTO;
import com.entropy.model.dto.testCase.TestCaseDTO;
import com.entropy.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemRepository problemRepository;

    @PostMapping
    public ResponseEntity<Problem> createProblem(@RequestBody CreateProblemDTO dto) {
        Problem newProblem = new Problem();
        newProblem.setTitle(dto.title());
        newProblem.setDescription(dto.description());
        newProblem.setMaxSubmissions(dto.maxSubmissions());
        newProblem.setPoints(dto.points());

        Problem savedProblem = problemRepository.save(newProblem);
        return ResponseEntity.ok(savedProblem);
    }

    @PostMapping("/{problemId}/testCases")
    public ResponseEntity<TestCaseDTO> addTestCase(
            @PathVariable Long problemId,
            @RequestBody ProblemTestCase testCase) {

        Optional<Problem> problem = problemRepository.findById(problemId);
        if(problem.isEmpty()){
            throw new RuntimeException("Problema não encontrado");
        }else{
            testCase.setProblem(problem.get());
            problem.get().getTestCases().add(testCase);
            problemRepository.save(problem.get());

            TestCaseDTO testCaseDTO = new TestCaseDTO(testCase.getInputs(),testCase.getExpectedOutput());

            return ResponseEntity.ok(testCaseDTO);
        }
    }

    @GetMapping
    public ResponseEntity<List<Problem>> findAllProblem() {
        List<Problem> problens = problemRepository.findAll();
        return ResponseEntity.ok(problens);
    }

    @GetMapping("/{problemId}")
    public ResponseEntity<Problem> getProblem(@PathVariable Long problemId) {
        Optional<Problem> problem = problemRepository.findById(problemId);
        if(problem.isPresent()){

            return ResponseEntity.ok(problem.get());
        }else{
            throw new RuntimeException("Problema não encontrado");
        }
    }

    @GetMapping("/{problemId}/testCases")
    public ResponseEntity<List<TestCaseDTO>> getTestCases(@PathVariable Long problemId) {
        Optional<Problem> problem = problemRepository.findById(problemId);
        if(problem.isPresent()){
            List<TestCaseDTO> testCases = problem.get().getTestCases().stream().map(tc-> new TestCaseDTO(tc.getInputs(), tc.getExpectedOutput())).toList();
            return ResponseEntity.ok(testCases);
        }else{
            throw new RuntimeException("Problema não encontrado");
        }
    }
}