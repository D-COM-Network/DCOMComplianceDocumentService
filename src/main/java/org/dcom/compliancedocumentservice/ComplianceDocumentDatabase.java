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

import java.util.ArrayList;
import org.dcom.compliancedocumentservice.ComplianceDocumentIndexItem;
import org.dcom.core.compliancedocument.ComplianceDocument;

/**
*An interface that defines how the compliance document service should communicate with a database implementation
*
*/
public interface ComplianceDocumentDatabase {

	public ArrayList<ComplianceDocumentIndexItem> getDocumentIndex();
	public ArrayList<ComplianceDocumentIndexItem> getDocumentIndex(String jurisdiction);
	public ArrayList<ComplianceDocumentIndexItem> getDocumentIndex(String jurisdiction,String type);
	public String getLatestVersion(String jurisdiction,String type,String shortName);
	public ComplianceDocument getDocument(String baseURI,String jurisdiction,String type,String shortName,String version);
	public boolean checkVersionExists(String jurisdiction,String type,String shortName,String version);
	public void updateDocument(String jurisdiction,String type,String shortName,ComplianceDocument inDoc) throws Exception;

}