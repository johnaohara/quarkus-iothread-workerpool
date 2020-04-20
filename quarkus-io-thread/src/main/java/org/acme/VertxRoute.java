package org.acme;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.core.http.HttpMethod;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.Set;


@Singleton
public class VertxRoute {

	@Inject
	Validator validator;

	final MessageService messageService;

	public VertxRoute(MessageService messageService) {
		this.messageService = messageService;
	}

	@Route(path = "/hello/:name", methods = HttpMethod.GET)
	void greetings(RoutingExchange ex) {
		Set<ConstraintViolation<RequestObj>> violations = validator.validate(new RequestWrapper(ex.getParam("name").get()));
		if( violations.size() == 0) {
			ex.ok(messageService.sayHello(ex.getParam("name").orElse("world")));
		} else {
			StringBuilder vaidationError = new StringBuilder();
			violations.stream().forEach(violation -> vaidationError.append(violation.getMessage()));
			ex.response().setStatusCode(400).end(vaidationError.toString());
		}
	}

	private class RequestWrapper {
		@NotBlank
		public String name;

		public RequestWrapper(String name) {
			this.name = name;
		}
	}

	@Route(path = "/environment", methods = HttpMethod.GET, produces = "text/plain")
	void env(RoutingExchange ex){
		StringBuilder stringBuilder = new StringBuilder();

		ProcessHandle processHandle = ProcessHandle.current();

		stringBuilder.append("pid: ").append(processHandle.pid()).append("\n");
		stringBuilder.append("\n");

		stringBuilder.append("commandLine: ").append(processHandle.info().commandLine().get()).append("\n");
		stringBuilder.append("\n");

		stringBuilder.append("thread-name: ").append(Thread.currentThread().getName()).append("\n");
		stringBuilder.append("\n\n");
		stringBuilder.append("stack trace: ").append("\n\n");

		Arrays.stream(Thread.currentThread().getStackTrace()).forEach(ste -> stringBuilder.append(ste.toString()).append("\n"));

		ex.ok(stringBuilder.toString());

	}

}

