package com.fis.excel.fisexcelproject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InputBean {
    //    {"Customer Name","Customer Number","SAP GL","Description (Optional)","Invoice Number","Invoice Date","Currency",
//   "Payment Terms","Bill Description","Value","DR/Cr","SAP (Profit Centre)","New Product","New CC","New LOC","New Cust"
//  ,"Document Header","Reference Number","Line Item Text Part 1","Line Item Text Part 2"};

    private String customerName;
    private String customerNo;
    private String sapGL;
    private String description;
    private String invoiceNumber;
    private String invoiceDate;
    private String currency;
    private String paymentTerm;
    private String billDescription;
    private String value;
    private String drCr;
    private String sapProfileCentre;
    private String newProduct;
    private String newCC;
    private String newLoc;
    private String newCust;
    private String documentHeader;
    private String referenceNumber;
    private String lineTextPart1;
    private String lineTextPart2;
    private String customerInd;
    private String oracleGl;
    private String lineNo;
    private String glDate;
    private String company;
    private String transactionType;


    //    private static final String DESCRIPTION_BULK_UPLOAD_PLAYLIST_ID_HEADER_NAME = "Customer IND";
//    private static final String DESCRIPTION_BULK_UPLOAD_LANGUAGE_CODE_HEADER_NAME = "Oracle GL";
//    private static final String DESCRIPTION_BULK_UPLOAD_LANGUAGE_DESCRIPTION_HEADER_NAME = "Line No";
//    private static final String DESCRIPTION_BULK_UPLOAD_LANGUAGE_DESCRIPTION_HEADER_NAME1 = "GL Date";
//    private static final String DESCRIPTION_BULK_UPLOAD_LANGUAGE_DESCRIPTION_HEADER_NAME2 = "Company";
//    private static final String DESCRIPTION_BULK_UPLOAD_LANGUAGE_DESCRIPTION_HEADER_NAME3 = "TransactionType";

}
