package com.parca.yedekparca;

import com.parca.yedekparca.config.SecurityConfig;
import com.parca.yedekparca.controller.DemoImageController;
import com.parca.yedekparca.security.JwtAuthenticationFilter;
import com.parca.yedekparca.service.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(DemoImageController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("dev-demo")
class DemoImageSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test void anonymousGetServesSvgOnlyInDemoProfile() throws Exception {
        mvc.perform(get("/api/dev-demo/images/engine.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("image/svg+xml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DEMO")));
    }

    @Test void unknownImageNameIsNotServed() throws Exception {
        mvc.perform(get("/api/dev-demo/images/other.svg")).andExpect(status().is4xxClientError());
    }

    @Test void everyAllowlistedPhotoIsAvailableAnonymouslyWithItsMediaTypeAndCachePolicy() throws Exception {
        for (String path : DemoImageController.PUBLIC_GET_PATHS.keySet()) {
            String expectedType = path.endsWith(".svg") ? "image/svg+xml" : "image/jpeg";
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType(expectedType)))
                    .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=86400")))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(result.getResponse().getContentLength() > 0));
        }
    }

    @Test void traversalAndUnknownNestedPathsAreNotServed() throws Exception {
        mvc.perform(get("/api/dev-demo/images/parts/application.properties")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/dev-demo/images/parts/%2e%2e/application.properties")).andExpect(status().is4xxClientError());
    }
}
