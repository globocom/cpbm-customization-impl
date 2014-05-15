/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package fragment.web;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.tiles.definition.NoSuchDefinitionException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.ModelAndView;

import web.WebTestsBase;

import com.citrix.cpbm.portal.fragment.controllers.RegistrationController;
import com.vmops.model.Tenant;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.service.exceptions.CreditCardFraudCheckException;
import com.vmops.service.exceptions.IPtoCountryException;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.service.exceptions.NoSuchTenantException;
import com.vmops.service.exceptions.NoSuchUserException;
import com.vmops.service.exceptions.UserAuthorizationInvalidException;
import com.vmops.service.exceptions.api.ApiException;

public class AbstractBaseControllerTest extends WebTestsBase {

  @Autowired
  RegistrationController controller;

  @BeforeClass
  public static void initMail() {
    setupMail();
  }

  @Test
  public void testHandlePageNotFound() {
    String viewName = "errors/notfound";

    ModelAndView mav = controller.handlePageNotFound(new IllegalArgumentException(), new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, viewName);

    mav = controller.handlePageNotFound(new NoSuchUserException(), new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, viewName);

    mav = controller.handlePageNotFound(new NoSuchTenantException(), new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, viewName);

    mav = controller.handlePageNotFound(new NoSuchDefinitionException(), new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, viewName);

    mav = controller.handlePageNotFound(new UserAuthorizationInvalidException(), new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, viewName);
  }

  @Test
  public void testHandleApiException() {
    ModelAndView mav = controller.handleApiException(new ApiException(), new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    // ModelAndViewAssert.assertViewName(mav, expectedName);
  }

  @Test
  public void testHandleMissingServletRequestParameterException() {
    ModelAndView mav = controller.handleMissingServletRequestParameterException(
        new MissingServletRequestParameterException("tenantParam", "String"), new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    // ModelAndViewAssert.assertViewName(mav, expectedName);
  }

  @Test
  public void testHandleAjaxFormValidationException() {
    Tenant tenant = new Tenant();
    Errors errors = new BindException(tenant, "invalid");
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = controller.handleAjaxFormValidationException(new AjaxFormValidationException(errors),
        new MockHttpServletRequest(), response);
    Assert.assertNotNull(mav.getModelMap());
    Assert.assertEquals(420, response.getStatus());
    // ModelAndViewAssert.assertViewName(mav, expectedName);
  }

  @Test
  public void testHandleAccessDeniedException() {
    AccessDeniedException ex = new AccessDeniedException("test");
    ModelAndView mav = controller.handleAccessDeniedException(ex, new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors/notauthorized");
  }

  @Test
  public void testHandleInvalidAjaxRequestException() {
    InvalidAjaxRequestException ex = new InvalidAjaxRequestException("test");
    ModelAndView mav = controller.handleInvalidAjaxRequestException(ex, new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors.messagepage");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "errorMessage");
    ModelAndViewAssert.assertModelAttributeValue(mav, "errorMessage", ex.getMessage());
  }

  @Test
  public void testHandleGenericException() {
    Exception ex = new Exception("generic");
    MockHttpServletRequest request = new MockHttpServletRequest();
    ModelAndView mav = controller.handleGenericException(ex, request);
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors/error");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "errorMessage");
    ModelAndViewAssert.assertModelAttributeValue(mav, "errorMessage", ex.getMessage());

    ex = new NullPointerException("NPE generic");
    mav = controller.handleGenericException(ex, request);
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors/error");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "errorMessage");
    ModelAndViewAssert.assertModelAttributeValue(mav, "errorMessage", ex.getMessage());

    request.addHeader("X-Requested-With", "XMLHttpRequest");
    mav = controller.handleGenericException(ex, request);
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors.messagepage");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "errorMessage");
    ModelAndViewAssert.assertModelAttributeValue(mav, "errorMessage", ex.getMessage());

    ex = new NullPointerException("NPE generic");
    mav = controller.handleGenericException(ex, request);
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors.messagepage");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "errorMessage");
    ModelAndViewAssert.assertModelAttributeValue(mav, "errorMessage", ex.getMessage());

  }

  @Test
  public void testHandleServiceException() {
    IPtoCountryException ex = new IPtoCountryException("IPtoCountryException");
    ModelAndView mav = controller.handleServiceException(ex, new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors/serviceerror");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "serviceerror");
    ModelAndViewAssert.assertModelAttributeValue(mav, "serviceerror", ex.getMessage());

    CreditCardFraudCheckException cex = new CreditCardFraudCheckException("CreditCardFraudCheckException");
    mav = controller.handleServiceException(cex, new MockHttpServletRequest());
    Assert.assertNotNull(mav.getModelMap());
    ModelAndViewAssert.assertViewName(mav, "errors/serviceerror");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "serviceerror");
    ModelAndViewAssert.assertModelAttributeValue(mav, "serviceerror", cex.getMessage());

  }

  @Test
  public void testGetDefaultLocale() {

    Locale locale = controller.getDefaultLocale();
    Assert.assertNotNull(locale);
  }

  @Test
  public void testListSupportedLocales() {
    List<Locale> lstLocale = controller.listSupportedLocales();
    Assert.assertNotNull(lstLocale);
  }

  @Test
  public void testGetLocaleDisplayName() {
    List<Locale> lstLocale = controller.listSupportedLocales();
    Map<Locale, String> displayMap = controller.getLocaleDisplayName(lstLocale);
    Assert.assertNotNull(displayMap);

    String displayName = controller.getLocaleDisplayName(lstLocale.get(0));
    Assert.assertNotNull(displayName);
  }

}
