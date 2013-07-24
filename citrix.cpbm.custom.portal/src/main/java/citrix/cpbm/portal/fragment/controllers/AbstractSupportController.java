/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.citrix.cpbm.platform.admin.service.ConnectorManagementService;
import com.vmops.internal.service.PrivilegeService;
import com.vmops.internal.service.TicketService.Capability;
import com.vmops.model.Tenant;
import com.vmops.model.Ticket;
import com.vmops.model.Ticket.TicketStatus;
import com.vmops.model.TicketComment;
import com.vmops.model.User;
import com.vmops.service.SupportService;
import com.vmops.service.exceptions.SupportServiceException;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.TicketCommentForm;
import com.vmops.web.forms.TicketForm;

/**
 * Handles Support requests.
 * 
 * @author Pallavi
 */
/**
 * Handles Support requests.
 * 
 * @author Pallavi
 */

public abstract class AbstractSupportController extends AbstractAuthenticatedController {

  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(AbstractSupportController.class);

  @Autowired
  private SupportService supportService;

  @Autowired
  private PrivilegeService privilegeService;

  @Autowired
  private ConnectorManagementService connectorManagementService;

  @RequestMapping(value = {
      "/", "/tickets"
  }, method = RequestMethod.GET)
  public String listTickets(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "statusFilter", required = false) String statusFilter,
      @RequestParam(value = "showNewTicket", required = false) boolean showNewTicket,
      @RequestParam(value = "sortType", required = false) String sortType,
      @RequestParam(value = "sortColumn", required = false) String sortColumn,
      @RequestParam(value = "queryLocator", required = false) String queryLocator,
      @RequestParam(value = "page", required = false, defaultValue = "1") int page,
      @RequestParam(value = "ticketNumber", required = false) String ticketNumber, ModelMap map) {
    logger.debug("Entering in listTickets");

    int perPage = getDefaultPageSize();
    map.addAttribute("defaultPageSize", getDefaultPageSize());

    User user = getCurrentUser();
    setPage(map, Page.SUPPORT_ALL_TICKETS);
    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null
        && !tenant.getParam().equals(tenantParam)) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
    }
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(user));
    map.addAttribute("tenant", tenant);
    Capability capability = supportService.getTicketCapability();
    if (capability.equals(Capability.C)) {
      TicketForm ticketForm = new TicketForm();
      map.addAttribute("ticketForm", ticketForm);
      return "support.tickets.createonly";
    } else {
      List<Ticket> tickets = new ArrayList<Ticket>();
      Integer totalTickets = 0;
      String status = "All";
      if (statusFilter != null) {
        status = statusFilter;
        map.addAttribute("statusFilter", statusFilter);
        logger.info(" filter by status = " + status);
      }

      if (ticketNumber != null) {
        Ticket tkt = supportService.get(ticketNumber);
        if (tkt != null) {
          tickets.add(tkt);
          totalTickets = 1;
        }
      }

      else {
        List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
        if (status.equalsIgnoreCase("Open")) {
          listTicketStatus.add(TicketStatus.NEW);
          listTicketStatus.add(TicketStatus.ESCALATED);
          listTicketStatus.add(TicketStatus.WORKING);
        } else if (!status.equalsIgnoreCase("All")) {
          listTicketStatus.add(TicketStatus.getTicketStatus(status));
        }
        List<User> users = new ArrayList<User>();
        users.add(user);

        if (sortType != null && sortColumn != null) {
          map.addAttribute("sortType", sortType);
          map.addAttribute("sortColumn", sortColumn);
        }
        Map<String, String> responseAttribute = new HashMap<String, String>();
        responseAttribute.put("queryLocator", queryLocator);
        List<Ticket> allTickets = supportService.list(page, perPage + 1, listTicketStatus, users, sortType, sortColumn,
            responseAttribute);

        totalTickets = allTickets.size();
        if (allTickets.size() > perPage) {
          allTickets.remove(allTickets.size() - 1);
        }
        tickets = allTickets;

        map.addAttribute("queryLocator", responseAttribute.get("queryLocator"));
        map.addAttribute("hasMore", responseAttribute.get("hasMore"));
      }

      map.addAttribute("tickets", tickets);
      if (tickets != null && tickets.size() > 0) {
        map.addAttribute("ticketForm", new TicketForm(tickets.get(0)));
        List<TicketComment> comments = new ArrayList<TicketComment>();
        for (Ticket tkt : tickets) {
          List<TicketComment> tktComments = supportService.listComments(tkt, user);
          if (tktComments != null && tktComments.size() > 0) {
            comments.addAll(tktComments);
          }
        }
        if (comments != null && comments.size() > 0) {
          map.addAttribute("ticketcomments", comments);
        }
        TicketCommentForm ticketCommentForm = new TicketCommentForm();
        TicketComment comment = new TicketComment();
        comment.setParentId(tickets.get(0).getUuid());
        ticketCommentForm.setComment(comment);
        map.addAttribute("ticketCommentForm", ticketCommentForm);
      }
      if (tickets != null && tickets.size() > 0) {
        map.addAttribute("size", tickets.size());
      } else {
        map.addAttribute("size", 0);
      }

      map.addAttribute("user", user);
      map.addAttribute("showNewTicket", showNewTicket);
      map.addAttribute("statuses", TicketStatus.values());

      if (totalTickets > perPage) {
        map.addAttribute("enable_next", "True");
      } else {
        map.addAttribute("enable_next", "False");
      }
      map.addAttribute("current_page", page);

      logger.debug("Leaving in listTickets");
      return "support.tickets";
    }
  }

  @RequestMapping(value = {
      "/", "/tickets/page"
  }, method = RequestMethod.GET)
  public String listTicketsPage(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "statusFilter", required = false) String statusFilter,
      @RequestParam(value = "showNewTicket", required = false) boolean showNewTicket,
      @RequestParam(value = "sortType", required = false) String sortType,
      @RequestParam(value = "sortColumn", required = false) String sortColumn,
      @RequestParam(value = "queryLocator", required = true) String queryLocator, ModelMap map,
      HttpServletRequest request) {
    logger.debug("Entering in listTicketsPage");

    if (queryLocator == null || "null".equalsIgnoreCase(queryLocator)) {
      logger.debug("Leaving in listTicketsPage: queryLocator is null. No more data to show");
      return "support.tickets.list";
    }
    User user = getCurrentUser();
    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null
        && !tenant.getParam().equals(tenantParam)) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
    }
    List<Ticket> tickets = null;
    String status = "All";
    if (statusFilter != null) {
      status = statusFilter;
      logger.info(" filter by status = " + status);
    }
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", queryLocator);

    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    if (status.equalsIgnoreCase("Open")) {
      listTicketStatus.add(TicketStatus.NEW);
      listTicketStatus.add(TicketStatus.ESCALATED);
      listTicketStatus.add(TicketStatus.WORKING);
    } else if (!status.equalsIgnoreCase("All")) {
      listTicketStatus.add(TicketStatus.getTicketStatus(status));
    }
    List<User> users = new ArrayList<User>();
    users.add(user);

    tickets = supportService.list(0, 0, listTicketStatus, users, sortType, sortColumn, responseAttribute);

    map.addAttribute("tickets", tickets);
    map.addAttribute("queryLocator", responseAttribute.get("queryLocator"));

    map.addAttribute("enable_next", "False");

    logger.debug("Leaving in listTicketsPage");
    return "support.tickets.list";
  }

  @RequestMapping(value = "/tickets/create", method = RequestMethod.GET)
  public String createTicket(@RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map) {
    logger.debug("###In createTicket method starting...");
    TicketForm ticketForm = new TicketForm();
    map.addAttribute("createTicketForm", ticketForm);
    logger.debug("###In createTicket method end...");
    return "support.tickets.create";
  }

  @ResponseBody
  @RequestMapping(value = "/tickets/create", method = RequestMethod.POST)
  public Ticket createTicket(@RequestParam(value = "tenant", required = false) String tenantParam,
      HttpServletRequest request, @ModelAttribute("createTicketForm") TicketForm ticketForm, ModelMap map) {
    logger.debug("###In createTicket method starting...(POST)");
    User user = getCurrentUser();
    Tenant tenant = user.getTenant();
    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null
        && !tenant.getParam().equals(tenantParam)) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
    }
    Ticket ticket = ticketForm.getTicket();
    ticket = supportService.create(ticket.getSubject(), ticket.getDescription(), "WEB", user);
    logger.debug("###In createTicket method end...(POST)");
    return ticket;
  }

  @RequestMapping(value = "/tickets/view", method = RequestMethod.GET)
  public String viewTicket(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "ticketNumber", required = true) String ticketNumber,
      @RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map) {
    logger.debug("###In viewTicket method starting...(GET) with tktNumber" + ticketNumber);

    User user = getCurrentUser();
    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null
        && !tenant.getParam().equals(tenantParam)) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
    }

    if (ticketNumber != null) {
      Ticket ticket = supportService.get(ticketNumber);
      if (ticket != null) {
        map.addAttribute("ticket", ticket);
        map.addAttribute("ticketForm", new TicketForm(ticket));
        List<TicketComment> comments = supportService.listComments(ticket, user);
        if (comments != null && comments.size() > 0) {
          map.addAttribute("ticketcomments", comments);
        }
        TicketCommentForm ticketCommentForm = new TicketCommentForm();
        TicketComment comment = new TicketComment();
        comment.setParentId(ticket.getUuid());
        ticketCommentForm.setComment(comment);
        map.addAttribute("ticketCommentForm", ticketCommentForm);
      }
    }
    map.addAttribute("tenant", tenant);
    map.addAttribute("statuses", TicketStatus.values());
    logger.debug("###In editTicket method end...(GET)");
    return "support.tickets.view";
  }

  @RequestMapping(value = "/tickets/{ticketId}/comment", method = RequestMethod.POST)
  @ResponseBody
  public String createNewComment(@ModelAttribute("currentTenant") Tenant tenant,
      @ModelAttribute("ticketCommentForm") TicketCommentForm ticketCommentForm,
      @PathVariable(value = "ticketId") String ticketNumber,
      @RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map) {
    logger.debug("###In createNewComment method starting...(POST)");
    TicketComment comment = ticketCommentForm.getComment();
    User user = getCurrentUser();
    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();

    }

    try {
      if (ticketNumber != null) {
        Ticket ticket = supportService.get(ticketNumber);
        if (ticket != null) {
          if (ticket.getUuid().equals(comment.getParentId())) {
            comment = supportService.createComment(ticket, comment.getComment(), user);
          }
        }
      }
    } catch (Exception e) {
      logger.error("Failed to post comment", e);
      return "failure";
    }

    logger.debug("###In createNewComment method end...(POST) ");
    return "success";
  }

  @RequestMapping(value = "/tickets/edit", method = RequestMethod.POST)
  public String editTicket(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "statusFilter", required = false) String statusFilter,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "sortType", required = false) String sortType,
      @RequestParam(value = "sortColumn", required = false) String sortColumn,
      @RequestParam(value = "caseNumber", required = false) String caseNumber, HttpServletRequest request,
      @ModelAttribute("ticketForm") TicketForm ticketForm, ModelMap map) {
    logger.debug("###In editTicket method starting...(POST)");
    User user = getCurrentUser();
    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
    }
    String status = (statusFilter == null || statusFilter.equalsIgnoreCase("null") ? "All" : statusFilter);
    sortType = (sortType == null || sortType.equalsIgnoreCase("null") ? "" : sortType);
    sortColumn = (sortColumn == null || sortColumn.equalsIgnoreCase("null") ? "" : sortColumn);
    Ticket ticket = ticketForm.getTicket();
    Ticket t = supportService.get(caseNumber);
    ticket.setUpdatedAt(new Date());
    ticket.setUpdatedBy(user);
    ticket.setSubject(ticket.getSubject() + "&" + t.getSubject().split("&")[1]);
    if (ticket.getStatus() == null || "null".equals(ticket.getStatus().name())) {
      ticket.setStatus(t.getStatus());
    }
    supportService.update(ticket, user);
    logger.debug("###In editTicket method end...(POST)");
    return "redirect:/portal/support/tickets?tenant=" + tenantParam + "&statusFilter=" + status + "&sortType="
        + sortType + "&sortColumn=" + sortColumn;
  }

  @RequestMapping(value = "/tickets/close", method = RequestMethod.POST)
  @ResponseBody
  public String closeTicket(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "caseNumber", required = true) String ticketNumber, HttpServletRequest request,
      @ModelAttribute("ticketForm") TicketForm ticketForm, ModelMap map) {
    logger.debug("###In closeTicket method starting...(POST) tkt#" + ticketNumber);
    String returnValue = "Failure";
    if (ticketNumber != null) {
      Ticket tkt = supportService.get(ticketNumber);
      tkt.setStatus(TicketStatus.CLOSED);
      tkt.setUpdatedAt(new Date());
      tkt.setUpdatedBy(getCurrentUser());
      supportService.update(tkt, tenant.getOwner());
      returnValue = "Success";
    }
    logger.debug("###In editTicket method end...(POST)");
    return returnValue;
  }

  @RequestMapping(value = {
    "/homeTicketsCount"
  }, method = RequestMethod.GET)
  public String homeTicketsCount(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenantParam", required = false) String tenantParam, ModelMap map, HttpSession session,
      HttpServletRequest request) {
    logger.debug("Entering in homeTicketsCount");
    User user = getCurrentUser();

    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
    }

    String error = null;
    try {
      List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
      List<User> users = new ArrayList<User>();
      users.add(user);

      List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, null, null,
          new HashMap<String, String>());
      int new_tickets = 0;
      int working_tickets = 0;
      int closed_tickets = 0;
      int escalated_tickets = 0;
      for (Ticket tkt : tickets) {
        if (tkt.getStatus().equals(TicketStatus.NEW)) {
          new_tickets++;
        } else if (tkt.getStatus().equals(TicketStatus.WORKING)) {
          working_tickets++;
        } else if (tkt.getStatus().equals(TicketStatus.CLOSED)) {
          closed_tickets++;
        } else if (tkt.getStatus().equals(TicketStatus.ESCALATED)) {
          escalated_tickets++;
        }
      }
      map.addAttribute("new_tickets_count", new_tickets);
      map.addAttribute("working_tickets_count", working_tickets);
      map.addAttribute("closed_tickets_count", closed_tickets);
      map.addAttribute("escalated_tickets_count", escalated_tickets);

    } catch (SupportServiceException ex) {
      if (ex.getMessage().equals("SERVICE_NOT_REACHABLE")) {
        error = "SERVICE_NOT_REACHABLE".toLowerCase();
      }
    }
    logger.debug("Leaving in homeTicketsCount");
    return "home.support.tickets.count";
  }

  @RequestMapping(value = "/tickets/create/email", method = RequestMethod.POST)
  public String createFormToEmail(@RequestParam(value = "tenant", required = false) String tenantParam,
      @ModelAttribute("currentTenant") Tenant tenant, HttpServletRequest request,
      @ModelAttribute("ticketForm") TicketForm ticketForm, ModelMap map) {
    setPage(map, Page.SUPPORT_ALL_TICKETS);
    User user = getCurrentUser();
    if (userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && tenantParam != null) {
      tenant = tenantService.get(tenantParam);
    }
    Ticket ticket = ticketForm.getTicket();
    ticket = supportService.create(ticket.getSubject(), ticket.getDescription(), ticket.getCategory().getName(), user);
    map.addAttribute("message",
        messageSource.getMessage("support.ticket.create.email.success", null, getSessionLocale(request)));
    map.addAttribute("tenant", tenant);
    map.addAttribute("status", true);
    map.addAttribute("ticketForm", new TicketForm());
    logger.info("Ticket has been created and details are : Description = " + ticket.getDescription() + ", Subject = "
        + ticket.getSubject());
    return "support.tickets.createonly";
  }
}
