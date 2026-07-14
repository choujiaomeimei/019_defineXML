package com.stat.service.impl;

import com.stat.service.CodelistExtractionResult;
import com.stat.service.CodelistExtractionService;
import com.stat.service.ProjectFilePathResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodelistExtractionServiceImpl implements CodelistExtractionService {
    private static final String RESULT_PREFIX = "__CODELIST_RESULT__=";
    private static final String SOURCE = "codelist_extractor";
    private static final Map<String, ReentrantLock> PROJECT_USER_LOCKS = new ConcurrentHashMap<>();

    private final ProjectFilePathResolver pathResolver;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/define_db}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:root}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:123123}")
    private String datasourcePassword;

    public CodelistExtractionServiceImpl(ProjectFilePathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    /**
     * Python 进程自身以单个数据库事务完成替换；本事务用于统一 Java 调用边界。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CodelistExtractionResult extract(String projectId, String username, Scope scope) {
        String normalizedProjectId = requireText(projectId, "项目ID不能为空");
        String normalizedUsername = requireText(username, "当前用户不能为空");
        Scope normalizedScope = scope == null ? Scope.ALL : scope;
        String lockKey = normalizedProjectId + '\u0000' + normalizedUsername;
        ReentrantLock lock = PROJECT_USER_LOCKS.computeIfAbsent(lockKey, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new IllegalStateException("该项目正在执行 Codelist 提取，请稍后重试");
        }

        long started = System.currentTimeMillis();
        try {
            return runExtractor(normalizedProjectId, normalizedUsername, normalizedScope, started);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                PROJECT_USER_LOCKS.remove(lockKey, lock);
            }
        }
    }

    private CodelistExtractionResult runExtractor(
            String projectId, String username, Scope scope, long started) {
        Path script = Path.of(pythonPath, "define", "codelist_extractor.py");
        if (!Files.isRegularFile(script)) {
            throw new IllegalStateException("Codelist 提取脚本不存在: " + script);
        }
        String python = findPythonExecutable();
        if (python == null) {
            throw new IllegalStateException("未找到可用的 Python 可执行文件");
        }

        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("codelist-extraction-", ".log");
            ProcessBuilder builder = new ProcessBuilder(python, script.toString());
            builder.directory(new File(pythonPath, "define"));
            builder.redirectErrorStream(true);
            builder.redirectOutput(outputFile.toFile());

            String standardType = pathResolver.resolveStandardType(projectId, null);
            Map<String, String> env = builder.environment();
            env.put("PROJECT_ID", projectId);
            env.put("USERNAME_CONTEXT", username);
            env.put("EXTRACT_SCOPE", scope.name());
            env.put("EXTRACTION_SCOPE", scope.name());
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHON_BASE_PATH", pythonPath);
            env.put("UPLOAD_BASE_PATH", uploadBasePath);
            env.put("DATA_PATH", pathResolver.xptDirectory(projectId, standardType).toString());
            env.put("OUTPUT_PATH", pathResolver.extractionOutputDirectory(projectId, standardType).toString());
            Path edcCodelist = pathResolver.edcCodelist(projectId, standardType);
            if (Files.isRegularFile(edcCodelist)) {
                env.put("EDC_CODELIST_PATH", edcCodelist.toString());
            }
            applyDatabaseEnvironment(env);

            Process process = builder.start();
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codelist 提取超时（5分钟），旧数据未被替换");
            }

            String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Codelist 提取失败: " + tail(output, 1200));
            }
            CodelistExtractionResult result = parseResult(output, projectId, scope);
            result.setDurationMillis(System.currentTimeMillis() - started);
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Codelist 提取被中断，旧数据未被替换", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("执行 Codelist 提取失败: " + ex.getMessage(), ex);
        } finally {
            if (outputFile != null) {
                try { Files.deleteIfExists(outputFile); } catch (Exception ignored) { }
            }
        }
    }

    private CodelistExtractionResult parseResult(String output, String projectId, Scope scope) {
        int marker = output.lastIndexOf(RESULT_PREFIX);
        if (marker < 0) {
            throw new IllegalStateException("提取器未返回结构化结果: " + tail(output, 800));
        }
        String json = output.substring(marker + RESULT_PREFIX.length()).lines().findFirst().orElse("{}");
        CodelistExtractionResult result = new CodelistExtractionResult();
        result.setProjectId(projectId);
        result.setScope(scope);
        result.setCodelistCount(intValue(json, "codelists"));
        result.setTermCount(intValue(json, "terms"));
        result.setInsertedCount(intValue(json, "inserted"));
        result.setUpdatedCount(intValue(json, "updated"));
        result.setDeletedCount(intValue(json, "deleted"));
        result.setSpecReferenceCount(intValue(json, "specReferences"));
        result.setVlmReferenceCount(intValue(json, "vlmReferences"));
        result.setSkippedDeletedCount(intValue(json, "skippedDeleted"));
        result.setNciMatched(intValue(json, "nciMatched"));
        result.setNciUnmatched(intValue(json, "nciUnmatched"));
        result.setPreservedManual(intValue(json, "preservedManual"));
        result.setReappliedMerges(intValue(json, "reappliedMerges"));
        result.setWarningCount(intValue(json, "warningCount"));
        result.setFallbackCount(intValue(json, "fallbackCount"));
        String failedDatasets = stringValue(json, "failedDatasets");
        result.setFailedDatasets(failedDatasets.isBlank()
                ? Collections.emptyList()
                : Arrays.stream(failedDatasets.split("\\|"))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList());
        result.setSource(SOURCE);
        result.setMessage(String.format(
                Locale.ROOT, "%s Codelist 提取完成：%d 个列表，%d 个 Term",
                scope.name(), result.getCodelistCount(), result.getTermCount()));
        return result;
    }

    private int intValue(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String stringValue(String json, String field) {
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private void applyDatabaseEnvironment(Map<String, String> env) {
        String url = datasourceUrl == null ? "" : datasourceUrl;
        Matcher matcher = Pattern.compile(
                "jdbc:mysql://([^/:?]+)(?::(\\d+))?/([^?]+)", Pattern.CASE_INSENSITIVE).matcher(url);
        if (matcher.find()) {
            env.put("DB_HOST", matcher.group(1));
            env.put("DB_PORT", matcher.group(2) == null ? "3306" : matcher.group(2));
            env.put("DB_NAME", matcher.group(3));
        }
        env.put("DB_USER", datasourceUsername);
        env.put("DB_PASSWORD", datasourcePassword);
    }

    private String findPythonExecutable() {
        for (String command : new String[]{"python", "python3", "py"}) {
            try {
                Process process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
                if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    return command;
                }
                process.destroyForcibly();
            } catch (Exception ignored) { }
        }
        return null;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String tail(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(trimmed.length() - maxLength);
    }
}
