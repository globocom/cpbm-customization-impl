package citrix.cpbm.portal.forms;

import citrix.cpbm.access.Subscription;
import citrix.cpbm.access.proxy.CustomProxy;

public class SubscriptionForm {

  private Subscription subscription = (Subscription) CustomProxy.newInstance(new com.vmops.model.Subscription());

  public SubscriptionForm() {
    super();
  }

  public SubscriptionForm(Subscription subscription) {
    super();
    this.subscription = subscription;
  }

  public Subscription getSubscription() {
    return subscription;
  }

  public void setSubscription(Subscription subscription) {
    this.subscription = subscription;
  }

}
