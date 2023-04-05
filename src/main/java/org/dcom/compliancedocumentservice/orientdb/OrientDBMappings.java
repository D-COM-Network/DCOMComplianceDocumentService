
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

/**
*This file contains a mapper between the compliance document metadata names and valid orientdb field names.
*
*/
public class OrientDBMappings {
	
	private ArrayList<String> mapDoc;
	private ArrayList<String> mapOdb;
	private ArrayList<Boolean> isArray;
	
	
	public OrientDBMappings() {
			mapDoc= new ArrayList<String>();
			mapOdb= new ArrayList<String>();
			isArray=new ArrayList<Boolean>();
			//now create the mappings
			makeMap("dcterms:title","title");
			makeMapArray("ckterms:projectStage","projectStage");
			makeMapArray("dcterms:subject","subject");
			makeMapArray("dcterms:coverage.temporal","coverageTemporal");
			makeMapArray("dcterms:coverage.spatial","coverageSpatial");
			makeMap("dcterms:dateCreated","dateCreated");
			makeMap("dcterms:modified","modified");
			makeMapArray("dcterms:accessRights","accessRights");
			makeMap("dcterms:identifier","identifier");
			makeMapArray("dcterms:language","language");
			makeMapArray("dcterms:relation","relation");
			makeMap("ckterms:version","version");
			makeMap("dcterms:replaces","replaces");
			makeMap("dcterms:isReplacedBy","isReplacedBy");
			makeMap("dcterms:rights","rights");
			makeMap("dcterms:publisher","publisher");
			makeMap("dcterms:description","description");
			makeMapArray("ckterms:sector","sector");
			makeMap("dcterms:type","type");
			makeMap("numbered","numbered");
			makeMap("colspan","colspan");
			makeMap("body","body");
			makeMap("caption","caption");
			makeMap("rowspan","rowspan");
			makeMap("cellType","cellType");
			makeMap("dcom:startSectionNumber","startSectionNumber");
			makeMap("dcom:startParagraphNumber","startParagraphNumber");
			makeMap("raseType","raseType");
			makeMap("raseId","raseId");
	}
	
	public int getNoMappings() {
		return mapDoc.size();
	}
	
	public String getO(int i) {
		return mapOdb.get(i);
	}
	
	public String getD(int i){
		return mapDoc.get(i);
	}
	
	public boolean getIsArray(int i) {
		return isArray.get(i);
	}
	
	private void makeMap(String d,String o) {
		mapDoc.add(d);
		mapOdb.add(o);
		isArray.add(false);
	}
	
	private void makeMapArray(String d,String o) {
		mapDoc.add(d);
		mapOdb.add(o);
		isArray.add(true);
	}
}