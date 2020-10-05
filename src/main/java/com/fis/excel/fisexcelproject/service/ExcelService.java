package com.fis.excel.fisexcelproject.service;

import com.fis.excel.fisexcelproject.InputBean;
import com.fis.excel.fisexcelproject.config.ProjectConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExcelService {

    public static final String FILE_NAME_SEPARATOR = "_";

    @Autowired
    private ProjectConfig projectConfig;


    public static final String DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME = "Line Item";
    public static final String DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME_SIX=   "Header";
    private static final String CUSTOMER_IND = "Customer IND";
    private static final String ORACLE_GL = "Oracle GL";
    private static final String LINE_NO = "Line No";
    private static final String GC_DATE = "GL Date";
    private static final String COMPANY = "Company";
    private static final String TRANSACTION_TYPE = "TransactionType";
    private static final String[] input_column_names =
            {"Customer Name", "Customer Number", "SAP GL", "Description (Optional)",
            "Invoice Number", "Invoice Date", "Currency", "Payment Terms", "Bill Description"
                    , "Value", "DR/Cr", "SAP (Profit Centre)", "New Product", "New CC",
                     "New Cust","New LOC", "Document Header", "Reference Number",
                    "Line Item Text Part 1", "Line Item Text Part 2"
            };

    private static final String[] output_column_names =
            {"Customer Name", "Customer Number",CUSTOMER_IND, "SAP GL",ORACLE_GL,LINE_NO, "Description (Optional)",
                    "Invoice Number", "Invoice Date",GC_DATE, "Currency", "Payment Terms", "Bill Description"
                    , "Value", "DR/Cr", "SAP (Profit Centre)", "New Product", "New CC",
                     "New Cust","New LOC",COMPANY,TRANSACTION_TYPE, "Document Header", "Reference Number",
                    "Line Item Text Part 1", "Line Item Text Part 2"
            };

    private static CellStyle createHeaderCellStyle(XSSFWorkbook workbook) {
        // Create a Font for styling header cells
        Font headerFont = workbook.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerFont.setFontHeightInPoints((short) 14);
        headerFont.setColor(IndexedColors.RED.getIndex());

        // Create a CellStyle with the font
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        return headerCellStyle;
    }


    public ByteArrayInputStream getDescriptionBulkUploadTemplate(File descriptionBulkFile) throws IOException {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook();

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            CellStyle headerCellStyle = createHeaderCellStyle(workbook);
            CreationHelper createHelper = workbook.getCreationHelper();
            Sheet sheet = workbook.getSheet(DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.createSheet(DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME);
            }
            Sheet sheetsix = workbook.getSheet(DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME_SIX);
            if (sheetsix == null) {
                sheetsix = workbook.createSheet(DESCRIPTION_BULK_UPLOAD_DOCUMENT_SHEET_NAME_SIX);
            }

            List<InputBean> inputBeans = processInBackground(descriptionBulkFile);
            // Create a Row
            List<InputBean> inputBeanWithExactSix = inputBeans.stream().filter(bean -> {
                return bean.getSapGL().length() == 6;
            }).collect(Collectors.toList());

            List<InputBean> inputBeanWithNotExactSix =inputBeans.stream().filter(bean -> {
                return bean.getSapGL().length() != 6;
            }).map(inputBean -> {
                if(inputBean.getDrCr().equalsIgnoreCase("DR")){
                    inputBean.setValue(inputBean.getValue().replaceFirst("-",""));
                }else if(inputBean.getDrCr().equalsIgnoreCase("CR")){
                    inputBean.setValue("-"+inputBean.getValue());
                }
                return inputBean;
            }).collect(Collectors.toList());


            createASheet(headerCellStyle, sheet, inputBeanWithExactSix);
            createASheet(headerCellStyle, sheetsix, inputBeanWithNotExactSix);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception exception) {
            log.error("Exception in preparing the sample template for bulk upload of description is :: ", exception);
        }
        return null;
    }

    private void createASheet(CellStyle headerCellStyle, Sheet sheet, List<InputBean> inputBeans) {
        Row headerRow = sheet.createRow(0);

        for (int column = 0; column < output_column_names.length; column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(output_column_names[column]);
            cell.setCellStyle(headerCellStyle);
        }
        int rownum = 1;

        for (InputBean bean : inputBeans)
        {
            Row row = sheet.createRow(rownum++);
            createList(bean, row);

        }
        sheet.createRow(rownum++);
        sheet.createRow(rownum++);
    }

    private void createList(InputBean bean, Row row) {
        Cell cell = row.createCell(0);
        cell.setCellValue(bean.getCustomerName());

        cell = row.createCell(1);
        cell.setCellValue(bean.getCustomerNo());

        cell = row.createCell(2);
        cell.setCellValue(bean.getCustomerInd());

        cell = row.createCell(3);
        cell.setCellValue(bean.getSapGL());

        cell = row.createCell(4);
        if(StringUtils.isNotBlank(bean.getOracleGl())){
            cell.setCellValue(Integer.parseInt(bean.getOracleGl().trim()));
        }else {
            cell.setCellValue(bean.getOracleGl());
        }


        cell = row.createCell(5);
        if(StringUtils.isNotBlank(bean.getLineNo())){
            cell.setCellValue(Integer.parseInt(bean.getLineNo().trim()));
        }else {
            cell.setCellValue(bean.getLineNo());
        }

        cell = row.createCell(6);
        cell.setCellValue(bean.getDescription());
        cell = row.createCell(7);
        cell.setCellValue(bean.getInvoiceNumber());
        cell = row.createCell(8);
        cell.setCellValue(bean.getInvoiceDate());
        cell = row.createCell(9);
        cell.setCellValue(bean.getGlDate());
        cell = row.createCell(10);
        cell.setCellValue(bean.getCurrency());
        cell = row.createCell(11);
        cell.setCellValue(bean.getPaymentTerm());
        cell = row.createCell(12);
        cell.setCellValue(bean.getBillDescription());

        cell = row.createCell(13);
        BigDecimal bd = new BigDecimal(bean.getValue()).setScale(2, RoundingMode.HALF_UP);
        cell.setCellValue(bd.doubleValue());


        cell = row.createCell(14);
        cell.setCellValue(bean.getDrCr());
        cell = row.createCell(15);
        cell.setCellValue(bean.getSapProfileCentre());

        cell = row.createCell(16);
        if(StringUtils.isNotBlank(bean.getNewProduct())){
            cell.setCellValue(Integer.parseInt(bean.getNewProduct().trim()));
        }


        cell = row.createCell(17);
        cell.setCellValue(bean.getNewCC());
        cell = row.createCell(18);
        cell.setCellValue(bean.getNewCust());
        cell = row.createCell(19);
        cell.setCellValue(bean.getNewLoc());

        cell = row.createCell(20);
        cell.setCellValue(Integer.parseInt(bean.getCompany().trim()));

        cell = row.createCell(21);
        cell.setCellValue(bean.getTransactionType());

        cell = row.createCell(22);
        cell.setCellValue(bean.getDocumentHeader());

        cell = row.createCell(23);
        cell.setCellValue(bean.getReferenceNumber());

        cell = row.createCell(24);
        cell.setCellValue(bean.getLineTextPart1());

        cell = row.createCell(25);
        cell.setCellValue(bean.getLineTextPart2());

    }


    public List<InputBean> processInBackground(File descriptionBulkFile) throws IOException {
        log.info("Started description bulk upload processing for file name : " + descriptionBulkFile.getName());
        XSSFWorkbook workbook = new XSSFWorkbook(FileUtils.openInputStream(descriptionBulkFile));
        XSSFSheet worksheet = workbook.getSheetAt(0);
        List<InputBean> listOfInputBean = new LinkedList<>();
        List<InputBean> listOfInputBeanMoreThanSixGL = new LinkedList<>();

        for (int excelRow = 1; excelRow < worksheet.getPhysicalNumberOfRows(); excelRow++) {
            XSSFRow row = worksheet.getRow(excelRow);
            if (row == null) {
                break;
            }
            InputBean inputBean = processRowForDescriptionUpdate(row, descriptionBulkFile);

            if (inputBean != null && inputBean.getSapGL().length() == 6) {
                listOfInputBean.add(inputBean);
            }else if (inputBean != null){
                listOfInputBeanMoreThanSixGL.add(inputBean);
            }
        }
        LinkedHashMap<String,List<InputBean>> mapOfBeans =new LinkedHashMap<>();
        List<InputBean> finalListOfCollection = listOfInputBean.stream().sorted((a, b) -> {
            return a.getInvoiceNumber().compareTo(b.getInvoiceNumber());
        }).collect(Collectors.toList());
        String prevInvoiceNumber ="";
        int lineNumber =1;
        for(InputBean bean : finalListOfCollection){
            if(!prevInvoiceNumber.equalsIgnoreCase(bean.getInvoiceNumber())){
                lineNumber=1;
                prevInvoiceNumber =bean.getInvoiceNumber();
            }
            bean.setLineNo(""+lineNumber);
            lineNumber++;
        }
        finalListOfCollection.addAll(listOfInputBeanMoreThanSixGL);
        return finalListOfCollection;

    }


    private InputBean processRowForDescriptionUpdate(XSSFRow row, File descriptionBulkFile) {
        InputBean inputBean = null;
        if (row.getPhysicalNumberOfCells() >= input_column_names.length) {
            inputBean = InputBean.builder()
                    .customerName(row.getCell(0).getStringCellValue())
                    .customerNo("" + row.getCell(1))
                    .sapGL("" + row.getCell(2))
                    .description(row.getCell(3).getStringCellValue())
                    .invoiceNumber(row.getCell(4).getStringCellValue())
                    .invoiceDate(row.getCell(5).getStringCellValue())
                    .currency(row.getCell(6).getStringCellValue())
                    .paymentTerm(row.getCell(7).getStringCellValue())
                    .billDescription(row.getCell(8).getStringCellValue())
                    .value("" + row.getCell(9))
                    .drCr(row.getCell(10).getStringCellValue())
                    .sapProfileCentre("" + row.getCell(11))
                    .newProduct(row.getCell(12).getStringCellValue())
                    .newCC(row.getCell(13).getStringCellValue())
                    .newCust(row.getCell(14).getStringCellValue())
                    .newLoc(row.getCell(15).getStringCellValue())
                    .documentHeader(row.getCell(16).getStringCellValue())
                    .referenceNumber(row.getCell(17).getStringCellValue())
                    .lineTextPart1(row.getCell(18).getStringCellValue())
                    .lineTextPart2(row.getCell(19).getStringCellValue())
                    .company("2169").build();
            inputBean.setCustomerInd("IND-" + inputBean.getCustomerNo());
            inputBean.setNewProduct(inputBean.getNewProduct().replaceFirst("P", ""));
            if (inputBean.getInvoiceNumber().contains("CN")) {
                inputBean.setTransactionType("2169 CM");
            } else {
                inputBean.setTransactionType("2169 INV");
            }
            if (inputBean.getDrCr().contains("CR")) {
                inputBean.setPaymentTerm("NET 30");
            } else {
                inputBean.setPaymentTerm("");
                inputBean.setValue("-" + inputBean.getValue());
            }
            if (inputBean.getSapGL().startsWith("201")) {
                inputBean.setNewLoc("0000");
            } else {
                inputBean.setNewLoc(inputBean.getNewLoc().replaceFirst("L", ""));
            }

            if((inputBean.getSapGL().startsWith("201") || inputBean.getSapGL().startsWith("208375")) && inputBean.getDescription().contains("CGST")){
                inputBean.setOracleGl("208375");
                inputBean.setNewProduct("500000");
            }else if ((inputBean.getSapGL().startsWith("201") || inputBean.getSapGL().startsWith("208372")) && inputBean.getDescription().contains("SGST")){
                inputBean.setOracleGl("208372");
                inputBean.setNewProduct("500000");
            }else if ((inputBean.getSapGL().startsWith("201") || inputBean.getSapGL().startsWith("208378")) && inputBean.getDescription().contains("IGST")){
                inputBean.setOracleGl("208378");
                inputBean.setNewProduct("500000");
            }else if ((inputBean.getSapGL().startsWith("201") || inputBean.getSapGL().startsWith("208382")) && inputBean.getDescription().contains("UTGST")){
                inputBean.setOracleGl("208382");
                inputBean.setNewProduct("500000");
            }

            inputBean.setReferenceNumber(StringUtils.left(inputBean.getReferenceNumber(), 25));
            inputBean.setLineTextPart1(StringUtils.left(inputBean.getLineTextPart1(), 25));
            inputBean.setLineTextPart2(StringUtils.left(inputBean.getLineTextPart2(), 25));

            inputBean.setGlDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy")));
            inputBean.setCurrency("INR");
            log.info("Processing file record  : " + inputBean);

//            if (inputBean.getSapGL().length() == 6) {
//                return inputBean;
//            }

        } else {
            log.error("Number of rows are less than column specified in processing row with files name : " + descriptionBulkFile.getName() + " Skipping row Number : " + row.getRowNum());
        }
        return inputBean;
    }

}
