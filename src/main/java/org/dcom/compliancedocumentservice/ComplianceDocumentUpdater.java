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

import org.dcom.core.compliancedocument.ComplianceDocument;
import org.dcom.core.compliancedocument.ComplianceItem;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;

/**
* This helper class merges an partial update of a compliance document (i.e. just one section) with the previous version of a document in order to produce a complete document
*
*/
public class ComplianceDocumentUpdater {
	
	
		private static String getAccessPath(ComplianceItem item) {
			if (!item.hasMetaData("ckterms:accessLocation")) return null;
			//filter out the URL
			String accessUrl=item.getMetaDataString("ckterms:accessLocation");
			int startRef=accessUrl.indexOf("//")+2;
			for (int i=0; i <5 ;i++) startRef=accessUrl.indexOf("/",startRef)+1;
			if (startRef==0) return "";
			return accessUrl.substring(startRef);
		}
	
		private static boolean inPath(String path1,String path2) {
			System.out.println("");
			System.out.println("IN:"+path1+":"+path2+":"+path1.startsWith(path2));
			System.out.println("");

			if (path1.startsWith(path2)) return true;	
	
			return false;
		}
	
		private static boolean isPath(String path1,String path2) {
			System.out.println("");	
			System.out.println("IS:"+path1+":"+path2+":"+path1.equals(path2));
			System.out.println("");
				if (path1.equals(path2)) return true;	
				
				return false;
		}
		
		private static ComplianceItem findStartPoint(ComplianceItem item, String documentReference) {
			if (isPath(documentReference,getAccessPath(item))) return item;
			for (int i=0; i < item.getNoSubItems();i++) {
				ComplianceItem currentSubItem=item.getSubItem(i);
				if (inPath(documentReference,getAccessPath(currentSubItem))) return findStartPoint(currentSubItem,documentReference);
			}
			return null;
		}

	public static ComplianceDocument update(ComplianceDocument oldDocument, String documentReference,ComplianceDocument newDocument) {
		if (documentReference.endsWith("/")) documentReference=documentReference.substring(0,documentReference.length()-1);
		ComplianceItem startPoint=findStartPoint(oldDocument,documentReference);
		if (startPoint==null) return null;
		startPoint.mergeIn(newDocument.getSection(0));
		return oldDocument;
	}
	

}