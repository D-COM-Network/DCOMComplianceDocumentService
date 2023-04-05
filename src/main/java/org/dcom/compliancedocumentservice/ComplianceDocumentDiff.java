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
import java.util.ArrayList;

/**
*This helper class takes two compliance documents and identifies the differences between them.
*
*/
public class ComplianceDocumentDiff {
	
	
		public static ComplianceDocument diff(ComplianceDocument current, ComplianceDocument previous) {
				scanAndUpdate(current,previous);
				return current;
		}
		
		private static void setAllInserted(ComplianceItem item)  {
			item.setMetaData("inserted","true");
			for (int i=0; i < item.getNoSubItems();i++) setAllInserted(item.getSubItem(i));
		}
		
		private static void setAllDeleted(ComplianceItem item)  {
			item.setMetaData("deleted","true");
			for (int i=0; i < item.getNoSubItems();i++) setAllDeleted(item.getSubItem(i));
		}
				
		private static void scanAndUpdate(ComplianceItem current, ComplianceItem previous) {
			System.out.println(current.getIdentifier()+":"+current.getNoSubItems()+":"+previous.getNoSubItems());
			for (int i=0; i < current.getNoSubItems();i++) {
					if (i >= previous.getNoSubItems() ) setAllInserted(current.getSubItem(i));
					else if (!metaDataEquals(current.getSubItem(i),previous.getSubItem(i))) setAllInserted(current.getSubItem(i));
					else scanAndUpdate(current.getSubItem(i),previous.getSubItem(i));
			}
			for (int i=0; i < previous.getNoSubItems();i++) {
				if (i >= previous.getNoSubItems() ) {
					setAllDeleted(previous.getSubItem(i));
					current.addSubItem(i,previous.getSubItem(i));
				} 
			}
		}

	private static boolean metaDataEquals(ComplianceItem newItem, ComplianceItem oldItem) {
			
			for (String mD: newItem.getMetaDataList()) {
				if (mD.equals("ckterms:accessLocation")) continue;
				if (mD.equals("dcterms:replaces")) continue;
				if (mD.equals("dcterms:replacedBy")) continue;
				if (mD.equals("dcterms:dateCreated")) continue;
				if (mD.equals("dcterms:modified")) continue;
				if (mD.equals("dcterms:identifier")) continue;
				if (!oldItem.hasMetaData(mD)) {
						//System.out.println("Failed on old item not having MD:"+mD);
						return false;
				}
				if (newItem.isListMetadata(mD)) {
					ArrayList<String> newMData=newItem.getMetaDataList(mD);
					ArrayList<String> oldMData=oldItem.getMetaDataList(mD);
					if (oldMData.size()!=newMData.size()) {
						//System.out.println("failed on MD size differences:"+oldMData.size()+":"+newMData.size());
						return false;
					}
					for (int i=0; i < newMData.size();i++) {
						if (!oldMData.contains(newMData.get(i))) {
							//System.out.println("Failed on old item not having MD:"+newMData.get(i));
							return false;
						}
					}
					for (int i=0; i < oldMData.size();i++) {
						if (!newMData.contains(oldMData.get(i)))	{
							//System.out.println("Failed on new item not having MD:"+oldMData.get(i));
							return false;
						}
					}
				} else {
					if (!newItem.getMetaDataString(mD).replaceAll("\\s+","").equals(oldItem.getMetaDataString(mD).replaceAll("\\s+",""))) {
						//done like this so we ignore white space changes
						//System.out.println("Failed lack of equality between("+mD+"):"+newItem.getMetaDataString(mD)+"->"+oldItem.getMetaDataString(mD));
						return false;
					}
				}
			}
			for (String mD: oldItem.getMetaDataList()) {
				if (mD.equals("ckterms:accessLocation")) continue;
				if (mD.equals("dcterms:replaces")) continue;
				if (mD.equals("dcterms:replacedBy")) continue;
				if (mD.equals("dcterms:dateCreated")) continue;
				if (mD.equals("dcterms:modified")) continue;
				if (!newItem.hasMetaData(mD)) {
					//System.out.println("Failed on New item not having MD:"+mD);
					return false;
				}
			}
			return true;
		}
}