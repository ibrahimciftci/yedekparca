package com.parca.yedekparca;

import com.parca.yedekparca.security.JwtAuthenticationFilter;
import com.parca.yedekparca.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @AfterEach void clearContext(){SecurityContextHolder.clearContext();}

    @Test void requestWithoutTokenContinuesUnauthenticated() throws Exception {
        JwtAuthenticationFilter filter=new JwtAuthenticationFilter(mock(JwtService.class),mock(UserDetailsService.class));
        MockHttpServletRequest request=new MockHttpServletRequest("GET","/api/catalog/products");
        MockHttpServletResponse response=new MockHttpServletResponse();
        FilterChain chain=mock(FilterChain.class);
        filter.doFilter(request,response,chain);
        verify(chain).doFilter(request,response);
        assertEquals(200,response.getStatus());
    }

    @Test void malformedBearerTokenIsRejectedWithoutCallingChain() throws Exception {
        JwtService jwt=mock(JwtService.class);
        when(jwt.extractEmail("broken")).thenThrow(new IllegalArgumentException("bad token"));
        JwtAuthenticationFilter filter=new JwtAuthenticationFilter(jwt,mock(UserDetailsService.class));
        MockHttpServletRequest request=new MockHttpServletRequest("GET","/api/catalog/products");
        request.addHeader("Authorization","Bearer broken");
        MockHttpServletResponse response=new MockHttpServletResponse();
        FilterChain chain=mock(FilterChain.class);
        filter.doFilter(request,response,chain);
        assertEquals(401,response.getStatus());
        verifyNoInteractions(chain);
    }
}
