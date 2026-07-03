package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "user_audit_logs_user_changed_at_id_idx", def = "{'userId': 1, 'changedAt': -1, '_id': -1}"),
        @CompoundIndex(name = "user_audit_logs_changed_at_id_idx", def = "{'changedAt': -1, '_id': -1}")
})
public class UserAuditLog {

    @Id
    private String id;
    private String userId;
    private String username;
    private String modifiedBy;
    private String action;
    private String field;
    private Object previousValue;
    private Object newValue;
    private LocalDateTime changedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public Object getPreviousValue() { return previousValue; }
    public void setPreviousValue(Object previousValue) { this.previousValue = previousValue; }
    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
