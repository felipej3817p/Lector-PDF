import sys

with open(r'c:\Users\andre\Downloads\Lector-PDF-CODEX-PR4\alturas-backend\src\main\java\com\backend\service\ReportService.java', 'r', encoding='utf-8') as f:
    content = f.read()

target = """        for (Employee employee : employees) {
            List<ManagedDocument> documents = managedDocumentRepository
                    .findByEmployeeIdOrderByUploadedAtDesc(employee.getId());

                if (to != null && conceptDate.isAfter(to)) {
                    continue;
                }

                employeeRows.add(new AptitudeReportRow("""

replacement = """        for (Employee employee : employees) {
            List<ManagedDocument> documents = managedDocumentRepository
                    .findByEmployeeIdOrderByUploadedAtDesc(employee.getId());

            List<AptitudeReportRow> employeeRows = new ArrayList<>();

            for (ManagedDocument document : documents) {
                Optional<DocumentAnalysis> analysisOptional = documentAnalysisRepository
                        .findByDocumentId(document.getId());

                if (analysisOptional.isEmpty()) {
                    continue;
                }

                DocumentAnalysis analysis = analysisOptional.get();
                String resultStatus = normalizeResultStatus(analysis.getResultStatus());

                if (!resultStatusFilter.isBlank()) {
                    if (!resultStatus.equals(resultStatusFilter)) {
                        continue;
                    }
                } else if (!"APTO".equals(resultStatus) && !"NO_APTO".equals(resultStatus)) {
                    continue;
                }

                LocalDate conceptDate = resolveConceptDate(document, analysis);

                if ((from != null || to != null) && conceptDate == null) {
                    continue;
                }

                if (from != null && conceptDate.isBefore(from)) {
                    continue;
                }

                if (to != null && conceptDate.isAfter(to)) {
                    continue;
                }

                employeeRows.add(new AptitudeReportRow("""

if target in content:
    content = content.replace(target, replacement)
    with open(r'c:\Users\andre\Downloads\Lector-PDF-CODEX-PR4\alturas-backend\src\main\java\com\backend\service\ReportService.java', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
