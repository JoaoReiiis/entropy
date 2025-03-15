package com.entropy.service;

import com.entropy.model.*;
import com.entropy.model.dto.submission.CodeExecutionRequest;
import com.entropy.model.dto.submission.CodeExecutionResult;
import com.entropy.model.dto.testCase.TestCaseResultDTO;
import com.entropy.repository.SubmissionRepository;
import com.entropy.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeExecutorService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TestCaseRepository testCaseResultRepository;

    private static final String TEMP_DIR = "temp_code/";

    public CodeExecutionResult executeCodeWithTests(CodeExecutionRequest request) {
        Users user = request.user();
        Problem problem = request.problem();
        List<TestCaseResultDTO> results = new ArrayList<>();

        try {
            //validateSubmission(user, problem);
            String comand = prepareExecution(request);
            results = executeTestCases(problem, comand);

            boolean allPassed = results.stream().allMatch(TestCaseResultDTO::passed);
            Submission submission = createSubmission(request, allPassed);
            updateUserScore(user, problem, allPassed);
            saveTestResults(results, submission);

            return new CodeExecutionResult(
                    results,
                    allPassed,
                    user.getScore(),
                    null,
                    submission.getSubmissionDate()
            );

        } catch (Exception e) {
            return handleExecutionError(user, e);
        }
    }

    private void validateSubmission(Users user, Problem problem) {
        if (user.getSubmissions().stream()
                .anyMatch(s -> s.isSolved() && s.getProblem().equals(problem))) {
            throw new IllegalStateException("Problema já resolvido");
        }

        long submissionCount = user.getSubmissions().stream()
                .filter(s -> s.getProblem().equals(problem))
                .count();

        if (submissionCount >= problem.getMaxSubmissions()) {
            throw new IllegalStateException("Limite de submissões excedido");
        }
    }

    private String prepareExecution(CodeExecutionRequest request) throws Exception {
        validateFileExtension(request.filename(), request.language());
        String fullPath = saveCodeToFile(request.code(), request.filename());
        String command = getExecutionCommand(request.language(), fullPath);

        if (request.language().equals(Language.JAVA) || request.language().equals(Language.CPP)) {
            compileCode(request.language(), fullPath);
        }

        return command;
    }

    private List<TestCaseResultDTO> executeTestCases(Problem problem, String command) {
        return problem.getTestCases().stream()
                .map(tc -> {
                    try {
                        String actualOutput = runProcessWithInputs(command, tc.getInputs());
                        boolean passed = actualOutput.trim().equals(tc.getExpectedOutput().trim());
                        return new TestCaseResultDTO(
                                tc.getInputs(),
                                tc.getExpectedOutput(),
                                actualOutput,
                                passed
                        );
                    } catch (Exception e) {
                        return createErrorResult(e);
                    }
                }).toList();
    }

    private Submission createSubmission(CodeExecutionRequest request, boolean allPassed) {
        Submission submission = new Submission();
        submission.setCode(request.code());
        submission.setLanguage(request.language());
        submission.setFilename(request.filename());
        submission.setSubmissionDate(LocalDateTime.now());
        submission.setSolved(allPassed);
        submission.setProblem(request.problem());
        submission.setUser(request.user());
        return submissionRepository.save(submission);
    }

    private void updateUserScore(Users user, Problem problem, boolean allPassed) {
        if (allPassed) {
            user.setScore(user.getScore() + problem.getPoints());
        }
    }

    private void saveTestResults(List<TestCaseResultDTO> results, Submission submission) {
        results.forEach(dto -> {
            TestCase testCase = new TestCase();
            testCase.setInputs(new ArrayList<>(dto.inputs()));
            testCase.setExpectedOutput(dto.expectedOutput());
            testCase.setActualOutput(dto.actualOutput());
            testCase.setPassed(dto.passed());
            testCase.setSubmission(submission);
            testCaseResultRepository.save(testCase);
        });
    }

    private CodeExecutionResult handleExecutionError(Users user, Exception e) {
        return new CodeExecutionResult(
                List.of(createErrorResult(e)),
                false,
                user.getScore(),
                e.getMessage(),
                LocalDateTime.now()
        );
    }

    private TestCaseResultDTO createErrorResult(Exception e) {
        return new TestCaseResultDTO(
                List.of(),
                "",
                "Erro durante execução: " + e.getMessage(),
                false
        );
    }


    private void compileCode(Language language, String fullPath) throws IOException, InterruptedException {
        String compileCommand = switch (language) {
            case JAVA -> "javac " + fullPath;
            case CPP -> "g++ " + fullPath + " -o " + fullPath.replace(".cpp", ".exe");
            default -> "";
        };

        if (!compileCommand.isEmpty()) {
            Process compileProcess = new ProcessBuilder()
                    .command(compileCommand.split(" "))
                    .start();

            int exitCode = compileProcess.waitFor();
            if (exitCode != 0) {
                String error = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()))
                        .lines().collect(Collectors.joining("\n"));
                throw new RuntimeException("Erro de compilação: " + error);
            }
        }
    }

    private void validateFileExtension(String filename, Language language) {
        String expectedExtension = switch (language) {
            case PYTHON -> ".py";
            case JAVA -> ".java";
            case CPP -> ".cpp";
            default -> throw new IllegalArgumentException("Linguagem não suportada");
        };

        if (!filename.endsWith(expectedExtension)) {
            throw new IllegalArgumentException("Extensão do arquivo não corresponde à linguagem selecionada");
        }
    }

    private String saveCodeToFile(String code, String filename) throws IOException {
        File directory = new File(TEMP_DIR);
        if (!directory.exists()) directory.mkdirs();

        String fullPath = TEMP_DIR + filename;
        try (FileWriter writer = new FileWriter(fullPath)) {
            writer.write(code);
        }
        return fullPath;
    }

    private String getExecutionCommand(Language language, String fullPath) {
        String filename = new File(fullPath).getName();
        switch (language) {
            case PYTHON:
                return "python " + fullPath;
            case JAVA:
                String className = filename.replace(".java", "");
                return "java -cp " + TEMP_DIR + " " + className;
            case CPP:
                return fullPath.replace(".cpp", ".exe");
            default:
                throw new IllegalArgumentException("Linguagem não suportada");
        }
    }

    private String runProcessWithInputs(String command, List<String> inputs) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            for (String input : inputs) {
                writer.write(input);
                writer.newLine();
            }
        }

        String actualOutput = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.joining("\n"));

        process.waitFor(1, TimeUnit.SECONDS);
        return actualOutput.trim();
    }
}
