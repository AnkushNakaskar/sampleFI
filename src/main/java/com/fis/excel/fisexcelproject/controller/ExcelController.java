package com.fis.excel.fisexcelproject.controller;

import com.fis.excel.fisexcelproject.CopySheets;
import com.fis.excel.fisexcelproject.InputBean;
import com.fis.excel.fisexcelproject.service.ExcelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/excel")
@Slf4j
public class ExcelController {

    private static final Set<String> excelExtentions = new HashSet<>(Arrays.asList("xls", "xlsx"));
    private static final String DESCRIPTION_BULK_UPLOAD_DOCUMENT_NAME = "fis_sample_output.xlsx";


    @Autowired
    private ExcelService excelService;

    @Autowired
    private CopySheets copySheets;


    @PostMapping(value = "/upload/description")
    public ResponseEntity<List<InputBean>> uploadDescriptionBulkUploadTemplate(@RequestParam("descriptionBulkUploadFile") MultipartFile descriptionBulkUploadFile) {
        List<InputBean> responseMessage = new LinkedList<>();
        HttpStatus responseStatus;
        try {
            String extension = FilenameUtils.getExtension(descriptionBulkUploadFile.getOriginalFilename());
            if (excelExtentions.contains(extension)) {
                File file = new File("" + System.currentTimeMillis() + "." + extension);
                FileUtils.writeByteArrayToFile(file, descriptionBulkUploadFile.getBytes());
                responseMessage = excelService.processInBackground(file);
                log.info("Input Lists are with record ::: " + responseMessage);
                responseStatus = HttpStatus.OK;
                FileUtils.deleteQuietly(file);
            } else {
                responseStatus = HttpStatus.NOT_ACCEPTABLE;
            }
        } catch (Exception exception) {
            log.error("Exception in uploading the  excel FIS bulk upload is :: ", exception);
            responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(responseStatus).body(responseMessage);
    }

    @PostMapping(value = "/download/description")
    public ResponseEntity<InputStreamResource> downLoadTemplate(@RequestParam("descriptionBulkUploadFile") MultipartFile descriptionBulkUploadFile) {
        ByteArrayInputStream in = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + DESCRIPTION_BULK_UPLOAD_DOCUMENT_NAME);

        try {
            String extension = FilenameUtils.getExtension(descriptionBulkUploadFile.getOriginalFilename());
            if (excelExtentions.contains(extension)) {
                File file = new File("" + System.currentTimeMillis() + "." + extension);
                FileUtils.writeByteArrayToFile(file, descriptionBulkUploadFile.getBytes());
                in = excelService.getDescriptionBulkUploadTemplate(file);
                log.info("Input file  are process with record ::: ");

                FileUtils.deleteQuietly(file);
            }

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(new InputStreamResource(in));
        } catch (Exception exception) {
            log.error("Exception in downloading the sample template for description bulk upload is :: ", exception);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }


    @PostMapping(value = "/download/description/zip")
    public ResponseEntity<String> processZipFile(@RequestParam("descriptionBulkUploadFile") MultipartFile descriptionBulkUploadFile) {
        ByteArrayInputStream in = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + DESCRIPTION_BULK_UPLOAD_DOCUMENT_NAME);

        try {
            String extension = FilenameUtils.getExtension(descriptionBulkUploadFile.getOriginalFilename());
            if ("zip".equalsIgnoreCase(extension)) {
                File zipFile = new File("" + System.currentTimeMillis() + "." + extension);
                FileUtils.writeByteArrayToFile(zipFile, descriptionBulkUploadFile.getBytes());
                unZipFiles(zipFile);
                processFile();
                mergeFiles();
                FileUtils.deleteQuietly(zipFile);
                FileUtils.deleteQuietly(new File("unzip"));

            }
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body("Success..!!");
        } catch (Exception exception) {
            log.error("Exception in downloading the sample template for description bulk upload is :: ", exception);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
    }

    private void processFile() throws IOException {
        File unzip = new File("unzip");
        File processed = new File("unzip/processed");
        FileUtils.forceMkdir(processed);
        Collection<File> files = FileUtils.listFiles(unzip,
                new String[]{"xls", "xlsx"}, false);
        LinkedList<File> listOfFiles = new LinkedList<>(files);
        for (File file : listOfFiles) {
            ByteArrayInputStream in = excelService.getDescriptionBulkUploadTemplate(file);
            if (in == null) {
                continue;
            }
            FileUtils.copyInputStreamToFile(in, new File("unzip/processed/"+file.getName()));
            log.info("Input file  are process with record ::: ");
        }

    }

    private void mergeFiles() throws Exception {
        File processed = new File("unzip/processed");
        Collection<File> files = FileUtils.listFiles(processed,
                new String[]{"xls", "xlsx"}, false);
        List<File> newFilesOrder = files.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        LinkedList<Sheet> listOfSheet = new LinkedList<>();
        LinkedList<Sheet> listOfSheetWithoutSix = new LinkedList<>();
        newFilesOrder.stream().forEach(file -> {
            try {
                System.out.println("File names : " + file.getAbsolutePath());
                InputStream inputStream = new FileInputStream(file);
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                Sheet sheet = workbook.getSheet(ExcelService.DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME);

                if (sheet != null)
                    listOfSheet.add(sheet);
                Sheet sheetWithoutSix = workbook.getSheet(ExcelService.DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME_SIX);
                if (sheetWithoutSix != null)
                    listOfSheetWithoutSix.add(sheetWithoutSix);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        XSSFWorkbook workbook = new XSSFWorkbook();
        //InputStream
        copySheets.mergeExcelFilesSheet(workbook, listOfSheet);
        copySheets.mergeExcelFilesWithoutSix(workbook, listOfSheetWithoutSix);

//        process(workbook);
        FileOutputStream out = new FileOutputStream(
                new File("finalOutputSheet.xlsx"));
        workbook.write(out);
        out.close();
        System.out.println("finalsheet.xlsx written successfully");


        File file =new File("finalOutputSheet.xlsx");
        shift(file);
        File file1 =new File("juni1.xlsx");
        shiftnew(file1);


    }

    private void shiftnew(File f) {

        File F=f;
        XSSFWorkbook wb = null;
        XSSFSheet sheet=null;
        try{

            FileInputStream is=new FileInputStream(F);

            wb= new XSSFWorkbook(is);
            sheet = wb.getSheetAt(1);
            for(int i = 0; i < sheet.getLastRowNum(); i++){
                boolean isRowEmpty=false;
                if(sheet.getRow(i)==null){
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                    i--;
                    continue;
                }
                for(int j =0; j<sheet.getRow(i).getLastCellNum();j++){
                    if(sheet.getRow(i).getCell(j).toString().trim().equals("")){
                        isRowEmpty=true;
                    }else {
                        isRowEmpty=false;
                        break;
                    }
                }
                if(isRowEmpty==true){
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                    i--;
                }
            }

            FileOutputStream fileOut = new FileOutputStream("newjuni1.xlsx");
            wb.write(fileOut);
            fileOut.close();
            //Here I want to write the new update file without empty rows!
        }
        catch(Exception e){
            System.out.print("SERRO "+e);
            e.printStackTrace();
        }

    }

    private void shift(File f){
        File F=f;
        XSSFWorkbook wb = null;
        XSSFSheet sheet=null;
        try{

            FileInputStream is=new FileInputStream(F);

            wb= new XSSFWorkbook(is);
            sheet = wb.getSheetAt(0);
            for(int i = 0; i < sheet.getLastRowNum(); i++){
                boolean isRowEmpty=false;
                if(sheet.getRow(i)==null){
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                    i--;
                    continue;
                }
                for(int j =0; j<sheet.getRow(i).getLastCellNum();j++){
                    if(sheet.getRow(i).getCell(j).toString().trim().equals("")){
                        isRowEmpty=true;
                    }else {
                        isRowEmpty=false;
                        break;
                    }
                }
                if(isRowEmpty==true){
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                    i--;
                }
            }

            FileOutputStream fileOut = new FileOutputStream("juni1.xlsx");
            wb.write(fileOut);
            fileOut.close();
            //Here I want to write the new update file without empty rows!
        }
        catch(Exception e){
            System.out.print("SERRO "+e);
        }

    }




    private void unZipFiles(File zipFile) throws IOException {

        ZipInputStream zis =
                new ZipInputStream(new FileInputStream(zipFile));

        byte[] buffer = new byte[1024];
        ZipEntry ze = zis.getNextEntry();

        while (ze != null) {

            String fileName = ze.getName();
            File newFile = new File("unzip" + File.separator + fileName);

            System.out.println("file unzip : " + newFile.getAbsoluteFile());

            new File(newFile.getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

        System.out.println("Done");
    }





    @PostMapping(value = "/upload/removeGaps")
    public ResponseEntity<InputStreamResource> uploadDescriptionBulkUploadTemplate() {
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + DESCRIPTION_BULK_UPLOAD_DOCUMENT_NAME);

            File file =new File("finalOutputSheet.xlsx");
            shift(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(new InputStreamResource(in));
        } catch (Exception exception) {
            log.error("Exception in downloading the sample template for description bulk upload is :: ", exception);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
