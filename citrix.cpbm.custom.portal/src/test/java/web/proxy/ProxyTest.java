/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
package web.proxy;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;


import com.citrix.cpbm.access.proxy.CustomProxy;
import com.vmops.model.Tenant;
import com.vmops.model.User;

public class ProxyTest {

  private Validator validator;

  @Before
  public void setUpTest() throws FileNotFoundException {
    Configuration<?> config = Validation.byDefaultProvider()
        .providerResolver(new com.citrix.cpbm.validator.constraint.impl.OSGIValidationProviderResolver()).configure();
    InputStream in = getClass().getResourceAsStream("/constraints.xml");
    config.addMapping(in);
    ValidatorFactory factory = config.buildValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void testProxy() {

    testPattern();
    com.citrix.cpbm.access.Tenant t = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(new Tenant());

    User user = new User();
    user.setUsername("proxyuser");
    user.setPhone("919886445511");
    //t.setFoo("foo");
    t.setOwner(user);

    BeanPropertyBindingResult result = new BeanPropertyBindingResult(t, "tenant");
    Set<ConstraintViolation<com.citrix.cpbm.access.Tenant>> constraintViolations = validator.validate(t);

    CustomProxy invocationHandler = (CustomProxy) Proxy.getInvocationHandler(t);
    Class clazz = invocationHandler.getTenantInterfaces()[0];
    System.err.println("..t.." + clazz.getName());

    System.err.println("..Proxy.isProxyClass(clazz)..." + Proxy.isProxyClass(invocationHandler.getTarget().getClass()));

    System.err.println("..Users.." + t.getOwner().getUsername());

    com.citrix.cpbm.access.User u = (com.citrix.cpbm.access.User) CustomProxy.newInstance(new User());

    invocationHandler = (CustomProxy) Proxy.getInvocationHandler(u);

    System.err.println("..u.." + invocationHandler.getTarget().getClass());

    for (ConstraintViolation<com.citrix.cpbm.access.Tenant> constraintViolation : constraintViolations) {
      System.err.println(constraintViolation.getPropertyPath().toString() + "...constraint.."
          + constraintViolation.getMessage());
    }

  }

  private void testPattern() {
    String txt = "91-9886445511";

    String re1 = "(\\d+)"; // Integer Number 1
    String re2 = "(-)"; // Any Single Character 1
    String re3 = "(\\d+)"; // Integer Number 2

    Pattern p = Pattern.compile(re1 + re2 + re3, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    System.err.println(p.pattern() + "----...pattren..");
    Matcher m = p.matcher(txt);
    if (m.find()) {
      String int1 = m.group(1);
      String c1 = m.group(2);
      String int2 = m.group(3);
      System.out.print("(" + int1.toString() + ")" + "(" + c1.toString() + ")" + "(" + int2.toString() + ")" + "\n");
    }

  }

}
