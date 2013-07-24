/* Copyright (C) 2013 Citrix Systems, Inc. All rights reserved */
package com.citrix.cpbm.admin;

import org.apache.log4j.Logger;

import com.citrix.cpbm.subscriber.TopicSubscriber;
import com.vmops.event.CloudServiceEvent;
import com.vmops.event.MissingProductEvent;
import com.vmops.event.SubscriptionActivation;
import com.vmops.event.SubscriptionChangeEvent;
import com.vmops.event.SubscriptionCreation;
import com.vmops.event.SubscriptionDeletion;
import com.vmops.event.SubscriptionTermination;
import com.vmops.event.TenantActivation;
import com.vmops.event.TenantLock;
import com.vmops.event.TenantReactivation;
import com.vmops.event.TenantSuspension;
import com.vmops.event.TenantTermination;
import com.vmops.event.UserCreation;
import com.vmops.event.UserDeactivateEmail;
import com.vmops.event.UserDeletion;
import com.vmops.event.UtilitySubscriptionEvent;

/**
 * A default implementation of TopicSubscriber which receives all CPBM events
 * 
 * @author Manish
 */
public class CustomTopicSubscriber implements TopicSubscriber {

  private static Logger logger = Logger.getLogger(CustomTopicSubscriber.class);

  /**
   * Receive subscription creation events
   * 
   * @param event
   */
  public void receive(SubscriptionCreation event) {
    logger.info("###Received subscription creation event with id:" + event.getSubscriptionId());
  }

  /**
   * Receive subscription deletion events
   * 
   * @param event
   */
  public void receive(SubscriptionDeletion event) {
    logger.info("###Received subscription deletion event with id:" + event.getSubscription().getId());
  }

  /**
   * Receive subscription termination events
   * 
   * @param event
   */
  public void receive(SubscriptionTermination event) {
    logger.info("###Received subscription termination event with id:" + event.getSubscriptionId());
  }

  /**
   * This receives the cs related events
   * 
   * @param event -- cs event
   */
  public void receive(CloudServiceEvent event) {
    logger.debug("Cloudstack event received: reference type:" + event.getCloudServiceReferenceType() + " reference id:"
        + event.getCloudServiceReferenceId());
  }

  public void receive(TenantActivation event) {
    logger.debug("Received a tenant activation event with account id:" + event.getAccountId());
  }

  public void receive(TenantReactivation event) {
    logger.debug("Received a tenant Reactivation event with account id:" + event.getAccountId());
  }

  public void receive(TenantLock event) {
    logger.debug("Received a tenant lock event with account id:" + event.getAccountId());
  }

  public void receive(TenantSuspension event) {
    logger.debug("Received a tenant suspension event with account id:" + event.getAccountId());
  }

  public void receive(TenantTermination event) {
    logger.debug("Received a tenant termination event with account id:" + event.getAccountId());
  }

  public void receive(UserCreation event) {
    logger.debug("Received a user creation event with username:" + event.getUsername());
  }

  public void receive(UserDeletion event) {
    logger.debug("Received a user deletion event with username:" + event.getUsername());
  }

  @Override
  public void receive(SubscriptionChangeEvent event) {
    logger.info("###Received SubscriptionChangeEvent  old subscription id:" + event.getOldSubscriptionId()
        + " new Utility Subscription Id:" + event.getNewSubscriptionId());
  }

  @Override
  public void receive(UtilitySubscriptionEvent event) {
    logger.info("###Received UtilitySubscriptionEvent  new utility subscription id:" + event.getSubscriptionId());
  }

  @Override
  public void receive(MissingProductEvent event) {
    logger.info("###Received MissingProductEvent ");

  }

  @Override
  public void receive(UserDeactivateEmail event) {
    logger.info("###Received UserDeactivateEvent ");

  }

  @Override
  public void receive(SubscriptionActivation event) {
    logger.info("###Received subscription activation event with id:" + event.getSubscriptionId());
  }

}
