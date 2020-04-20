package org.acme;

import javax.validation.constraints.NotBlank;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;

@Path("/")
public class MessageResource {

    private MessageService messageService;

    public MessageResource(MessageService messageService) {
        this.messageService = messageService;
    }

    @GET
    @Path("/hello/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@NotBlank @PathParam("name") String name) {
        return messageService.sayHello(name);
    }

    @GET
    @Path("/environment")
    @Produces(MediaType.TEXT_PLAIN)
    public String env() {
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

        return stringBuilder.toString();

    }
}
