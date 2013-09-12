/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package com.citrix.cpbm.portal.forms;

import com.citrix.cpbm.access.Subscription;
import com.citrix.cpbm.access.proxy.CustomProxy;

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
