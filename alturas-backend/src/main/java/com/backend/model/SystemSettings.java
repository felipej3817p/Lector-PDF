    package com.backend.model;

    import org.springframework.data.annotation.Id;
    import org.springframework.data.mongodb.core.mapping.Document;

    import java.time.LocalDateTime;
    import java.util.LinkedHashMap;
    import java.util.Map;

    @Document(collection = "system_settings")
    public class SystemSettings {

        public static final String GLOBAL_ID = "GLOBAL";

        @Id
        private String id = GLOBAL_ID;

        private boolean autoSendApproverEmail = true;

        private String frontendBaseUrl = "";

        private int userAuditRetentionMonths = 12;

        /*
        * Primer envío:
        * PDFs cargados y analizados -> correo al aprobador.
        */
        private String approverEmails = "";
        private String approverCc = "";

        /*
        * Segundo envío:
        * Aprobador aprueba -> correo al trabajador + copias.
        */
        private String humanTalentEmails = "";
        private String payrollEmails = "";

        /*
        * Copia dinámica según zona del trabajador.
        * Ejemplo:
        * CENTRO -> coordinador.centro@empresa.com
        */
        private Map<String, String> zoneCoordinatorEmails = new LinkedHashMap<>();

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public SystemSettings() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isAutoSendApproverEmail() {
            return autoSendApproverEmail;
        }

        public void setAutoSendApproverEmail(boolean autoSendApproverEmail) {
            this.autoSendApproverEmail = autoSendApproverEmail;
        }

        public String getFrontendBaseUrl() {
            return frontendBaseUrl;
        }

        public void setFrontendBaseUrl(String frontendBaseUrl) {
            this.frontendBaseUrl = frontendBaseUrl;
        }

        public int getUserAuditRetentionMonths() {
            return userAuditRetentionMonths;
        }

        public void setUserAuditRetentionMonths(int userAuditRetentionMonths) {
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

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
