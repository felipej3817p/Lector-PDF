package com.backend.dto.settings;

import java.util.LinkedHashMap;
import java.util.Map;

public class SystemSettingsRequest {

    private Boolean autoSendApproverEmail;

    private String frontendBaseUrl;

    private Integer userAuditRetentionMonths;

    private String approverEmails;
    private String approverCc;

    private String humanTalentEmails;
    private String payrollEmails;

    private Map<String, String> zoneCoordinatorEmails = new LinkedHashMap<>();

    public SystemSettingsRequest() {
    }

    public Boolean getAutoSendApproverEmail() {
        return autoSendApproverEmail;
    }

    public void setAutoSendApproverEmail(Boolean autoSendApproverEmail) {
        this.autoSendApproverEmail = autoSendApproverEmail;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public Integer getUserAuditRetentionMonths() {
        return userAuditRetentionMonths;
    }

    public void setUserAuditRetentionMonths(Integer userAuditRetentionMonths) {
        this.userAuditRetentionMonths = userAuditRetentionMonths;
    }

    public String getApproverEmails() {
        return approverEmails;
    }

    public void setApproverEmails(String approverEmails) {
        this.approverEmails = approverEmails;
    }

    public String getApproverCc() {
        return approverCc;
    }

    public void setApproverCc(String approverCc) {
        this.approverCc = approverCc;
    }

    public String getHumanTalentEmails() {
        return humanTalentEmails;
    }

    public void setHumanTalentEmails(String humanTalentEmails) {
        this.humanTalentEmails = humanTalentEmails;
    }

    public String getPayrollEmails() {
        return payrollEmails;
    }

    public void setPayrollEmails(String payrollEmails) {
        this.payrollEmails = payrollEmails;
    }

    public Map<String, String> getZoneCoordinatorEmails() {
        return zoneCoordinatorEmails;
    }

    public void setZoneCoordinatorEmails(Map<String, String> zoneCoordinatorEmails) {
        this.zoneCoordinatorEmails = zoneCoordinatorEmails;
    }
}
