/*
Copyright (C) 2022 Cardiff University

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

*/

package org.dcom.compliancedocumentservice;

import org.dcom.core.security.ServiceCertificate;
import org.dcom.core.servicehelper.CORSFilter;
import org.dcom.core.DCOM;
import org.slf4j.Logger;
import java.io.File;
import org.slf4j.LoggerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.dcom.core.servicehelper.ServiceBaseInfo;
import javax.ws.rs.ApplicationPath;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.dcom.core.servicehelper.UserAuthorisationValidator;
import org.dcom.core.servicehelper.KeycloakUserAuthorisationValidator;
import org.dcom.core.services.ServiceLookup;
import org.dcom.compliancedocumentservice.orientdb.OrientDBComplianceDocumentDatabase;

/**
*The startup class of the ComplianceDocument web service, this configures, sets global variables and then starts the restful web service.
*
*/
@ApplicationPath("/*") // set the path to REST web services
public class ComplianceDocumentService extends ResourceConfig {
  
    private static final Logger LOGGER = LoggerFactory.getLogger( ComplianceDocumentService.class );

    public ComplianceDocumentService() {
        //creat the service certificate
        if (!DCOM.checkDCOMCertificatePassword() || !DCOM.checkDCOMCertificatePath()) {
          LOGGER.error("Certificate Variables Not Defined");
          System.exit(0);
        }
        ServiceCertificate myCert=null;
        try {
          myCert=new ServiceCertificate(new File(DCOM.getDCOMCertificatePath()),DCOM.getDCOMCertificatePassword());
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }

        //create the database connection
        if (!DCOM.existsEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBURL") || !DCOM.existsEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBUsername") || !DCOM.existsEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBPassword") || !DCOM.existsEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBDatabase")) {
          LOGGER.error("OrientDB Connection Variables Not Defined");
          System.exit(0);
        }
        final ComplianceDocumentDatabase database=new OrientDBComplianceDocumentDatabase(DCOM.getEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBURL"),DCOM.getEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBUsername"),DCOM.getEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBPassword"),DCOM.getEnvironmentVariable("DCOM_ComplianceDocumentService_OrientDBDatabase"));

        //create base service info
        final ServiceBaseInfo serviceBaseInfo=new ServiceBaseInfo(ServiceBaseInfo.NAME,ServiceBaseInfo.DESCRIPTION,ServiceBaseInfo.OPERATOR,ServiceBaseInfo.SECURITY_SERVICE_TYPE,ServiceBaseInfo.SECURITY_SERVICE_URI,ServiceBaseInfo.HOSTNAME,ServiceBaseInfo.PORT);
        
        
        //create authenticator
        UserAuthorisationValidator authenticator=null;
        if (serviceBaseInfo.getProperty(ServiceBaseInfo.SECURITY_SERVICE_TYPE).equalsIgnoreCase("Keycloak")) {
           authenticator = new KeycloakUserAuthorisationValidator(serviceBaseInfo.getProperty(ServiceBaseInfo.SECURITY_SERVICE_URI));
        } else {
          LOGGER.error("Authentication Service Type Not Supported");
          System.exit(0);
        } 
    

        LOGGER.info("Registering to Service Lookup");
        ServiceLookup serviceLookup=DCOM.getServiceLookup();
        serviceLookup.registerMyself(ServiceLookup.COMPLIANCEDOCUMENTSERVICE,serviceBaseInfo.getProperty(ServiceBaseInfo.NAME),serviceBaseInfo.getProperty(ServiceBaseInfo.HOSTNAME),Integer.parseInt(serviceBaseInfo.getProperty(ServiceBaseInfo.PORT)),myCert.generateBearerToken());
        
        final UserAuthorisationValidator finalAuthenticator=authenticator;
        register(new CORSFilter());
        register(ComplianceDocumentAPI.class);
        register(new AbstractBinder() {
          @Override
          protected void configure() {
            bind(database).to(ComplianceDocumentDatabase.class);
            bind(finalAuthenticator).to(UserAuthorisationValidator.class);
            bind(serviceBaseInfo).to(ServiceBaseInfo.class);
          }
        });
    }
}