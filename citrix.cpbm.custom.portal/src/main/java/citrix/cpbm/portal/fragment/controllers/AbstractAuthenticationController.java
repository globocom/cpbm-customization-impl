/* Copyright (C) 2011 Citrix Systems, Inc. All rights reserved */
package citrix.cpbm.portal.fragment.controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import com.citrix.cpbm.platform.admin.service.ConnectorManagementService;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory.ConnectorType;
import com.vmops.internal.service.EventService;
import com.vmops.internal.service.PrivilegeService;
import com.vmops.internal.service.TelephoneVerificationService;
import com.vmops.model.Event.Category;
import com.vmops.model.Event.Scope;
import com.vmops.model.Event.Severity;
import com.vmops.model.Event.Source;
import com.vmops.model.User;
import com.vmops.model.UserAlertPreferences;
import com.vmops.model.UserAlertPreferences.AlertType;
import com.vmops.portal.config.Configuration;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.SupportService;
import com.vmops.service.TenantService;
import com.vmops.service.UserAlertPreferencesService;
import com.vmops.service.UserService;
import com.vmops.service.exceptions.NoSuchUserException;
import com.vmops.service.exceptions.TelephoneVerificationServiceException;
import com.vmops.web.controllers.AbstractBaseController;
import com.vmops.web.filters.CaptchaAuthenticationFilter;
import com.vmops.web.filters.exceptions.CaptchaValidationException;

public abstract class AbstractAuthenticationController extends AbstractBaseController {

  /**
   * The parameter name that indicates login failed.
   */
  private static final String LOGIN_FAILED_PARAM = "login_failed";

  private static final String LOGOUT_PARAM = "logout";

  private static final String TIME_OUT = "timeout";

  private static final String VERIFY = "verify";

  private static final String CAS = "cas";

  /**
   * The user service.
   */
  @Autowired
  private TenantService tenantService;

  /**
   * The user service.
   */
  @Autowired
  private UserService userService;

  /**
   * PrivilegeService
   */
  @Autowired
  private PrivilegeService privilegeService;

  @Autowired
  private EventService eventService;

  @Autowired
  protected ConnectorManagementService connectorManagementService;

  @Autowired
  private UserAlertPreferencesService userAlertPreferencesService;

  @Autowired
  private SupportService supportService;

  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(AbstractAuthenticationController.class);

  @RequestMapping({
      "/login", "/portal", "/"
  })
  public String login(HttpServletRequest request, ModelMap map, HttpSession session) {
    logger.debug("###Entering in login(req,map,session) method");
    if (request.getUserPrincipal() != null) {
      map.clear();
      return "redirect:/portal/home";
    }
    String showSuffixControl = "false";
    String suffixControlType = "textbox";
    List<String> suffixList = null;
    if (config.getValue(Names.com_citrix_cpbm_username_duplicate_allowed).equals("true")) {
      showSuffixControl = "true";
      if (config.getValue(Names.com_citrix_cpbm_login_screen_tenant_suffix_dropdown_enabled).equals("true")) {
        suffixControlType = "dropdown";
        suffixList = tenantService.getSuffixList();
      }
    }
    map.addAttribute("showSuffixControl", showSuffixControl);
    map.addAttribute("suffixControlType", suffixControlType);
    map.addAttribute("suffixList", suffixList);
    if (config.getBooleanValue(Configuration.Names.com_citrix_cpbm_portal_directory_service_enabled)
        && config.getValue(Names.com_citrix_cpbm_directory_mode).equals("pull")) {
      map.addAttribute("directoryServiceAuthenticationEnabled", "true");
    }
    if (config.getValue(Names.com_citrix_cpbm_public_catalog_display).equals("true"))
      map.addAttribute("showAnonymousCatalogBrowsing", "true");
    map.addAttribute("showLanguageSelection", "true");
    map.addAttribute("supportedLocaleList", this.getLocaleDisplayName(listSupportedLocales()));
    map.addAttribute("selected_language", request.getParameter("lang"));
    String redirect = null;
    boolean loginFailed = request.getParameter(LOGIN_FAILED_PARAM) != null;
    boolean loggedOut = request.getParameter(LOGOUT_PARAM) != null;
    final Throwable ex = (Throwable) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

    // capture previous CAPTCHA position
    Boolean captchaRequiredSessionObj = (Boolean) session.getAttribute(CaptchaAuthenticationFilter.CAPTCHA_REQUIRED);

    // Get last user
    String username = (String) session.getAttribute(CaptchaAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY);

    // this as spring does a text-escape when it saves this attribute
    final String uUsername = HtmlUtils.htmlUnescape(username);

    if (loginFailed) {
      String error = " " + messageSource.getMessage("error.auth.username.password.invalid", null, request.getLocale());

      try {
        User user = privilegeService.runAsPortal(new PrivilegedAction<User>() {

          public User run() {
            User user = userService.getUserByParam("username", uUsername, false);

            // All user writes here.
            // Every time there is a login failure but not invalid CAPTCHA,
            // we update failed login attempts for the user
            if (!(ex instanceof CaptchaValidationException) && !(ex instanceof LockedException))
              user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            int attempts = user.getFailedLoginAttempts();

            // Also locking the root user and quite easily too. Clearly this needs an eye!
            if (attempts >= config.getIntValue(Names.com_citrix_cpbm_accountManagement_security_logins_lockThreshold)) {
              user.setEnabled(false);
            }

            return user;
          }
        });

        int attempts = user.getFailedLoginAttempts();
        if (attempts >= config.getIntValue(Names.com_citrix_cpbm_accountManagement_security_logins_captchaThreshold)) {
          session.setAttribute(CaptchaAuthenticationFilter.CAPTCHA_REQUIRED, true);
        }
      } catch (NoSuchUserException e) {
        // map.addAttribute("showCaptcha", true);
      }

      captchaRequiredSessionObj = (Boolean) session.getAttribute(CaptchaAuthenticationFilter.CAPTCHA_REQUIRED);

      map.addAttribute("loginFailed", loginFailed);
      String lastUsername = uUsername;

      if (config.getValue(Names.com_citrix_cpbm_username_duplicate_allowed).equals("true")) {
        if (!lastUsername.equals("root") && !lastUsername.equals("")) {
          lastUsername = lastUsername.substring(0, lastUsername.lastIndexOf('@'));
        }
      }
      map.addAttribute("lastUser", lastUsername);

      // Compose error string
      if (ex instanceof DisabledException) {
        error = " " + messageSource.getMessage("error.auth.username.password.invalid", null, request.getLocale());
      } else if (ex instanceof CaptchaValidationException) {
        error = " " + messageSource.getMessage("error.auth.captcha.invalid", null, request.getLocale());
      } else if (ex instanceof LockedException) {
        error = " " + messageSource.getMessage("error.auth.account.locked", null, request.getLocale());
      } else if (ex instanceof BadCredentialsException) {
        if (ex.getMessage() != null && ex.getMessage().length() > 0)
          // error = " " + ex.getMessage();
          error = " " + messageSource.getMessage("error.auth.username.password.invalid", null, request.getLocale());
      } else if (ex instanceof AuthenticationException) {
        error = " " + messageSource.getMessage("error.auth.username.password.invalid", null, request.getLocale());
      } else {
        logger.error("Error occurred in authentication", ex);
        error = " " + messageSource.getMessage("error.auth.unknown", null, request.getLocale());
      }

      if (captchaRequiredSessionObj != null && captchaRequiredSessionObj == true
          && !(ex instanceof CaptchaValidationException) && !(ex instanceof LockedException)) {
        error += " " + messageSource.getMessage("error.auth.account.may.locked", null, request.getLocale());
      }

      map.addAttribute("error", error);

    }

    if (loggedOut) {
      map.addAttribute("logout", loggedOut);
    }

    // This could come from session or from user
    if (captchaRequiredSessionObj != null && captchaRequiredSessionObj.booleanValue()) {
      map.addAttribute("showCaptcha", true);
      map.addAttribute("recaptchaPublicKey", config.getRecaptchaPublicKey());
    }

    map.addAttribute(TIME_OUT, request.getParameter(TIME_OUT) != null);
    map.addAttribute(VERIFY, request.getParameter(VERIFY) != null);
    logger.debug("###Exiting login(req,map,session) method");

    if (config.getAuthenticationService().compareToIgnoreCase(CAS) == 0) {
      try {
        redirect = StringUtils.isEmpty(config.getCasLoginUrl()) ? null
            : (config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        logger.error("Exception encoding: " + redirect, e);
      }
      if (redirect == null) {
        throw new InternalError("CAS authentication required, but login url not set");
      }
    }
    return redirect == null ? "auth.login" : "redirect:" + redirect;
  }

  @RequestMapping(value = {
      "/{userParam}/loggedout", "{userParam}/j_spring_security_logout"
  })
  public String loggedout(@PathVariable String userParam, ModelMap map, HttpSession session,
      HttpServletResponse response) {
    logger.debug("###Entering in loggedout(response) method");
    String showSuffixControl = "false";
    String suffixControlType = "textbox";
    List<String> suffixList = null;
    if (config.getValue(Names.com_citrix_cpbm_username_duplicate_allowed).equals("true")) {
      showSuffixControl = "true";
      if (config.getValue(Names.com_citrix_cpbm_login_screen_tenant_suffix_dropdown_enabled).equals("true")) {
        suffixControlType = "dropdown";
        suffixList = tenantService.getSuffixList();
      }
    }
    map.addAttribute("showSuffixControl", showSuffixControl);
    map.addAttribute("suffixControlType", suffixControlType);
    map.addAttribute("suffixList", suffixList);
    if (config.getBooleanValue(Configuration.Names.com_citrix_cpbm_portal_directory_service_enabled)
        && config.getValue(Names.com_citrix_cpbm_directory_mode).equals("pull")) {
      map.addAttribute("directoryServiceAuthenticationEnabled", "true");
    }
    if (config.getValue(Names.com_citrix_cpbm_public_catalog_display).equals("true"))
      map.addAttribute("showAnonymousCatalogBrowsing", "true");
    map.addAttribute("showLanguageSelection", "true");
    map.addAttribute("supportedLocaleList", this.getLocaleDisplayName(listSupportedLocales()));
    map.addAttribute("logout", true);
    String redirect = null;
    Enumeration<String> en = session.getAttributeNames();
    while (en.hasMoreElements()) {
      String attr = en.nextElement();
      session.removeAttribute(attr);
    }
    Cookie cookie = new Cookie("JforumSSO", "");
    cookie.setMaxAge(0);
    cookie.setPath("/");
    response.addCookie(cookie);
    // create logout notification begins
    User user = userService.get(userParam);
    String message = "logged.out";
    String messageArgs = user.getUsername();
    eventService.createEvent(new Date(), user, message, messageArgs, Source.PORTAL, Scope.USER, Category.ACCOUNT,
        Severity.INFORMATION, true);
    if (config.getAuthenticationService().compareToIgnoreCase(CAS) == 0) {
      try {
        redirect = StringUtils.isEmpty(config.getCasLogoutUrl()) ? null
            : (config.getCasLogoutUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        logger.error("Exception encoding: " + redirect, e);
      }
      if (redirect == null) {
        throw new InternalError("CAS authentication required, but login url not set");
      }
    }
    session.invalidate();
    SecurityContextHolder.getContext().setAuthentication(null);
    // ends
    logger.debug("###Exiting loggedout(response) method");
    return redirect == null ? "auth.loggedout" : "redirect:" + redirect;
  }

  @RequestMapping(value = "/reset_password", method = RequestMethod.GET)
  public String requestReset(ModelMap map) {
    String showSuffixControl = "false";
    if (config.getValue(Names.com_citrix_cpbm_username_duplicate_allowed).equals("true")) {
      showSuffixControl = "true";
      map.addAttribute("useSmallCss", "true");
    }
    map.addAttribute("showSuffixControl", showSuffixControl);
    return "auth.request_reset";
  }

  @RequestMapping(value = "/requestCallByUser", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> requestCall(@RequestParam(value = "userName", required = false) final String userName,
      @RequestParam(value = "pickValFromReset", required = false) final String pickValFromReset,
      HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException {
    Map<String, String> returnResponse = new HashMap<String, String>();
    String generatedPhoneVerificationPin = request.getSession().getAttribute("phoneVerificationPin").toString();
    final String userNameLoc;

    if (pickValFromReset != null && pickValFromReset.equals("pick")) {
      userNameLoc = (String) request.getSession().getAttribute(RESET_USER_KEY);
    } else if (userName != null) {
      userNameLoc = userName;
    } else {
      returnResponse.put("result", "failed");
      returnResponse.put("message",
          messageSource.getMessage("js.errors.register.callFailed", null, request.getLocale()));
      return returnResponse;
    }

    request.getSession().setAttribute("userName", userNameLoc);
    User user = null;
    try {
      user = privilegeService.runAsPortal(new PrivilegedAction<User>() {

        public User run() {
          User user = userService.getUserByParam("username", userNameLoc, false);
          return user;
        }
      });
    } catch (NoSuchUserException e) {
    }

    if (user == null) {
      returnResponse.put("result", "failed");
      returnResponse.put("message",
          messageSource.getMessage("js.errors.register.callFailed", null, request.getLocale()));
      return returnResponse;
    }

    try {
      String refId = ((TelephoneVerificationService) connectorManagementService
          .getOssServiceInstancebycategory(ConnectorType.PHONE_VERIFICATION)).requestCall(user.getCountryCode(),
          user.getPhoneWithoutIsdCode(), generatedPhoneVerificationPin);
      if (refId != null) {
        returnResponse.put("result", "success");
        returnResponse.put("message",
            messageSource.getMessage("js.errors.register.callRequested", null, request.getLocale()));
      } else {
        returnResponse.put("result", "failed");
        returnResponse.put("message",
            messageSource.getMessage("js.errors.register.callFailed", null, request.getLocale()));
      }
    } catch (TelephoneVerificationServiceException e) {
      returnResponse.put("result", "failed");
      returnResponse.put("message",
          messageSource.getMessage("js.errors.register.callFailed", null, request.getLocale()));
    }

    return returnResponse;
  }

  @RequestMapping(value = "/requestSMSByUser", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> requestSMS(@RequestParam(value = "userName", required = false) final String userName,
      @RequestParam(value = "pickValFromReset", required = false) final String pickValFromReset,
      HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException {
    String generatedPhoneVerificationPin = request.getSession().getAttribute("phoneVerificationPin").toString();
    final String userNameLoc;
    Map<String, String> returnResponse = new HashMap<String, String>();

    if (pickValFromReset != null && pickValFromReset.equals("pick")) {
      userNameLoc = (String) request.getSession().getAttribute(RESET_USER_KEY);
    } else if (userName != null) {
      userNameLoc = userName;
    } else {
      returnResponse.put("result", "failed");
      returnResponse.put("message",
          messageSource.getMessage("js.errors.register.textMessageFailed", null, request.getLocale()));
      return returnResponse;
    }

    request.getSession().setAttribute("userName", userNameLoc);
    User user = null;
    try {
      user = privilegeService.runAsPortal(new PrivilegedAction<User>() {

        public User run() {
          User user = userService.getUserByParam("username", userNameLoc, false);
          return user;
        }
      });
    } catch (NoSuchUserException e) {
    }

    if (user == null) {
      returnResponse.put("result", "success"); // Returning success for security reason.
      returnResponse.put("message",
          messageSource.getMessage("js.errors.register.textMessageRequested", null, request.getLocale()));
    }

    try {
      String refId = ((TelephoneVerificationService) connectorManagementService
          .getOssServiceInstancebycategory(ConnectorType.PHONE_VERIFICATION)).requestSMS(user.getCountryCode(),
          user.getPhoneWithoutIsdCode(), generatedPhoneVerificationPin);
      if (refId != null) {
        returnResponse.put("result", "success");
        returnResponse.put("message",
            messageSource.getMessage("js.errors.register.textMessageRequested", null, request.getLocale()));
      } else {
        returnResponse.put("result", "failed");
        returnResponse.put("message",
            messageSource.getMessage("js.errors.register.textMessageFailed", null, request.getLocale()));
      }
    } catch (TelephoneVerificationServiceException e) {
      returnResponse.put("result", "failed");
      returnResponse.put("message",
          messageSource.getMessage("js.errors.register.textMessageFailed", null, request.getLocale()));
    }

    return returnResponse;
  }

  @RequestMapping(value = "/phoneverification/verifyPINForUnlock", method = RequestMethod.GET)
  @ResponseBody
  public String verifyPhoneVerificationPINForUnlock(@RequestParam(value = "PIN", required = true) String PIN,
      HttpServletRequest request) {

    String generatedPhoneVerificationPin = (String) request.getSession().getAttribute("phoneVerificationPin");

    if (PIN != null && PIN.equals(generatedPhoneVerificationPin))
      return "success";
    return "failed";
  }

  @RequestMapping(value = "/reset_password", method = RequestMethod.POST, params = "username")
  public String requestReset(@RequestParam(value = "username", required = true) final String username,
      HttpServletRequest request, ModelMap map) {
    logger.debug("###Entering in requestReset(username,request) method @POST");
    String showSuffixControl = "false";
    if (config.getValue(Names.com_citrix_cpbm_username_duplicate_allowed).equals("true")) {
      showSuffixControl = "true";
      map.addAttribute("useSmallCss", "true");
    }
    map.addAttribute("showSuffixControl", showSuffixControl);
    try {
      User user = privilegeService.runAsPortal(new PrivilegedAction<User>() {

        public User run() {
          return userService.getUserByParam("username", username, false);
        }
      });
      if (user.isEmailVerified()) {
        userService.sendResetPasswordMail(user, "reset.password", user.getUsername());
      }
    } catch (NoSuchUserException ex) {
      // no indication to user that this failed.
      return "auth.request_reset_success";
    }
    logger.debug("###Exiting requestReset(username,request) method @POST");
    return "auth.request_reset_success";
  }

  @RequestMapping(value = "/reset_password", method = RequestMethod.GET, params = "a")
  public String reset(@RequestParam(value = "a", required = true) final String auth,
      @RequestParam(value = "t", required = true) final long ts,
      @RequestParam(value = "i", required = true) final String userParam, HttpSession session, ModelMap map) {
    logger.debug("###Entering in reset(auth,ts,userId,session) method @GET");

    Random rnd = new Random();
    int n = 99999 - 1000;
    session.setAttribute("phoneVerificationPin", "" + rnd.nextInt(n));

    User user = privilegeService.runAsPortal(new PrivilegedAction<User>() {

      public User run() {
        User user = userService.get(userParam);
        userService.verifyAuthorization(user, auth, ts);
        return user;
      }
    });

    // Whether telesign is enabled and the user is master user?
    boolean isTelesignVerificationEnable = false;
    if ((TelephoneVerificationService) connectorManagementService
        .getOssServiceInstancebycategory(ConnectorType.PHONE_VERIFICATION) != null) {
      isTelesignVerificationEnable = ((TelephoneVerificationService) connectorManagementService
          .getOssServiceInstancebycategory(ConnectorType.PHONE_VERIFICATION)).isEnabled()
          && user.equals(user.getTenant().getOwner());
    }

    map.addAttribute("isTelesignVerificationEnable", isTelesignVerificationEnable);
    session.setAttribute(RESET_USER_KEY, user.getUsername());
    logger.debug("###Exiting in reset(auth,ts,userId,session) method @GET");
    return "auth.reset";
  }

  @RequestMapping(value = "/reset_password", method = RequestMethod.POST, params = "password")
  public String reset(@RequestParam(value = "password", required = true) final String password, HttpSession session) {
    logger.debug("###Entering in reset(password,session) method @POST");
    final String username = (String) session.getAttribute(RESET_USER_KEY);
    final int maxFailCount = config.getIntValue(Names.com_citrix_cpbm_accountManagement_security_logins_lockThreshold);

    User user = privilegeService.runAsPortal(new PrivilegedAction<User>() {

      public User run() {
        User user = userService.getUserByParam("username", username, false);
        user.setClearPassword(password);
        if (!user.isEnabled() || user.getFailedLoginAttempts() >= maxFailCount) {
          user.setFailedLoginAttempts(0);
          user.setEnabled(true);
        }

        return user;
      }
    });

    // Looks out of place, but useful.
    if (user.getFailedLoginAttempts() >= config
        .getIntValue(Names.com_citrix_cpbm_accountManagement_security_logins_captchaThreshold)) {
      session.setAttribute(CaptchaAuthenticationFilter.CAPTCHA_REQUIRED, true);
    }

    session.removeAttribute(RESET_USER_KEY);
    logger.debug("###Exiting in reset(password,session) method @POST");
    return "redirect:/portal/login";
  }

  @RequestMapping(value = "/verify_additional_email", method = RequestMethod.GET)
  public String verifyAdditionalEmail(@RequestParam(value = "a", required = true) final String auth,
      @RequestParam(value = "i", required = true) final String userParam,
      @RequestParam(value = "pi", required = true) final long userAlertPrefId, final HttpServletRequest request,
      ModelMap map, HttpSession session) {
    logger.debug("###Entering in verifyAlertEmail(map) method @GET");
    privilegeService.runAsPortal(new PrivilegedAction<User>() {

      public User run() {
        User user = userService.get(userParam);
        userService.verifyAuthorization(user, auth, 0);
        UserAlertPreferences userAlertPreferences = userAlertPreferencesService
            .locateUserAlertPreference(userAlertPrefId);

        if (userAlertPreferences.getAlertType() == AlertType.USER_EMAIL) {
          user.setEmail(userAlertPreferences.getEmailAddress());
          userService.save(user);
          userAlertPreferencesService.deleteUserAlertPreference(userAlertPreferences);
        } else {
          userAlertPreferences.setEmailVerified(true);
          userAlertPreferencesService.save(userAlertPreferences);
        }
        return user;
      }
    });

    map.clear(); // No need for map to propagate

    logger.debug("###Exiting verifyAlertEmail(map) method @GET");
    return "redirect:/portal/home";
  }
}
