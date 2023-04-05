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

package org.dcom.compliancedocumentservice.orientdb;

import java.util.ArrayList;
import org.dcom.compliancedocumentservice.ComplianceDocumentIndexItem;
import org.dcom.core.compliancedocument.ComplianceDocument;
import org.dcom.core.compliancedocument.ComplianceItem;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.OrientDBConfigBuilder;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.exception.OAcquireTimeoutException;
import com.orientechnologies.orient.core.record.OElement;
import org.slf4j.Logger;
import java.util.HashMap;
import org.slf4j.LoggerFactory;
import org.dcom.compliancedocumentservice.ComplianceDocumentDatabase;


/**
*This class provides the implementation of the ComplianceDocumentDatabase interface for orientDB. This means that if a new database backend were to be utilised reimplementation of this class and any helpers is all that is required.
*
*/
public class OrientDBComplianceDocumentDatabase implements ComplianceDocumentDatabase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger( ComplianceDocumentDatabase.class );
	private  ODatabasePool dbPool;
	

	
	public OrientDBComplianceDocumentDatabase(String url,String username,String password,String database) {
    OrientDB db=new OrientDB("remote:"+url,username,password,OrientDBConfig.defaultConfig());
    OrientDBConfigBuilder poolCfg = OrientDBConfig.builder();
    poolCfg.addConfig(OGlobalConfiguration.CLIENT_CONNECT_POOL_WAIT_TIMEOUT,5000);
    poolCfg.addConfig(OGlobalConfiguration.CLIENT_DB_RELEASE_WAIT_TIMEOUT,5000);
    poolCfg.addConfig(OGlobalConfiguration.STORAGE_LOCK_TIMEOUT,5000);
    poolCfg.addConfig(OGlobalConfiguration.DB_POOL_IDLE_TIMEOUT,5000);
    poolCfg.addConfig(OGlobalConfiguration.DB_POOL_IDLE_CHECK_DELAY,1000);
    poolCfg.addConfig(OGlobalConfiguration.DB_POOL_ACQUIRE_TIMEOUT,1000);
		boolean create=false;
		if(!db.exists(database)) {
			db.create(database,ODatabaseType.PLOCAL);
			create=true;
		}
		dbPool=new ODatabasePool(db,database,username,password, poolCfg.build());
    LOGGER.info("Connecting to Database:"+url+":"+username);
		if (create) {
				ODatabaseSession session=getSession();
				session.command("CREATE CLASS ComplianceDocument extends V");
				session.command("CREATE CLASS Version extends V");
				session.command("CREATE CLASS Section extends V");
				session.command("CREATE CLASS Paragraph extends V");
				session.command("CREATE CLASS Figure extends V");
				session.command("CREATE CLASS Table extends V");
				session.command("CREATE CLASS TableGroup extends V");
				session.command("CREATE CLASS Row extends V");
				session.command("CREATE CLASS Cell extends V");
				session.command("CREATE CLASS TitleCell extends Cell");
				session.command("CREATE CLASS DataCell extends Cell");
				session.command("CREATE CLASS TableFooter extends TableGroup");
				session.command("CREATE CLASS TableHeader extends TableGroup");
				session.command("CREATE CLASS TableBody extends TableGroup");
				session.close();
		}
	}
	
	private synchronized ODatabaseSession getSession() {
    ODatabaseSession session=null;
    try {
      session= dbPool.acquire();
    }  catch (OAcquireTimeoutException e) {
      e.printStackTrace();
    }
    return session;
  }

	public ArrayList<ComplianceDocumentIndexItem> getDocumentIndex() {
		return getDocumentIndex(null,null);
	}
	
	public ArrayList<ComplianceDocumentIndexItem> getDocumentIndex(String jurisdiction) {
			return getDocumentIndex(jurisdiction,null);
	}
	
	public ArrayList<ComplianceDocumentIndexItem> getDocumentIndex(String jurisdiction,String type) {
		ODatabaseSession session=getSession();
		ArrayList<ComplianceDocumentIndexItem> results = new ArrayList<ComplianceDocumentIndexItem>();
		String q="select @rid from ComplianceDocument";
		if (jurisdiction!=null || type!=null) q+=" where ";
		if (jurisdiction!=null) q+="spatialCoverage contains '"+jurisdiction+"'";
		if (type!=null && jurisdiction!=null) q+=" and ";
		if (type!=null) q+=" type='"+type+"'";
		OResultSet rs = session.command(q);
		while (rs.hasNext()) {
				OResult r=rs.next();
				results.add(getDocumentIndexData(session,r.getProperty("@rid").toString()));
		}
		session.close();
		return results;
	}
	

	private ComplianceDocumentIndexItem getDocumentIndexData(ODatabaseSession session,String dId) {
		OResultSet rs = session.command("select from "+dId);
		if (rs.hasNext()) {
			OResult r=rs.next();
			String uid=r.getProperty("identifier").toString();
			String shortName=r.getProperty("shortName").toString();
			String fullName=r.getProperty("title").toString();
			String language=r.getProperty("language").toString().replace("[","").replace("]","");
			String documentType=r.getProperty("type").toString();
			String jurisdiction=r.getProperty("spatialCoverage").toString().replace("[","").replace("]","");
			String embeddedLogic=null;
			if (r.hasProperty("embeddedLogic")) embeddedLogic=r.getProperty("embeddedLogic").toString();
			String latestVersion=null;
			String latestVersionDate=null;
			OResultSet rs2 = session.command("select from Version where partOf="+dId+" order by versionDate DESC");
			boolean first=true;
			ArrayList<HashMap<String,String>> versions=new ArrayList<HashMap<String,String>>();
			while (rs2.hasNext()) {
				OResult r2=rs2.next();
				HashMap<String,String> versionInfo=new HashMap<String,String>();
					if (first){
						latestVersion=r2.getProperty("versionName").toString();
						latestVersionDate=r2.getProperty("versionDate").toString();
						first=false;
					}
				String version=r2.getProperty("versionName").toString();
				String versionDate=r2.getProperty("versionDate").toString();
				versionInfo.put("versionName",version);
				versionInfo.put("versionDate",versionDate);
				versions.add(versionInfo);
					
			}
			return new ComplianceDocumentIndexItem(uid,shortName,fullName,documentType,jurisdiction,embeddedLogic,latestVersion,latestVersionDate,versions);
		}
		return null;
	}
	
	private String getDocumentId(String jurisdiction,String type,String shortName) {
		ODatabaseSession session=getSession();
		ArrayList<ComplianceDocumentIndexItem> results = new ArrayList<ComplianceDocumentIndexItem>();
		String q="select @rid from ComplianceDocument where spatialCoverage contains '"+jurisdiction+"'";
		q+=" and type='"+type+"' and shortName='"+shortName+"'";
		OResultSet rs = session.command(q);
		String documentId=null;
		if (rs.hasNext()) {
				OResult r=rs.next();
				documentId=r.getProperty("@rid").toString();
		}
		session.close();
		return documentId;
	}
	
	
	public String getLatestVersion(String jurisdiction,String type,String shortName) {
		String dId=getDocumentId(jurisdiction,type,shortName);
		ODatabaseSession session=getSession();
		OResultSet rs = session.command("select from Version where partOf="+dId+" order by versionDate DESC limit 1");
		String versionName=null;
		if (rs.hasNext()) {
			OResult r=rs.next();
			versionName=r.getProperty("versionName").toString();
		}
		session.close();
		return versionName;
	}
	
	
	public ComplianceDocument getDocument(String baseURI,String jurisdiction,String type,String shortName,String version) {
		String dId=getDocumentId(jurisdiction,type,shortName);
		ODatabaseSession session=getSession();
		String url=baseURI+"/"+jurisdiction+"/"+type+"/"+shortName+"/"+version;
		ComplianceDocument doc=OrientDBComplianceDocumentDeserialiser.parseComplianceDocument(url,session,dId,version);
		session.close();
		return doc;
	}
	
	public boolean checkVersionExists(String jurisdiction,String type,String shortName,String version) {
			String dId=getDocumentId(jurisdiction,type,shortName);
			ODatabaseSession session=getSession();
			OResultSet rs = session.command("select from Version where partOf="+dId+" and versionName='"+version+"'");
			boolean response=rs.hasNext();
			session.close();
			return response;
	}
	

	
	public void updateDocument(String jurisdiction,String type,String shortName,ComplianceDocument inDoc) throws Exception {
		String dId=getDocumentId(jurisdiction,type,shortName);
		if (dId==null) {
				//we need to create the document
				ODatabaseSession session=getSession();
				dId=OrientDBHelpers.getInsertId(session.command("create vertex ComplianceDocument"));
				OrientDBComplianceDocumentSerialiser.updateMetaData(session,dId,inDoc);
				StringBuffer str=new StringBuffer();
				str.append("update ").append(dId).append(" set spatialCoverage=['").append(jurisdiction).append("'], type='").append(type).append("', shortName='").append(shortName).append("'");
				session.command(str.toString());
				session.close();
				
		}
		System.out.println(getLatestVersion(jurisdiction,type,shortName)+":"+inDoc.getVersion());
		if (!checkVersionExists(jurisdiction,type,shortName,inDoc.getVersion())) {
			//check if this version exists and if it does not create it as a clone
			String oVersion=getLatestVersion(jurisdiction,type,shortName);
			
			ODatabaseSession session=getSession();

			OResultSet rs = session.command("select from Version where partOf="+dId+" and versionName='"+oVersion+"'");
			if (rs.hasNext()) {
					OResult r=rs.next();
					StringBuffer str=new StringBuffer();
					str.append("create vertex Version set versionName='").append(inDoc.getVersion()).append("', versionDate=date(),");
					str.append(" replaces=").append(r.getProperty("@rid").toString()).append(", partOf=").append(dId).append(", sections=[");
					ArrayList<Object> consistsOf=(ArrayList<Object>)r.getProperty("sections");
					boolean first=true;
					for (Object section:consistsOf) {
						if (first) first=false;
						else str.append(",");
						OElement element=(OElement)section;
						str.append(element.getIdentity().toString());
					}
					str.append("]");
					String nId=OrientDBHelpers.getInsertId(session.command(str.toString()));
					session.command("update "+r.getProperty("@rid").toString()+" set replacedBy="+nId);
			} else {
				//ok this is the first version of this document
				StringBuffer str=new StringBuffer();
				str.append("create vertex Version set versionName='").append(inDoc.getVersion()).append("', versionDate=date()").append(", partOf=").append(dId);
				System.out.println(str.toString());
				session.command(str.toString());
			}
			session.close();
		} 
		ODatabaseSession session=getSession();
		OrientDBComplianceDocumentSerialiser.serialise(session,dId,inDoc);
		session.close();
	}

}