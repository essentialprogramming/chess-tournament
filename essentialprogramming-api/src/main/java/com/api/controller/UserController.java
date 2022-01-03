package com.api.controller;

import com.api.config.Anonymous;
import com.api.output.PlayerJSON;
import com.api.output.UserJSON;
import com.api.service.UserPlatformService;
import com.api.service.UserService;
import com.api.model.*;
import com.util.async.ExecutorsProvider;
import com.exception.ExceptionHandler;
import com.internationalization.Messages;
import com.token.validation.auth.AuthUtils;
import com.util.async.Computation;
import com.util.enums.HTTPCustomStatus;
import com.util.enums.Language;
import com.util.enums.PlatformType;
import com.util.exceptions.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Tag(description = "User API", name = "User")
@Path("/security/")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserPlatformService userPlatformService;

    @Context
    private Language language;

    @POST
    @Path("admin/create")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("SUPER_ADMIN")
    @Operation(summary = "Create admin",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Return admin if successfully added",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PlayerJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Email already taken."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void createAdmin(@Valid UserInput input, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> createAdmin(input, language), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.status(201).entity(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));

    }

    private Serializable createAdmin(@Valid UserInput input, Language language) throws GeneralSecurityException, ApiException {

        boolean isEmailValid = userService.checkAvailabilityByEmail(input.getEmail());
        if (isEmailValid) {
            return userService.saveUser(input, PlatformType.ADMIN);
        }

        boolean isPlatformValid = userPlatformService.isPlatformAvailable(PlatformType.ADMIN, input.getEmail());
        if (isPlatformValid) {
            return userPlatformService.addPlatform(PlatformType.ADMIN, input.getEmail());
        }
        throw new ApiException(Messages.get("EMAIL.ALREADY.TAKEN", language), HTTPCustomStatus.INVALID_REQUEST);
    }

    @POST
    @Path("user/load")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Load user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Return user if it was successfully found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "User not found."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void load(@HeaderParam("Authorization") String authorization, @Suspended AsyncResponse asyncResponse) {

        final String bearer = AuthUtils.extractBearerToken(authorization);
        final String email = AuthUtils.getClaim(bearer, "email");

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> loadUser(email), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));

    }

    private Serializable loadUser(String email) throws ApiException {
        return userService.loadUser(email, language);
    }

    @POST
    @Path("player/create")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Register player",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Return player if successfully added",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PlayerJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Email already taken."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void createPlayer(@Valid UserInput userInput, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> createPlayer(userInput), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.status(201).entity(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));

    }

    private Serializable createPlayer(UserInput userInput) throws GeneralSecurityException, ApiException {

        boolean isEmailValid = userService.checkAvailabilityByEmail(userInput.getEmail());
        if (isEmailValid) {
            return userService.saveUser(userInput, PlatformType.PLAYER);
        }

        boolean isPlatformValid = userPlatformService.isPlatformAvailable(PlatformType.PLAYER, userInput.getEmail());
        if (isPlatformValid) {
            return userPlatformService.addPlatform(PlatformType.PLAYER, userInput.getEmail());
        } else
            throw new ApiException(Messages.get("EMAIL.ALREADY.TAKEN", language), HTTPCustomStatus.INVALID_REQUEST);

    }

    @POST
    @Path("referee/create")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN"})
    @Operation(summary = "Register referee",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Return referee if successfully added",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserJSON.class))),
                    @ApiResponse(responseCode = "422", description = "Email already taken.")
            })
    public void createReferee(@Valid UserInput userInput, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> createReferee(userInput), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));

    }

    private Serializable createReferee(UserInput userInput) throws GeneralSecurityException, ApiException {

        boolean isEmailValid = userService.checkAvailabilityByEmail(userInput.getEmail());
        if (isEmailValid) {
            return userService.saveUser(userInput, PlatformType.REFEREE);
        }

        boolean isPlatformValid = userPlatformService.isPlatformAvailable(PlatformType.REFEREE, userInput.getEmail());
        if (isPlatformValid) {
            return userPlatformService.addPlatform(PlatformType.REFEREE, userInput.getEmail());
        } else
            throw new ApiException(Messages.get("EMAIL.ALREADY.TAKEN", language), HTTPCustomStatus.INVALID_REQUEST);
    }


    @PUT
    @Path("add/referee/platform")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN"})
    @Operation(summary = "Adds referee platform for an existing user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Adds referee platform and returns a custom JSON response if it was successful",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Referee platform was successfully added!\"}"))),
                    @ApiResponse(responseCode = "422", description = "This user already has this platform attributed!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void addRefereePlatform(@Valid @NotNull(message = "User key must be provided.")
                                   @QueryParam("userKey") String userKey,
                                   @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> addRefereePlatform(userKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable addRefereePlatform(String userKey) {

        boolean isPlatformValid = userPlatformService.isPlatformAvailableByKey(PlatformType.REFEREE, userKey);
        if (isPlatformValid) {
            return userPlatformService.addPlatformByKey(PlatformType.REFEREE, userKey);
        } else
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This user already has this platform attributed!");
    }


    @PUT
    @Path("add/admin/platform")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN"})
    @Operation(summary = "Adds admin platform for an existing user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Adds admin platform and returns a custom JSON response if it was successful",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Admin platform was successfully added!\"}"))),
                    @ApiResponse(responseCode = "422", description = "This user already has this platform attributed!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void addAdminPlatform(@Valid @NotNull(message = "User key must be provided.")
                                 @QueryParam("userKey") String userKey,
                                 @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> addAdminPlatform(userKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable addAdminPlatform(String userKey) {

        boolean isPlatformValid = userPlatformService.isPlatformAvailableByKey(PlatformType.ADMIN, userKey);
        if (isPlatformValid) {
            return userPlatformService.addPlatformByKey(PlatformType.ADMIN, userKey);
        } else
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This user already has this platform attributed!");
    }

    @PUT
    @Path("add/player/platform")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SUPER_ADMIN", "ADMIN", "REFEREE"})
    @Operation(summary = "Adds player platform for an existing user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Adds player platform and returns a custom JSON response if it was successful",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Player platform was successfully added!\"}"))),
                    @ApiResponse(responseCode = "422", description = "This user already has this platform attributed!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void addPlayerPlatform(@Valid @NotNull(message = "User key must be provided.")
                                  @QueryParam("userKey") String userKey,
                                  @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> addPlayerPlatform(userKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable addPlayerPlatform(String userKey) {

        boolean isPlatformValid = userPlatformService.isPlatformAvailableByKey(PlatformType.PLAYER, userKey);
        if (isPlatformValid) {
            return userPlatformService.addPlatformByKey(PlatformType.PLAYER, userKey);
        } else
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This user already has this platform attributed!");
    }

}


