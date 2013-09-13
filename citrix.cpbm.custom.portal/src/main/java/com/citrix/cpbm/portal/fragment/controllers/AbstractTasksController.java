/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
/**
 * 
 */
package com.citrix.cpbm.portal.fragment.controllers;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import com.citrix.cpbm.core.workflow.model.Task;
import com.citrix.cpbm.core.workflow.service.TaskService;
import com.vmops.model.Tenant;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;

/**
 * @author rajanik
 */
public abstract class AbstractTasksController extends AbstractAuthenticatedController {

  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(AbstractTasksController.class);

  @Autowired
  private TaskService taskService;

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String getTasks(@ModelAttribute("currentTenant") Tenant currentTenant, @RequestParam(value = "tenant") final String tenantParam,
      @RequestParam(value = "filter", defaultValue = "PENDING") String filter,
      @RequestParam(value = "page", defaultValue = "1") Integer page, ModelMap map, HttpServletRequest request) {
    logger.debug("###Entering in getTasks(filter) method @GET");
    setPage(map, Page.DASHBOARD_ALL_TASKS);
    Tenant tenant = tenantService.get(tenantParam);
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      map.addAttribute("showUserProfile", true);
      map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(tenant.getOwner()));
    } else {
      map.addAttribute("showUserProfile", false);
      map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(currentTenant.getOwner()));
    }

    Integer perPage = getDefaultPageSize();

    Task.State[] taskStates;
    if (filter.equalsIgnoreCase("COMPLETED")) {
      // success and error states
      taskStates = new Task.State[] {
          Task.State.FAILURE, Task.State.SUCCESS
      };

    } else if (filter.equalsIgnoreCase("ALL")) {
      // success, error and pending states
      taskStates = new Task.State[] {
          Task.State.FAILURE, Task.State.SUCCESS, Task.State.PENDING
      };
    } else {
      // pending states
      taskStates = new Task.State[] {
        Task.State.PENDING
      };
    }

    Map<Task, String> tasksMap = taskService.getTasksMap(tenant, getCurrentUser(), taskStates, page, perPage);

    map.addAttribute("tasksMap", tasksMap);
    map.addAttribute("tenant", tenant);
    
    map.addAttribute("taskfilters", Arrays.asList("ALL", "PENDING", "COMPLETED"));
    map.addAttribute("currentFilter", filter);
    
    if (tasksMap.entrySet().size() == perPage) {
      // if tasks map size is equal to perpage, next page may or may not exist
      map.addAttribute("nextPage", page + 1);
    }
    if (page > 1) {
      // if the current page is > 1, assuming a previous page exists
      map.addAttribute("prevPage", page - 1);
    }
    
    logger.debug("###Exiting in getTasks(filter) method @GET");
    return "tasks.all";
  }
  
  @RequestMapping(value = "/{taskUuid}/", method = RequestMethod.GET)
  public String getTask(@PathVariable String taskUuid, @RequestParam(value = "tenant") final String tenantParam, ModelMap map) {
    Map<Task,String> taskMap = taskService.getTaskMap(tenantService.get(tenantParam), taskUuid);
    Task task = taskMap.keySet().iterator().next();
    map.addAttribute("task", task);
    map.addAttribute("taskUrl", taskMap.get(task));
    return "task.view";
  }
  
  @RequestMapping(value = "/approval-task/{taskUuid}", method = RequestMethod.GET)
  public String getApprovalTask(@ModelAttribute("currentLocale") Locale currentLocale, @PathVariable String taskUuid, ModelMap map) {
    Task task = taskService.get(taskUuid);
    map.addAttribute("task", task);
    map.addAttribute("transactionType", task.getBusinessTransaction().getClass().getSimpleName());
    String taskDetails = taskService.getDetails(task, currentLocale);
    map.addAttribute("taskDetails", taskDetails);
    return "approval.task";
  }

  @RequestMapping(value = "/approval-task", method = RequestMethod.POST)
  @ResponseBody
  public String actOnApprovalTask(
      @RequestParam(value = "uuid", required = true) String pendingActionId,
      @RequestParam(value = "state", required = true) String state,
      @RequestParam(value = "memo", required = false) String memo, HttpServletRequest request) {
    logger.debug("### In actOnPendingAction() method starting...");
    Task.State stateChangeTo;
    if(state.equalsIgnoreCase("success")) {
      stateChangeTo = Task.State.SUCCESS;
    } else {
      if(StringUtils.isEmpty(memo)) {
        throw new InvalidAjaxRequestException(messageSource.getMessage("ui.accounts.all.pending.changes.memorequired", null, request.getLocale()));
      }
      stateChangeTo = Task.State.FAILURE;
    }
    Task currentTask = taskService.get(pendingActionId);
    Task action = taskService.completeTask(currentTask, stateChangeTo, memo);
    logger.debug("### In actOnPendingAction() method ending.");
    return messageSource.getMessage("ui.task.state."+action.getState(), null, request.getLocale());
  }

}
