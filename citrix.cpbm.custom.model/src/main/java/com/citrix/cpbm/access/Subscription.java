/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package com.citrix.cpbm.access;

import com.citrix.cpbm.access.SubscriptionEntity;

public interface Subscription extends SubscriptionEntity {

	public String getEndpoint();
	
	public void setEndpoint(String endpoint);
	
	public String getCredentialUser();
	
	public void setCredentialUser(String credentialUser);
	
	public String getCredentialPassword();
	
	public void setCredentialPassword(String credentialPassword);

}
