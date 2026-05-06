package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "document_batches")
public class DocumentBatch {
    @Id private String id;
    private String batchCode; private String uploadedBy; private LocalDateTime uploadedAt;
    private int totalFiles; private int successCount; private int failedCount; private int pendingReviewCount;
    private int aptCount; private int notAptCount; private int errorCount;
    private String status; private String notes;
    private String approverNotificationStatus; private LocalDateTime approverNotifiedAt; private String approverNotificationError;
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getBatchCode(){return batchCode;} public void setBatchCode(String v){batchCode=v;}
    public String getUploadedBy(){return uploadedBy;} public void setUploadedBy(String v){uploadedBy=v;}
    public LocalDateTime getUploadedAt(){return uploadedAt;} public void setUploadedAt(LocalDateTime v){uploadedAt=v;}
    public int getTotalFiles(){return totalFiles;} public void setTotalFiles(int v){totalFiles=v;}
    public int getSuccessCount(){return successCount;} public void setSuccessCount(int v){successCount=v;}
    public int getFailedCount(){return failedCount;} public void setFailedCount(int v){failedCount=v;}
    public int getPendingReviewCount(){return pendingReviewCount;} public void setPendingReviewCount(int v){pendingReviewCount=v;}
    public int getAptCount(){return aptCount;} public void setAptCount(int v){aptCount=v;}
    public int getNotAptCount(){return notAptCount;} public void setNotAptCount(int v){notAptCount=v;}
    public int getErrorCount(){return errorCount;} public void setErrorCount(int v){errorCount=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getApproverNotificationStatus(){return approverNotificationStatus;} public void setApproverNotificationStatus(String v){approverNotificationStatus=v;}
    public LocalDateTime getApproverNotifiedAt(){return approverNotifiedAt;} public void setApproverNotifiedAt(LocalDateTime v){approverNotifiedAt=v;}
    public String getApproverNotificationError(){return approverNotificationError;} public void setApproverNotificationError(String v){approverNotificationError=v;}
}
