package com.api.config;


import com.api.controller.*;
import com.exception.BeanValidationExceptionHandler;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.util.enums.Language;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import java.util.Collections;

/**
 * JAX-RS application configuration class.
 */

@ApplicationPath("/api")

public class ApplicationConfig extends ResourceConfig {

    public ApplicationConfig() {

        //make sure this is performed before the registration of the Jackson staff
        register(BeanValidationExceptionHandler.class, 1);
        register(CorsFilter.class);
        register(JacksonJaxbJsonProvider.class);
        register(UserController.class);
        register(WelcomeController.class);
        register(SecurityFeature.class);
        register(MultiPartFeature.class);
        register(TournamentController.class);
        register(MatchController.class);

        register(new AbstractBinder(){
            @Override
            protected void configure() {
                bindFactory(LanguageContextProvider.class)
                        .to(Language.class)
                        .in(RequestScoped.class);
            }
        });

        OpenAPI openAPI = new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Chess Tournament API")
                        .description(
                                "API's for Chess Tournament Management Application")
                        .version("v1")
                )
                .schemaRequirement("Bearer", new SecurityScheme()
                        .name("Authorization")
                        .description("JWT Authorization header using the Bearer scheme. Example: \\\\\\\"Authorization: Bearer {token}\\\\\\\"")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                )
                .security(Collections.singletonList(new SecurityRequirement().addList("Bearer")));

        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true);

        AcceptHeaderOpenApiResource openApiResource = new AcceptHeaderOpenApiResource();
        openApiResource.setOpenApiConfiguration(oasConfig);
        register(openApiResource);
    }

}