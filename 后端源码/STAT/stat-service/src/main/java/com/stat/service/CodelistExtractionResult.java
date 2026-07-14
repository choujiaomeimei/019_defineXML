package com.stat.service;

import java.util.Collections;
import java.util.List;

/**
 * 一次 Codelist 提取的结构化结果。
 */
public class CodelistExtractionResult {
    private String projectId;
    private CodelistExtractionService.Scope scope;
    private int codelistCount;
    private int termCount;
    private int insertedCount;
    private int updatedCount;
    private int deletedCount;
    private int specReferenceCount;
    private int vlmReferenceCount;
    private int skippedDeletedCount;
    private int nciMatched;
    private int nciUnmatched;
    private int preservedManual;
    private int reappliedMerges;
    private int warningCount;
    private int fallbackCount;
    private List<String> failedDatasets = Collections.emptyList();
    private long durationMillis;
    private String source;
    private String message;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public CodelistExtractionService.Scope getScope() { return scope; }
    public void setScope(CodelistExtractionService.Scope scope) { this.scope = scope; }
    public int getCodelistCount() { return codelistCount; }
    public void setCodelistCount(int codelistCount) { this.codelistCount = codelistCount; }
    public int getTermCount() { return termCount; }
    public void setTermCount(int termCount) { this.termCount = termCount; }
    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int insertedCount) { this.insertedCount = insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getDeletedCount() { return deletedCount; }
    public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }
    public int getSpecReferenceCount() { return specReferenceCount; }
    public void setSpecReferenceCount(int specReferenceCount) { this.specReferenceCount = specReferenceCount; }
    public int getVlmReferenceCount() { return vlmReferenceCount; }
    public void setVlmReferenceCount(int vlmReferenceCount) { this.vlmReferenceCount = vlmReferenceCount; }
    public int getSkippedDeletedCount() { return skippedDeletedCount; }
    public void setSkippedDeletedCount(int skippedDeletedCount) { this.skippedDeletedCount = skippedDeletedCount; }
    public int getNciMatched() { return nciMatched; }
    public void setNciMatched(int nciMatched) { this.nciMatched = nciMatched; }
    public int getNciUnmatched() { return nciUnmatched; }
    public void setNciUnmatched(int nciUnmatched) { this.nciUnmatched = nciUnmatched; }
    public int getPreservedManual() { return preservedManual; }
    public void setPreservedManual(int preservedManual) { this.preservedManual = preservedManual; }
    public int getReappliedMerges() { return reappliedMerges; }
    public void setReappliedMerges(int reappliedMerges) { this.reappliedMerges = reappliedMerges; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
    public int getFallbackCount() { return fallbackCount; }
    public void setFallbackCount(int fallbackCount) { this.fallbackCount = fallbackCount; }
    public List<String> getFailedDatasets() { return failedDatasets; }
    public void setFailedDatasets(List<String> failedDatasets) {
        this.failedDatasets = failedDatasets == null ? Collections.emptyList() : failedDatasets;
    }
    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
