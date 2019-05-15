package com.jgb;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@Configuration
@EnableSwagger2WebMvc
@Controller
@Import({SpringDataRestConfiguration.class})
public class SwaggerConfiguration {

    @RequestMapping("/")
    public RedirectView redirectToSwagger() {
        return new RedirectView("swagger-ui.html");
    }
}
