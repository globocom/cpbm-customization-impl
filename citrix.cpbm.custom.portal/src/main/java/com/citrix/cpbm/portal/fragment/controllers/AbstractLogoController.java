/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package com.citrix.cpbm.portal.fragment.controllers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.vmops.model.Channel;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.Service;
import com.vmops.model.ServiceImage;
import com.vmops.model.ServiceInstance;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ChannelService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;
import com.vmops.service.UserService;
import com.vmops.web.controllers.AbstractAuthenticatedController;

public abstract class AbstractLogoController extends AbstractAuthenticatedController {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private UserService userService;

  @Autowired
  private ChannelService channelService;

  @Autowired
  private ConnectorConfigurationManager connectorConfigurationManagerService;

  @RequestMapping(value = ("/tenant/{tenantParam}/favicon"), method = RequestMethod.GET)
  public void getTenantFavicon(@PathVariable String tenantParam, ModelMap map, HttpServletResponse response) {
    Tenant tenant = tenantService.get(tenantParam);
    logoResponse(tenant.getFaviconPath(), config.getDefaultFavicon(), response);
  }

  @RequestMapping(value = ("/tenant/{tenantParam}"), method = RequestMethod.GET)
  public void getTenantLogo(@PathVariable String tenantParam, ModelMap map, HttpServletResponse response) {
    Tenant tenant = tenantService.get(tenantParam);
    logoResponse(tenant.getImagePath(), config.getDefaultTenantLogo(), response);
  }

  @RequestMapping(value = ("/user/{userParam}"), method = RequestMethod.GET)
  public void getUserLogo(@PathVariable String userParam, ModelMap map, HttpServletResponse response) {
    User user = userService.get(userParam);
    logoResponse(user.getImagePath(), "", response);
  }

  @RequestMapping(value = ("/serviceInstance/{serviceInstanceId}"), method = RequestMethod.GET)
  public void getServiceInstanceLogo(@PathVariable String serviceInstanceId, ModelMap map, HttpServletResponse response) {
    ServiceInstance serviceInstance = connectorConfigurationManagerService.getInstance(serviceInstanceId);
    logoResponse(serviceInstance.getImagePath(), "", response);
  }

  @RequestMapping(value = ("/product/{productId}"), method = RequestMethod.GET)
  public void getProductLogo(@PathVariable String productId, ModelMap map, HttpServletResponse response) {
    Product product = productService.locateProductById(productId);
    logoResponse(product.getImagePath(), "", response);
  }

  @RequestMapping(value = ("/productBundles/{bundleId}"), method = RequestMethod.GET)
  public void getProductBundlesLogo(@PathVariable String bundleId, ModelMap map, HttpServletResponse response) {
    ProductBundle bundle = productBundleService.locateProductBundleById(bundleId);
    logoResponse(bundle.getImagePath(), "", response);
  }

  @RequestMapping(value = ("/connector/{serviceId}/{type}"), method = RequestMethod.GET)
  public void getServiceLogo(@PathVariable String serviceId, @PathVariable String type, ModelMap map,
      HttpServletResponse response) {
    Service service = connectorConfigurationManagerService.getService(serviceId);
    List<ServiceImage> images = new ArrayList<ServiceImage>(service.getImages());
    for (ServiceImage serviceImage : images) {
      if (serviceImage.getImagetype().equals(type)) {
        String cssdkFilesDirectory = FilenameUtils.concat(
            config.getValue(Names.com_citrix_cpbm_portal_settings_services_datapath), service.getServiceName() + "_"
                + service.getVendorVersion());
        logoResponse(FilenameUtils.concat(CssdkConstants.IMAGES_DIRECTORY, serviceImage.getImagepath()), "", response,
            cssdkFilesDirectory);
        return;
      }
    }
  }

  private void logoResponse(String imagePath, String defaultImagePath, HttpServletResponse response) {
    logoResponse(imagePath, defaultImagePath, response, null);
  }

  private void logoResponse(String imagePath, String defaultImagePath, HttpServletResponse response,
      String cssdkFilesDirectory) {
    FileInputStream fileinputstream = null;
    String rootImageDir = config.getValue(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
    if (StringUtils.isNotBlank(cssdkFilesDirectory)) {
      rootImageDir = cssdkFilesDirectory;
    }
    if (rootImageDir != null && !rootImageDir.trim().equals("")) {
      try {
        if (imagePath != null && !imagePath.trim().equals("")) {
          String absoluteImagePath = FilenameUtils.concat(rootImageDir, imagePath);
          fileinputstream = new FileInputStream(absoluteImagePath);
          if (fileinputstream != null) {
            int numberBytes = fileinputstream.available();
            byte bytearray[] = new byte[numberBytes];
            fileinputstream.read(bytearray);
            response.setContentType("image/" + FilenameUtils.getExtension(imagePath));
            // TODO:Set Cache headers for browser to force browser to cache to reduce load
            OutputStream outputStream = response.getOutputStream();
            response.setContentLength(numberBytes);
            outputStream.write(bytearray);
            outputStream.flush();
            outputStream.close();
            fileinputstream.close();
            return;
          }
        }
      } catch (FileNotFoundException e) {
        logger.debug("###File not found in retrieving logo " + imagePath);
      } catch (IOException e) {
        logger.debug("###IO Error in retrieving logo");
      }
    }
    response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    response.setHeader("Location", defaultImagePath);
  }

  @RequestMapping(value = ("/channel/{channelId}"), method = RequestMethod.GET)
  public void getChannelLogo(@PathVariable String channelId, ModelMap map, HttpServletResponse response) {
    Channel channel = channelService.getChannelById(channelId);
    logoResponse(channel.getImagePath(), "", response);
  }

}
