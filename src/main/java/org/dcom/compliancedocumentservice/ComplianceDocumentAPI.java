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

import javax.ws.rs.Path;
import javax.inject.Inject;
import org.dcom.core.servicehelper.ServiceBaseInfo;
import org.dcom.core.servicehelper.UserAuthorisationValidator;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import org.dcom.core.compliancedocument.serialisers.JSONComplianceDocumentSerialiser;
import org.dcom.core.compliancedocument.serialisers.XMLComplianceDocumentSerialiser;
import org.dcom.core.compliancedocument.deserialisers.JSONComplianceDocumentDeserialiser;
import org.dcom.core.compliancedocument.deserialisers.XMLComplianceDocumentDeserialiser;
import org.dcom.core.compliancedocument.ComplianceDocument;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ws.rs.core.MultivaluedMap;

/**
*This is the implementation of the REST API for the compliance document service
*
*/
@Path("/")
public class ComplianceDocumentAPI {


	@Inject
	public ComplianceDocumentDatabase database;

	@Inject
	public UserAuthorisationValidator authenticator;

	@Inject
	public ServiceBaseInfo serviceInfo;
	
	//utility functions
	
	private String successMessageJSON="{\"success\":true}";
	private String successMessageXML="<success>true</success>";
	private SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
	
	
	private String generateURL() {
		String prefix="http";
		String port = serviceInfo.getProperty(ServiceBaseInfo.PORT);
		String hostName = serviceInfo.getProperty(ServiceBaseInfo.HOSTNAME);
		if (port.equals("443")) prefix="https";
		if (hostName.contains(":") || port.equals("80") || port.equals("443")) {
			return prefix +"://"+hostName;
		} else {
			return prefix +"://"+hostName+":"+port;
		}
	}
	
	private boolean authorize(String token) {
		if (token==null) return false;
		return authenticator.validatePermission(token,"editor");
	}
	
	private ComplianceDocument manipulateDocument(ComplianceDocument doc, UriInfo contextInfo,String jurisdiction,String type,String shortName) {
			MultivaluedMap<String,String> queryParams=contextInfo.getQueryParameters();
			if (queryParams!=null && queryParams.keySet().size() >= 1) {
						String query=queryParams.getFirst("diff");
						String path="";
						if (query!=null) {
							ComplianceDocument document = database.getDocument(generateURL(),jurisdiction,type,shortName,query);
							doc=ComplianceDocumentDiff.diff(doc,document);
						}
						query=queryParams.getFirst("query");
						if (query!=null) {
							if (query.equals("structure")){
								doc=ComplianceDocumentFilter.filterBodies(doc);
							}
						}
			}
			return doc;
	}
	
	private String generateVersionString() {
		return dateFormat.format(new Date());
	}
	
	private ComplianceDocument getPreviousVersion(ComplianceDocumentDatabase database,String jurisdiction,String type,String shortName) {
			String oldVersion=database.getLatestVersion(jurisdiction,type,shortName);
			if (oldVersion==null) return null;
			return database.getDocument(generateURL(),jurisdiction,type,shortName,oldVersion);
	}

	//all query functions

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response serviceInfoJSON() {
		StringBuffer str=new StringBuffer();
		str.append("{");
		str.append(serviceInfo.toJSONContent());
		str.append(",\"documentList\":[");
		ArrayList<ComplianceDocumentIndexItem> documents=database.getDocumentIndex();
		for (int i=0; i < documents.size();i++) {
			if (i!=0) str.append(",");
			str.append(documents.get(i).toJSONContent());
		}
		str.append("]}");
		return  Response.ok(str.toString()).build();
	}
	
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_XML)
	public Response serviceInfoXML() {
		StringBuffer str=new StringBuffer();
		str.append("<ServerIdentity>");
		str.append(serviceInfo.toXMLContent());
		str.append("<DocumentList>");
		ArrayList<ComplianceDocumentIndexItem> documents=database.getDocumentIndex();
		for (int i=0; i < documents.size();i++) {
			str.append(documents.get(i).toXMLContent());
		}
		str.append("</DocumentList>");
		str.append("</ServerIdentity>");
		return  Response.ok(str.toString()).build();
	}
	
	@GET
	@Path("/{jurisdiction}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response jurisdictionJSON(@PathParam("jurisdiction") String jurisdiction) {
			ArrayList<ComplianceDocumentIndexItem> documents=database.getDocumentIndex(jurisdiction);
			StringBuffer str=new StringBuffer();
			str.append("[");
			for (int i=0; i < documents.size();i++) {
				if (i!=0) str.append(",");
				str.append(documents.get(i).toJSONContent());
			}
			str.append("]");
			return  Response.ok(str.toString()).build();
	}
	
	@GET
	@Path("/{jurisdiction}")
	@Produces(MediaType.APPLICATION_XML)
	public Response jurisdictionXML(@PathParam("jurisdiction") String jurisdiction) {
		ArrayList<ComplianceDocumentIndexItem> documents=database.getDocumentIndex(jurisdiction);
		StringBuffer str=new StringBuffer();
		str.append("<DocumentList>");
		for (int i=0; i < documents.size();i++) {
			str.append(documents.get(i).toXMLContent());
		}
		str.append("</DocumentList>");
		return  Response.ok(str.toString()).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}")
	@Produces(MediaType.APPLICATION_XML)
	public Response jurisdictionTypeXML(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type) {
		ArrayList<ComplianceDocumentIndexItem> documents=database.getDocumentIndex(jurisdiction,type);
		StringBuffer str=new StringBuffer();
		str.append("<DocumentList>");
		for (int i=0; i < documents.size();i++) {
			str.append(documents.get(i).toXMLContent());
		}
		str.append("</DocumentList>");
		return  Response.ok(str.toString()).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response jurisdictionTypeJSON(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type) {
		ArrayList<ComplianceDocumentIndexItem> documents=database.getDocumentIndex(jurisdiction,type);
		StringBuffer str=new StringBuffer();
		str.append("[");
		for (int i=0; i < documents.size();i++) {
			if (i!=0) str.append(",");
			str.append(documents.get(i).toJSONContent());
		}
		str.append("]");
		return  Response.ok(str.toString()).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}/{shortName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response jurisdictionTypeShortNameJSON(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@Context UriInfo info) {
			String latestVersion=database.getLatestVersion(jurisdiction,type,shortName);
			ComplianceDocument document=database.getDocument(generateURL(),jurisdiction,type,shortName,latestVersion);
			document=manipulateDocument(document,info,jurisdiction,type,shortName);
			return  Response.ok(JSONComplianceDocumentSerialiser.serialise(document)).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}/{shortName}/{version}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response shortNameVersionJSON(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@PathParam("version") String version,@Context UriInfo info) {
		ComplianceDocument document=database.getDocument(generateURL(),jurisdiction,type,shortName,version);
		document=manipulateDocument(document,info,jurisdiction,type,shortName);
		return  Response.ok(JSONComplianceDocumentSerialiser.serialise(document)).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}/{shortName}/{version}")
	@Produces(MediaType.APPLICATION_XML)
	public Response shortNameVersionXML(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@PathParam("version") String version,@Context UriInfo info) {
		ComplianceDocument document=database.getDocument(generateURL(),jurisdiction,type,shortName,version);
		document=manipulateDocument(document,info,jurisdiction,type,shortName);
		return  Response.ok(XMLComplianceDocumentSerialiser.serialise(document)).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}/{shortName}")
	@Produces(MediaType.APPLICATION_XML)
	public Response jurisdictionTypeShortNameXML(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@Context UriInfo info) {
		String latestVersion=database.getLatestVersion(jurisdiction,type,shortName);
		ComplianceDocument document=database.getDocument(generateURL(),jurisdiction,type,shortName,latestVersion);
		document=manipulateDocument(document,info,jurisdiction,type,shortName);
		return  Response.ok(XMLComplianceDocumentSerialiser.serialise(document)).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}/{shortName}/{version}/{documentReference:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fullQueryJSON(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@PathParam("version") String version,@PathParam("documentReference") String documentReference,@Context UriInfo info) {
		ComplianceDocument document=database.getDocument(generateURL(),jurisdiction,type,shortName,version);
		document=manipulateDocument(document,info,jurisdiction,type,shortName);
		document=ComplianceDocumentFilter.filterPath(document,documentReference);
		return  Response.ok(JSONComplianceDocumentSerialiser.serialise(document)).build();
	}
	
	@GET
	@Path("/{jurisdiction}/{type}/{shortName}/{version}/{documentReference:.+}")
	@Produces(MediaType.APPLICATION_XML)
	public Response fullQueryXML(@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@PathParam("version") String version,@PathParam("documentReference") String documentReference,@Context UriInfo info) {
		ComplianceDocument document=database.getDocument(generateURL(),jurisdiction,type,shortName,version);
		document=manipulateDocument(document,info,jurisdiction,type,shortName);
		document=ComplianceDocumentFilter.filterPath(document,documentReference);
		return  Response.ok(XMLComplianceDocumentSerialiser.serialise(document)).build();
	}
	
	// all updating functions
	
	@PUT
	@Path("/{jurisdiction}/{type}/{shortName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response uploadShortNameJSON(@HeaderParam("Authorization") String token,@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,String body) {
		if (!authorize(token)) return Response.status(403).type("text/plain").entity("Not Authorised").build();
		try {
			ComplianceDocument document=JSONComplianceDocumentDeserialiser.parseComplianceDocument(body);
			document.setVersion(generateVersionString());
			database.updateDocument(jurisdiction,type,shortName,document);
			return Response.ok(successMessageJSON).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(500).build();

	}
	
	@PUT
	@Path("/{jurisdiction}/{type}/{shortName}")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	public Response uploadShortNameXML(@HeaderParam("Authorization") String token,@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,String body) {
		if (!authorize(token)) return Response.status(403).type("text/plain").entity("Not Authorised").build();
		try {
			ComplianceDocument document=XMLComplianceDocumentDeserialiser.parseComplianceDocument(body);
			document.setVersion(generateVersionString());
			database.updateDocument(jurisdiction,type,shortName,document);
			return Response.ok(successMessageXML).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(500).build();
	}
	
	@PUT
	@Path("/{jurisdiction}/{type}/{shortName}/{version}")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	public Response uploadShortNameVersionXML(@HeaderParam("Authorization") String token,@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@PathParam("version") String version,String body) {
		if (!authorize(token)) return Response.status(403).type("text/plain").entity("Not Authorised").build();
		try {
			ComplianceDocument document=XMLComplianceDocumentDeserialiser.parseComplianceDocument(body);
			document.setVersion(version);			
			database.updateDocument(jurisdiction,type,shortName,document);
			return Response.ok(successMessageXML).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(500).build();
	}
	
	@PUT
	@Path("/{jurisdiction}/{type}/{shortName}/{version}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response uploadShortNameVersionJSON(@HeaderParam("Authorization") String token,@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@PathParam("version") String version,String body) {
		if (!authorize(token)) return Response.status(403).type("text/plain").entity("Not Authorised").build();
		try {
			ComplianceDocument document=JSONComplianceDocumentDeserialiser.parseComplianceDocument(body);
			document.setVersion(version);
			database.updateDocument(jurisdiction,type,shortName,document);
			return Response.ok(successMessageJSON).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(500).build();
	}
	
	@PUT
	@Path("/{jurisdiction}/{type}/{shortName}/{version}/{documentReference:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response uploadFullJSON(@HeaderParam("Authorization") String token,@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("shortName") String shortName,@PathParam("version") String version,@PathParam("documentReference") String documentReference,String body,@Context UriInfo info) {
		if (!authorize(token)) return Response.status(403).type("text/plain").entity("Not Authorised").build();
		try {
			ComplianceDocument document=JSONComplianceDocumentDeserialiser.parseComplianceDocument(body);
			document.setVersion(version);
			document=ComplianceDocumentUpdater.update(getPreviousVersion(database,jurisdiction,type,shortName),documentReference,document);
			document.setVersion(version);
			database.updateDocument(jurisdiction,type,shortName,document);
			return Response.ok(successMessageJSON).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(500).build();
	}
	
	@PUT
	@Path("/{jurisdiction}/{type}/{shortName}/{version}/{documentReference:.+}")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	public Response uploadFullXML(@HeaderParam("Authorization") String token,@PathParam("jurisdiction") String jurisdiction,@PathParam("type") String type,@PathParam("version") String version,@PathParam("shortName") String shortName,@PathParam("documentReference") String documentReference,String body,@Context UriInfo info) {
			if (!authorize(token)) return Response.status(403).type("text/plain").entity("Not Authorised").build();
			try {
				ComplianceDocument document=XMLComplianceDocumentDeserialiser.parseComplianceDocument(body);
				document.setVersion(version);
				document=ComplianceDocumentUpdater.update(getPreviousVersion(database,jurisdiction,type,shortName),documentReference,document);
				document.setVersion(version);
				database.updateDocument(jurisdiction,type,shortName,document);
				return Response.ok(successMessageXML).build();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return Response.status(500).build();
	}

}
