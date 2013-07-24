package fragment.web;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.ui.ModelMap;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;

import citrix.cpbm.portal.fragment.controllers.AbstractManageResourceController;

import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.Tenant;
import com.vmops.persistence.SubscriptionHandleDAO;
import com.vmops.persistence.TenantDAO;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;

/**
 * @author anushab
 */
public class AbstractManageResourceControllerTest extends WebTestsBaseWithMockConnectors {

  @Autowired
  ProductBundleService bundleservice;

  @Autowired
  ProductService service;

  @Autowired
  private AbstractManageResourceController controller;

  @Autowired
  TenantDAO tenantdao;

  @Autowired
  SubscriptionHandleDAO subscriptionHandleDAO;

  Tenant tenant;

  ProductBundle bundle;

  Product template;

  Product vmProduct;

  Map<String, String> responseMap = new HashMap<String, String>();

  @Before
  public void init() {
    tenant = tenantdao.find(2L);
    // Get a Product bundle
    bundle = bundleservice.getProductBundleById(2L);
    // Get Template
    template = service.locateProductById("17");
    // Get Running VM
    vmProduct = service.locateProductById("13");
  }

  @Test
  public void testRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Class<? extends AbstractManageResourceController> controllerClass = controller.getClass();
    Method expected = locateMethod(controllerClass, "getSSOCmdString", new Class[] {
        Tenant.class, String.class, String.class, ModelMap.class, HttpServletRequest.class, HttpServletResponse.class
    });
    Method handler = servlet
        .recognize(getRequestTemplate(HttpMethod.POST, "/dashboard/manageresource/getSSOCmdString"));
    Assert.assertEquals(expected, handler);
  }

}
