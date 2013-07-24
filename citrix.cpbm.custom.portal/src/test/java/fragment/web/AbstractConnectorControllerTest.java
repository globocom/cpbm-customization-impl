package fragment.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import web.WebTestsBase;
import citrix.cpbm.portal.fragment.controllers.AbstractConnectorController;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.utils.ServiceInstanceConfiguration;
import com.citrix.cpbm.platform.bootstrap.service.BootstrapActivator;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.vmops.model.BaseServiceConfigurationMetadata;
import com.vmops.model.Configuration;
import com.vmops.model.Service;
import com.vmops.model.ServiceInstance;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.persistence.ServiceDAO;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ConfigurationService;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.ServiceInstanceLogoForm;

public class AbstractConnectorControllerTest extends WebTestsBase {

  private ModelMap map;

  private BootstrapActivator bootstrapActivator = new BootstrapActivator();

  @Autowired
  private AbstractConnectorController controller;

  @Autowired
  private ConnectorConfigurationManager connectorConfigurationManager;
  
  @Autowired
  ConfigurationService configurationService;
  
  @Autowired
  ServiceInstanceDao serviceInstanceDao;

  private String validServiceUuid = "";

  private String validServiceInstanceUuid = "";

  protected BundleContext bc;

  private ServiceReference<?> mockServiceRef;
  
  @Autowired
  ServiceDAO servicedao;


  private MockHttpServletRequest request;

	@Before
	public void init() throws Exception {
		request = new MockHttpServletRequest();

	}
  
  public void setUpMyTestCase() throws Exception {
    mockServiceRef = EasyMock.createMock(ServiceReference.class);
    bc = EasyMock.createMock(BundleContext.class);
    bootstrapActivator.start(bc);
  }

  private void setValidServiceAndServiceInstance() throws Exception {
    Boolean instanceFound = false;
    // finding valid Service and ServiceInstance
    for (Service service : connectorConfigurationManager.getAllServicesByType("OSS")) {
      if (!service.getServiceInstances().isEmpty()) {
        for (ServiceInstance serviceInstance : service.getServiceInstances()) {
          validServiceUuid = service.getUuid();
          validServiceInstanceUuid = serviceInstance.getUuid();
          instanceFound = true;
          break;
        }
        if (instanceFound)
          break;
      }
    }
  }

  @Test
  public void testShowOSSServices() throws Exception {
    map = new ModelMap();
    setValidServiceAndServiceInstance();
    String viewService = controller.showOSSServices(validServiceUuid, "", "view", map);
    Assert.assertTrue(map.containsAttribute("viewServiceDetails"));
    Assert.assertEquals(map.get("page"), Page.HOME_CONNECTORS_OSS);
    Assert.assertEquals("main.home_oss.instance.edit", viewService);

    map = new ModelMap();
    String confService = controller.showOSSServices(validServiceUuid, "", "", map);
    Assert.assertTrue(map.containsAttribute("service_config_properties"));
    Assert.assertEquals(map.get("page"), Page.HOME_CONNECTORS_OSS);
    Assert.assertEquals("main.home_oss.instance.edit", confService);

    map = new ModelMap();
    String viewServiceInstance = controller.showOSSServices("", validServiceInstanceUuid, "view", map);
    Assert.assertTrue(map.containsAttribute("instance"));
    Assert.assertTrue(map.containsAttribute("service"));
    Assert.assertTrue(map.containsAttribute("instance_properties"));
    Assert.assertEquals(map.get("page"), Page.HOME_CONNECTORS_OSS);
    Assert.assertEquals("main.home_oss.instance.edit", viewServiceInstance);
  }

  @Test
  public void testShowCloudServices() throws Exception {
    map = new ModelMap();
    setValidServiceAndServiceInstance();
    String viewService = controller.showCloudServices(validServiceUuid, "", "view", map);
    Assert.assertTrue(map.containsAttribute("viewServiceDetails"));
    Assert.assertEquals(map.get("page"), Page.HOME_CONNECTORS_CS);
    Assert.assertEquals("main.home_cs.instance.edit", viewService);

    map = new ModelMap();
    String viewServiceInstance = controller.showCloudServices("", validServiceInstanceUuid, "view", map);
    Assert.assertTrue(map.containsAttribute("instance"));
    Assert.assertTrue(map.containsAttribute("service"));
    Assert.assertTrue(map.containsAttribute("instance_properties"));
    Assert.assertEquals("main.home_cs.instance.edit", viewServiceInstance);
  }

  @Test
  public void testSaveInstance() throws Exception {
    setUpMyTestCase();
    map = new ModelMap();
    String classType = "com.citrix.cpbm.platform.spi.CloudServiceConnector";
    String filter = "(org.springframework.osgi.bean.name=OsgiServiceRef1)";
    prepareMock(classType, filter, true);
    String configProperties = "[{\"name\":\"instancename\",\"value\":\"MyCPIN\"},{\"name\":\"instancedescription\",\"value\":\"Test\"},{\"name\":\"country\",\"value\":\"India\"},{\"name\":\"password\",\"value\":\"password\"},{\"name\":\"accountid\",\"value\":\"456783\"},{\"name\":\"username\",\"value\":\"cpbm\"},{\"name\":\"endpoint\",\"value\":\"http://google.com\"}]";
    map = controller.saveInstance("b1c9fbb0-8dab-42dc-ae0a-ce13ec84a1e6", "", configProperties, null, map,
        new MockHttpServletRequest());
    Assert.assertTrue(map.containsAttribute("validationResult"));
    Assert.assertTrue(map.containsAttribute("instanceid"));
    Assert.assertTrue(map.containsAttribute("result"));
    EasyMock.reset(mockServiceRef, bc);

  }

  @Test
  public void testviewInstance() {
    map = new ModelMap();
    map = controller.viewInstance("b1c9fbb0-8dab-42dc-ae0a-ce13ec84a1e6", map);
    Assert.assertTrue(map.containsAttribute("instances"));
  }

  @Test
  public void testEnable() throws Exception {
    setUpMyTestCase();
    map = new ModelMap();
    String classType = "com.citrix.cpbm.platform.spi.CloudServiceConnector";
    String filter = "(org.springframework.osgi.bean.name=salesForceTicketingConnector)";
    prepareMock(classType, filter, true);
    map = controller.enable("d3577427-d837-4def-ae1f-8569fa518da5", true, map, new MockHttpServletRequest());
    Assert.assertTrue(map.containsAttribute("enabled"));
    Assert.assertEquals(true, map.get("enabled"));
    Assert.assertEquals("success", map.get("result"));
  }

  @SuppressWarnings("rawtypes")
  private void prepareMock(String classType, String filter, boolean adaptor) {
    CloudConnectorFactory mcc = null;

    CloudConnector cc = EasyMock.createMock(CloudConnector.class);

    if (adaptor) {
      mcc = EasyMock.createMock(CloudConnectorFactory.class);
    }
    EasyMock.reset(bc);
    EasyMock.reset(mockServiceRef);
    EasyMock.reset(cc);
    EasyMock.reset(mcc);
    try {
      EasyMock.expect(bc.getServiceReferences(classType, filter)).andReturn(new ServiceReference[] {
        mockServiceRef
      }).anyTimes();
      EasyMock.expect(mockServiceRef.getProperty(Constants.SERVICE_ID)).andReturn(new Long(2)).anyTimes();
      EasyMock.expect(mockServiceRef.getProperty(Constants.SERVICE_RANKING)).andReturn(new Integer(1)).anyTimes();
      EasyMock.expect((Object) bc.getService(mockServiceRef)).andReturn(mcc).anyTimes();
      EasyMock.expect(mcc.initialize(EasyMock.<ServiceInstanceConfiguration> anyObject())).andReturn(cc).anyTimes();

    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    EasyMock.replay(bc);
    EasyMock.replay(mockServiceRef);
    EasyMock.replay(cc);
    EasyMock.replay(mcc);
  }

  @Test
  public void testGetDefaultServiceValues() {
    map = new ModelMap();

    String result = controller.showCloudServices("fc3c6f30-a44a-4754-a8cc-9cea97e0a129", null, "add", map);

    List<BaseServiceConfigurationMetadata> propertiesList = (List<BaseServiceConfigurationMetadata>) map
        .get("service_config_properties");
    for (BaseServiceConfigurationMetadata scm : propertiesList) {
      if (scm.getName().equals("publicProtocol")) {
        Assert.assertEquals("http", scm.getDefaultVal());
      }
      if (scm.getName().equals("publicPort")) {
        Assert.assertEquals("8080", scm.getDefaultVal());
      }
      if (scm.getName().equals("cloud.jdbc.username")) {
        Assert.assertEquals("cloud", scm.getDefaultVal());
      }
      if (scm.getName().equals("cloud.jdbc.database.schemaname")) {
        Assert.assertEquals("cloud", scm.getDefaultVal());
      }
      if (scm.getName().equals("cloud.usage.jdbc.username")) {
        Assert.assertEquals("cloud", scm.getDefaultVal());
      }
      if (scm.getName().equals("cloud.usage.jdbc.database.schemaname")) {
        Assert.assertEquals("cloud_usage", scm.getDefaultVal());
      }
      System.out.println(scm.getName() + "------------------" + scm.getDefaultVal());
    }
  }

	@Test
	public void testShowCloudServicesasRoot() throws Exception {
		map = new ModelMap();
		Tenant tenant = tenantDAO.find(2L);
		asRoot();
		setValidServiceAndServiceInstance();
		request.setAttribute("isSurrogatedTenant", true);
		String viewService = controller.showCloudServices(tenant.getUuid(),
				map, request);
		Assert.assertEquals(tenant, map.get("tenant"));
		List<Service> services = connectorConfigurationManager
				.getAllServicesByType(CssdkConstants.CLOUD);
		Assert.assertEquals(tenant.getOwner(), map.get("effectiveUser"));
		Assert.assertEquals("company_setup.connector_cs_admin", viewService);
		Assert.assertEquals(services.toString(), map.get("services").toString());
	}
  
	@Test
	public void testShowCloudServicesasServiceProvider() throws Exception {
		map = new ModelMap();
		String[] profiles = { "3", "4", "5", "6", "7" };
		for (String profile : profiles) {
			User user = userDAO.find("3");
			Tenant tenant = tenantDAO.find(3L);
			user.setProfile(profileDAO.find(profile));
			userDAO.save(user);
			asUser(user);
			setValidServiceAndServiceInstance();
			request.setAttribute("isSurrogatedTenant", true);
			String viewService = controller.showCloudServices(tenant.getUuid(),
					map, request);
			Assert.assertEquals(tenant, map.get("tenant"));
			List<Service> services = connectorConfigurationManager
					.getAllServicesByType(CssdkConstants.CLOUD);
			Assert.assertEquals(tenant.getOwner(), map.get("effectiveUser"));
			Assert.assertEquals("company_setup.connector_cs_admin", viewService);
			Assert.assertEquals(services.toString(), map.get("services")
					.toString());

		}
	}
	
	  @Test
	  public void testShowCloudServicesForRoot() throws Exception {
	    map = new ModelMap();
	    Tenant tenant=tenantDAO.find(1L);
	    setValidServiceAndServiceInstance();
	    String viewService = controller.showCloudServices(tenant.getUuid(),map,request);
	    Assert.assertEquals("redirect:/portal/home", viewService);
	  }
	  
  @Test
	public void testUploadServiceInstanceLogo() throws Exception {

		Configuration configuration = configurationService
				.locateConfigurationByName(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
		configuration.setValue("src\\test\\resources");
		configurationService.update(configuration);
		ServiceInstance serviceInstance = serviceInstanceDao.find(1L);
		Assert.assertEquals(null, serviceInstance.getImagePath());
		MultipartFile logo = new MockMultipartFile("ServiceInstanceLogo.jpeg",
				"ServiceInstanceLogo.jpeg", "byte",
				"ServiceInstance".getBytes());
		ServiceInstanceLogoForm form = new ServiceInstanceLogoForm(
				serviceInstance);
		form.setLogo(logo);
		BindingResult result = validate(form);
		HttpServletRequest request = new MockHttpServletRequest();
		map = new ModelMap();
		String resultString = controller.uploadServiceInstanceLogo(form,
				result, request, map);
		Assert.assertNotNull(resultString);
		Assert.assertEquals("success", resultString);
		serviceInstance = serviceInstanceDao.find(1L);
		Assert.assertEquals("serviceInstance\\1\\ServiceInstanceLogo.jpeg",
				serviceInstance.getImagePath());
	}

	@Test
	public void testUploadServiceInstanceLogoInvalidFile() throws Exception {

		Configuration configuration = configurationService
				.locateConfigurationByName(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
		configuration.setValue("src\\test\\resources");
		configurationService.update(configuration);
		ServiceInstance serviceInstance = serviceInstanceDao.find(1L);
		Assert.assertEquals(null, serviceInstance.getImagePath());
		MultipartFile logo = new MockMultipartFile("ServiceInstanceLogo.jpeg",
				"ServiceInstanceLogo.jpe", "byte", "ServiceInstance".getBytes());
		ServiceInstanceLogoForm form = new ServiceInstanceLogoForm(
				serviceInstance);
		form.setLogo(logo);
		BindingResult result = validate(form);
		HttpServletRequest request = new MockHttpServletRequest();
		map = new ModelMap();
		String resultString = controller.uploadServiceInstanceLogo(form,
				result, request, map);
		Assert.assertNotNull(resultString);
		String Error = messageSource.getMessage(result.getFieldError("logo")
				.getCode(), null, request.getLocale());
		Assert.assertEquals(Error, resultString);
		serviceInstance = serviceInstanceDao.find(1L);
		Assert.assertEquals(null, serviceInstance.getImagePath());
	}

	@Test
	public void testUploadServiceInstanceLogoNullDirectoryPath()
			throws Exception {

		Configuration configuration = configurationService
				.locateConfigurationByName(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
		configuration.setValue(null);
		configurationService.update(configuration);

		ServiceInstance serviceInstance = serviceInstanceDao.find(1L);
		Assert.assertEquals(null, serviceInstance.getImagePath());
		MultipartFile logo = new MockMultipartFile("ServiceInstanceLogo.jpeg",
				"ServiceInstanceLogo.jpeg", "byte",
				"ServiceInstance".getBytes());
		ServiceInstanceLogoForm form = new ServiceInstanceLogoForm(
				serviceInstance);
		form.setLogo(logo);
		BindingResult result = validate(form);
		HttpServletRequest request = new MockHttpServletRequest();
		map = new ModelMap();
		String resultString = controller.uploadServiceInstanceLogo(form,
				result, request, map);
		Assert.assertNotNull(resultString);
		String Error = messageSource.getMessage(result.getFieldError("logo")
				.getCode(), null, request.getLocale());
		Assert.assertEquals(Error, resultString);
		serviceInstance = serviceInstanceDao.find(1L);
		Assert.assertEquals(null, serviceInstance.getImagePath());
	}
	  
	  
}
