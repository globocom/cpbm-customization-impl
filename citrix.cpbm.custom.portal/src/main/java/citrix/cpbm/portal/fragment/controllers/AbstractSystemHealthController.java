/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.vmops.model.Health;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceNotification;
import com.vmops.model.ServiceNotification.Type;
import com.vmops.service.SystemHealthService;
import com.vmops.utils.DateTimeUtils;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.ServiceNotificationForm;

/**
 * Controller for System Health related requests.
 * 
 * @author vaibhav
 */

public abstract class AbstractSystemHealthController extends AbstractAuthenticatedController {

  /**
   * No. of days to show per page
   */
  public static final int DAYS_PER_PAGE = 7;

  @Autowired
  private SystemHealthService healthService;

  @Autowired
  protected ConnectorConfigurationManager connectorConfigurationManager;

  Logger logger = Logger.getLogger("com.vmops.web.controllers.AbstractSystemHealthController");

  @RequestMapping(value = {
      "", "/"
  }, method = RequestMethod.GET)
  public String health(@RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      ModelMap map) {
    logger.debug("###Entering in health(map) method @GET");
    setPage(map, Page.SUPPORT_HEALTH);
    // Fetching category list and prepending it with All category
    List<String> serviceCategoryList = userService.getAllAccessibleCloudServiceCategories(getCurrentUser());
    serviceCategoryList.add(0, CssdkConstants.ALL);
    map.addAttribute("serviceCategoryList", serviceCategoryList);

    List<ServiceNotification> list = null;
    List<ServiceInstance> cloudTypeServiceInstances = null;
    if (getCurrentUser().getTenant().equals(tenantService.getSystemTenant())) {
      cloudTypeServiceInstances = connectorManagementService.getCloudTypeServiceInstances();
    } else {
      cloudTypeServiceInstances = userService.getCloudServiceInstance(getCurrentUser(), null);
    }
    ServiceInstance serviceInstance = null;
    if (cloudTypeServiceInstances != null && cloudTypeServiceInstances.size() > 0) {
      if (serviceInstanceUUID != null && !serviceInstanceUUID.equals("")) {
        serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
        map.addAttribute("selectedCloudServiceInstance", serviceInstanceUUID);
        map.addAttribute("selectedCategory", serviceInstance.getService().getCategory());
      }
      list = healthService.listNotificationsByServiceInstance(serviceInstance, 0, 0);
    }

    map.addAttribute("notifications", list);
    map.addAttribute("today", new Date());
    Map<Date, Health> dateStatus = new TreeMap<Date, Health>(Collections.reverseOrder());
    Map<Date, List<ServiceNotification>> dateStatusHistory = new TreeMap<Date, List<ServiceNotification>>(
        Collections.reverseOrder());
    getStatusAndHistoryList(dateStatus, dateStatusHistory, serviceInstance);
    map.addAttribute("dateStatus", dateStatus);
    map.addAttribute("dateStatusHistory", dateStatusHistory);
    map.addAttribute("maintenance", healthService.listPlannedNotifications(serviceInstance));
    map.addAttribute("tenant", getTenant());
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(getCurrentUser()));
    logger.debug("###Exiting health(map) method @GET");
    return "system.health";
  }

  @RequestMapping(value = "/healthmaintainance", method = RequestMethod.GET)
  public String healthmaintainance(
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "size", required = false) String size, ModelMap map) {
    logger.debug("###Entering in healthmaintainance(map) method @GET");
    setPage(map, Page.SUPPORT_HEALTH);
    int currentPageValue = currentPage != null ? Integer.parseInt(currentPage) : 1;
    int perPageValue = getDefaultPageSize();
    int sizeInt = 0;

    // Fetching category list and prepending it with All category
    List<String> serviceCategoryList = userService.getAllAccessibleCloudServiceCategories(getCurrentUser());
    serviceCategoryList.add(0, CssdkConstants.ALL);
    map.addAttribute("serviceCategoryList", serviceCategoryList);

    List<ServiceNotification> list = null;

    List<ServiceInstance> cloudTypeServiceInstances = null;
    if (getCurrentUser().getTenant().equals(tenantService.getSystemTenant())) {
      cloudTypeServiceInstances = connectorManagementService.getCloudTypeServiceInstances();
    } else {
      cloudTypeServiceInstances = userService.getCloudServiceInstance(getCurrentUser(), null);
    }
    ServiceInstance serviceInstance = null;
    if (cloudTypeServiceInstances != null && cloudTypeServiceInstances.size() > 0) {
      if (serviceInstanceUUID != null && !serviceInstanceUUID.equals("")) {
        serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
        map.addAttribute("selectedCloudServiceInstance", serviceInstanceUUID);
        map.addAttribute("selectedCategory", serviceInstance.getService().getCategory());
      }
      list = healthService.listNotificationsByServiceInstance(serviceInstance, 0, 0);
    }

    map.addAttribute("notifications", list);
    map.addAttribute("today", new Date());
    Map<Date, Health> dateStatus = new TreeMap<Date, Health>(Collections.reverseOrder());
    Map<Date, List<ServiceNotification>> dateStatusHistory = new TreeMap<Date, List<ServiceNotification>>(
        Collections.reverseOrder());
    getStatusAndHistoryList(dateStatus, dateStatusHistory, serviceInstance);
    map.addAttribute("dateStatus", dateStatus);
    map.addAttribute("dateStatusHistory", dateStatusHistory);
    List<ServiceNotification> maintenance = new ArrayList<ServiceNotification>();
    try {
      if (size == null || size.equals("")) {
        sizeInt = healthService.listPlannedNotifications(serviceInstance, 0, 0).size();
      } else {
        sizeInt = Integer.parseInt(size);
      }
      maintenance = healthService.listPlannedNotifications(serviceInstance, currentPageValue, perPageValue);
      // List<Alert> subscribedAlerts = alertService.getSubscribedAlerts(user);
      // alerts.addAll(subscribedAlerts);
      map.addAttribute("maintenance", maintenance);
      map.addAttribute("size", maintenance.size());
    } catch (Exception e) {
      logger.error("Error while retrieving notifications: " + e);
    }

    map.addAttribute("tenant", getTenant());
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(getCurrentUser()));
    setPaginationValues(map, perPageValue, currentPageValue, sizeInt, null);
    logger.debug("###Exiting healthmaintainance(map) method @GET");
    return "system.health.maintenance";
  }

  /**
   * View Status Details
   * 
   * @param ID
   * @param map
   * @return
   */
  @RequestMapping(value = ("/showStatusDetails"), method = RequestMethod.GET)
  public String showStatusDetails(@RequestParam(value = "date", required = true) String date,
      @RequestParam(value = "serviceInstanceUUID", required = true) String serviceInstanceUUID,
      @RequestParam(value = "dateFormat", required = true) String dateFormat, ModelMap map) {
    logger.debug("### showStatusDetails method starting...(GET)");
    Calendar zeroHourOfTheTargetDate = Calendar.getInstance(TimeZone.getTimeZone(getCurrentUserTimeZone()));
    zeroHourOfTheTargetDate.clear();
    // localDateAtZeroHour = DateUtils.truncate(localDateAtZeroHour, Calendar.DAY_OF_MONTH);
    // IMPORTANT!!! don't zero out after setting the time zone, won't work.

    SimpleDateFormat sd = new SimpleDateFormat(dateFormat);
    sd.setTimeZone(TimeZone.getTimeZone(getCurrentUserTimeZone()));
    try {
      zeroHourOfTheTargetDate.setTime(sd.parse(date));
    } catch (ParseException e) {
      logger.error("Error found ...", e);
    }
    // calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
    // calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
    // calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
    ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
    map.addAttribute("health", healthService.getSystemHealth(zeroHourOfTheTargetDate.getTime(), serviceInstance));
    map.addAttribute("notifications", healthService.listNotifications(zeroHourOfTheTargetDate.getTime(),
        getCurrentUserTimeZone(), 0, 0, serviceInstance));
    // map.addAttribute("notifications", healthService.listNotifications(calendar.getTime(), 0, 0, zoneId));
    map.addAttribute("date", zeroHourOfTheTargetDate.getTime());
    // TODO:FIX this. List service Insatnces if required
    /*
     * List<Map<String, String>> zones = listZonesAsPortalUser(); if (zoneId == null || zoneId.equals("")) { zoneId =
     * zones.get(0).get("id"); } map.addAttribute("zones", zones); map.addAttribute("zone", getZoneById(zones, zoneId));
     */
    logger.debug("### showStatusDetails method end");
    return "system.health.statusDetails";
  }

  @RequestMapping(value = ("/getHealthStatusForServiceInstance"), method = RequestMethod.GET)
  public String getHealthStatusForServiceInstance(
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      HttpServletRequest request, ModelMap map) {
    ServiceInstance serviceInstance = null;
    try {
      serviceInstance = connectorConfigurationManager.getInstance(serviceInstanceUUID);
    } catch (Exception e) {
      logger.error("Error in getting service Instance in getHealthStatusForServiceInstance method", e);
    }
    Health health = healthService.getSystemHealthStatus(new Date(), serviceInstance);
    ServiceNotification latestNotification = healthService.getLatestNotification(serviceInstance);
    String message = messageSource.getMessage(health.getDescription(), null, getSessionLocale(request));
    map.addAttribute("message", message);
    map.addAttribute("status", health.toString());
    map.addAttribute("latestNotification", latestNotification);

    return "service.health.chart";
  }

  @RequestMapping(value = "/addStatus", method = RequestMethod.GET)
  public String showAddStatus(
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID, ModelMap map) {

    logger.debug("###Entering in addStatus() method @GET ");
    ServiceNotificationForm form = new ServiceNotificationForm(new ServiceNotification());
    form.setDateFormat("dd MMM yyyy");
    map.addAttribute("serviceNotificationForm", form);
    if (serviceInstanceUUID == null) {
      List<ServiceInstance> cloudTypeServiceInstances = connectorManagementService.getCloudTypeServiceInstances();
      map.addAttribute("cloudTypeServiceInstances", cloudTypeServiceInstances);
    } else {
      map.addAttribute("selectedServiceInstanceUUID", serviceInstanceUUID);
    }
    logger.debug("###Leaving addStatus() method @GET ");
    return "system.health.addStatus";
  }

  @RequestMapping(value = "/addSchedMaintenance", method = RequestMethod.GET)
  public String showAddSchedMaintenance(@RequestParam(value = "Id", required = false) String ID,
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID, ModelMap map) {
    logger.debug("###Entering in showAddSchedMaintenance() method @GET ");
    ServiceNotification notification;
    if (ID != null) {
      notification = healthService.locateServiceNotificationById(ID);
      map.addAttribute("newSchedule", false);
      serviceInstanceUUID = notification.getServiceInstance().getUuid();
    } else { // new schedule
      notification = new ServiceNotification();
      map.addAttribute("newSchedule", true);
    }
    ServiceNotificationForm serviceNotificationForm = new ServiceNotificationForm(notification);
    serviceNotificationForm.getServiceNotification().setNotificationType(Type.MAINTENANCE);
    serviceNotificationForm.setNotificationId(notification.getId() + "");

    map.addAttribute("serviceNotificationForm", serviceNotificationForm);
    if (serviceInstanceUUID == null) {
      List<ServiceInstance> cloudTypeServiceInstances = connectorManagementService.getCloudTypeServiceInstances();
      map.addAttribute("cloudTypeServiceInstances", cloudTypeServiceInstances);
    } else {
      map.addAttribute("selectedServiceInstanceUUID", serviceInstanceUUID);
    }

    logger.debug("###Leaving showAddSchedMaintenance() method @GET ");
    return "system.health.addSchedMaintenance";
  }

  @ResponseBody
  @RequestMapping(value = "/addStatus", method = RequestMethod.POST)
  public HashMap<String, String> addStatus(@ModelAttribute("serviceNotificationForm") ServiceNotificationForm form,
      HttpServletRequest request) {
    logger.debug("###Entering in addStatus() method @POST");
    HashMap<String, String> map = new HashMap<String, String>();
    ServiceNotification notification = form.getServiceNotification();
    Health status = Health.NORMAL;

    String serviceInstanceUUID = form.getServiceInstanceUUID();

    ServiceInstance serviceInstance = null;
    try {
      serviceInstance = connectorConfigurationManager.getInstance(serviceInstanceUUID);
    } catch (Exception e) {
      logger.error("Error in getting instance from service and instance uuids..", e);
    }
    notification.setServiceInstance(serviceInstance);

    switch (notification.getNotificationType()) {
      case DISRUPTION:
        notification = healthService.recordDisruption(notification.getSubject(), notification.getDescription(),
            serviceInstance);
        status = Health.DOWN;
        break;
      case ISSUE:
        notification = healthService.recordIssue(notification.getSubject(), notification.getDescription(),
            serviceInstance);
        status = Health.ISSUE;
        break;
      case RESOLUTION:
        notification = healthService.recordResolution(notification.getSubject(), notification.getDescription(),
            serviceInstance);
        status = Health.NORMAL;
        break;
      default:
        notification = null;
    }
    if (notification != null) {
      map.put("id", notification.getId().toString());
      map.put("description", notification.getDescription().toString());
      map.put("recordedOn",
          getDateStringInLocal(notification.getRecordedOn(), getCurrentUserTimeZone(), form.getDateFormat()));
      map.put("notificationType", notification.getNotificationType().toString());

      String statusDesc = messageSource.getMessage(status.getDescription(), null, getSessionLocale(request));
      map.put("status", statusDesc);
    }
    logger.debug("###Exiting addStatus() method @POST");

    return map;
  }

  private Date getDateInSystemTZ(Date date, String timeZone) {
    if (date != null && timeZone != null) {
      return DateTimeUtils.getDateInSystemTZ(date, timeZone);
    }
    Date defaultTimeZoneForRecording = Calendar.getInstance().getTime();
    return defaultTimeZoneForRecording;
  }

  private String getDateStringInLocal(Date date, String timeZone, String dateFormat) {
    SimpleDateFormat df = new SimpleDateFormat(dateFormat);
    if (date != null && timeZone != null) {
      Date localDate = DateTimeUtils.getDateInLocal(date, timeZone);
      return df.format(localDate).toString();
    }
    return df.format(Calendar.getInstance(TimeZone.getTimeZone(getCurrentUserTimeZone())).getTime()).toString();
  }

  @RequestMapping(value = "/saveMaintenanceSchedule", method = RequestMethod.POST)
  public String saveMaintenanceSchedule(@ModelAttribute("serviceNotificationForm") ServiceNotificationForm form,
      ModelMap map) {
    logger.debug("###Entering in saveMaintenanceSchedule() method @POST");
    ServiceNotification notification = form.getServiceNotification();
    String serviceInstanceUUID = form.getServiceInstanceUUID();

    ServiceInstance serviceInstance = null;
    try {
      serviceInstance = connectorConfigurationManager.getInstance(serviceInstanceUUID);
    } catch (Exception e) {
      logger.error("Error in getting instance from service and instance uuids..", e);
    }
    notification.setServiceInstance(serviceInstance);
    String error = null;
    try {
      notification = healthService.recordPlannedMaintenance(notification.getSubject(), notification.getDescription(),
          notification.getServiceInstance(),
          getDateInSystemTZ(notification.getPlannedStart(), getCurrentUserTimeZone()),
          getDateInSystemTZ(notification.getPlannedEnd(), getCurrentUserTimeZone()));
    } catch (IllegalArgumentException ex) {
      logger.error("Error while recording Scheduled Maintenance", ex);
      error = ex.getMessage();
    }
    if (error != null) {
      map.addAttribute("error", error);
    } else {
      map.addAttribute("item", notification);
    }
    logger.debug("###Exiting saveMaintenanceSchedule() method @POST");
    return "system.health.maintenanceView";
  }

  @RequestMapping(value = "/updateMaintenanceSchedule", method = RequestMethod.POST)
  public String updateMaintenanceSchedule(@ModelAttribute("serviceNotificationForm") ServiceNotificationForm form,
      ModelMap map) {
    logger.debug("###Entering in updateMaintenanceSchedule() method @POST");
    ServiceNotification notification = form.getServiceNotification();
    notification.setPlannedStart(getDateInSystemTZ(notification.getPlannedStart(), getCurrentUserTimeZone()));
    notification.setPlannedEnd(getDateInSystemTZ(notification.getPlannedEnd(), getCurrentUserTimeZone()));
    healthService.updateServiceNotification(notification);
    map.addAttribute("item", notification);
    logger.debug("###Exiting updateMaintenanceSchedule() method @POST");
    return "system.health.maintenanceView";
  }

  @RequestMapping(value = "/maintenanceView", method = RequestMethod.GET)
  public String maintenanceView(
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      @RequestParam(value = "id", required = false) String notificationId, ModelMap map) {
    logger.debug("###Entering in healthmaintainance View");
    setPage(map, Page.SUPPORT_HEALTH);
    Long notificationId2 = Long.valueOf(notificationId);
    List<ServiceNotification> list = null;
    // List<Map<String, String>> zones = listZonesAsPortalUser();
    List<ServiceInstance> cloudTypeServiceInstances = null;
    cloudTypeServiceInstances = connectorManagementService.getCloudTypeServiceInstances();

    ServiceInstance serviceInstance = null;
    if (cloudTypeServiceInstances != null && cloudTypeServiceInstances.size() > 0) {
      if (serviceInstanceUUID != null && !serviceInstanceUUID.equals("")) {
        serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
      }
      list = healthService.listNotificationsByServiceInstance(serviceInstance, 0, 0);
      map.addAttribute("cloudTypeServiceInstances", cloudTypeServiceInstances);
      // map.addAttribute("selectedZone", getZoneById(zones, zoneId));
    }

    list = healthService.listNotificationsByServiceInstance(serviceInstance, 0, 0);

    map.addAttribute("notifications", list);
    map.addAttribute("today", new Date());
    Map<Date, Health> dateStatus = new TreeMap<Date, Health>(Collections.reverseOrder());
    Map<Date, List<ServiceNotification>> dateStatusHistory = new TreeMap<Date, List<ServiceNotification>>(
        Collections.reverseOrder());
    getStatusAndHistoryList(dateStatus, dateStatusHistory, serviceInstance);
    map.addAttribute("dateStatus", dateStatus);
    map.addAttribute("dateStatusHistory", dateStatusHistory);
    map.addAttribute("item", healthService.getPlannedNotification(notificationId2));
    map.addAttribute("tenant", getTenant());
    logger.debug("###Exiting healthmaintainance View");
    return "system.health.maintenanceView";
  }

  /**
   * Edit Status
   * 
   * @param ID
   * @param map
   * @return
   */
  @RequestMapping(value = ("/editStatusDetails"), method = RequestMethod.GET)
  public String editStatusDetails(@RequestParam(value = "Id", required = true) String ID, ModelMap map) {
    logger.debug("### editStatusDetails method starting...(GET)");
    ServiceNotification notification = healthService.locateServiceNotificationById(ID);
    ServiceNotificationForm serviceNotificationForm = new ServiceNotificationForm(notification);
    serviceNotificationForm.setNotificationId(notification.getId() + "");
    map.addAttribute("serviceNotificationForm", serviceNotificationForm);
    map.addAttribute("serviceNotification", notification);
    logger.debug("### editStatusDetails method end");
    return "system.health.editStatusDetails";
  }

  @RequestMapping(value = {
    "/{id}/removeStatus"
  }, method = RequestMethod.GET)
  @ResponseBody
  public String deleteServiceNotification(@PathVariable String id, ModelMap map) {
    // TODO: Needs to be redone based on service instance.
    logger.debug("### In deleteServiceNotification()  start method...");
    ServiceNotification serviceNotification = healthService.locateServiceNotificationById(id);
    healthService.deleteServiceNotification(serviceNotification);
    ServiceInstance serviceInstance = serviceNotification.getServiceInstance();

    List<ServiceNotification> list = null;

    list = healthService.listNotificationsByServiceInstance(serviceInstance, 0, 0);

    map.addAttribute("notifications", list);
    map.addAttribute("today", new Date());
    Map<Date, Health> dateStatus = new TreeMap<Date, Health>(Collections.reverseOrder());
    Map<Date, List<ServiceNotification>> dateStatusHistory = new TreeMap<Date, List<ServiceNotification>>(
        Collections.reverseOrder());
    getStatusAndHistoryList(dateStatus, dateStatusHistory, serviceInstance);
    map.addAttribute("dateStatus", dateStatus);
    map.addAttribute("dateStatusHistory", dateStatusHistory);
    map.addAttribute("maintenance", healthService.listPlannedNotifications(serviceInstance, 0, 0));
    logger.debug("###Exiting deleteServiceNotification() method @POST");
    return "success";
  }

  /**
   * move this to service later
   * 
   * @param dateStatus a map (keyed with the local date) of the latest status of the day
   * @param dateStatusHistory a map (keyed with the local date) of the status history of the day
   * @param zoneId id of the zone
   */
  private void getStatusAndHistoryList(Map<Date, Health> dateStatus,
      Map<Date, List<ServiceNotification>> dateStatusHistory, ServiceInstance serviceInstance) {
    logger.debug("#### Start getStatusAndHistoryList");
    Calendar localDateAtZeroHour = Calendar.getInstance(TimeZone.getTimeZone(getCurrentUserTimeZone()));
    // localDateAtZeroHour = DateUtils.truncate(localDateAtZeroHour, Calendar.DAY_OF_MONTH);
    // IMPORTANT!!! don't zero out after setting the time zone, won't work.
    localDateAtZeroHour.set(Calendar.HOUR_OF_DAY, 0);
    localDateAtZeroHour.set(Calendar.MINUTE, 0);
    localDateAtZeroHour.set(Calendar.SECOND, 0);
    // localDateAtZeroHour.setTimeZone(TimeZone.getTimeZone(getCurrentUserTimeZone())); // does this do anything??
    Date dates[] = getDays(localDateAtZeroHour, false, true);
    for (Date localDate : dates) {
      logger.debug("#### localDate" + localDate + "-->" + healthService.getSystemHealth(localDate, serviceInstance));

      dateStatus.put(localDate, healthService.getSystemHealth(localDate, serviceInstance));
      dateStatusHistory.put(localDate,
          healthService.listNotifications(localDate, getCurrentUserTimeZone(), 0, 0, serviceInstance));
      logger.debug("#### Start Notifications");
      for (ServiceNotification s : healthService.listNotifications(localDate, getCurrentUserTimeZone(), 0, 0,
          serviceInstance)) {
        logger.debug(s);
      }
      logger.debug("#### End Notifications");
    }
    logger.debug("#### End getStatusAndHistoryList");
  }

  private String getCurrentUserTimeZone() {
    String timezone = getCurrentUser().getTimeZone();
    if (timezone != null) {
      return timezone;
    }
    return DateTimeUtils.SYS_DEFAULT_TIMEZONE;
  }

  /**
   * @param instance
   * @return
   */
  private Date[] getDays(Calendar calendar, boolean prev, boolean inclusive) {
    List<Date> dates = new ArrayList<Date>();
    int num = DAYS_PER_PAGE;
    if (inclusive) {
      dates.add(calendar.getTime());
      num--;
    }
    int increment = prev ? 1 : -1;
    for (int i = 0; i < num; i++) {
      calendar.add(Calendar.DAY_OF_MONTH, increment);
      if (prev) {
        dates.add(0, calendar.getTime());
      } else {
        dates.add(calendar.getTime());
      }
    }
    return dates.toArray(new Date[dates.size()]);
  }
}
