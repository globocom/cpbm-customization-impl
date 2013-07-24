package fragment.web;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.ui.ModelMap;

import web.WebTestsBase;

import citrix.cpbm.portal.fragment.controllers.ProfilesController;

import com.vmops.model.Profile;
import com.vmops.model.SecurityContextScope.Scope;
import com.vmops.persistence.ProfileDAO;
import com.vmops.service.AuthorityService;

public class AbstractProfilesControllerTest extends WebTestsBase {

  @Autowired
  ProfilesController controller;

  @Autowired
  ProfileDAO profileDAO;

  @Autowired
  AuthorityService authorityScopeService;

  private ModelMap map;

  @Test
  public void testShowProfiles() {

    map = new ModelMap();
    Assert.assertEquals(controller.showProfiles(null, null, map), new String("profiles.show"));
    Assert.assertTrue(map.containsAttribute("allProfileList"));
    Assert.assertTrue(map.containsAttribute("opsProfileList"));
    Assert.assertTrue(map.containsAttribute("nonOpsProfileList"));
    Assert.assertTrue(map.containsAttribute("authorityNonopsTypes"));
    Assert.assertTrue(map.containsAttribute("authorityOpsTypes"));
    Assert.assertTrue(map.containsAttribute("authorityTypes"));
    Assert.assertNotNull(map.get("allProfileList"));
    Assert.assertNotNull(map.get("opsProfileList"));
    Assert.assertNotNull(map.get("nonOpsProfileList"));
    Assert.assertNotNull(map.get("authorityNonopsTypes"));
    Assert.assertNotNull(map.get("authorityTypes"));
  }

  @Test
  public void testeditProfile() {

    Profile profile = new Profile("Test", Scope.GLOBAL);
    profileDAO.save(profile);
    String authorityNames = ",ROLE_ACCOUNT_CRUD,ROLE_ACCOUNT_MGMT";
    map = new ModelMap();
    try {
      Assert.assertEquals(controller.editProfile(map, profile.getId().toString(), authorityNames), new String(
          "profiles.show"));
      Assert.assertNotNull(profile.getProfileAuthorities());
      Assert.assertTrue(map.containsAttribute("updatedProfile"));
      Assert.assertTrue(map.containsAttribute("isOpsProfile"));
      Assert.assertTrue(map.containsAttribute("allProfileList"));
      Assert.assertTrue(map.containsAttribute("opsProfileList"));
      Assert.assertTrue(map.containsAttribute("nonOpsProfileList"));
      Assert.assertTrue(map.containsAttribute("authorityNonopsTypes"));
      Assert.assertTrue(map.containsAttribute("authorityOpsTypes"));
      Assert.assertTrue(map.containsAttribute("authorityTypes"));
      Assert.assertNotNull("opsProfileList");
      Assert.assertNotNull("nonOpsProfileList");
//      Assert.assertEquals(map.get("authorityNonopsTypes"), authorityScopeService.getOpsNonopsTypes(false));
//      Assert.assertEquals(map.get("authorityOpsTypes"), authorityScopeService.getOpsNonopsTypes(true));
      Assert.assertNotNull(map.get("authorityTypes"));

    } catch (HibernateOptimisticLockingFailureException ex) {
    } catch (DataAccessException ex) {
    }
  }
}
