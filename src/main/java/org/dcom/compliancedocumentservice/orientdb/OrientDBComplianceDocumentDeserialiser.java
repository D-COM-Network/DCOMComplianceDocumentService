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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.ORecord;
import java.util.ArrayList;
import java.util.HashSet;

/**
*This class takes a orientdb database that contains a compliance document and creates from it an in memory compliance document.
*
*/
public class OrientDBComplianceDocumentDeserialiser {

  private static final Logger LOGGER = LoggerFactory.getLogger( OrientDBComplianceDocumentDeserialiser.class );

  public static ComplianceDocument parseComplianceDocument(String url,ODatabaseSession session,String docId,String version) {
    try {
      ComplianceDocument document = new ComplianceDocument();
      document.setMetaData("ckterms:accessLocation",url);
      OResultSet rs = session.command("select from "+docId);
      if (rs.hasNext()) {
        OResult r=rs.next();
        parseMetaData(document,r);
        OResultSet rs2=session.command("select from Version where versionName='"+version+"' and partOf="+docId);
        if (rs2.hasNext()) {
          OResult r2=rs2.next();
          document.removeMetaData("dcterms:version"); // remove the version so it is pulled from the version
          document.setMetaData("dcterms:version",version);
          parseMetaData(document,r2);
          if (document.hasMetaData("dcterms:replaces")) {
              OResultSet rsLookup=session.command("select from "+document.getMetaDataString("dcterms:replaces"));
              document.removeMetaData("dcterms:replaces");
              if (rsLookup.hasNext()) {
                OResult rLookup=rsLookup.next();
                document.setMetaData("dcterms:replaces",rLookup.getProperty("versionName"));
              }
          }
          if (document.hasMetaData("dcterms:replacedBy")) {
            OResultSet rsLookup=session.command("select from "+document.getMetaDataString("dcterms:replacedBy"));
            document.removeMetaData("dcterms:replacedBy");
            if (rsLookup.hasNext()) {
              OResult rLookup=rsLookup.next();
              document.setMetaData("dcterms:replacedBy",rLookup.getProperty("versionName"));
            }
          }
          if (r2.hasProperty("sections")) {
            ArrayList<ORecord> sections =(ArrayList<ORecord>)r2.getProperty("sections");
            int number=1;
            if (document.hasMetaData("dcom:startSectionNumber")){
              number=Integer.parseInt(document.getMetaDataString("dcom:startSectionNumber"));    
            }
            int startParaNumber=1;
            if (document.hasMetaData("dcom:startParagraphNumber")){
                startParaNumber=Integer.parseInt(document.getMetaDataString("dcom:startParagraphNumber"));
            }
            for (int i=0; i < sections.size();i++) {
              Section s=parseSection(document,url,number,startParaNumber,session,sections.get(i).getIdentity().toString(),document);
              if (s.hasMetaData("numbered") && s.getMetaDataString("numbered").equalsIgnoreCase("global")){
                number++;
              }
              document.addSection(s);
            }
          }  
        }
        
      LOGGER.trace("Deserialising "+document);
      return document;
    }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static Section parseSection(ComplianceDocument document,String url,Integer myNumber,Integer paraNumberContinuation,ODatabaseSession session,String id,ComplianceItem parent) {
    Section section=new Section(parent);
    if (myNumber!=null) section.setNumber(myNumber);
    OResult r=session.command("select from "+id).next();
    parseMetaData(section,r);
    if (url!=null && section.hasMetaData("numbered") && section.getMetaDataString("numbered").equalsIgnoreCase("global")){
      url=url+"/"+myNumber;
      section.setMetaData("ckterms:accessLocation",url);
    } else if (url!=null &&  section.getMetaDataString("dcterms:title")!=null && !section.getMetaDataString("dcterms:title").equals("null") ){
      url=url+"/"+section.getMetaDataString("dcterms:title").replace(" ","_");
      section.setMetaData("ckterms:accessLocation",url);
    } else {
      //then it is a dummy section caused by RASE it doesn't have a url but children may do.
      url=url;
    }
    int sectionNumber=1;
    if (document.hasMetaData("dcom:startSectionNumber")){
      sectionNumber=Integer.parseInt(document.getMetaDataString("dcom:startSectionNumber"));    
    }
    int paraNumber=paraNumberContinuation;
    if (r.hasProperty("subItems")) {
      
          ArrayList<ORecord> subItems = (ArrayList<ORecord>)r.getProperty("subItems");
          HashSet<String> sections=new HashSet<String>();
          HashSet<String> paragraphs=new HashSet<String>();
          
          if (r.hasProperty("sections")) {
            ArrayList<ORecord> sectionsList = (ArrayList<ORecord>)r.getProperty("sections");
            for (int i=0; i < sectionsList.size();i++) sections.add(sectionsList.get(i).getIdentity().toString());
          }
          if (r.hasProperty("paragraphs")) {
            ArrayList<ORecord> paragraphsList = (ArrayList<ORecord>)r.getProperty("paragraphs");
            for (int i=0; i < paragraphsList.size();i++) paragraphs.add(paragraphsList.get(i).getIdentity().toString());
          }
          
          
          for (int i=0; i < subItems.size();i++) {
            String identifier=subItems.get(i).getIdentity().toString();
            
            if (paragraphs.contains(identifier)) {
                Paragraph p=parseParagraph(url,paraNumber,session,identifier,document);
                if (p.hasMetaData("numbered") && p.getMetaDataString("numbered").equalsIgnoreCase("global")) paraNumber++;
                section.addParagraph(p);
  
            } else if (sections.contains(identifier)) {
                Section s=new Section(parent);
                //construct a temporary section here
                OResult rTemp=session.command("select from "+identifier).next();
                parseMetaData(s,rTemp);
                if (s.hasMetaData("numbered") && s.getMetaDataString("numbered").equalsIgnoreCase("global")) {
                  sectionNumber++;
                  int startParaNumber=1;
                  if (document.hasMetaData("dcom:startParagraphNumber")){
                      startParaNumber=Integer.parseInt(document.getMetaDataString("dcom:startParagraphNumber"));
                  }
                  s=parseSection(document,url,sectionNumber,startParaNumber,session,identifier,s);
                } else {
                  s=parseSection(document,url,null,paraNumber,session,identifier,s);
                  paraNumber=s.getHighestParaNumber();
                }
                section.addSection(s);
            }
          }
        }
    LOGGER.info("Deserialising "+section);
    section.setHighestParaNumber(paraNumber);
    return section;
  }

  private static Paragraph parseParagraph(String url,Integer myNumber,ODatabaseSession session,String id,ComplianceItem parent) {
    Paragraph paragraph=new Paragraph(parent);
    OResult r=session.command("select from "+id).next();
    paragraph.setNumber(myNumber);
    parseMetaData(paragraph,r);
    if (url!=null && paragraph.hasMetaData("numbered") && paragraph.getMetaDataString("numbered").equalsIgnoreCase("global")){
      url=url+"/"+myNumber;
      paragraph.setMetaData("ckterms:accessLocation",url);
    } else if (url!=null && paragraph.getMetaDataString("dcterms:title")!=null && !paragraph.getMetaDataString("dcterms:title").equals("null") ){
      url=url+"/"+paragraph.getMetaDataString("dcterms:title").replace(" ","_");
      paragraph.setMetaData("ckterms:accessLocation",url);
    } else {
      url=null;
    }
    if (r.hasProperty("body")) { 
      paragraph.setBodyText(r.getProperty("body").toString());
    }
    if (r.hasProperty("paragraphs")) {
      int number=1;
      ArrayList<ORecord> paragraphs =  (ArrayList<ORecord>)r.getProperty("paragraphs");
      for (int i=0; i < paragraphs.size();i++) {
        Paragraph p=parseParagraph(url,number,session,paragraphs.get(i).getIdentity().toString(),paragraph);
        if (p.hasMetaData("numbered") && p.getMetaDataString("numbered").equalsIgnoreCase("global")) number++;
        paragraph.addParagraph(p);
      }
    }
    if (r.hasProperty("rules")) {
      ArrayList<ORecord> rules =  (ArrayList<ORecord>)r.getProperty("rules");
      for (int i=0; i < rules.size();i++) paragraph.addRule(parseRule(session,rules.get(i).getIdentity().toString(),parent));
    }

    if (r.hasProperty("inserts")) {
      ArrayList<ORecord> inserts =  (ArrayList<ORecord>)r.getProperty("inserts");
      for (int i=0; i < inserts.size();i++) paragraph.addInsert(parseInsert(session,inserts.get(i).getIdentity().toString(),parent));
    }
    LOGGER.info("Deserialising "+paragraph);
    return paragraph;
  }

  private static Insert parseInsert(ODatabaseSession session, String id,ComplianceItem parent){
      OResult r=session.command("select from "+id).next();
      if (r.hasProperty("imageData")) {
        //its an image
        Figure i=new Figure(parent);
        parseMetaData(i,r);
        i.setImageData(r.getProperty("imageData").toString());
        LOGGER.info("Deserialising "+i);
        return i;
      } else {
        //its a table
        Table t=new Table(parent);
        parseMetaData(t,r);
        if (r.hasProperty("header") && r.getProperty("header")!=null ) t.setHeader(parseTableGroup(session,r.getProperty("header").toString(),new TableHeader(t)));
        if (r.hasProperty("footer") && r.getProperty("footer")!=null ) t.setFooter(parseTableGroup(session,r.getProperty("footer").toString(),new TableFooter(t)));
        if (r.hasProperty("body") && r.getProperty("body")!=null ) t.setBody(parseTableGroup(session,r.getProperty("body").toString(),new TableBody(t)));
        LOGGER.info("Deserialising "+t);
        return t;
      }
  }


  private static <T extends TableGroup> T parseTableGroup(ODatabaseSession session,String id,T tg) {
    OResult r=session.command("select from "+id).next();
    parseMetaData(tg,r);
    if (r.hasProperty("rows")) {
        ArrayList<ORecord> listRows=(ArrayList<ORecord>)r.getProperty("rows");
        for (int i=0; i < listRows.size();i++) {
          OResult r2=session.command("select from "+listRows.get(i).getIdentity().toString()).next();
          Row row=new Row(tg);
          parseMetaData(row,r2);
          tg.addRow(row);
          ArrayList<ORecord> listCells=(ArrayList<ORecord>)r2.getProperty("cells");
          for (int x=0; x < listCells.size();x++) {
            OResult r3=session.command("select from "+listCells.get(x).getIdentity().toString()).next();
            Cell c;
            if (r3.getProperty("@class").toString().equals("TitleCell")) {
              c=new TitleCell(row);
            } else {
              c=new DataCell(row);
            }
            parseMetaData(c,r3);
            row.addCell(c);
          }

        }
    }
    LOGGER.info("Deserialising "+tg);
    return tg;
  }

  private static Rule parseRule(ODatabaseSession session,String id,ComplianceItem parent) {
    Rule rule =new Rule(parent);
    parseMetaData(rule,session.command("select from "+id).next());
    LOGGER.info("Deserialising "+rule);
    return rule;
  }

  private static void parseMetaData(ComplianceItem item, OResult r) {
    OrientDBMappings mappings=new OrientDBMappings();
    for (int i=0; i < mappings.getNoMappings();i++) {
      if (r.hasProperty(mappings.getO(i))) {
        if (mappings.getIsArray(i)) {
          ArrayList<String> arrayIn=(ArrayList<String>)r.getProperty(mappings.getO(i));
          for (int x=0; x< arrayIn.size();x++)  item.setMetaData(mappings.getD(i),arrayIn.get(x).toString());
    
        } else {
          item.setMetaData(mappings.getD(i),r.getProperty(mappings.getO(i)).toString());
        }
      
      }
    }
  }
}