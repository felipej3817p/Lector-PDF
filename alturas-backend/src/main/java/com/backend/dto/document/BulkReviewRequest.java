package com.backend.dto.document;

import java.util.List;

public class BulkReviewRequest {
    private List<String> documentIds;
    private String comment;
    public List<String> getDocumentIds(){return documentIds;} public void setDocumentIds(List<String> v){documentIds=v;}
    public String getComment(){return comment;} public void setComment(String v){comment=v;}
}
