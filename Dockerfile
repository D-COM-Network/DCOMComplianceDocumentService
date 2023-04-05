FROM tomcat:9.0.44-jdk16-openjdk-buster

LABEL maintainer="beachth@cf.ac.uk"

RUN rm -rf /usr/local/webapps/*
ADD target/ComplianceDocumentService.war /usr/local/tomcat/webapps/ROOT.war
ADD ComplianceDocumentService.pem /opt/ComplianceDocumentService.pem
ENV DCOMCertificatePath /opt/ComplianceDocumentService.pem
ENV DCOMCertificatePassword a5b50932


EXPOSE 8080
CMD ["catalina.sh", "run"]
