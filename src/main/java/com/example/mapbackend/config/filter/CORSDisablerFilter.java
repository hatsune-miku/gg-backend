package com.example.mapbackend.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.Logger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CORSDisablerFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Force filter JWT error to prevent it pouring into the log
        // ExceptionHandler is not working for this
        try {
            filterChain.doFilter(request, response);
        }
        catch (AuthenticationServiceException e) {
            Logger.getLogger("CORSDisablerFilter").warning("Invalid JWT token from " + ((HttpServletRequest) request).getHeader("X-Real-IP"));
        }

        if (response instanceof HttpServletResponse servletResponse) {
            servletResponse.setHeader("Access-Control-Allow-Origin", ((HttpServletRequest) request).getHeader("Origin"));
            servletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            servletResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            servletResponse.setHeader("Access-Control-Max-Age", "3600");

            HttpServletRequest servletRequest = (HttpServletRequest) request;
            if (servletRequest.getMethod().equalsIgnoreCase("OPTIONS")) {
                servletResponse.setStatus(HttpServletResponse.SC_OK);
            }
        }
    }
}
