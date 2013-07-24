/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Controller that handles all the product pages
 * 
 * @author Venkat
 */
@Controller
@RequestMapping("/products")
@SessionAttributes({
    "productForm", "catalogForm", "productLogoForm"
})
public class ProductsController extends AbstractProductsController {

}
