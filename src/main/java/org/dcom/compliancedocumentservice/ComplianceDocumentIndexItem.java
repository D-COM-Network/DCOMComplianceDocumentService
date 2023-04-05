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
import java.util.HashMap;
import com.owlike.genson.Genson;
import java.util.ArrayList;

/**
*This is the programmatic representation of the index of documents on the services. Used for communication between the database layer and web service layer.
*
*/
public class ComplianceDocumentIndexItem {
	
	private HashMap<String,Object> dataSet;
	
	public ComplianceDocumentIndexItem(String uid,String shortName,String fullName,String documentType,String jurisdiction,String embeddedLogic,String latestVersion,String latestVersionDate,ArrayList<HashMap<String,String>> versions) {
			dataSet=new HashMap<String,Object>();
			dataSet.put("uid",uid);
			dataSet.put("shortName",shortName);
			dataSet.put("fullName",fullName);
			dataSet.put("documentType",documentType);
			dataSet.put("jurisdiction",jurisdiction);
			if (embeddedLogic!=null) dataSet.put("embeddedLogic",embeddedLogic);
			if (latestVersion!=null) dataSet.put("latestVersion",latestVersion);
			if (latestVersionDate!=null) dataSet.put("latestVersionDate",latestVersionDate);
			if (versions!=null) dataSet.put("versions",versions);
	}
	
	public String toXMLContent() {
		StringBuffer str=new StringBuffer();
		str.append("<ComplianceDocument>");
		for (String item : dataSet.keySet()) {
				if (dataSet.get(item) instanceof String) {
					str.append("<").append(item).append(">").append(dataSet.get(item)).append("</").append(item).append(">");
				} else if (dataSet.get(item) instanceof ArrayList) {
					str.append("<").append(item).append(">");
					ArrayList<Object> versions=(ArrayList<Object>) dataSet.get(item);
					for (Object o: versions) {
							HashMap<String,Object> versionData=(HashMap<String,Object>)o;
							str.append("<version>");
							for (String subItem : versionData.keySet()) {
								str.append("<").append(subItem).append(">").append(versionData.get(subItem)).append("</").append(subItem).append(">");						
							}
							str.append("</version>");
					}
					str.append("</").append(item).append(">");
				} else {
					HashMap<String,String> subDataSet= (HashMap<String,String>)dataSet.get(item);
					for (String subItem : subDataSet.keySet()) {
						str.append("<").append(subItem).append(">").append(subDataSet.get(subItem)).append("</").append(subItem).append(">");						
				}
			}
		}
		str.append("</ComplianceDocument>");
		return str.toString();
	}
	
	public String toJSONContent() {
		return new Genson().serialize(dataSet);
	}

}