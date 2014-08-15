package no.java.moosehead.web;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;

public class WebServer {

    private final Integer port;
    private String warFile;

    public WebServer(Integer port, String warFile) {
        this.port = port;
        this.warFile = warFile;
    }

    public static void main(String[] args) throws Exception {
        String configFilename = null;
        if (args.length > 0) {
            configFilename = args[0];
            System.setProperty("mooseheadConfFile",configFilename);
        } else {
            System.out.println("Running without config");
        }
        new WebServer(getPort(8088),null).start();
    }

    private void start() throws Exception {
        Server server = new Server(port);

        WebAppContext webAppContext;
        webAppContext = new WebAppContext("src/main/webapp", "/");
        webAppContext.getInitParams().put("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
        webAppContext.setContextPath("/");
        setupLogging();

        if (isDevEnviroment()) {
            // Development ie running in ide
            webAppContext.setResourceBase("src/main/resources/webapp");
        } else {
            // Prod ie running from jar
            webAppContext.setBaseResource(Resource.newClassPathResource("webapp", true, false));
        }

         Handler serverHandler;

        if (Configuration.secureAdmin()) {
            serverHandler = setupSecurity(server, webAppContext);
        } else {
            serverHandler = webAppContext;
        }

        server.setHandler(serverHandler);

        server.start();
        System.out.println(server.getURI() + " at " + LocalDateTime.now());
    }

    private boolean isDevEnviroment() {
        return new File("pom.xml").exists();
    }

    private void setupLogging() {
        //LogManager.getRootLogger().setLevel(Level.INFO);
        //LogManager.getLogger("org.eclipse.jetty").setLevel(Level.INFO);
        //LogManager.getLogger("org.eclipse.jetty.security").setLevel(Level.TRACE);
    }

    private ConstraintSecurityHandler setupSecurity(Server server, WebAppContext theContextToSecure) {
        LoginService loginService = new HashLoginService("MooseRealm",  Configuration.loginConfigLocation());
        server.addBean(loginService);

        Constraint constraint = new Constraint();
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{"adminrole"});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/admin/*");
        mapping.setConstraint(constraint);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setRealmName("MooseRealm");
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);

        security.setHandler(theContextToSecure);

        return security;
    }

    private static int getPort(int defaultPort) {
        Integer serverPort = Configuration.serverPort();
        return serverPort != null ? serverPort : defaultPort;
    }
}