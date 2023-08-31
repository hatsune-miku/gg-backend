package com.example.mapbackend.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.IOException;

public class TokenRemovalFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if ("/auth/token".equals(httpRequest.getServletPath())) {
            chain.doFilter(new RemoveTokenHttpServletRequestWrapper(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class RemoveTokenHttpServletRequestWrapper extends HttpServletRequestWrapper {

        public RemoveTokenHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }
    }
}
