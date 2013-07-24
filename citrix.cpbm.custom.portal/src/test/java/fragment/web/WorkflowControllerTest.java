/* Copyright (C) 2013 Citrix Systems, Inc. All rights reserved */
package fragment.web;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.ui.ModelMap;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;

import citrix.cpbm.portal.fragment.controllers.WorkflowController;

import com.citrix.cpbm.workflow.activity.Activity.Bucket;
import com.citrix.cpbm.workflow.model.WorkflowActivity;

/**
 * @author damodar
 */
public class WorkflowControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  @Autowired
  private WorkflowController controller;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRouting() throws Exception {

    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Class<WorkflowController> controllerClass = (Class<WorkflowController>) controller.getClass();

    Method expected = locateMethod(controllerClass, "show", new Class[] {
        String.class, ModelMap.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/workflow/abcuuid"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "reset", new Class[] {
      String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/workflow/abcuuid/reset"));
    Assert.assertEquals(expected, handler);

  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testShow() {
    String tilesDef = controller.show("4CD810BC-8167-4676-8A4C-FD60D84E1736", map);
    Assert.assertNotNull(tilesDef);
    Assert.assertEquals("workflow.details.popup", tilesDef);
    Assert.assertNotNull(map.get("workflow"));
    Map<Bucket, List<WorkflowActivity>> workflowActivites = (Map<Bucket, List<WorkflowActivity>>) map.get("bucketMap");
    Assert.assertNotNull(workflowActivites);
    Assert.assertEquals(1, workflowActivites.size());
    Bucket bucket = new Bucket();
    bucket.setOrder(1);
    bucket.setName("bucket1");
    Assert.assertNotNull(workflowActivites.get(bucket));
    Assert.assertEquals(1, workflowActivites.get(bucket).size());
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testShowUnOrderList() {
    String tilesDef = controller.show("4CD810BC-8167-4676-8A4F-FD60D84E1736", map);
    Assert.assertNotNull(tilesDef);
    Assert.assertEquals("workflow.details.popup", tilesDef);
    Assert.assertNotNull(map.get("workflow"));
    Assert.assertNotNull(map.get("bucketMap"));
    Map<Bucket, List<WorkflowActivity>> workflowActivites = (Map<Bucket, List<WorkflowActivity>>) map.get("bucketMap");
    Assert.assertNotNull(workflowActivites);
    Assert.assertEquals(2, workflowActivites.size());
    Set<Bucket> buckets = workflowActivites.keySet();
    Integer i = 1;
    for(Bucket bucket : buckets){
      Assert.assertEquals(i, bucket.getOrder());
      i++;
    }
  }

}
