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

import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.modules.organisation.Organisation;
import org.olat.modules.organisation.OrganisationManagedFlag;
import org.olat.modules.organisation.OrganisationRef;
import org.olat.modules.organisation.OrganisationService;
import org.olat.modules.organisation.OrganisationType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 9 févr. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class OrganisationServiceImpl implements OrganisationService, InitializingBean {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private OrganisationDAO organisationDao;

	@Override
	public void afterPropertiesSet() throws Exception {
		List<Organisation> defaultOrganisations = organisationDao.loadByIdentifier(DEFAULT_ORGANISATION_IDENTIFIER);
		if(defaultOrganisations.isEmpty()) {
			Organisation organisation = organisationDao.create("OpenOLAT", DEFAULT_ORGANISATION_IDENTIFIER, null, null, null);
			organisation.setManagedFlags(new OrganisationManagedFlag[] {
					OrganisationManagedFlag.identifier, OrganisationManagedFlag.externalId,
					OrganisationManagedFlag.move, OrganisationManagedFlag.delete
				});
			organisationDao.update(organisation);
			dbInstance.commitAndCloseSession();
		}
	}

	@Override
	public Organisation createOrganisation(String displayName, String identifier, String description, Organisation organisation, OrganisationType type) {
		return organisationDao.createAndPersistOrganisation(displayName, identifier, description, organisation, type);
	}

	@Override
	public Organisation getOrganisation(OrganisationRef organisation) {
		return organisationDao.loadByKey(organisation.getKey());
	}

	@Override
	public Organisation updateOrganisation(Organisation organisation) {
		return organisationDao.update(organisation);
	}

	@Override
	public List<Organisation> getOrganisations() {
		return organisationDao.find();
	}
	
	

}
