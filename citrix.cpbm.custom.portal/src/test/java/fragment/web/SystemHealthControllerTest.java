/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
/**
 * 
 */
package fragment.web;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;

import web.WebTestsBase;
import web.support.DispatcherTestServlet;

import citrix.cpbm.portal.fragment.controllers.SystemHealthController;

import com.vmops.model.Health;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceNotification;
import com.vmops.model.ServiceNotification.Type;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.persistence.ServiceNotificationDAO;
import com.vmops.web.forms.ServiceNotificationForm;

/**
 * @author vijay
 */
public class SystemHealthControllerTest extends WebTestsBase {

  @Autowired
  SystemHealthController controller;

  @Autowired
  ServiceNotificationDAO serviceNotificationDAO;

  @Autowired
  ServiceInstanceDao serviceInstanceDao;

  private ServiceInstance defaultServiceInstance;

  private ModelMap map;

  private Calendar calendar;

  private MockHttpServletRequest request;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    calendar = Calendar.getInstance();
    defaultServiceInstance = serviceInstanceDao.find(1L);

  }

  @Test
  public void testRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Class<? extends SystemHealthController> controllerClass = controller.getClass();

    Method expected = locateMethod(controllerClass, "health", new Class[] {
        String.class, ModelMap.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/health"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showStatusDetails", new Class[] {
        String.class, String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/health/showStatusDetails"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "getHealthStatusForServiceInstance", new Class[] {
        String.class, HttpServletRequest.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/health/getHealthStatusForServiceInstance"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showAddStatus", new Class[] {
      ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/health/addStatus"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showAddSchedMaintenance", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/health/addSchedMaintenance"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "addStatus", new Class[] {
        ServiceNotificationForm.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/health/addStatus"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "saveMaintenanceSchedule", new Class[] {
        ServiceNotificationForm.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/health/saveMaintenanceSchedule"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "updateMaintenanceSchedule", new Class[] {
        ServiceNotificationForm.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/health/updateMaintenanceSchedule"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editStatusDetails", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/health/editStatusDetails"));
    Assert.assertEquals(expected, handler);

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testHealth() throws Exception {
    Date now = calendar.getTime();
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Date start = calendar.getTime();
    calendar.add(Calendar.HOUR_OF_DAY, 5);
    Date end = calendar.getTime();
    ServiceNotification notification = new ServiceNotification(getRootUser(), start, end, "planned",
        "planned down time", defaultServiceInstance);
    serviceNotificationDAO.save(notification);

    for (int i = 0; i < 3; i++) {
      calendar.add(Calendar.DAY_OF_MONTH, random.nextInt(5) + 2);
      start = calendar.getTime();
      calendar.add(Calendar.HOUR_OF_DAY, random.nextInt(5) + 1);
      end = calendar.getTime();
      notification = new ServiceNotification(getRootUser(), start, end, "planned", "planned down time", defaultServiceInstance);
      serviceNotificationDAO.save(notification);
    }
    serviceNotificationDAO.flush();

    String view = controller.health("1", map);
    Assert.assertEquals("system.health", view);
    Object o = map.get("dateStatus");
    Assert.assertTrue(o instanceof Map);
    Map<Date, Health> dates = (Map<Date, Health>) o;
    Assert.assertEquals(SystemHealthController.DAYS_PER_PAGE, dates.keySet().size());
    for (Date date : dates.keySet()) {
      System.out.println(date);
      Assert.assertTrue(DateUtils.isSameDay(new Date(), date) || date.before(new Date()));
    }
    o = map.get("maintenance");
    Assert.assertTrue(o instanceof List<?>);
    List<ServiceNotification> notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(4, notifications.size());
    for (ServiceNotification item : notifications) {
      Assert.assertEquals(Type.MAINTENANCE, item.getNotificationType());
      Assert.assertTrue(item.getPlannedEnd().after(now));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSystemStatus() throws Exception {

    controller.getCurrentUser().setTimeZone("GMT");

    controller.showStatusDetails("07082011", "1", "ddMMyyyy", map);
    Object o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    Health health = (Health) o;
    Assert.assertEquals(Health.NORMAL, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    List<ServiceNotification> notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(0, notifications.size());

    controller.showStatusDetails("08082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.ISSUE, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(3, notifications.size());
    Assert.assertEquals(Type.ISSUE, notifications.get(0).getNotificationType());

    controller.showStatusDetails("09082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.ISSUE, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(0, notifications.size());

    controller.showStatusDetails("10082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.NORMAL, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(2, notifications.size());
    Assert.assertEquals(Type.RESOLUTION, notifications.get(0).getNotificationType());

    controller.getCurrentUser().setTimeZone("GMT-08:00");
    controller.showStatusDetails("08082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.ISSUE, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(2, notifications.size());
    Assert.assertEquals(Type.ISSUE, notifications.get(0).getNotificationType());

    controller.showStatusDetails("09082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.DOWN, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(1, notifications.size());
    Assert.assertEquals(Type.DISRUPTION, notifications.get(0).getNotificationType());

    controller.showStatusDetails("10082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.ISSUE, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(2, notifications.size());
    Assert.assertEquals(Type.ISSUE, notifications.get(0).getNotificationType());

    controller.getCurrentUser().setTimeZone("GMT+06:00");
    controller.showStatusDetails("08082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.ISSUE, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(3, notifications.size());
    Assert.assertEquals(Type.ISSUE, notifications.get(0).getNotificationType());

    controller.showStatusDetails("09082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.ISSUE, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(0, notifications.size());

    controller.showStatusDetails("10082011", "1", "ddMMyyyy", map);
    o = map.get("health");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof Health);
    health = (Health) o;
    Assert.assertEquals(Health.NORMAL, health);
    o = map.get("notifications");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List<?>);
    notifications = (List<ServiceNotification>) o;
    Assert.assertEquals(2, notifications.size());
    Assert.assertEquals(Type.RESOLUTION, notifications.get(0).getNotificationType());
  }

  @Test
  public void testSaveMaintenanceSchedule() throws Exception {
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Date start = calendar.getTime();
    calendar.add(Calendar.HOUR_OF_DAY, 5);
    Date end = calendar.getTime();
    ServiceNotification notification = new ServiceNotification(getRootUser(), start, end, "planned",
        "planned down time", defaultServiceInstance);
    ServiceNotificationForm form = new ServiceNotificationForm(notification);
    controller.saveMaintenanceSchedule(form, map);
    Assert.assertTrue(map.containsKey("item"));
  }

  @Test
  public void testshowAddStatus() {
    String showStatus = controller.showAddStatus(null, map);
    Assert.assertEquals(showStatus, new String("system.health.addStatus"));
  }

  @Test
  public void testshowAddSchedMaintenance() {
    String showMaintenace = controller.showAddSchedMaintenance(null, null, map);
    Assert.assertEquals(showMaintenace, new String("system.health.addSchedMaintenance"));
    Assert.assertEquals(map.get("newSchedule"), true);
    ServiceNotificationForm serviceNotificationForm = ((ServiceNotificationForm) map.get("serviceNotificationForm"));
    Assert.assertNotNull(serviceNotificationForm.getServiceNotification());
    Assert.assertEquals(serviceNotificationForm.getServiceNotification().getNotificationType(), Type.MAINTENANCE);
    Assert.assertNotNull(map.get("zones"));

    map.clear();
    showMaintenace = controller.showAddSchedMaintenance("1", null, map);
    Assert.assertEquals(map.get("newSchedule"), false);

  }

  @Test
  public void testaddStatus() {
    request = new MockHttpServletRequest();
    ServiceNotification notification = serviceNotificationDAO.find(1L);
    ServiceNotificationForm form = new ServiceNotificationForm(notification);
    form.setDateFormat("ddMMyyyy");
    form.setServiceInstanceUUID(defaultServiceInstance.getUuid());
    HashMap<String, String> map = controller.addStatus(form, request);
    Assert.assertEquals(map.get("description"), notification.getDescription().toString());
    Assert.assertEquals(map.get("notificationType"), notification.getNotificationType().toString());
    Assert.assertNotNull(map.get("status"));
    Assert.assertNotNull(map.get("recordedOn"));
    Assert.assertNotNull(map.get("status"));

    map.clear();
    notification.setNotificationType(Type.ISSUE);
    serviceNotificationDAO.save(notification);
    map = controller.addStatus(form, request);
    Assert.assertEquals(map.get("notificationType"), notification.getNotificationType().toString());

    map.clear();
    notification.setNotificationType(Type.RESOLUTION);
    serviceNotificationDAO.save(notification);
    map = controller.addStatus(form, request);
    Assert.assertEquals(map.get("notificationType"), notification.getNotificationType().toString());

  }

  @Test
  public void testupdateMaintenanceSchedule() {
    ServiceNotification notification = serviceNotificationDAO.find(1L);
    ServiceNotificationForm form = new ServiceNotificationForm(notification);
    String update = controller.updateMaintenanceSchedule(form, map);
    Assert.assertEquals(update, new String("system.health.maintenanceView"));
    Assert.assertEquals(map.get("item"), notification);
    Assert.assertNotNull(notification.getPlannedStart());
    Assert.assertNotNull(notification.getPlannedEnd());

  }

  @Test
  public void testeditStatusDetails() {
    String edit = controller.editStatusDetails("1", map);
    Assert.assertEquals(edit, new String("system.health.editStatusDetails"));
    ServiceNotificationForm serviceNotificationForm = ((ServiceNotificationForm) map.get("serviceNotificationForm"));
    Assert.assertNotNull(serviceNotificationForm);
    Assert.assertEquals(map.get("serviceNotification"), serviceNotificationForm.getServiceNotification());
  }

  @Test
  public void testdeleteServiceNotification() {
    String delete = controller.deleteServiceNotification("1", map);
    Assert.assertEquals(delete, new String("success"));
    // List<Map<String, String>> zones = (List<Map<String, String>>) (map.get("zones"));
    // Assert.assertTrue(zones.size() == 1);
    Assert.assertNotNull(map.get("today"));
    Assert.assertNotNull(map.get("dateStatus"));
    Assert.assertNotNull(map.get("dateStatusHistory"));

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testhealthmaintainance() {
    String healthMaintainance = controller.healthmaintainance(defaultServiceInstance.getUuid(), "1", null, map);
    Assert.assertEquals(healthMaintainance, new String("system.health.maintenance"));
    List<ServiceNotification> list = ((List<ServiceNotification>) map.get("notifications"));
    Assert.assertEquals(list.size(), serviceNotificationDAO.count());
    Assert.assertNotNull(map.get("today"));
    Assert.assertNotNull(map.get("dateStatus"));
    Assert.assertNotNull(map.get("dateStatusHistory"));

  }
}
