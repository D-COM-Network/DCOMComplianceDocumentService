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

import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.executor.OResult;

/**
*This contains some helper methods to help the serialiser and deserialiser.
*
*/
public class OrientDBHelpers {

		public static String getInsertId(OResultSet r) {
			String resId=null;
			while (r.hasNext()) {
				OResult res=r.next();
				if (!res.getVertex().isPresent()) continue;
				resId=res.getVertex().get().getRecord().getIdentity().toString();
			}
			return resId;
		}
		
}