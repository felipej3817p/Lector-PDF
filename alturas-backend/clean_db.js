db.getSiblingDB('ebsa_medical').managedDocument.deleteMany({});
db.getSiblingDB('ebsa_medical').documentAnalysis.deleteMany({});
db.getSiblingDB('ebsa_medical').emailLog.deleteMany({});
db.getSiblingDB('ebsa_medical').trainingCertificate.deleteMany({});
db.getSiblingDB('ebsa_medical').documentBatch.deleteMany({});
print("Database collections cleaned.");
