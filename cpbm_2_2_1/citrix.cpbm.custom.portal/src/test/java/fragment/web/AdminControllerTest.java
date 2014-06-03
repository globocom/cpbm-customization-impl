/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package fragment.web;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import web.WebTestsBase;

import com.citrix.cpbm.platform.util.CssdkConstants;
import com.citrix.cpbm.portal.fragment.controllers.AdminController;
import com.vmops.internal.service.EmailService.EmailTemplate;
import com.vmops.model.AccountType;
import com.vmops.model.BaseServiceConfigurationMetadata;
import com.vmops.model.Configuration;
import com.vmops.model.EmailTemplates;
import com.vmops.model.EmailTemplates.Category;
import com.vmops.model.JobStatus;
import com.vmops.model.ModuleType;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceInstanceConfig;
import com.vmops.persistence.ConfigurationDAO;
import com.vmops.persistence.EmailTemplatesDAO;
import com.vmops.persistence.EventDAO;
import com.vmops.persistence.JobStatusDAO;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.service.AccountTypeService;
import com.vmops.service.ConfigurationService;
import com.vmops.service.EmailTemplateService;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.web.controllers.menu.Level3;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.AccountTypeForm;

public class AdminControllerTest extends WebTestsBase {

  private ModelMap map;

  @Autowired
  private AdminController controller;

  @Autowired
  private AccountTypeService accountTypeService;

  @Autowired
  JobStatusDAO JobDAO;

  @Autowired
  private EmailTemplateService emailTemplateService;

  @Autowired
  protected ConfigurationService configurationService;

  @Autowired
  private ConfigurationDAO configurationDAO;

  @Autowired
  private ServiceInstanceDao serviceInstanceDao;

  @Autowired
  private EmailTemplatesDAO emailTemplatesDAO;

  @Autowired
  private EventDAO eventDAO;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAccountTypesList() {
    List<AccountType> expected = accountTypeService.getAccountTypes();
    String view = controller.listAccountTypes(map);
    Assert.assertEquals("accounttypes.list", view);
    Assert.assertTrue(map.containsKey("accountTypesList"));
    List<AccountType> found = (List<AccountType>) map.get("accountTypesList");
    Assert.assertEquals(expected.size(), found.size());
    Assert.assertTrue(map.get("page").equals(Page.ADMIN_ACCOUNT_TYPES));
  }

  @Test
  public void testViewAccountType() {
    AccountType expected = accountTypeService.locateAccountTypeName("SYSTEM");
    String view = controller.viewAccounttype(expected.getId().toString(), "1", map);
    Assert.assertEquals("accounttype.view", view);
    Assert.assertTrue(map.containsKey("accounttype"));
    AccountType found = (AccountType) map.get("accounttype");
    Assert.assertTrue(found.equals(expected));
  }

  @Test
  public void testEditAccountType() throws Exception {
    AccountType expected = accountTypeService.locateAccountTypeName("Retail");
    String view = controller.editAccountType(expected.getId().toString(), "1", map);
    AccountTypeForm form = (AccountTypeForm) map.get("accountTypeForm");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getAccountType());
    Assert.assertEquals(expected, form.getAccountType());
    Assert.assertEquals("accounttype.edit", view);
  }

  @Test
  public void testEditAccountTypePost() throws Exception {
    AccountType expected = accountTypeService.locateAccountTypeName("Retail");
    long maxUsers = 100L;
    expected.setMaxUsers(maxUsers);
    AccountTypeForm form = new AccountTypeForm(expected);
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(form, "validation");
    AccountType returned = controller.edit(form, result, map);
    long foundMaxUsers = returned.getMaxUsers();
    Assert.assertEquals(foundMaxUsers, maxUsers);

    expected = accountTypeService.locateAccountTypeById("3");

    try {
      expected.setMaxUsers(0L);
      form = new AccountTypeForm(expected);
      result = new BeanPropertyBindingResult(form, "validation");
      returned = controller.edit(form, result, map);
      Assert.assertTrue(false);
    } catch (InvalidAjaxRequestException e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testEditAccountTypePostForSystem() throws Exception {
    AccountType expected = accountTypeService.locateAccountTypeName("SYSTEM");
    AccountTypeForm form = new AccountTypeForm(expected);
    BindingResult result = validate(form);
    try {
      controller.edit(form, result, map);
      Assert.fail();
    } catch (InvalidAjaxRequestException e) {

    }
  }

  @Test
  public void testEditAccountTypePostForNegative() throws Exception {
    AccountType expected = accountTypeService.locateAccountTypeName("Retail");
    long maxUsers = -100L;
    expected.setMaxUsers(maxUsers);
    AccountTypeForm form = new AccountTypeForm(expected);
    BindingResult result = validate(form);
    try {
      controller.edit(form, result, map);
      Assert.fail();
    } catch (InvalidAjaxRequestException e) {

    }

  }

  @Test
  public void testEditCreditExposure() throws Exception {
    AccountType expected = accountTypeService.locateAccountTypeName("Retail");
    String view = controller.editCreditExposure(expected.getId().toString(), map);
    AccountTypeForm form = (AccountTypeForm) map.get("accountTypeForm");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getAccountType());
    Assert.assertEquals(expected, form.getAccountType());
    Assert.assertEquals("creditexposure.edit", view);
  }

  @Test
  public void testEditCreditExposurePost() throws Exception {
    AccountType expected = accountTypeService.locateAccountTypeName("Retail");
    expected.getAccountTypeCreditExposureList().get(0).setCreditExposureLimit(BigDecimal.TEN);
    AccountTypeForm form = new AccountTypeForm(expected);
    BindingResult result = validate(form);
    String view = controller.editcreditexposure(form, result, map);
    Assert.assertEquals("success", view);
    AccountType found = accountTypeService.locateAccountTypeName("Retail");
    Assert.assertEquals(found.getAccountTypeCreditExposureList().get(0).getCreditExposureLimit(), BigDecimal.TEN);
  }

  private JobStatus createJobStatus() {
    JobStatus jobstatus;
    Calendar calendar = Calendar.getInstance();
    calendar.set(2011, 11, 24, 17, 0, 0);
    Date startdate = calendar.getTime();
    jobstatus = new JobStatus("NEW_JOB" + Integer.toString(random.nextInt()), startdate, "RUNNING");
    JobDAO.save(jobstatus);
    return jobstatus;
  }

  @Test
  public void testviewBatchJob() {
    JobStatus jobstatus = createJobStatus();
    String viewbatchjob = controller.viewBatchJob(jobstatus.getId().toString(), map);
    Assert.assertNotNull(viewbatchjob);
    Assert.assertEquals(viewbatchjob, new String("batch.view"));
    Assert.assertTrue(map.containsAttribute("jobStatus"));
    Assert.assertEquals(map.get("jobStatus"), jobstatus);

  }

  @Test
  public void testshowBatchStatus() {
    JobStatus jobstatus = createJobStatus();
    String BatchList = controller.showBatchStatus("0", "0", map);
    Assert.assertNotNull(BatchList);
    Assert.assertEquals(BatchList, new String("batch.list"));
    Assert.assertTrue(map.containsAttribute("batchList"));

    @SuppressWarnings("unchecked")
    List<JobStatus> list = (List<JobStatus>) map.get("batchList");
    Assert.assertEquals(1, list.size());
    Assert.assertEquals(list.get(0), jobstatus);
  }

  @Test
  public void testupdateEmailTemplate() {
    controller.updateEmailTemplate(EmailTemplate.WELCOME_EMAIL.toString(), "en_US", "JUNIT TEST",
        new MockHttpServletRequest(), map);
    Assert.assertTrue(map.containsAttribute("templateName"));
    Assert.assertTrue(map.containsAttribute("emailText"));
    Assert.assertTrue(map.containsAttribute("parseError"));
    Assert.assertTrue(map.containsAttribute("lastUpdatedAt"));
    Assert.assertEquals(EmailTemplate.WELCOME_EMAIL.toString(), map.get("templateName").toString());
    EmailTemplates emailTemplate = emailTemplateService.getEmailTemplateByLocale(EmailTemplate.WELCOME_EMAIL.name(),
        "en_US");

    Assert.assertEquals(new String("JUNIT TEST"), emailTemplate.getTemplateText());
    Assert.assertEquals(map.get("parseError"), false);
    Assert.assertNotNull(map.get("lastUpdatedAt"));

  }

  @Test
  public void testeditEmailTemplate() {
    String editTemplate = controller.editEmailTemplate(EmailTemplate.WELCOME_EMAIL.toString(), "en_US", map);
    Assert.assertNotNull(editTemplate);
    Assert.assertEquals(editTemplate, new String("emailtemplate.edit"));
    Assert.assertTrue(map.containsAttribute("templateName"));
    Assert.assertTrue(map.containsAttribute("emailText"));
    Assert.assertEquals(map.get("templateName").toString(), EmailTemplate.WELCOME_EMAIL.toString());
    Assert.assertEquals(map.get("emailText"),
        emailTemplateService.getEmailTemplateByLocale(EmailTemplate.WELCOME_EMAIL.name(), "en_US").getTemplateText());
  }

  @Test
  public void testviewEmailTemplate() {
    String viewEmailtemplate = controller.viewEmailTemplate(EmailTemplate.WELCOME_EMAIL.toString(), "en_US", request,
        map);
    Assert.assertNotNull(viewEmailtemplate);
    Assert.assertEquals(viewEmailtemplate, new String("emailtemplate.view"));
    Assert.assertTrue(map.containsAttribute("templateName"));
    Assert.assertTrue(map.containsAttribute("template"));
    Assert.assertTrue(map.containsAttribute("parseError"));
    Assert.assertTrue(map.containsAttribute("emailText"));
    Assert.assertEquals(map.get("templateName").toString(), EmailTemplate.WELCOME_EMAIL.toString());
    String emailText = emailTemplateService.getEmailTemplateAsStringByLocale(EmailTemplate.WELCOME_EMAIL.name(), map,
        "en_US");
    Assert.assertEquals(map.get("emailText"), emailText);
    Assert.assertEquals(map.get("parseError"), false);
    Assert.assertNotNull(map.get("template"));

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testlistEmailTemplates() {
    String listEmailTemplates = controller.listEmailTemplates(null, "1", null, "en_US", request, map);
    Assert.assertNotNull(listEmailTemplates);
    Assert.assertEquals(listEmailTemplates, new String("email.templates"));
    Assert.assertTrue(map.containsAttribute("templates"));
    Assert.assertTrue(map.containsAttribute("filtersMap"));
    Assert.assertTrue(map.containsAttribute("filterBy"));

    List<EmailTemplates> list = ((List<EmailTemplates>) map.get("templates"));
    Assert.assertTrue(list.size() == 14);
    Assert.assertEquals(map.get("filterBy"), new String("0"));
    Assert.assertNotNull(map.get("filtersMap"));

    Map<String, String> filterMap = (Map<String, String>) map.get("filtersMap");
    Assert.assertTrue(filterMap.size() > 0);
    for (Category category : Category.values()) {
      Assert.assertNotNull(filterMap.get("" + category.ordinal()));
    }
    map.clear();
    controller.listEmailTemplates(null, "1", "1", "en_US", request, map);
    list = ((List<EmailTemplates>) map.get("templates"));
    Assert.assertEquals(14, list.size());

  }

  @Test
  public void testeditConfiguration() {
    String configProperties = "[{\"name\":\"2\",\"value\":\"www.JunitTest.com\"}]";
    String editConfiguration = controller.editConfiguration(configProperties, map);
    Configuration configuration = configurationService.locateConfigurationById("2");
    Assert.assertEquals(editConfiguration, new String("success"));
    Assert.assertEquals(configuration.getValue(), new String("www.JunitTest.com"));
    configuration.setIsEncryptionRequired(true);
    configurationDAO.save(configuration);
    configProperties = "[{\"name\":\"2\",\"value\":\"5253412537238738981C9747D0E09BE9\"}]";
    controller.editConfiguration(configProperties, map);
    configuration = configurationService.locateConfigurationById("2");
    Assert.assertTrue(configuration.getIsCurrentlyEncrypted());
    Assert.assertEquals(configuration.getValue(), new String(
        "A7E4C44C1E2468B950DEADE1E9D219673096633E7B681EE1817BC30371FA61EA451E503715DDF7D2D7BC46EE428ED2FC"));
  }

  @Test
  public void testeditInitialDeposit() {
    AccountType accType = accountTypeService.locateAccountTypeById("1");
    String edit = controller.editInitialDeposit("1", map);
    Assert.assertNotNull(edit);
    Assert.assertEquals(edit, new String("initialdeposit.edit"));
    Assert.assertTrue(map.containsAttribute("accountTypeForm"));
    AccountTypeForm accountTypeForm = ((AccountTypeForm) map.get("accountTypeForm"));
    Assert.assertEquals(accountTypeForm.getAccountType(), accType);
  }

  @Test
  public void testshowConfigurations() {

    String showconfigurations = controller.showConfigurations(Level3.ConfigAccountManagement.getName(), null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.ConfigAccountManagement.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.AccountManagement.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.ConfigAccountManagement.getCode());

    showconfigurations = controller.showConfigurations(Level3.CRM.getName(), null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.CRM.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.CRM.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.CRM.getCode());

    showconfigurations = controller.showConfigurations(Level3.Integration.getName(), null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.Integration.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.Integration.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.Integration.getCode());

    showconfigurations = controller.showConfigurations(Level3.Portal.getName(), null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.Portal.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.Portal.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.Portal.getCode());

    showconfigurations = controller.showConfigurations(Level3.Reports.getName(), null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.Reports.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.Reports.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.Reports.getCode());

    showconfigurations = controller.showConfigurations(Level3.Server.getName(), null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.Server.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.Server.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.Server.getCode());

    showconfigurations = controller.showConfigurations(Level3.TrialManagement.getName(), null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.TrialManagement.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.TrialManagement.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.TrialManagement.getCode());

    showconfigurations = controller.showConfigurations(Level3.Server.getName(), "Notifications", map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("configuration.edit", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.Server.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.Server.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.Server.getCode());
    List<Configuration> configList = (List<Configuration>) map.get("configurationList");
    Assert.assertEquals(6, configList.size());

    showconfigurations = controller.showConfigurations(null, null, map);
    Assert.assertNotNull(showconfigurations);
    Assert.assertEquals("config.list", showconfigurations);
    Assert.assertEquals(map.get("module"), Level3.ConfigAccountManagement.getName());
    Assert.assertEquals(map.get("moduleName"), ModuleType.AccountManagement.getModuleName());
    Assert.assertEquals(map.get("labelCode"), Level3.ConfigAccountManagement.getCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddAccountTypeControls() {

    ServiceInstance serviceInstance = serviceInstanceDao.find(7L);
    String result = controller.addAccountTypeControls(serviceInstance.getUuid(), "3", map);
    Assert.assertNotNull(result);
    Assert.assertEquals("accounttypecontrols.edit", result);
    List<BaseServiceConfigurationMetadata> sortedProperties = (List<BaseServiceConfigurationMetadata>) map
        .get("account_control_add_properties");
    Assert.assertEquals(12, sortedProperties.size());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddAccountTypeControlsExisting() {

    ServiceInstance serviceInstance = serviceInstanceDao.find(1L);
    String result = controller.addAccountTypeControls(serviceInstance.getUuid(), "3", map);
    Assert.assertNotNull(result);
    Assert.assertEquals("accounttypecontrols.edit", result);
    List<ServiceInstanceConfig> instanceProperties = (List<ServiceInstanceConfig>) map
        .get("account_control_edit_properties");
    Assert.assertEquals(1, instanceProperties.size());
  }

  @Test
  public void testPersistAccountTypeControlsUpdate() {

    ServiceInstance serviceInstance = serviceInstanceDao.find(1L);
    String configProperties = "[{\"name\":\"defaultNetworkOffering\",\"value\":\"10\"}]";
    Map<String, String> resultMap = controller.persistAccountTypeControls(serviceInstance.getUuid(), "3", "update",
        configProperties);
    Assert.assertNotNull(resultMap);
    String status = (String) resultMap.get("result");
    Assert.assertEquals(CssdkConstants.SUCCESS, status);
  }

  @Test
  public void testPersistAccountTypeControlsAdd() {

    ServiceInstance serviceInstance = serviceInstanceDao.find(1L);
    String configProperties = "[{\"name\":\"defaultNetworkOffering\",\"value\":\"10\"}]";
    Map<String, String> resultMap = controller.persistAccountTypeControls(serviceInstance.getUuid(), "3", "add",
        configProperties);
    Assert.assertNotNull(resultMap);
    String status = (String) resultMap.get("result");
    Assert.assertEquals(CssdkConstants.SUCCESS, status);
  }

  @Test
  public void testEditInitialDeposit() throws Exception {

    AccountType accountType = accountTypeDAO.find(3L);
    accountType.getAccountTypeCreditExposureList().get(0).setInitialDeposit(BigDecimal.TEN);
    AccountTypeForm form = new AccountTypeForm(accountType);
    BindingResult result = validate(form);
    String resultString = controller.editInitialDeposit(form, result, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("success", resultString);
    Assert.assertEquals(BigDecimal.TEN, accountType.getAccountTypeCreditExposureList().get(0).getInitialDeposit());
  }

  @Test
  public void testSendTestMail() {

    EmailTemplates emailTemplates = emailTemplatesDAO.find(1L);
    int before = eventDAO.count();
    String resultString = controller.sendEmailTemplate(emailTemplates.getTemplateName(), "test@test.com", response,
        "en_US", request, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("success", resultString);
    int after = eventDAO.count();
    Assert.assertEquals(before + 1, after);
  }

}
