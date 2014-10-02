package sfdcFilesTool;

import java.io.IOException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.*;
import com.sforce.ws.util.Base64;

public class SfdcDownloadFiles {

    static final String authEndPointDefault = "https://test.salesforce.com/services/Soap/u/24.0/";
    static final String authEndPointSandBox = "https://test.salesforce.com/services/Soap/u/24.0/";
    static final String authEndPointProd = "https://login.salesforce.com/services/Soap/u/24.0/";
    static final String USERNAME = "";
    static final String PASSWORD = "";
    //static PartnerConnection connection;
    static PartnerConnection sfdcConnection;

//  ------------------------------------------------------------------------------------------------
    /**
     * @throws Exception
     * @Desc:   Main, Entry method
     * @Param:
     * @Return:
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Start Date/Time : " + (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")).format(new Date()));
        String authEndPoint = authEndPointDefault;
        SfdcDownloadFiles theCaller = new SfdcDownloadFiles();
        sfdcConnection = SfdcCommon.doLogin(USERNAME, PASSWORD, authEndPoint);
        String soqlQuery = "Select Id, Name From Account";
        String folderName = "D:/Temp/SFDC-Attachments/";

        if (sfdcConnection != null) {
            theCaller.processAttachmentsExport(folderName, soqlQuery);
            SfdcCommon.doLogout(sfdcConnection);
        }
        System.out.println("End Date/Time: " + (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")).format(new Date()));
    }

//  ------------------------------------------------------------------------------------------------
    /**
     * @throws Exception
     * @Desc:
     * @Param:
     * @Return:
     */
    private void processAttachmentsExport(String folderName, String soqlQuery) throws Exception {
        System.out.println("soqlQuery: " + soqlQuery);
        System.out.println();
        QueryResult queryResults = sfdcConnection.query(soqlQuery);
        boolean done = false;

        if (queryResults.getSize() > 0) {
            //System.out.println("The query returned % ", queryResults.getSize());
            System.out.format("The query returned %d records\n", queryResults.getSize());
            while (!done) {
                SObject[] records = queryResults.getRecords();
                for (int i = 0; i < records.length; ++i) {
                    SObject sObjResult = queryResults.getRecords()[i];
                    String parentIdStr = (String) sObjResult.getField("Id");
                    processDownloadAttachments(folderName, parentIdStr);
                }

                if (queryResults.isDone()) {
                    done = true;
                } else {
                    queryResults = sfdcConnection.queryMore(queryResults.getQueryLocator());
                }
            }
        } else {
            System.out.println("No records found for soqlQuery = " + soqlQuery);
            throw new Exception("No records found for soqlQuery = " + soqlQuery);
        }
    }

//  ------------------------------------------------------------------------------------------------
    /**
     * @Desc:
     * @Param:
     * @Return:
     */
    private void processDownloadAttachments(String folderName, String parentIdStr) {
        try {
            System.out.println("processMoveAttachments");

            QueryResult queryResults = sfdcConnection.query("Select Id, ParentId, Name, Description, ContentType, Body " +
                                       "From Attachment WHERE parentId = '" + parentIdStr + "'");
            if (queryResults.getSize() > 0 && queryResults.getSize() == 1) {
                // cast the SObject to a strongly-typed Contact
                SObject sObjResult = queryResults.getRecords()[0];
                String fileName = (String) sObjResult.getField("Name");

                String bodyStr = (String) sObjResult.getField("Body");

                String filePath = folderName + "/" + parentIdStr + "-" + fileName;

                writeOnDisk(filePath, bodyStr);

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

//  ------------------------------------------------------------------------------------------------
    /**
     * @Desc:
     * @Param:
     * @Return:
     */
    private void writeOnDisk(String filePath, String bodyStr) {
        try {
            System.out.println("Saving File: " + filePath);
            FileOutputStream fos = new FileOutputStream(filePath); //File OutPutStream is used to write Binary Contents like pictures
            byte[] bodyByte = bodyStr.getBytes();
            fos.write(Base64.decode(bodyByte));
            fos.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


//  ------------------------------------------------------------------------------------------------
}