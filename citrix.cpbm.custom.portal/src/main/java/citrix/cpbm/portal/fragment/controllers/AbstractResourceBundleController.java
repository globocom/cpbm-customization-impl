package citrix.cpbm.portal.fragment.controllers;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.synyx.messagesource.InitializableMessageSource;


import com.vmops.service.ReloadableApplicationResource;
import com.vmops.web.controllers.AbstractAuthenticatedController;

@Controller
public class AbstractResourceBundleController extends AbstractAuthenticatedController {

  @Resource(name = "messageSource")
  InitializableMessageSource messageSource;

  @Autowired
  ReloadableApplicationResource reloadableApplicationResource;

  @PostConstruct
  public void setResourceBean() {
    reloadableApplicationResource.setMessageSource(messageSource);
  }

}
