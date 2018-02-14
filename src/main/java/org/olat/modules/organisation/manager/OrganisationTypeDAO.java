/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.organisation.manager;

import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.modules.organisation.OrganisationType;
import org.olat.modules.organisation.model.OrganisationTypeImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 9 févr. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class OrganisationTypeDAO {
	
	@Autowired
	private DB dbInstance;
	
	public OrganisationType createAndPersist(String displayName, String identifier) {
		OrganisationTypeImpl type = new OrganisationTypeImpl();
		type.setCreationDate(new Date());
		type.setLastModified(type.getCreationDate());
		type.setDisplayName(displayName);
		type.setIdentifier(identifier);
		dbInstance.getCurrentEntityManager().persist(type);
		return type;
	}
	
	public OrganisationType loadByKey(Long key) {
		String q = "select type from organisationtype type where type.key=:key";
		List<OrganisationType> types = dbInstance.getCurrentEntityManager()
				.createQuery(q, OrganisationType.class).setParameter("key", key)
				.getResultList();
		return types == null || types.isEmpty() ? null : types.get(0);
	}

}
