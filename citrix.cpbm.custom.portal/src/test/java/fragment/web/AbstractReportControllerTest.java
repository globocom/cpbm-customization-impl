/** Copyright (C) 2011 Cloud.com, Inc. All rights reserved.
 * 
 * @author vinayv 
 * 
 **/
package fragment.web;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;

import web.WebTestsBase;
import citrix.cpbm.portal.fragment.controllers.ReportController;

public class AbstractReportControllerTest extends WebTestsBase {

	@Autowired
	ReportController reportController;

	private ModelMap map;

	private MockHttpServletResponse response;

	private MockHttpServletRequest request;

	@Before
	public void init() throws Exception {

		map = new ModelMap();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	public void testCustomReportForDateWithData() throws Exception {

		String result = reportController.generatecustomreports("1",
				"05/01/2012", null, null, request, response, map);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.contains("testReport"));
	}

	@Test
	public void testCustomReportForDateWithoutData() throws Exception {

		String result = reportController.generatecustomreports("1",
				"05/03/2012", null, null, request, response, map);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.contains("none"));
	}

	@Test
	public void testCustomReportWithInvalidDate() {

		try {
			reportController.generatecustomreports("1", "05-03-2012", null,
					null, request, response, map);
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("Unparseable date"));
		}
	}

	@Test
	public void testCustomReportWithInvalidId() {

		try {
			reportController.generatecustomreports("2", "05-03-2012", null,
					null, request, response, map);
		} catch (Exception e) {
			Assert.assertNull(e.getMessage());
		}
	}

	@Test
	public void testEmailCustomReport() throws Exception {

		String reportFilename = reportController.generatecustomreports("1",
				"05/01/2012", null, null, request, response, map);
		Assert.assertNotNull(reportFilename);
		Assert.assertTrue(reportFilename.contains("testReport"));
		String result = reportController.emailCustomReport(reportFilename,
				"vinay,vegesna@test.com", map, response);
		Assert.assertNotNull(result);
		Assert.assertEquals("success", result);
	}

	@Test
	public void testDownloadCustomReport() throws Exception {

		String reportFilename = reportController.generatecustomreports("1",
				"05/01/2012", null, null, request, response, map);
		Assert.assertNotNull(reportFilename);
		Assert.assertTrue(reportFilename.contains("testReport"));
		reportController.downloadCustomReport(reportFilename, map, response);
	}

}
