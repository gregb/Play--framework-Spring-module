package play.modules.spring.test;

import javax.inject.Inject;

import play.mvc.Controller;

class TestController extends Controller {
	@Inject
	public static TestService testService;
}