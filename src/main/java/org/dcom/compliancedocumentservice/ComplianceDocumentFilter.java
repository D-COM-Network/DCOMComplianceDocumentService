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
import org.dcom.core.compliancedocument.Paragraph;
import org.dcom.core.compliancedocument.Section;
import org.dcom.core.compliancedocument.Figure;
import org.dcom.core.compliancedocument.Insert;
import org.dcom.core.compliancedocument.Table;
import java.util.HashSet;
import org.apache.commons.lang3.mutable.MutableInt;

/**
*This helper class filters a compliance document so only specific sections/clauses are returned.
*
*/
public class ComplianceDocumentFilter {
	
	
		private static void removeListed(ComplianceItem item,HashSet<ComplianceItem> toRemove) {
			for (ComplianceItem i:toRemove) item.removeSubItem(i);
		}
		
		private static Insert findInsert(ComplianceItem current,Class insertType,int insertNumber,MutableInt insertCount) {
			if (current instanceof Paragraph) {
				Paragraph p=(Paragraph) current;
				for (int i=0; i < p.getNoInserts();i++) {
						Insert insert=p.getInsert(i);
						if (insertType.isInstance(insert)) {
								insertCount.increment();
								if (insertNumber==insertCount.getValue()) return insert;
						}
				}
			}
		
			for (int i=0; i < current.getNoSubItems();i++) {
				ComplianceItem item=current.getSubItem(i);
				Insert insert=findInsert(item,insertType,insertNumber,insertCount);
				if (insert!=null) return insert;
			}
			return null;
		}
		
		public static ComplianceDocument filterPath(ComplianceDocument current, String filterPath) {
			if (filterPath.startsWith("/")) filterPath=filterPath.substring(1);
			String[] pathArray=filterPath.split("/");
			if (pathArray[0].equals("Figure") || pathArray[0].equals("Table")) {
				int insertNumber=Integer.parseInt(pathArray[1]);
				Insert insert=null;
				if (pathArray[0].equals("Figure")) insert=findInsert(current,Figure.class,insertNumber,new MutableInt(0));
				if (pathArray[0].equals("Table")) insert=findInsert(current,Table.class,insertNumber,new MutableInt(0));
				Section s=new Section(current);
				Paragraph p=new Paragraph(s);
				p.addInsert(insert);
				s.addParagraph(p);
				HashSet<ComplianceItem> sections=new HashSet<ComplianceItem>();
				for (int i=0; i < current.getNoSections();i++) sections.add(current.getSection(i));
				removeListed(current,sections);
				current.addSection(s);
				return current;
			} else {
				//change the section number start to match the section filtered for
				current.removeMetaData("dcom:startSectionNumber");
				current.setMetaData("dcom:startSectionNumber",pathArray[0]);
			}
			System.out.println("Filtering For:"+filterPath);
			HashSet<ComplianceItem> toRemove=new HashSet<ComplianceItem>();
			for (int i=0; i < current.getNoSubItems();i++) {
				ComplianceItem item=current.getSubItem(i);
				if (!match(item,0,pathArray)) toRemove.add(item);
			}
			removeListed(current,toRemove);
			return current;
		}
		
		private static boolean match(ComplianceItem item, int filterLevel,String[] filterPath) {
			boolean match=false;
			// do I match?
			if (filterLevel>=filterPath.length) return true;
			String myItem=filterPath[filterLevel];
			int numberedToMatch=-1;
			try {
				numberedToMatch=Integer.parseInt(myItem);
			} catch (Exception e) {
				numberedToMatch=-1;
			}
		
			if (item.hasNumber() && numberedToMatch!=-1) {
				if (numberedToMatch==item.getNumber()) match=true;
			} else {
				String title=item.getMetaDataString("dcterms:title");
				if (title!=null && (title.equals(filterPath[filterLevel]) || title.equals(filterPath[filterLevel].replace("_"," ")))) match=true;
			}
			
			//do my children match
			if (item.hasNumber()) {
					if (!match) return false;
					if (match) filterLevel++;
			} else {
				if (match) filterLevel++;
			}
			
			
			
			HashSet<ComplianceItem> toRemove=new HashSet<ComplianceItem>();
			for (int i=0; i < item.getNoSubItems();i++) {
					ComplianceItem currentItem=item.getSubItem(i);
					if (!match(currentItem,filterLevel,filterPath)) toRemove.add(currentItem);
					else {
						match=true;
						//is it a direct match?
						if (filterLevel==filterPath.length-1) {
							if (currentItem.getAccessURL()!=null) {
								String[] aUrl=currentItem.getAccessURL().split("/");	
								if (aUrl[aUrl.length-1].equals(filterPath[filterLevel])) {
									i++;
									for (;i< item.getNoSubItems();i++){
										if (item.getSubItem(i) instanceof Section) {
											i--;
											break;
										}
										if (item.getSubItem(i).hasMetaData("numbered") && item.getSubItem(i).getMetaDataString("numbered").equals("global")) {
											i--;
											break;
										}
									}
								}
							}
						}
					}
			}
			
		
			
			removeListed(item,toRemove);
			return match;
		}
		
		private static void stripBody(ComplianceItem item) {
				if (item instanceof Section) {
						Section s=(Section)item;
						for (int i=0; i < s.getNoSections();i++) stripBody(s.getSection(i));
						for (int i=0; i < s.getNoParagraphs();i++) stripBody(s.getParagraph(i));
				}
				if (item instanceof Paragraph) {
						Paragraph p=(Paragraph)item;
						p.setBodyText("");
						for (int i=0; i < p.getNoParagraphs();i++) stripBody(p.getParagraph(i));
						for (int i=0; i < p.getNoInserts();i++) p.removeSubItem(p.getInsert(i));
				}
		}
		
		public static ComplianceDocument filterBodies(ComplianceDocument current) {
			for (int i=0; i < current.getNoSections();i++) stripBody(current.getSection(i));
			return current;
		}
		
	
}