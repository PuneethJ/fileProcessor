package com.pj.fileprocessor.fileprocessor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@RestController
@RequestMapping("/api")
public class ExcelUploadController {

    @PostMapping("/upload")
    public ResponseEntity<String> handleUpload(@RequestParam("file") MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(in);
            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            int rulesColIndex = header.getLastCellNum();
            Cell rulesHeader = header.createCell(rulesColIndex);
            rulesHeader.setCellValue("Rules");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(1); // "Required Tasks"
                if (cell == null || cell.getCellType() != CellType.STRING) continue;

                String taskExpr = cell.getStringCellValue();
                JsonNode ruleJson = RuleBuilder.buildRule(taskExpr);
                row.createCell(rulesColIndex).setCellValue(ruleJson.toString());
            }

            // Save the output as .xlsx
            String outputPath = "EWNworkstreamAutomationOutput.xlsx";
            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                workbook.write(out);
            }

            return ResponseEntity.ok("File processed and saved as " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}
