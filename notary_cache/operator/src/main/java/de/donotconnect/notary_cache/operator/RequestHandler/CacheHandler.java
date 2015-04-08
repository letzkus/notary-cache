package de.donotconnect.notary_cache.operator.RequestHandler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class CacheHandler extends AbstractHandler {

	public void handle(String target, Request baseRequest, HttpServletRequest req,
			HttpServletResponse resp) throws IOException, ServletException {
		
        resp.setContentType("text/html; charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        
        PrintWriter out = resp.getWriter();
        
        out.println(target+" --- "+baseRequest.toString());
        
        baseRequest.setHandled(true);
		
	}

}
