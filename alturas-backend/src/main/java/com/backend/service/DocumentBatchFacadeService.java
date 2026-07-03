package com.backend.service;

import com.backend.model.DocumentBatch;
import com.backend.model.EmailLog;
import com.backend.model.ManagedDocument;
import com.backend.repository.DocumentBatchRepository;
import com.backend.repository.EmailLogRepository;
import com.backend.repository.ManagedDocumentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DocumentBatchFacadeService {
    private final DocumentBatchRepository batchRepository; private final ManagedDocumentRepository managedDocumentRepository; private final EmailLogRepository emailLogRepository;
    public DocumentBatchFacadeService(DocumentBatchRepository b, ManagedDocumentRepository m, EmailLogRepository e){this.batchRepository=b; this.managedDocumentRepository=m; this.emailLogRepository=e;}
    public DocumentBatch createBatch(String uploadedBy, int total){ DocumentBatch b=new DocumentBatch(); b.setBatchCode("BATCH-"+System.currentTimeMillis()); b.setUploadedBy(uploadedBy); b.setUploadedAt(LocalDateTime.now()); b.setTotalFiles(total); b.setStatus("PROCESSING"); b.setApproverNotificationStatus("OMITIDO"); return batchRepository.save(b);}    
    public void completeBatch(DocumentBatch b, List<Map<String,Object>> results){
        completeBatch(b, results, false);
    }
    public void completeBatch(DocumentBatch b, List<Map<String,Object>> results, boolean historical){
        int ok=0,err=0,apt=0,noApt=0,pending=0;
        for(var r:results){ if("OK".equals(r.get("status"))){ok++; String rs=String.valueOf(r.getOrDefault("resultStatus","")); if("APTO".equals(rs)) apt++; if("NO_APTO".equals(rs)) noApt++; if(!historical) pending++;} else err++; }
        b.setSuccessCount(ok); b.setFailedCount(err); b.setErrorCount(err); b.setAptCount(apt); b.setNotAptCount(noApt); b.setPendingReviewCount(pending);
        b.setStatus(historical ? (err>0?"HISTORICAL_PARTIAL_ERROR":"HISTORICAL_COMPLETED") : (err>0?"PARTIAL_ERROR":"PENDING_REVIEW")); batchRepository.save(b);
    }
    public List<DocumentBatch> findAll(){ return batchRepository.findAllByOrderByUploadedAtDesc(); }
    public DocumentBatch findById(String id){ return batchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Lote no encontrado")); }
    public DocumentBatch save(DocumentBatch batch){ return batchRepository.save(batch); }
    public List<ManagedDocument> getBatchDocuments(String id){ return managedDocumentRepository.findByBatchIdOrderByUploadedAtDesc(id); }
    public List<EmailLog> getBatchEmails(String id){ return emailLogRepository.findByBatchId(id); }
}
