package citrix.cpbm.portal.fragment.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@RequestMapping("/channels")
@SessionAttributes({
  "channelLogoForm"
})
public class ChannelController extends AbstractChannelController {

}
