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

import org.dcom.core.compliancedocument.*;
import org.dcom.core.compliancedocument.utils.GuidHelper;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

/**
* This class takes an in memory compliance document and writes it to an oritentDB database.
*
*/
public class OrientDBComplianceDocumentSerialiser {

    private static final Logger LOGGER = LoggerFactory.getLogger( OrientDBComplianceDocumentSerialiser.class );
    
  
    
    private static void updateDBRecord(ODatabaseSession session, String id,String subItemName,ArrayList<String> inArray) {
      StringBuffer str=new StringBuffer();
      
      str.append("update ").append(id).append(" set ").append(subItemName).append("=[");
      boolean first=true;
      for (String nId:inArray) {
        if (first) first=false; else str.append(",");
        str.append(nId);
      }
      str.append("]");
      session.command(str.toString());
    }
    
    private static String createNew(ODatabaseSession session,String type,String version) {
      String nId=OrientDBHelpers.getInsertId(session.command("create vertex "+type+" set identifier='"+GuidHelper.generateGuid()+"', versionName='"+version+"'"));
      return nId;
    }
    
    private static String mapToId(ODatabaseSession session, ComplianceItem item) {
      return  session.command("select @rid from V where identifier='"+item.getIdentifier()+"'").next().getProperty("@rid").toString();
    }
  
    
    private static boolean detectChanges(ODatabaseSession session,ComplianceItem item,String version,ArrayList<String> ... inputs) throws Exception{
      boolean isChanges=false;
      boolean versionExists=false;
      OResultSet rs=session.command("select from V where identifier='"+item.getIdentifier()+"'");
      if (!rs.hasNext()) return true;
      OResult r=rs.next();
      if (r.hasProperty("versionName")) {
          if (!r.getProperty("versionName").toString().equals(version)) {
            item.generateNewGuid();
            return true;
          } 
      } else {
        throw new Exception("No Version on Object:"+item.getIdentifier());
      }
      
      OrientDBMappings mappings=new OrientDBMappings();
      for(String mDName: item.getMetaDataList()) {
        for (int i=0; i < mappings.getNoMappings();i++) {
          if (mappings.getD(i).equals(mDName)) {
                String newName=mappings.getO(i);
                if (!r.hasProperty(newName)) {
                  isChanges=true;
                  break;
                }
                if (item.isListMetadata(mDName)) {
                  ArrayList<String> a=item.getMetaDataList(mDName);
                  ArrayList<Object> b=r.getProperty(newName);
                  if (!a.containsAll(b) || b.containsAll(a)) {
                    isChanges=true;
                    break;
                  }
                }else {
                  if (!r.getProperty(newName).toString().equals(item.getMetaDataString(mDName))) {
                    isChanges=true;
                    break;
                  }
                }
          }
        }
        if (isChanges) break;
      }
      if (!isChanges && item instanceof Section) {
        Section s=(Section)item;
        ArrayList<String> newSections=inputs[0];
        ArrayList<String> currentSections=new ArrayList<String>();
        for (int i=0; i < s.getNoSections();i++) {
            currentSections.add(mapToId(session,s.getSection(i)));
        }
        if (!newSections.containsAll(currentSections) || currentSections.containsAll(newSections)) {
          isChanges=true;
        }
    
        ArrayList<String> newParagraphs=inputs[1];
        ArrayList<String> currentParagraphs=new ArrayList<String>();
        for (int i=0; i < s.getNoParagraphs();i++) {
            currentParagraphs.add(mapToId(session,s.getParagraph(i)));
        }
        if (!newParagraphs.containsAll(currentParagraphs) || currentParagraphs.containsAll(newParagraphs)) {
          isChanges=true;
        }
      }
      if (!isChanges && item instanceof Row) {
        Row rw=(Row)item;
        ArrayList<String> newCells=inputs[0];
        ArrayList<String> currentCells=new ArrayList<String>();
        for (int i=0; i < rw.getNoCells();i++) {
            currentCells.add(mapToId(session,rw.getCell(i)));
        }
        if (!newCells.containsAll(currentCells) || currentCells.containsAll(newCells)) {
          isChanges=true;
        }
      } 
      if (!isChanges && item instanceof Table) {
        Table t=(Table)item;
        ArrayList<String> newTableGroups=inputs[0];
        ArrayList<String> currentTableGroups=new ArrayList<String>();
        currentTableGroups.add(mapToId(session,t.getHeader()));
        currentTableGroups.add(mapToId(session,t.getBody()));
        currentTableGroups.add(mapToId(session,t.getFooter()));
        if (!newTableGroups.containsAll(currentTableGroups) || currentTableGroups.containsAll(newTableGroups)) {
          isChanges=true;
        }
      } 
      if (!isChanges && item instanceof TableGroup) {
            TableGroup tg=(TableGroup)item;
            ArrayList<String>  newRows=inputs[0];
            ArrayList<String> currentRows=new ArrayList<String>();
            for (int i=0; i < tg.getNoRows();i++) {
                currentRows.add(mapToId(session,tg.getRow(i)));
            }
            if (!newRows.containsAll(currentRows) || currentRows.containsAll(newRows)) {
              isChanges=true;
            }
      } 
      if (!isChanges && item instanceof Figure) {
        Figure f=(Figure)item;
        if (!f.getImageDataString().equals(inputs[0].get(0))) {
            isChanges=true;
        }
      } 
      if (!isChanges && item instanceof Paragraph) {
        Paragraph p=(Paragraph)item;
        ArrayList<String> newParagraphs=inputs[0];
        ArrayList<String> currentParagraphs=new ArrayList<String>();
        for (int i=0; i < p.getNoParagraphs();i++) {
            currentParagraphs.add(mapToId(session,p.getParagraph(i)));
        }
        if (!newParagraphs.containsAll(currentParagraphs) || currentParagraphs.containsAll(newParagraphs)) {
          isChanges=true;
        }
        

        ArrayList<String> newInserts=inputs[0];
        ArrayList<String> currentInserts=new ArrayList<String>();
        for (int i=0; i < p.getNoInserts();i++) {
            currentInserts.add(mapToId(session,p.getInsert(i)));
        }
        if (!newInserts.containsAll(currentInserts) || currentInserts.containsAll(newInserts)) {
          isChanges=true;
        }
        

        ArrayList<String> newRules=inputs[0];
        ArrayList<String> currentRules=new ArrayList<String>();
        for (int i=0; i < p.getNoRules();i++) {
            currentRules.add(mapToId(session,p.getRule(i)));
        }
        if (!newRules.containsAll(currentRules) || currentRules.containsAll(newRules)) {
          isChanges=true;
        }
      }
      
      
      if (isChanges) throw new Exception("Trying to Change a Version that Exists");
      return isChanges;
      
    }

    public static void serialise(ODatabaseSession session, String dId, ComplianceDocument document) throws Exception {
        LOGGER.trace("Serialising "+document);
        ArrayList<String> newSections=new ArrayList<String>();
        for (int i=0; i < document.getNoSections();i++) newSections.add(serialiseSection(session, document.getVersion(), document.getSection(i)));
        OResult vId=session.command("select from Version where versionName='"+document.getVersion()+"' and partOf="+dId).next();
        updateDBRecord(session,vId.getProperty("@rid").toString(),"sections",newSections);
    }
    
    private static String serialiseSection(ODatabaseSession session, String version, Section s) throws Exception  {
        LOGGER.info("Serialising "+s);
        ArrayList<String> newSections=new ArrayList<String>();
        ArrayList<String> newParas=new ArrayList<String>();
        ArrayList<String> newSubItems=new ArrayList<String>();
        
        for (int i=0; i < s.getNoSubItems();i++) {
          ComplianceItem item=s.getSubItem(i);
          if (item instanceof Section) {
            String nS=serialiseSection(session,version,(Section)item);
            newSections.add(nS);
            newSubItems.add(nS);
          }
          if (item instanceof Paragraph) {
            String nP=serialiseParagraph(session,version,(Paragraph)item);
            newParas.add(nP);
            newSubItems.add(nP);
          }
        }
        
  
        if (detectChanges(session,s,version,newSections,newParas)) {
          String id=createNew(session,"Section",version);
          updateMetaData(session,id,s);
          updateDBRecord(session,id,"sections",newSections);
          updateDBRecord(session,id,"subItems",newSubItems);
          updateDBRecord(session,id,"paragraphs",newParas);
          return id;
        }
        return mapToId(session,s);
    }



    private static String serialiseParagraph(ODatabaseSession session, String version,Paragraph p) throws Exception  {
        LOGGER.info("Serialising "+p);
        ArrayList<String> newRules=new ArrayList<String>();
        ArrayList<String> newParas=new ArrayList<String>();
        ArrayList<String> newInserts=new ArrayList<String>();
        ArrayList<String> newSubItems=new ArrayList<String>();
          
        for (int i=0; i < p.getNoSubItems();i++) {
          ComplianceItem item=p.getSubItem(i);
          if (item instanceof Paragraph) {
            String nP=serialiseParagraph(session,version,(Paragraph)item);
            newParas.add(nP);
            newSubItems.add(nP);
          }
          if (item instanceof Insert) {
            String nI=serialiseInsert(session,version,(Insert)item);
            newInserts.add(nI);
            newSubItems.add(nI);
          }
        }

        for (int i=0; i < p.getNoRules();i++) newRules.add(serialiseRule(session,version,p.getRule(i)));

        if (detectChanges(session,p,version,newParas,newInserts,newRules)) {
          String id=createNew(session,"Paragraph",version);
          updateMetaData(session,id,p);
          updateDBRecord(session,id,"paragraphs",newParas);
          updateDBRecord(session,id,"inserts",newInserts);
          updateDBRecord(session,id,"rules",newRules);  
          updateDBRecord(session,id,"subItems",newSubItems);   
          return id;
        }
        return mapToId(session,p);
    }

    private static String serialiseTableGroup(ODatabaseSession session, String version,TableGroup g,String name) throws Exception  {
        LOGGER.info("Serialising "+g);
        ArrayList<String> newRows=new ArrayList<String>();
        for (int i=0; i < g.getNoRows();i++) newRows.add(serialiseRow(session,version,g.getRow(i)));
        if (detectChanges(session,g,version,newRows)) {
          String id=createNew(session,name,version);
          updateMetaData(session,id,g);
          updateDBRecord(session,id,"rows",newRows);
          return id;
        }
      return mapToId(session,g);
    }
    
    private static String serialiseRow(ODatabaseSession session, String version,Row r) throws Exception  {
        LOGGER.info("Serialising "+r);
        ArrayList<String> newCells=new ArrayList<String>();
        for (int z=0; z< r.getNoCells();z++) newCells.add(serialiseCell(session,version,r.getCell(z)));
        if (detectChanges(session,r,version,newCells)) {
          String id=createNew(session,"Row",version);
          updateMetaData(session,id,r);
          updateDBRecord(session,id,"cells",newCells);
          return id;
        }
        return mapToId(session,r);
    }
    
    private static String serialiseCell(ODatabaseSession session, String version,Cell c) throws Exception  {
        LOGGER.info("Serialising "+c);
        if (detectChanges(session,c,version)) {
          String type;
          if (c instanceof DataCell) type="DataCell";
          else type="TitleCell";
          
          String id=createNew(session,type,version);
          updateMetaData(session,id,c);
          return id;
        }
        return mapToId(session,c);
    }

    private static String serialiseInsert(ODatabaseSession session, String version,Insert i) throws Exception  {
        LOGGER.info("Serialising "+i);
        if (i instanceof Table) {

            Table t=(Table)i;
            String header=null,footer=null,body=null;
            ArrayList<String> newGroups=new ArrayList<String>();
            if (t.getHeader()!=null) {
              header=serialiseTableGroup(session,version,t.getHeader(),"TableHeader");
              newGroups.add(header);
            }
            if (t.getBody()!=null) {
              body=serialiseTableGroup(session,version,t.getBody(),"TableBody");
              newGroups.add(body);
            }
            if (t.getFooter()!=null) {
              footer=serialiseTableGroup(session,version,t.getFooter(),"TableFooter");
              newGroups.add(footer);
            }
            if (detectChanges(session,t,version,newGroups)) {
              String id=createNew(session,"Table",version);
              updateMetaData(session,id,t);
              session.command("update "+id+" set header="+header+",body="+body+", footer="+footer);
              return id;
            }
        } else if (i instanceof Figure) {
          
          Figure f=(Figure)i;
          ArrayList<String> imageData=new ArrayList<String>();
          imageData.add(f.getImageDataString());
          if (detectChanges(session,f,version,imageData)) {
            String id=createNew(session,"Figure",version);
            updateMetaData(session,id,f);
            session.command("update "+id+" set imageData='"+((Figure)i).getImageDataString()+"'");
            return id;
          }
        }
        return mapToId(session,i);
    }

    private static String serialiseRule(ODatabaseSession session, String version,Rule r) throws Exception  {
        LOGGER.info("Serialising "+r);
        if (detectChanges(session,r,version)) {
          String id=createNew(session,"Rule",version);
          updateMetaData(session,id,r);
          return id;
        }
        return mapToId(session,r);
    }

    
    public static void updateMetaData(ODatabaseSession session, String id,ComplianceItem item) {
      OrientDBMappings mappings=new OrientDBMappings();
      StringBuffer str=new StringBuffer();
      str.append("update "+id+" set ");
      boolean hasSomeMetadata=false;
      HashMap<String,Object> params=new HashMap<String,Object>();
      boolean first=true;
      for(String mDName: item.getMetaDataList()) {
          String mdNewName=null;
          boolean isArray=false;
          for (int i=0; i < mappings.getNoMappings();i++) {
            if (mappings.getD(i).equals(mDName)) {
                mdNewName=mappings.getO(i);
                isArray=mappings.getIsArray(i);
                break;
            }
          }
          if (mdNewName==null) {
            LOGGER.error("Metadata Not Found! - "+mDName);
            continue;
          }
          if (first) first=false; else str.append(",");
          str.append(mdNewName).append("=:"+mdNewName);
          hasSomeMetadata=true;
          if (isArray){
            ArrayList<String> data;
            if (item.isListMetadata(mDName)) {
                data=item.getMetaDataList(mDName);
            }else {
              data=new ArrayList<String>();
              data.add(item.getMetaDataString(mDName));
            }
            params.put(mdNewName,data);
          }else {
            params.put(mdNewName,item.getMetaDataString(mDName));
          }
      }
      if (hasSomeMetadata) session.command(str.toString(),params);
    }
}
