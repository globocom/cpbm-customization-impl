package citrix.cpbm.portal.fragment.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@RequestMapping("/connector")
@SessionAttributes({
  "serviceInstanceLogoForm"
})
public class ConnectorController extends AbstractConnectorController {

}
