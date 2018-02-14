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
package org.olat.basesecurity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.services.webdav.manager.WebDAVAuthManager;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Encoder;
import org.olat.login.LoginModule;
import org.olat.resource.OLATResource;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test the basic functions of the base security manager.
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class BaseSecurityManagerTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private UserManager userManager;
	@Autowired
	private LoginModule loginModule;
	@Autowired
	private BaseSecurity securityManager;
	
	
	@Test
	public void testCreateIdentity() {
		String username = "createid-" + UUID.randomUUID().toString();
		User user = userManager.createUser("first" + username, "last" + username, username + "@frentix.com");
		Identity identity = securityManager.createAndPersistIdentityAndUser(username, null, user, BaseSecurityModule.getDefaultAuthProviderIdentifier(), username, "secret");
		dbInstance.commitAndCloseSession();
		
		Assert.assertNotNull(identity);
		Assert.assertNotNull(identity.getKey());
		Assert.assertEquals(username, identity.getName());
		Assert.assertNotNull(identity.getUser());
		Assert.assertEquals(user, identity.getUser());
		Assert.assertEquals("first" + username, identity.getUser().getFirstName());
		Assert.assertEquals("last" + username, identity.getUser().getLastName());
		Assert.assertEquals("first" + username, identity.getUser().getProperty(UserConstants.FIRSTNAME, null));
		Assert.assertEquals("last" + username, identity.getUser().getProperty(UserConstants.LASTNAME, null));
		Assert.assertEquals(username + "@frentix.com", identity.getUser().getProperty(UserConstants.EMAIL, null));
	}
	
	/**
	 * This test is primarily made against Oracle
	 */
	@Test
	public void testCreateUpdateIdentity() {
		String username = "update-id-" + UUID.randomUUID().toString();
		User user = userManager.createUser("first" + username, "last" + username, username + "@frentix.com");
		user.setProperty(UserConstants.COUNTRY, "");
		user.setProperty(UserConstants.CITY, "Basel");
		Identity identity = securityManager.createAndPersistIdentityAndUser(username, null, user, BaseSecurityModule.getDefaultAuthProviderIdentifier(), username, "secret");
		dbInstance.commitAndCloseSession();
		
		//reload and update
		Identity identityPrime = securityManager.loadIdentityByKey(identity.getKey());
		identityPrime.getUser().setProperty(UserConstants.FIRSTNAME, "firstname");
		identityPrime.getUser().setProperty(UserConstants.COUNTRY, "CH");
		identityPrime.getUser().setProperty(UserConstants.CITY, "Lausanne");
		userManager.updateUserFromIdentity(identityPrime);
		dbInstance.commitAndCloseSession();
		
		//reload and check
		Identity identitySecond = securityManager.loadIdentityByKey(identity.getKey());
		dbInstance.commitAndCloseSession();//check the fetch join on user
		Assert.assertEquals("firstname", identitySecond.getUser().getProperty(UserConstants.FIRSTNAME, null));
		Assert.assertEquals("last" + username, identitySecond.getUser().getProperty(UserConstants.LASTNAME, null));
		Assert.assertEquals(username + "@frentix.com", identitySecond.getUser().getProperty(UserConstants.EMAIL, null));
		Assert.assertEquals("CH", identitySecond.getUser().getProperty(UserConstants.COUNTRY, null));
		Assert.assertEquals("Lausanne", identitySecond.getUser().getProperty(UserConstants.CITY, null));
	}
	
	
	@Test
	public void testEquals() {
		String identityTest1Name = "eq-1-" + UUID.randomUUID().toString();
		Identity ident1 = JunitTestHelper.createAndPersistIdentityAsUser(identityTest1Name);
		Identity ident2 = JunitTestHelper.createAndPersistIdentityAsUser("eq-2-" + UUID.randomUUID().toString());
		
		assertFalse("Wrong equals implementation, different types are recognized as equals ",ident1.equals(new Integer(1)));
		assertFalse("Wrong equals implementation, different users are recognized as equals ",ident1.equals(ident2));
		assertFalse("Wrong equals implementation, null value is recognized as equals ",ident1.equals(null));
		assertTrue("Wrong equals implementation, same users are NOT recognized as equals ",ident1.equals(ident1));
		Identity ident1_2 = securityManager.findIdentityByName(identityTest1Name);
		assertTrue("Wrong equals implementation, same users are NOT recognized as equals ",ident1.equals(ident1_2));
	}
	
	@Test
	public void testHashCode() {
		String identityTest1Name = "hash-1-" + UUID.randomUUID().toString();
		Identity ident1 = JunitTestHelper.createAndPersistIdentityAsUser(identityTest1Name);
		Identity ident2 = JunitTestHelper.createAndPersistIdentityAsUser("hash-2-" + UUID.randomUUID().toString());
		
		assertTrue("Wrong hashCode implementation, same users have NOT same hash-code ",ident1.hashCode() == ident1.hashCode());
		assertFalse("Wrong hashCode implementation, different users have same hash-code",ident1.hashCode() == ident2.hashCode());
		Identity ident1_2 = securityManager.findIdentityByName(identityTest1Name);
		assertTrue("Wrong hashCode implementation, same users have NOT same hash-code ",ident1.hashCode() == ident1_2.hashCode());
	}

	@Test
	public void testFindIdentityByUser() {
		//create a user it
		String username = "find-me-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(username);
		Assert.assertNotNull(id);
		Assert.assertNotNull(id.getUser());
		dbInstance.commitAndCloseSession();
		
		//find it
		Identity foundId = securityManager.findIdentityByUser(id.getUser());
		Assert.assertNotNull(foundId);
		Assert.assertEquals(username, foundId.getName());
		Assert.assertEquals(id, foundId);
		Assert.assertEquals(id.getUser(), foundId.getUser());
	}
	
	@Test
	public void testFindIdentityByName() {
		//create a user it
		String username = "find-me-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(username);
		Assert.assertNotNull(id);
		Assert.assertEquals(username, id.getName());
		dbInstance.commitAndCloseSession();
		
		//find it
		Identity foundId = securityManager.findIdentityByName(username);
		Assert.assertNotNull(foundId);
		Assert.assertEquals(username, foundId.getName());
		Assert.assertEquals(id, foundId);
	}
	
	@Test
	public void findIdentityByNameCaseInsensitive() {
		//create a user it
		String username = "find-ME-2-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(username);
		Assert.assertNotNull(id);
		Assert.assertEquals(username, id.getName());
		dbInstance.commitAndCloseSession();
		
		//find it
		Identity foundId = securityManager.findIdentityByNameCaseInsensitive(username);
		Assert.assertNotNull(foundId);
		Assert.assertEquals(username, foundId.getName());
		Assert.assertEquals(id, foundId);
		
		//find it lower case
		Identity foundLoweredId = securityManager.findIdentityByNameCaseInsensitive(username.toLowerCase());
		Assert.assertNotNull(foundLoweredId);
		Assert.assertEquals(username, foundLoweredId.getName());
		Assert.assertEquals(id, foundLoweredId);
		
		//find it upper case
		Identity foundUpperedId = securityManager.findIdentityByNameCaseInsensitive(username.toUpperCase());
		Assert.assertNotNull(foundUpperedId);
		Assert.assertEquals(username, foundUpperedId.getName());
		Assert.assertEquals(id, foundUpperedId);
	}
	
	@Test
	public void testFindIdentityByNames() {
		//create a user it
		String name1 = "find-me-1-" + UUID.randomUUID().toString();
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser(name1);
		String name2 = "find-me-2-" + UUID.randomUUID().toString();
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser(name2);
		dbInstance.commitAndCloseSession();
		
		//find it
		List<String> names = new ArrayList<String>(2);
		names.add(name1);
		names.add(name2);
		List<Identity> foundIds = securityManager.findIdentitiesByName(names);
		Assert.assertNotNull(foundIds);
		Assert.assertEquals(2, foundIds.size());
		Assert.assertTrue(foundIds.contains(id1));
		Assert.assertTrue(foundIds.contains(id2));
	}
	
	@Test
	public void findIdentityByNamesCaseInsensitive() {
		//create a user it
		String username1 = "fINd-ME-4-" + UUID.randomUUID();
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser(username1);
		String username2 = "fINd-ME-5-" + UUID.randomUUID();
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser(username2);
		Assert.assertNotNull(id1);
		Assert.assertEquals(username1, id1.getName());
		Assert.assertNotNull(id2);
		Assert.assertEquals(username2, id2.getName());
		dbInstance.commitAndCloseSession();
		
		List<String> names = new ArrayList<String>(2);
		names.add(username1);
		names.add(username2);
		
		//find it
		List<Identity> foundIds = securityManager.findIdentitiesByNameCaseInsensitive(names);
		Assert.assertNotNull(foundIds);
		Assert.assertEquals(2, foundIds.size());
		Assert.assertTrue(foundIds.contains(id1));
		Assert.assertTrue(foundIds.contains(id2));
	}
	
	@Test
	public void findIdentitiesByNumber() {
		//create a user it
		String username = "fINd-ME-6-" + UUID.randomUUID();
		String institutionalNumber = UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(username);
		id.getUser().setProperty(UserConstants.INSTITUTIONALUSERIDENTIFIER, institutionalNumber);
		userManager.updateUserFromIdentity(id);
		dbInstance.commitAndCloseSession();
		
		List<String> numbers = new ArrayList<String>(2);
		numbers.add(institutionalNumber);

		//find it
		List<Identity> foundIds = securityManager.findIdentitiesByNumber(numbers);
		Assert.assertNotNull(foundIds);
		Assert.assertEquals(1, foundIds.size());
		Assert.assertTrue(foundIds.contains(id));
	}
	
	@Test
	public void loadIdentityShortByKey() {
		//create a user it
		String idName = "find-me-short-1-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(idName);
		dbInstance.commitAndCloseSession();
		
		//find it
		IdentityShort foundId = securityManager.loadIdentityShortByKey(id.getKey());
		Assert.assertNotNull(foundId);
		Assert.assertEquals(id.getKey(), foundId.getKey());
		Assert.assertEquals(idName, foundId.getName());
		Assert.assertEquals(id.getUser().getEmail(), foundId.getEmail());
		Assert.assertEquals(id.getUser().getFirstName(), foundId.getFirstName());
		Assert.assertEquals(id.getUser().getLastName(), foundId.getLastName());
		Assert.assertNotNull(foundId.getLastLogin());
		Assert.assertEquals(id.getUser().getKey(), foundId.getUserKey());
		Assert.assertTrue(foundId.getStatus() < Identity.STATUS_VISIBLE_LIMIT);
	}
	
	@Test
	public void testGetSecurityGroupsForIdentity() {
		// create
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser( "find-sec-" + UUID.randomUUID().toString());
		SecurityGroup secGroup1 = securityManager.createAndPersistSecurityGroup();
		SecurityGroup secGroup2 = securityManager.createAndPersistSecurityGroup();
		SecurityGroup secGroup3 = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(id, secGroup1);
		securityManager.addIdentityToSecurityGroup(id, secGroup2);
		dbInstance.commitAndCloseSession();
		
		//check
		List<SecurityGroup> secGroups = securityManager.getSecurityGroupsForIdentity(id);
		Assert.assertNotNull(secGroups);
		Assert.assertTrue(secGroups.contains(secGroup1));
		Assert.assertTrue(secGroups.contains(secGroup2));
		Assert.assertFalse(secGroups.contains(secGroup3));
	}
	
	@Test
	public void testCreateNamedGroup() {
		String username = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		SecurityGroup ng = securityManager.createAndPersistNamedSecurityGroup(username);
		dbInstance.commitAndCloseSession();
		
		SecurityGroup sgFound = securityManager.findSecurityGroupByName(username);
		Assert.assertNotNull(sgFound);
		Assert.assertEquals(sgFound.getKey(), ng.getKey());
	}
	
	@Test
	public void testRemoveFromSecurityGroup() {
		//create a security group with 2 identites
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser( "rm-1-sec-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser( "rm-2-sec-" + UUID.randomUUID().toString());
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(id1, secGroup);
		securityManager.addIdentityToSecurityGroup(id2, secGroup);
		dbInstance.commitAndCloseSession();
		
		//remove the first one
		securityManager.removeIdentityFromSecurityGroup(id1, secGroup);
		dbInstance.commitAndCloseSession();
		
		int countMembers = securityManager.countIdentitiesOfSecurityGroup(secGroup);
		Assert.assertEquals(1, countMembers);
		List<Identity> members = securityManager.getIdentitiesOfSecurityGroup(secGroup);
		Assert.assertNotNull(members);
		Assert.assertEquals(1, members.size());
		Assert.assertEquals(id2, members.get(0));
	}
	
	@Test
	public void testRemoveFromSecurityGroup_list() {
		//create a security group with 2 identites
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser( "rm-3-sec-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser( "rm-4-sec-" + UUID.randomUUID().toString());
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(id1, secGroup);
		securityManager.addIdentityToSecurityGroup(id2, secGroup);
		dbInstance.commitAndCloseSession();
		
		//remove the first one
		List<Identity> ids = new ArrayList<Identity>();
		ids.add(id1);
		ids.add(id2);
		securityManager.removeIdentityFromSecurityGroups(ids, Collections.singletonList(secGroup));
		dbInstance.commitAndCloseSession();
		
		int countMembers = securityManager.countIdentitiesOfSecurityGroup(secGroup);
		Assert.assertEquals(0, countMembers);
		List<Identity> members = securityManager.getIdentitiesOfSecurityGroup(secGroup);
		Assert.assertNotNull(members);
		Assert.assertTrue(members.isEmpty());
		
		//check if robust against null and empty
		securityManager.removeIdentityFromSecurityGroups(ids, Collections.<SecurityGroup>emptyList());
		securityManager.removeIdentityFromSecurityGroups(Collections.<Identity>emptyList(), Collections.singletonList(secGroup));
		securityManager.removeIdentityFromSecurityGroups(ids, null);
		securityManager.removeIdentityFromSecurityGroups(null, Collections.singletonList(secGroup));
	}
	
	/**
	 * 
	 */
	@Test
	public void testLoadIdentityByKeys() {
		//create a security group with 2 identites
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser( "load-1-sec-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser( "load-2-sec-" + UUID.randomUUID().toString());
		dbInstance.commitAndCloseSession();
		
		List<Long> keys = new ArrayList<Long>(2);
		keys.add(id1.getKey());
		keys.add(id2.getKey());
		List<Identity> identities = securityManager.loadIdentityByKeys(keys);
		Assert.assertNotNull(identities);
		Assert.assertEquals(2, identities.size());
		Assert.assertTrue(identities.contains(id1));
		Assert.assertTrue(identities.contains(id2));
	}
	
	/**
	 * Update roles
	 */
	@Test
	public void testUpdateRoles_giveAllRights() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser( "roles-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser( "roles-" + UUID.randomUUID().toString());
		Roles roles = securityManager.getRoles(id1);
		Assert.assertNotNull(roles);
		dbInstance.commitAndCloseSession();

		//update roles
		Roles modifiedRoles = new Roles(true, true, true, true, false, true, true, true, false);
		securityManager.updateRoles(id2, id1, modifiedRoles);
		dbInstance.commitAndCloseSession();
		
		//check roles
		Roles reloadRoles = securityManager.getRoles(id1);
		Assert.assertNotNull(reloadRoles);
		Assert.assertTrue(reloadRoles.isAuthor());
		Assert.assertTrue(reloadRoles.isGroupManager());
		Assert.assertFalse(reloadRoles.isGuestOnly());
		Assert.assertTrue(reloadRoles.isInstitutionalResourceManager());
		Assert.assertFalse(reloadRoles.isInvitee());
		Assert.assertTrue(reloadRoles.isOLATAdmin());
		Assert.assertTrue(reloadRoles.isPoolAdmin());
		Assert.assertTrue(reloadRoles.isCurriculumManager());
		Assert.assertTrue(reloadRoles.isUserManager());
	}
	
	/**
	 * Update roles
	 */
	@Test
	public void testUpdateRoles_someRights() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser( "roles-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser( "roles-" + UUID.randomUUID().toString());
		Roles roles = securityManager.getRoles(id1);
		Assert.assertNotNull(roles);
		dbInstance.commitAndCloseSession();

		//update roles
		Roles modifiedRoles = new Roles(false, true, false, true, false, false, false, true, false);
		securityManager.updateRoles(id2, id1, modifiedRoles);
		dbInstance.commitAndCloseSession();
		
		//check roles
		Roles reloadRoles = securityManager.getRoles(id1);
		Assert.assertNotNull(reloadRoles);
		Assert.assertTrue(reloadRoles.isAuthor());
		Assert.assertFalse(reloadRoles.isGroupManager());
		Assert.assertFalse(reloadRoles.isGuestOnly());
		Assert.assertFalse(reloadRoles.isInstitutionalResourceManager());
		Assert.assertFalse(reloadRoles.isInvitee());
		Assert.assertFalse(reloadRoles.isOLATAdmin());
		Assert.assertFalse(reloadRoles.isPoolAdmin());
		Assert.assertTrue(reloadRoles.isCurriculumManager());
		Assert.assertTrue(reloadRoles.isUserManager());
	}
	
	/**
	 * Update roles, check that invitee don't become rights
	 */
	@Test
	public void testUpdateRoles_guest() {
		Identity invitee = JunitTestHelper.createAndPersistIdentityAsUser("invitee-" + UUID.randomUUID().toString());
		Identity user = JunitTestHelper.createAndPersistIdentityAsUser("invitee-" + UUID.randomUUID().toString());
		Roles roles = securityManager.getRoles(invitee);
		Assert.assertNotNull(roles);
		dbInstance.commitAndCloseSession();

		//update roles
		Roles modifiedRoles = new Roles(true, true, true, true, true, true, false);
		securityManager.updateRoles(user, invitee, modifiedRoles);
		dbInstance.commitAndCloseSession();

		//check roles
		Roles reloadRoles = securityManager.getRoles(invitee);
		Assert.assertNotNull(reloadRoles);
		Assert.assertFalse(reloadRoles.isAuthor());
		Assert.assertFalse(reloadRoles.isGroupManager());
		Assert.assertTrue(reloadRoles.isGuestOnly());
		Assert.assertFalse(reloadRoles.isInstitutionalResourceManager());
		Assert.assertFalse(reloadRoles.isInvitee());
		Assert.assertFalse(reloadRoles.isOLATAdmin());
		Assert.assertFalse(reloadRoles.isUserManager());
	}
	
	/**
	 * Test method @see org.olat.basesecurity.BaseSecurityManager.getIdentitiesByPowerSearch()
	 * with a list of identity keys as parameters.<br/>
	 * getIdentitiesByPowerSearch is a dynamic generated query and we need
	 * to test some aspects of it.
	 */
	@Test
	public void testGetIdentityByPowerSearch_IdentityKeys() {
		String login = "pow-1-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(login);
		dbInstance.commitAndCloseSession();
		
		SearchIdentityParams params = new SearchIdentityParams();
		params.setIdentityKeys(Collections.singletonList(id.getKey()));
		
		List<Identity> ids = securityManager.getIdentitiesByPowerSearch(params, 0, -1);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals(id, ids.get(0));
	}
	
	/**
	 * Check the method @see getIdentitiesByPowerSearch
	 * with a login as parameter.<br/>
	 * getIdentitiesByPowerSearch is a dynamic generated query and we need
	 * to test some aspects of it.
	 */
	@Test
	public void testGetIdentityByPowerSearch_Login() {
		String login = "pow-2-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(login);
		dbInstance.commitAndCloseSession();
		
		SearchIdentityParams params = new SearchIdentityParams();
		params.setLogin(login);
		
		List<Identity> ids = securityManager.getIdentitiesByPowerSearch(params, 0, -1);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals(id, ids.get(0));
	}
	
	/**
	 * Check the method @see getIdentitiesByPowerSearch
	 * with a user property as parameter.<br/>
	 * getIdentitiesByPowerSearch is a dynamic generated query and we need
	 * to test some aspects of it.
	 */
	@Test
	public void testGetIdentityByPowerSearch_UserProperty() {
		//create a user with a first name
		String login = "pow-3-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(login);
		String firstName = id.getUser().getProperty(UserConstants.FIRSTNAME, null);
		dbInstance.commitAndCloseSession();
		
		SearchIdentityParams params = new SearchIdentityParams();
		Map<String,String> props = new HashMap<String,String>();
		props.put(UserConstants.FIRSTNAME, firstName);
		params.setUserProperties(props);
		
		List<Identity> ids = securityManager.getIdentitiesByPowerSearch(params, 0, -1);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals(id, ids.get(0));
	}
	
	/**
	 * Check the method @see getIdentitiesByPowerSearch
	 * with a login and a list of identity keys as parameters.<br/>
	 * getIdentitiesByPowerSearch is a dynamic generated query and we need
	 * to test some aspects of it.
	 */
	@Test
	public void testGetIdentityByPowerSearch_LoginIdentityKeys() {
		String login = "pow-4-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(login);
		dbInstance.commitAndCloseSession();
		
		SearchIdentityParams params = new SearchIdentityParams();
		params.setLogin(login);
		params.setIdentityKeys(Collections.singletonList(id.getKey()));
		
		List<Identity> ids = securityManager.getIdentitiesByPowerSearch(params, 0, -1);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals(id, ids.get(0));
	}
	
	/**
	 * Test the method @see getIdentitiesByPowerSearch
	 * with 2 user properties and a list of identity keys as parameters.<br/>
	 * getIdentitiesByPowerSearch is a dynamic generated query and we need
	 * to test some aspects of it.
	 */
	@Test
	public void testGetIdentityByPowerSearch_LoginIdentityKeysProperty() {
		String login = "pow-5-" + UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(login);
		dbInstance.commitAndCloseSession();
		
		SearchIdentityParams params = new SearchIdentityParams();
		params.setLogin(login);
		Map<String,String> props = new HashMap<String,String>();
		props.put(UserConstants.FIRSTNAME, id.getUser().getProperty(UserConstants.FIRSTNAME, null));
		props.put(UserConstants.LASTNAME, id.getUser().getProperty(UserConstants.LASTNAME, null));
		params.setUserProperties(props);
		params.setIdentityKeys(Collections.singletonList(id.getKey()));
		
		List<Identity> ids = securityManager.getIdentitiesByPowerSearch(params, 0, -1);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals(id, ids.get(0));
	}
	
	@Test
	public void testGetIdentityByPowerSearch_managed() {
		String login = "pow-6-" + UUID.randomUUID();
		String externalId = UUID.randomUUID().toString();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser(login);
		dbInstance.commitAndCloseSession();
		securityManager.setExternalId(id, externalId);
		dbInstance.commitAndCloseSession();
		
		//search managed
		SearchIdentityParams params = new SearchIdentityParams();
		params.setManaged(Boolean.TRUE);
		List<Identity> managedIds = securityManager.getIdentitiesByPowerSearch(params, 0, -1);
		Assert.assertNotNull(managedIds);
		Assert.assertFalse(managedIds.isEmpty());
		Assert.assertTrue(managedIds.contains(id));
		for(Identity managedId:managedIds) {
			Assert.assertNotNull(managedId.getExternalId());
		}
		
		//search not managed
		params.setManaged(Boolean.FALSE);
		List<Identity> naturalIds = securityManager.getIdentitiesByPowerSearch(params, 0, -1);
		Assert.assertNotNull(naturalIds);
		Assert.assertFalse(naturalIds.contains(id));
		for(Identity naturalId:naturalIds) {
			Assert.assertNull(naturalId.getExternalId());
		}
	}
	
	
	@Test
	public void testGetIdentitiesByPowerSearchWithGroups() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("user-1-" + UUID.randomUUID().toString());
		SecurityGroup usersGroup = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		dbInstance.commitAndCloseSession();
		
		//test positive result
	  SecurityGroup[] groups = { usersGroup };
	  List<Identity> userList = securityManager.getVisibleIdentitiesByPowerSearch(id.getName(), null, true, groups, null, null, null, null);
		Assert.assertNotNull(userList);
	  Assert.assertEquals(1, userList.size());
		Assert.assertEquals(id, userList.get(0));
	  
		//test negatif -> with author security group
		SecurityGroup[] authors = { securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS) };
		List<Identity> authorList = securityManager.getVisibleIdentitiesByPowerSearch(id.getName(), null, true, authors, null, null, null, null);
		Assert.assertNotNull(authorList);
		Assert.assertTrue(authorList.isEmpty());
	}
	
	@Test
	public void testGetIdentitiesOfSecurityGroup() {
		//create 3 identities and 2 security groups
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("user-sec-1-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("user-sec-2-" + UUID.randomUUID().toString());
		Identity id3 = JunitTestHelper.createAndPersistIdentityAsUser("user-sec-3-" + UUID.randomUUID().toString());
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(id1, secGroup);
		securityManager.addIdentityToSecurityGroup(id2, secGroup);
		securityManager.addIdentityToSecurityGroup(id3, secGroup);
		dbInstance.commitAndCloseSession();
		
		//retrieve them
		List<Identity> identities = securityManager.getIdentitiesOfSecurityGroup(secGroup, 0, -1);
		Assert.assertNotNull(identities);
		Assert.assertEquals(3, identities.size());
		Assert.assertTrue(identities.contains(id1));
		Assert.assertTrue(identities.contains(id2));
		Assert.assertTrue(identities.contains(id3));
	}
	
	@Test
	public void testGetIdentitiesOfSecurityGroups() {
		//create 3 identities and 2 security groups
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("user-sec-1-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("user-sec-2-" + UUID.randomUUID().toString());
		Identity id3 = JunitTestHelper.createAndPersistIdentityAsUser("user-sec-3-" + UUID.randomUUID().toString());
		SecurityGroup secGroup1 = securityManager.createAndPersistSecurityGroup();
		SecurityGroup secGroup2 = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(id1, secGroup1);
		securityManager.addIdentityToSecurityGroup(id2, secGroup1);
		securityManager.addIdentityToSecurityGroup(id2, secGroup2);
		securityManager.addIdentityToSecurityGroup(id3, secGroup2);
		dbInstance.commitAndCloseSession();
		
		//retrieve them
		List<SecurityGroup> secGroups = new ArrayList<SecurityGroup>();
		secGroups.add(secGroup1);
		secGroups.add(secGroup2);
		List<Identity> identities = securityManager.getIdentitiesOfSecurityGroups(secGroups);
		Assert.assertNotNull(identities);
		Assert.assertEquals(3, identities.size());
		Assert.assertTrue(identities.contains(id1));
		Assert.assertTrue(identities.contains(id2));
		Assert.assertTrue(identities.contains(id3));
	}
	

	@Test
	public void testGetPoliciesOfSecurityGroup() {
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		OLATResource resource = JunitTestHelper.createRandomResource();
		Policy policy_1 = securityManager.createAndPersistPolicy(secGroup, "test.right1", resource);
		Policy policy_2 = securityManager.createAndPersistPolicy(secGroup, "test.right2", resource);
		dbInstance.commitAndCloseSession();
		
		List<Policy> policies = securityManager.getPoliciesOfSecurityGroup(secGroup);
		Assert.assertNotNull(policies);
		Assert.assertEquals(2, policies.size());
		Assert.assertTrue(policies.contains(policy_1));
		Assert.assertTrue(policies.contains(policy_2));		
	}
		
	/**
	 * Test the method
	 * @see public List<Policy> getPoliciesOfResource(OLATResource resource, SecurityGroup secGroup)
	 */
	@Test
	public void testGetPoliciesOfResource() {
		//create 3 rights
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		OLATResource resource = JunitTestHelper.createRandomResource();
		Policy policy_1 = securityManager.createAndPersistPolicy(secGroup, "test.right1", resource);
		Policy policy_2 = securityManager.createAndPersistPolicy(secGroup, "test.right2", resource);
		Policy policy_3 = securityManager.createAndPersistPolicy(secGroup, "test.right3", resource);
		dbInstance.commitAndCloseSession();
		
		//test the method
		List<Policy> policies = securityManager.getPoliciesOfResource(resource, secGroup);
		Assert.assertNotNull(policies);
		Assert.assertEquals(3, policies.size());
		Assert.assertTrue(policies.contains(policy_1));
		Assert.assertTrue(policies.contains(policy_2));
		Assert.assertTrue(policies.contains(policy_3));
	}
	
	@Test
	public void testGetPoliciesOfIdentity() {
		//create 3 security groups and 2 resources and an identity
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("test-right-1-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("test-right-2-" + UUID.randomUUID().toString());
		SecurityGroup secGroup_1 = securityManager.createAndPersistSecurityGroup();
		SecurityGroup secGroup_2 = securityManager.createAndPersistSecurityGroup();
		SecurityGroup secGroup_3 = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(id1, secGroup_1);
		securityManager.addIdentityToSecurityGroup(id1, secGroup_2);
		securityManager.addIdentityToSecurityGroup(id2, secGroup_2);
		OLATResource resource_1 = JunitTestHelper.createRandomResource();
		OLATResource resource_2 = JunitTestHelper.createRandomResource();
		Policy policy_1_1 = securityManager.createAndPersistPolicy(secGroup_1, "test.right11", resource_1);
		Policy policy_1_2 = securityManager.createAndPersistPolicy(secGroup_1, "test.right12", resource_2);
		Policy policy_2_1 = securityManager.createAndPersistPolicy(secGroup_2, "test.right21", resource_1);
		Policy policy_3_1 = securityManager.createAndPersistPolicy(secGroup_3, "test.right31", resource_1);
		Policy policy_3_2 = securityManager.createAndPersistPolicy(secGroup_3, "test.right32", resource_2);
		dbInstance.commitAndCloseSession();
		
		//test the method for id1
		List<Policy> policiesId1 = securityManager.getPoliciesOfIdentity(id1);
		Assert.assertNotNull(policiesId1);
		Assert.assertTrue(3 <= policiesId1.size());// OpenOLAT add automatically some standard policy to every user
		Assert.assertTrue(policiesId1.contains(policy_1_1));
		Assert.assertTrue(policiesId1.contains(policy_1_2));
		Assert.assertTrue(policiesId1.contains(policy_2_1));
		Assert.assertFalse(policiesId1.contains(policy_3_1));
		Assert.assertFalse(policiesId1.contains(policy_3_2));
		
		//test the method for id2
		List<Policy> policiesId2 = securityManager.getPoliciesOfIdentity(id2);
		Assert.assertNotNull(policiesId2);
		Assert.assertTrue(1 <= policiesId2.size());// OpenOLAT add automatically some standard policy to every user
		Assert.assertFalse(policiesId2.contains(policy_1_1));
		Assert.assertFalse(policiesId2.contains(policy_1_2));
		Assert.assertTrue(policiesId2.contains(policy_2_1));
		Assert.assertFalse(policiesId2.contains(policy_3_1));
		Assert.assertFalse(policiesId2.contains(policy_3_2));
	}
	
	@Test
	public void testGetPoliciesOfIdentity_2() {
		Identity s1 = JunitTestHelper.createAndPersistIdentityAsUser("s1-" + UUID.randomUUID().toString());
		OLATResource olatres = JunitTestHelper.createRandomResource();
		SecurityGroup olatUsersGroup = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		securityManager.createAndPersistPolicy(olatUsersGroup, Constants.PERMISSION_ACCESS, olatres);
		List<Policy> policies = securityManager.getPoliciesOfIdentity(s1);
		
		boolean foundPolicy = false;
		for (Iterator<Policy> iterator = policies.iterator(); iterator.hasNext();) {
			Policy policy = iterator.next();
			Long resourcableId = policy.getOlatResource().getResourceableId();
			if ((resourcableId != null) && (resourcableId.equals(olatres.getResourceableId()))) {
				assertEquals(olatUsersGroup.getKey(), policy.getSecurityGroup().getKey());
				assertEquals(Constants.PERMISSION_ACCESS, policy.getPermission());
				assertEquals(olatres.getResourceableId(), policy.getOlatResource().getResourceableId());
				foundPolicy = true;
			}
		}
		assertTrue("Does not found policy", foundPolicy);
	}
	
	@Test
	public void isIdentityPermittedOnResourceable_checkType() {
		//create an identity, a security group, a resource and give the identity some
		//permissions on the resource
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		OLATResource resource = JunitTestHelper.createRandomResource();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("test-ipor-1-" + UUID.randomUUID().toString());
		securityManager.addIdentityToSecurityGroup(id, secGroup);
		securityManager.createAndPersistPolicy(secGroup, "test.ipor-1_1", resource);
		securityManager.createAndPersistPolicy(secGroup, "test.ipor-1_2", resource);
		dbInstance.commitAndCloseSession();
		
		//check
		boolean hasIpor_1_1 = securityManager.isIdentityPermittedOnResourceable(id, "test.ipor-1_1", resource);
		Assert.assertTrue(hasIpor_1_1);
		boolean hasIpor_1_2 = securityManager.isIdentityPermittedOnResourceable(id, "test.ipor-1_2", resource);
		Assert.assertTrue(hasIpor_1_2);
		boolean hasIpor_1_3 = securityManager.isIdentityPermittedOnResourceable(id, "test.ipor-1_3", resource);
		Assert.assertFalse(hasIpor_1_3);
		
		//check type
		boolean hasIpor_1_1_ct = securityManager.isIdentityPermittedOnResourceable(id, "test.ipor-1_1", resource, true);
		Assert.assertTrue(hasIpor_1_1_ct);
		boolean hasIpor_1_2_ct = securityManager.isIdentityPermittedOnResourceable(id, "test.ipor-1_2", resource, true);
		Assert.assertTrue(hasIpor_1_2_ct);
		boolean hasIpor_1_3_ct = securityManager.isIdentityPermittedOnResourceable(id, "test.ipor-1_3", resource, true);
		Assert.assertFalse(hasIpor_1_3_ct);
	}
	
	@Test
	public void isIdentityPermittedOnResourceable_noCheckType() {
		//create an identity, a security group, a resource and give the identity some
		//permissions on the resource
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		OLATResource resource = JunitTestHelper.createRandomResource();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("test-ipornc-1-" + UUID.randomUUID().toString());
		securityManager.addIdentityToSecurityGroup(id, secGroup);
		securityManager.createAndPersistPolicy(secGroup, "test.ipornc-1_1", resource);
		securityManager.createAndPersistPolicy(secGroup, "test.ipornc-1_2", resource);
		dbInstance.commitAndCloseSession();
		
		//check
		boolean hasIpor_1_1 = securityManager.isIdentityPermittedOnResourceable(id, "test.ipornc-1_1", resource, false);
		Assert.assertTrue(hasIpor_1_1);
		boolean hasIpor_1_2 = securityManager.isIdentityPermittedOnResourceable(id, "test.ipornc-1_2", resource, false);
		Assert.assertTrue(hasIpor_1_2);
		boolean hasIpor_1_3 = securityManager.isIdentityPermittedOnResourceable(id, "test.ipornc-1_3", resource, false);
		Assert.assertFalse(hasIpor_1_3);
	}
	
	@Test
	public void isIdentityPermittedOnResourceable_null() {
		//create an identity, a security group, a resource and give the identity some
		//permissions on the resource
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		OLATResource resource = JunitTestHelper.createRandomResource();
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("test-ipornc-null-" + UUID.randomUUID().toString());
		securityManager.addIdentityToSecurityGroup(id, secGroup);
		securityManager.createAndPersistPolicy(secGroup, "test.ipornc-null", resource);
		dbInstance.commitAndCloseSession();
		
		//check that null doesn't return an exception but false
		boolean hasIpor = securityManager.isIdentityPermittedOnResourceable(null, "test.ipornc-null", resource, false);
		Assert.assertFalse(hasIpor);
	}

	/**
	 * Dummy test to make sure all works as wanted
	 */
	@Test
	public void createSecurityGroupMembership() {
		//create a user with the default provider
		Identity identity = JunitTestHelper.createAndPersistIdentityAsUser("update-membership-" + UUID.randomUUID().toString());
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(identity, secGroup);
		dbInstance.commitAndCloseSession();

		boolean member = securityManager.isIdentityInSecurityGroup(identity, secGroup);
		Assert.assertTrue(member);
	}
	
	/**
	 * We remove the optimistic locking from SecurityGroupMembershipImpl mapping
	 */
	@Test
	public void createAndUpdateSecurityGroupMembership_lastCommitWin() {
		//create a user with the default provider
		Identity identity = JunitTestHelper.createAndPersistIdentityAsUser("update-membership-" + UUID.randomUUID().toString());
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		
		SecurityGroupMembershipImpl sgmsi = new SecurityGroupMembershipImpl();
		sgmsi.setIdentity(identity);
		sgmsi.setSecurityGroup(secGroup);
		sgmsi.setLastModified(new Date());
		dbInstance.getCurrentEntityManager().persist(sgmsi);
		dbInstance.commitAndCloseSession();

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		sgmsi.setLastModified(cal.getTime());
		dbInstance.getCurrentEntityManager().merge(sgmsi);
		dbInstance.commitAndCloseSession();
	
		cal.add(Calendar.DATE, -1);
		sgmsi.setLastModified(cal.getTime());
		dbInstance.getCurrentEntityManager().merge(sgmsi);
		dbInstance.commitAndCloseSession();	
	}
	
	@Test
	public void findAuthenticationName() {
		Identity ident = JunitTestHelper.createAndPersistIdentityAsRndUser("auth-d-");
		dbInstance.commitAndCloseSession();
		
		Authentication auth = securityManager.findAuthentication(ident, "OLAT");
		Assert.assertNotNull(auth);
		
		String authName = securityManager.findAuthenticationName(ident, "OLAT");
		Assert.assertNotNull(authName);
	}
	
	@Test
	public void updateToSaltedAuthentication() {
		Identity ident = JunitTestHelper.createAndPersistIdentityAsUser("auth-c-" + UUID.randomUUID().toString());
		dbInstance.commitAndCloseSession();
		
		Authentication auth = securityManager.findAuthentication(ident, "OLAT");
		String credentials = auth.getCredential();
		Authentication updatedAuth = securityManager.updateCredentials(auth, "secret", loginModule.getDefaultHashAlgorithm());
		Assert.assertNotNull(auth);
		Assert.assertNotNull(updatedAuth);
		Assert.assertEquals(auth, updatedAuth);
		Assert.assertFalse(credentials.equals(updatedAuth.getCredential()));
		dbInstance.commitAndCloseSession();
		
		Authentication auth2 = securityManager.findAuthentication(ident, "OLAT");
		String credentials2 = auth2.getCredential();
		Authentication notUpdatedAuth = securityManager.updateCredentials(auth2, "secret", loginModule.getDefaultHashAlgorithm());
		Assert.assertNotNull(auth2);
		Assert.assertNotNull(notUpdatedAuth);
		Assert.assertSame(auth2, notUpdatedAuth);
		Assert.assertEquals(credentials2, notUpdatedAuth.getCredential());
		Assert.assertFalse(credentials.equals(notUpdatedAuth.getCredential()));
		dbInstance.commitAndCloseSession();
	}
	
	@Test
	public void deleteAuthentication() {
		Identity identity = JunitTestHelper.createAndPersistIdentityAsUser("auth-del-" + UUID.randomUUID().toString());
		Authentication auth = securityManager.createAndPersistAuthentication(identity, "del-test", identity.getName(), "secret", Encoder.Algorithm.sha512);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(auth);
		
		//reload and check
		Authentication reloadedAuth = securityManager.findAuthentication(identity, "del-test");
		Assert.assertNotNull(reloadedAuth);
		Assert.assertEquals(auth, reloadedAuth);
		dbInstance.commitAndCloseSession();
		
		//delete
		securityManager.deleteAuthentication(auth);
	}
	
	@Test
	public void deleteAuthentication_checkTransactionSurvive() {
		Identity identity = JunitTestHelper.createAndPersistIdentityAsUser("auth-del-" + UUID.randomUUID().toString());
		Authentication auth = securityManager.createAndPersistAuthentication(identity, "del-test", identity.getName(), "secret", Encoder.Algorithm.sha512);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(auth);
		
		//delete
		securityManager.deleteAuthentication(auth);
		dbInstance.commitAndCloseSession();
		
		//delete deleted auth
		securityManager.deleteAuthentication(auth);
		//check that the transaction is not in "rollback" mode
		Identity reloadedId = securityManager.loadIdentityByKey(identity.getKey());
		Assert.assertEquals(identity, reloadedId);
		dbInstance.commitAndCloseSession();
	}
	
	@Test
	public void deleteInvalidAuthenticationsByEmail() {
		Identity identity = JunitTestHelper.createAndPersistIdentityAsUser("auth-del-email-" + UUID.randomUUID().toString());
		User user = identity.getUser();
		String email = user.getEmail();
		securityManager.createAndPersistAuthentication(identity, "OLAT", email, "secret", Encoder.Algorithm.sha512);
		securityManager.createAndPersistAuthentication(identity, "del-mail", email, "secret", Encoder.Algorithm.sha512);
		securityManager.createAndPersistAuthentication(identity, WebDAVAuthManager.PROVIDER_HA1_EMAIL, email, "secret", Encoder.Algorithm.sha512);
		securityManager.createAndPersistAuthentication(identity, WebDAVAuthManager.PROVIDER_HA1_INSTITUTIONAL_EMAIL, email, "secret", Encoder.Algorithm.sha512);
		securityManager.createAndPersistAuthentication(identity, WebDAVAuthManager.PROVIDER_WEBDAV_EMAIL, email, "secret", Encoder.Algorithm.sha512);
		securityManager.createAndPersistAuthentication(identity, WebDAVAuthManager.PROVIDER_WEBDAV_INSTITUTIONAL_EMAIL, email, "secret", Encoder.Algorithm.sha512);
		dbInstance.commitAndCloseSession();
		
		// User with email address exists: The authentications are valid.
		securityManager.deleteInvalidAuthenticationsByEmail(email);
		dbInstance.commitAndCloseSession();
		
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_HA1_EMAIL));
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_HA1_INSTITUTIONAL_EMAIL));
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_WEBDAV_EMAIL));
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_WEBDAV_INSTITUTIONAL_EMAIL));
		Assert.assertNull(securityManager.findAuthenticationByAuthusername(email, "OLAT"));
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(identity.getName(), "OLAT"));
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(email, "del-mail"));
		
		// Email of the user changed: The authentications are not valid any longer.
		user.setProperty(UserConstants.EMAIL, "new@trashcmail.com");
		user.setProperty(UserConstants.INSTITUTIONALEMAIL, "new@trashcmail.com");
		userManager.updateUser(user);
		dbInstance.commitAndCloseSession();
		
		securityManager.deleteInvalidAuthenticationsByEmail(email);
		dbInstance.commitAndCloseSession();
		
		Assert.assertNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_HA1_EMAIL));
		Assert.assertNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_HA1_INSTITUTIONAL_EMAIL));
		Assert.assertNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_WEBDAV_EMAIL));
		Assert.assertNull(securityManager.findAuthenticationByAuthusername(email, WebDAVAuthManager.PROVIDER_WEBDAV_INSTITUTIONAL_EMAIL));
		Assert.assertNull(securityManager.findAuthenticationByAuthusername(email, "OLAT"));
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(identity.getName(), "OLAT"));
		Assert.assertNotNull(securityManager.findAuthenticationByAuthusername(email, "del-mail"));
	}
	
	@Test
	public void deleteSecurityGroup() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("test-del-2");
		SecurityGroup secGroup = securityManager.createAndPersistSecurityGroup();
		securityManager.addIdentityToSecurityGroup(id, secGroup);
		OLATResource resource = JunitTestHelper.createRandomResource();
		Policy policy = securityManager.createAndPersistPolicy(secGroup, "test.right11", resource);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(policy);
		
		//delete the security group (and membership, and policies)
		securityManager.deleteSecurityGroup(secGroup);
		dbInstance.commit();
		
		//checks
		List<Policy> deletedPolicies = securityManager.getPoliciesOfResource(resource, secGroup);
		Assert.assertNotNull(deletedPolicies);
		Assert.assertTrue(deletedPolicies.isEmpty());
		
		boolean membership = securityManager.isIdentityInSecurityGroup(id, secGroup);
		Assert.assertFalse(membership);
	}
	
}
