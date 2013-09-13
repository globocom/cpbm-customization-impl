/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
/**
 * 
 */
package fragment.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.util.ReflectionUtils;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;

import com.citrix.cpbm.core.workflow.model.BusinessTransaction;
import com.citrix.cpbm.core.workflow.model.Task;
import com.citrix.cpbm.core.workflow.model.Task.DisplayMode;
import com.citrix.cpbm.core.workflow.model.TenantStateChangeTransaction;
import com.citrix.cpbm.core.workflow.service.BusinessTransactionService;
import com.citrix.cpbm.core.workflow.service.TaskService;
import com.citrix.cpbm.portal.fragment.controllers.TasksController;
import com.vmops.model.Tenant;
import com.vmops.service.AuthorityService;

/**
 * @author rajanik
 */
public class TasksControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  private HttpServletRequest request;

  @Autowired
  private TasksController tasksController;
  
  @Autowired
  private AuthorityService authorityService;
  
  @Autowired
  private TaskService taskService;
  
  @Autowired
  private BusinessTransactionService businessTransactionService;

  @Before
  public void setUp() {
    map = new ModelMap();
    request = new MockHttpServletRequest();
  }

  @After
  public void tearDown() {
    map = null;
    request = null;
  }

  @Test
  public void testRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    @SuppressWarnings("unchecked")
    Class<TasksController> controllerClass = (Class<TasksController>) tasksController.getClass();
    Method expected = locateMethod(controllerClass, "getTasks", new Class[] {
        Tenant.class, String.class, String.class, Integer.class, ModelMap.class, HttpServletRequest.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tasks/"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "getTask", new Class[] {
        String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tasks/1/"));
    Assert.assertEquals(expected, handler);    
    
    expected = locateMethod(controllerClass, "getApprovalTask", new Class[] {Locale.class, 
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tasks/approval-task/1"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "actOnApprovalTask", new Class[] {
        String.class, String.class, String.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tasks/approval-task"));
    Assert.assertEquals(expected, handler);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetTasks() {
    String tenantParam = "4bdbbe90-f6a5-4e16-a140-d38b4db490c0";
    
    
    request.setAttribute("isSurrogatedTenant", false);
    String tilesDef = tasksController.getTasks(tenantService.get(tenantParam), tenantParam, "ALL", 1, map, request);
    Assert.assertEquals("correct tiles def is not returned", "tasks.all", tilesDef);
    Assert.assertTrue(map.containsKey("tasksMap"));
    Map<Task, String> tasksMap = (Map<Task, String>) map.get("tasksMap");
    Assert.assertEquals(5, tasksMap.size());
    Assert.assertTrue(map.containsKey("taskfilters"));
    List<String> filters = (List<String>) map.get("taskfilters");
    Assert.assertEquals(3, filters.size());
    Assert.assertTrue(map.containsKey("currentFilter"));
    String currentFilter = (String) map.get("currentFilter");
    Assert.assertEquals("ALL", currentFilter);
    Assert.assertTrue(map.containsKey("tenant"));
    Tenant tenant = (Tenant) map.get("tenant");
    Assert.assertEquals(tenantParam, tenant.getUuid());
    Assert.assertTrue(map.containsKey("showUserProfile"));
    Assert.assertEquals(false, map.get("showUserProfile"));
    Assert.assertTrue(map.containsKey("userHasCloudServiceAccount"));
    Assert.assertEquals(false, map.get("userHasCloudServiceAccount"));
    
    request.setAttribute("isSurrogatedTenant", true);
    tilesDef = tasksController.getTasks(getSystemTenant(), tenantParam, "PENDING", 1, map, request);
    Assert.assertEquals("correct tiles def is not returned", "tasks.all", tilesDef);
    Assert.assertTrue(map.containsKey("tasksMap"));
    tasksMap = (Map<Task, String>) map.get("tasksMap");
    Assert.assertEquals(3, tasksMap.size());
    Assert.assertTrue(map.containsKey("taskfilters"));
    filters = (List<String>) map.get("taskfilters");
    Assert.assertEquals(3, filters.size());
    Assert.assertTrue(map.containsKey("currentFilter"));
    currentFilter = (String) map.get("currentFilter");
    Assert.assertEquals("PENDING", currentFilter);
    Assert.assertTrue(map.containsKey("tenant"));
    tenant = (Tenant) map.get("tenant");
    Assert.assertEquals(tenantParam, tenant.getUuid());
    Assert.assertTrue(map.containsKey("showUserProfile"));
    Assert.assertEquals(true, map.get("showUserProfile"));
    Assert.assertTrue(map.containsKey("userHasCloudServiceAccount"));
    Assert.assertEquals(false, map.get("userHasCloudServiceAccount"));

    request.setAttribute("isSurrogatedTenant", false);
    tilesDef = tasksController.getTasks(tenantService.get(tenantParam), tenantParam, "COMPLETED", 1, map, request);
    Assert.assertEquals("correct tiles def is not returned", "tasks.all", tilesDef);
    Assert.assertTrue(map.containsKey("tasksMap"));
    tasksMap = (Map<Task, String>) map.get("tasksMap");
    Assert.assertEquals(2, tasksMap.size());
    Assert.assertTrue(map.containsKey("taskfilters"));
    filters = (List<String>) map.get("taskfilters");
    Assert.assertEquals(3, filters.size());
    Assert.assertTrue(map.containsKey("currentFilter"));
    currentFilter = (String) map.get("currentFilter");
    Assert.assertEquals("COMPLETED", currentFilter);
    Assert.assertTrue(map.containsKey("tenant"));
    tenant = (Tenant) map.get("tenant");
    Assert.assertEquals(tenantParam, tenant.getUuid());
    Assert.assertTrue(map.containsKey("showUserProfile"));
    Assert.assertEquals(false, map.get("showUserProfile"));
    Assert.assertTrue(map.containsKey("userHasCloudServiceAccount"));
    Assert.assertEquals(false, map.get("userHasCloudServiceAccount"));
  }

  @Test
  public void testGetTask() {
    String tilesDef = tasksController.getTask("A4644A95-51D4-4B30-A0CE-D567A06B9AA7", "4bdbbe90-f6a5-4e16-a140-d38b4db490c0", map);
    Assert.assertEquals("correct tiles def is not returned", "task.view", tilesDef);
    Assert.assertTrue(map.containsKey("task"));
    Assert.assertTrue(map.containsKey("taskUrl"));
  }
  
  @Test
  public void testActOnPendingAction() {
    Tenant tenant = tenantService.get("dfc84388-d44d-4d8e-9d6a-a62c1c16b7e4");
    BusinessTransaction bt = new TenantStateChangeTransaction();
    bt.setUuid(UUID.randomUUID().toString());
    bt.setWorkflowId("1e42822b-cad6-4dc0-bb77-99abb9395f1a");

    Field field = ReflectionUtils.findField(BusinessTransaction.class, "id");
    field.setAccessible(true);
    ReflectionUtils.setField(field, bt, 1l);

    Task task = new Task();
    task.setActorRole(authorityService.findByAuthority("ROLE_FINANCE_CRUD"));
    task.setCreatedAt(new Date());
    task.setState(com.citrix.cpbm.core.workflow.model.Task.State.PENDING);
    task.setTenant(tenant);
    task.setUser(tenant.getOwner());
    task.setUpdatedBy(getRootUser());
    task.setBusinessTransaction(bt);
    task.setType("FINANCE_APPROVAL");
    task.setDisplayMode(DisplayMode.POPUP);
    task = taskService.save(task);
    String memo = "Approved";
    String actedAction = tasksController.actOnApprovalTask(task
        .getUuid(), Task.State.SUCCESS.toString(), memo,request);
    Assert.assertEquals("ui.task.state.SUCCESS", actedAction);
  }

  @Test
  public void testGetApprovalTask() {
    Tenant tenant = tenantService.get("dfc84388-d44d-4d8e-9d6a-a62c1c16b7e4");
    
    TenantStateChangeTransaction bt = new TenantStateChangeTransaction();
    bt.setTenant(tenant);
    bt.setTenantInitialState(tenant.getState());
    bt.setTenantTargetState(Tenant.State.ACTIVE);
    bt = (TenantStateChangeTransaction) businessTransactionService.save(bt);
    bt.setWorkflowId("1e42822b-cad6-4dc0-bb77-99abb9395f1a");

    Task task = new Task();
    task.setActorRole(authorityService.findByAuthority("ROLE_FINANCE_CRUD"));
    task.setCreatedAt(new Date());
    task.setState(com.citrix.cpbm.core.workflow.model.Task.State.PENDING);
    task.setTenant(tenant);
    task.setUser(tenant.getOwner());
    task.setUpdatedBy(getRootUser());
    task.setBusinessTransaction(bt);
    task.setType("FINANCE_APPROVAL");
    task.setDisplayMode(DisplayMode.POPUP);
    task = taskService.save(task);

    String expectedView = "approval.task";
    String retView = tasksController.getApprovalTask(task.getTenant().getOwner().getLocale(), task.getUuid(), map);
    Assert.assertEquals(expectedView, retView);
    Assert.assertTrue(map.containsValue(task));
    Assert.assertEquals(task,map.get("task"));
    Assert.assertEquals(task.getBusinessTransaction().getClass().getSimpleName(),map.get("transactionType"));
  }

  @Test
  public void testGetApprovalTaskTaskDetails() {
    Task task = taskService.get("6866D6BB-46BB-41DA-B2FD-F721FE7B00D9");

    String expectedView = "approval.task";
    String retView = tasksController.getApprovalTask(task.getTenant().getOwner().getLocale(), task.getUuid(), map);
    Assert.assertEquals(expectedView, retView);
    Assert.assertTrue(map.containsValue(task));
    Assert.assertEquals(task,map.get("task"));
    Assert.assertEquals(task.getBusinessTransaction().getClass().getSimpleName(),map.get("transactionType"));
    Assert.assertNotNull(map.get("taskDetails"));
    String details = (String) map.get("taskDetails");
    Assert.assertTrue(details.contains("2_corporate2"));
    Assert.assertTrue(details.contains("Initial State"));
    Assert.assertTrue(details.contains("New Account"));
    Assert.assertTrue(details.contains("Target State"));
    Assert.assertTrue(details.contains("Active"));
  }
  
  @Test
  public void testActOnPendingActionRejectWithMemo() {
    Tenant tenant = tenantService.get("dfc84388-d44d-4d8e-9d6a-a62c1c16b7e4");
    BusinessTransaction bt = new TenantStateChangeTransaction();
    bt.setUuid(UUID.randomUUID().toString());
    bt.setWorkflowId("1e42822b-cad6-4dc0-bb77-99abb9395f1a");

    Field field = ReflectionUtils.findField(BusinessTransaction.class, "id");
    field.setAccessible(true);
    ReflectionUtils.setField(field, bt, 1l);

    Task task = new Task();
    task.setActorRole(authorityService.findByAuthority("ROLE_FINANCE_CRUD"));
    task.setCreatedAt(new Date());
    task.setState(com.citrix.cpbm.core.workflow.model.Task.State.PENDING);
    task.setTenant(tenant);
    task.setUser(tenant.getOwner());
    task.setUpdatedBy(getRootUser());
    task.setBusinessTransaction(bt);
    task.setType("FINANCE_APPROVAL");
    task.setDisplayMode(DisplayMode.POPUP);
    task = taskService.save(task);
    String memo = "Rejected";
    String actedAction = tasksController.actOnApprovalTask(task
        .getUuid(), Task.State.FAILURE.toString(), memo,request);
    Assert.assertEquals("ui.task.state.FAILURE", actedAction);
  }
  
  @Test
  public void testActOnPendingActionRejectWithoutMemo() {
	  try{
		  Tenant tenant = tenantService.get("dfc84388-d44d-4d8e-9d6a-a62c1c16b7e4");
		    BusinessTransaction bt = new TenantStateChangeTransaction();
		    bt.setUuid(UUID.randomUUID().toString());
		    bt.setWorkflowId("1e42822b-cad6-4dc0-bb77-99abb9395f1a");

		    Field field = ReflectionUtils.findField(BusinessTransaction.class, "id");
		    field.setAccessible(true);
		    ReflectionUtils.setField(field, bt, 1l);

		    Task task = new Task();
		    task.setActorRole(authorityService.findByAuthority("ROLE_FINANCE_CRUD"));
		    task.setCreatedAt(new Date());
		    task.setState(com.citrix.cpbm.core.workflow.model.Task.State.PENDING);
		    task.setTenant(tenant);
		    task.setUser(tenant.getOwner());
		    task.setUpdatedBy(getRootUser());
		    task.setBusinessTransaction(bt);
		    task.setType("FINANCE_APPROVAL");
		    task.setDisplayMode(DisplayMode.POPUP);
		    task = taskService.save(task);
		    String memo = "";
		    tasksController.actOnApprovalTask(task
		        .getUuid(), Task.State.FAILURE.toString(), memo,request);
	  }catch(Exception e){
		  
		  Assert.assertEquals("Memo is required in case of Rejection", e.getMessage());
	  }
  }
}
