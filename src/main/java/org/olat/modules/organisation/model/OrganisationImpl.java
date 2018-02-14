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
package org.olat.modules.organisation.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.olat.basesecurity.Group;
import org.olat.basesecurity.model.GroupImpl;
import org.olat.core.id.Persistable;
import org.olat.modules.organisation.Organisation;
import org.olat.modules.organisation.OrganisationManagedFlag;
import org.olat.modules.organisation.OrganisationType;

/**
 * 
 * Initial date: 9 févr. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Entity(name="organisation")
@Table(name="o_org_organisation")
public class OrganisationImpl implements Persistable, Organisation {

	private static final long serialVersionUID = 3062294568262911860L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id", nullable=false, unique=true, insertable=true, updatable=false)
	private Long key;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creationdate", nullable=false, insertable=true, updatable=false)
	private Date creationDate;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="lastmodified", nullable=false, insertable=true, updatable=true)
	private Date lastModified;
	
	@Column(name="o_identifier", nullable=true, insertable=true, updatable=true)
	private String identifier;
	@Column(name="o_displayname", nullable=true, insertable=true, updatable=true)
	private String displayName;
	@Column(name="o_description", nullable=true, insertable=true, updatable=true)
	private String description;
	@Column(name="o_css_class", nullable=true, insertable=true, updatable=true)
	private String cssClass;
	@Column(name="o_status", nullable=true, insertable=true, updatable=true)
	private String status;
	
	@Column(name="o_m_path_keys", nullable=true, insertable=true, updatable=true)
	private String materializedPathKeys;
	
	@Column(name="o_external_id", nullable=true, insertable=true, updatable=true)
	private String externalId;
	@Column(name="o_managed_flags", nullable=true, insertable=true, updatable=true)
	private String managedFlagsString;
	
	@ManyToOne(targetEntity=GroupImpl.class,fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="fk_group", nullable=false, insertable=true, updatable=false)
	private Group group;
	
	@ManyToOne(targetEntity=OrganisationImpl.class,fetch=FetchType.LAZY,optional=true)
	@JoinColumn(name="fk_root", nullable=true, insertable=true, updatable=true)
	private Organisation root;
	
	@ManyToOne(targetEntity=OrganisationImpl.class,fetch=FetchType.LAZY,optional=true)
	@JoinColumn(name="fk_parent", nullable=true, insertable=true, updatable=true)
	private Organisation parent;
	
	@ManyToOne(targetEntity=OrganisationTypeImpl.class,fetch=FetchType.LAZY,optional=true)
	@JoinColumn(name="fk_type", nullable=true, insertable=true, updatable=true)
	private OrganisationType type;

	
	@Override
	public Long getKey() {
		return key;
	}
	
	public void setKey(Long key) {
		this.key = key;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}
	
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@Override
	public Date getLastModified() {
		return lastModified;
	}
	
	@Override
	public void setLastModified(Date date) {
		lastModified = date;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	public String getCssClass() {
		return cssClass;
	}

	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMaterializedPathKeys() {
		return materializedPathKeys;
	}

	public void setMaterializedPathKeys(String materializedPathKeys) {
		this.materializedPathKeys = materializedPathKeys;
	}

	@Override
	public String getExternalId() {
		return externalId;
	}

	@Override
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getManagedFlagsString() {
		return managedFlagsString;
	}

	public void setManagedFlagsString(String managedFlagsString) {
		this.managedFlagsString = managedFlagsString;
	}
	
	@Override
	public OrganisationManagedFlag[] getManagedFlags() {
		return OrganisationManagedFlag.toEnum(managedFlagsString);
	}
	
	@Override
	public void setManagedFlags(OrganisationManagedFlag[] flags) {
		managedFlagsString = OrganisationManagedFlag.toString(flags);
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Organisation getRoot() {
		return root;
	}

	public void setRoot(Organisation root) {
		this.root = root;
	}

	public Organisation getParent() {
		return parent;
	}

	public void setParent(Organisation parent) {
		this.parent = parent;
	}

	public OrganisationType getType() {
		return type;
	}

	public void setType(OrganisationType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		return key == null ? 147518 : key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj instanceof OrganisationImpl) {
			OrganisationImpl org = (OrganisationImpl)obj;
			return key != null && key.equals(org.getKey());
		}
		return super.equals(obj);
	}

	@Override
	public boolean equalsByPersistableKey(Persistable persistable) {
		return equals(persistable);
	}
}
