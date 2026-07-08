package com.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stat.common.entity.SdrgContent;
import com.stat.common.entity.SdrgTemplate;
import com.stat.dal.mapper.SdrgContentMapper;
import com.stat.dal.mapper.SdrgTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SdrgServiceImpl {

    private static final List<SectionDef> DEFAULT_SECTIONS = Arrays.asList(
            new SectionDef("study_info", "1. Study Information", 1),
            new SectionDef("submission_info", "2. Submission Information", 2),
            new SectionDef("datasets_overview", "3. Datasets Overview", 3),
            new SectionDef("data_conformance", "4. Data Conformance Summary", 4),
            new SectionDef("deviations", "5. Known Issues and Deviations", 5),
            new SectionDef("special_considerations", "6. Special Considerations", 6),
            new SectionDef("additional_info", "7. Additional Information", 7)
    );

    @Autowired
    private SdrgContentMapper sdrgContentMapper;

    @Autowired
    private SdrgTemplateMapper sdrgTemplateMapper;

    @Value("${app.projects.base-path:C:/Project_Web/019_defineXML/projects}")
    private String projectsBasePath;

    public List<SdrgContent> getSections(String projectId) {
        LambdaQueryWrapper<SdrgContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SdrgContent::getProjectId, projectId)
                .orderByAsc(SdrgContent::getSectionOrder);
        List<SdrgContent> sections = sdrgContentMapper.selectList(wrapper);

        if (sections.isEmpty()) {
            sections = initDefaultSections(projectId);
        }
        return sections;
    }

    public SdrgContent saveSection(SdrgContent content) {
        LambdaQueryWrapper<SdrgContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SdrgContent::getProjectId, content.getProjectId())
                .eq(SdrgContent::getSectionKey, content.getSectionKey());
        SdrgContent existing = sdrgContentMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setSectionTitle(content.getSectionTitle());
            existing.setContentText(content.getContentText());
            existing.setContentJson(content.getContentJson());
            existing.setUpdatedBy(content.getUpdatedBy());
            sdrgContentMapper.updateById(existing);
            return existing;
        } else {
            sdrgContentMapper.insert(content);
            return content;
        }
    }

    public String exportSdrg(String projectId) throws IOException {
        List<SdrgContent> sections = getSections(projectId);

        Path outputDir = Paths.get(projectsBasePath, projectId, "sdrg");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("SDRG_" + projectId + ".docx");

        // Check if a template exists
        SdrgTemplate template = getDefaultTemplate();

        if (template != null && Files.exists(Paths.get(template.getFilePath()))) {
            exportWithTemplate(sections, Paths.get(template.getFilePath()), outputFile);
        } else {
            exportWithoutTemplate(sections, projectId, outputFile);
        }

        log.info("SDRG exported: {}", outputFile);
        return outputFile.toString();
    }

    private void exportWithoutTemplate(List<SdrgContent> sections, String projectId, Path output) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Study Data Reviewer's Guide");
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            XWPFParagraph subTitle = doc.createParagraph();
            subTitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subRun = subTitle.createRun();
            subRun.setText("Project: " + projectId);
            subRun.setFontSize(14);

            doc.createParagraph();

            for (SdrgContent section : sections) {
                XWPFParagraph heading = doc.createParagraph();
                heading.setStyle("Heading1");
                XWPFRun headingRun = heading.createRun();
                headingRun.setText(section.getSectionTitle());
                headingRun.setBold(true);
                headingRun.setFontSize(14);

                if (section.getContentText() != null && !section.getContentText().isEmpty()) {
                    String plainText = section.getContentText()
                            .replaceAll("<[^>]+>", "")
                            .replaceAll("&nbsp;", " ")
                            .replaceAll("&amp;", "&")
                            .replaceAll("&lt;", "<")
                            .replaceAll("&gt;", ">");

                    for (String para : plainText.split("\n")) {
                        if (!para.trim().isEmpty()) {
                            XWPFParagraph p = doc.createParagraph();
                            XWPFRun r = p.createRun();
                            r.setText(para.trim());
                            r.setFontSize(11);
                        }
                    }
                } else {
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun r = p.createRun();
                    r.setText("[待填写]");
                    r.setItalic(true);
                    r.setColor("999999");
                }

                doc.createParagraph();
            }

            try (OutputStream os = Files.newOutputStream(output)) {
                doc.write(os);
            }
        }
    }

    private void exportWithTemplate(List<SdrgContent> sections, Path templatePath, Path output) throws IOException {
        Map<String, String> sectionMap = sections.stream()
                .collect(Collectors.toMap(SdrgContent::getSectionKey,
                        s -> s.getContentText() != null ? s.getContentText().replaceAll("<[^>]+>", "") : "",
                        (a, b) -> a));

        try (InputStream is = Files.newInputStream(templatePath);
             XWPFDocument doc = new XWPFDocument(is)) {

            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.getText(0);
                    if (text != null) {
                        for (Map.Entry<String, String> entry : sectionMap.entrySet()) {
                            String placeholder = "{{" + entry.getKey() + "}}";
                            if (text.contains(placeholder)) {
                                text = text.replace(placeholder, entry.getValue());
                                run.setText(text, 0);
                            }
                        }
                    }
                }
            }

            try (OutputStream os = Files.newOutputStream(output)) {
                doc.write(os);
            }
        }
    }

    private List<SdrgContent> initDefaultSections(String projectId) {
        List<SdrgContent> sections = new ArrayList<>();
        for (SectionDef def : DEFAULT_SECTIONS) {
            SdrgContent content = new SdrgContent();
            content.setProjectId(projectId);
            content.setSectionKey(def.key);
            content.setSectionTitle(def.title);
            content.setSectionOrder(def.order);
            content.setContentText("");
            sdrgContentMapper.insert(content);
            sections.add(content);
        }
        return sections;
    }

    private SdrgTemplate getDefaultTemplate() {
        LambdaQueryWrapper<SdrgTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SdrgTemplate::getIsDefault, true).last("LIMIT 1");
        return sdrgTemplateMapper.selectOne(wrapper);
    }

    public List<SdrgTemplate> listTemplates() {
        return sdrgTemplateMapper.selectList(null);
    }

    private static class SectionDef {
        final String key;
        final String title;
        final int order;

        SectionDef(String key, String title, int order) {
            this.key = key;
            this.title = title;
            this.order = order;
        }
    }
}
