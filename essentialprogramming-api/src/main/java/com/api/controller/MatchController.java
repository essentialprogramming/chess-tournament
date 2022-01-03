package com.api.controller;

import com.api.config.Anonymous;
import com.api.model.MatchSearchCriteria;
import com.api.output.MatchJSON;
import com.api.output.SearchMatchesJSON;
import com.api.service.MatchService;
import com.exception.ExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.util.async.Computation;
import com.util.async.ExecutorsProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Tag(description = "Match API", name = "Match")
@Path("/match/")
public class MatchController {
    private final MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @POST
    @Path("/{matchKey}/report")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PLAYER")
    @Operation(summary = "One player can report the match",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Player's result report will be added " +
                            "and returns a JSON of the match where it was added.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MatchJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void reportMatchByPlayer(@Valid @NotNull(message = "Match key must be provided.")
                                    @PathParam("matchKey") String matchKey,
                                    @Valid @NotNull(message = "Player key must be provided.")
                                    @QueryParam("playerKey") String playerKey,
                                    @Valid @NotNull(message = "Result must be provided.")
                                    @QueryParam("result") String result,
                                    @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> reportMatchByPlayer(playerKey, matchKey, result), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable reportMatchByPlayer(String playerKey, String matchKey, String result) throws GeneralSecurityException, JsonProcessingException {
        return matchService.reportMatchByPlayer(playerKey, matchKey, result);
    }

    @POST
    @Path("/referee-report-match")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("REFEREE")
    @Operation(summary = "Referee reports the match",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Referee's result will be added" +
                            " and returns the JSON of the match where it was added.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MatchJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void reportMatchByReferee(@Valid @NotNull(message = "Match key must be provided.")
                                     @QueryParam("matchKey") String matchKey,
                                     @Valid @NotNull(message = "Result must be provided.")
                                     @QueryParam("result") String result,
                                     @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> reportMatchByReferee(matchKey, result), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable reportMatchByReferee(String matchKey, String result) throws JsonProcessingException {

        return matchService.reportMatchByReferee(matchKey, result);
    }

    @POST
    @Path("assign/referee")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Assign referee to match",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Adds referee to the match and returns a custom JSON response.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Referee added for this match!\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Referee is already added to this match!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void assignRefereeToMatch(@NotNull(message = "Referee key must be provided!") @QueryParam("refereeKey") String refereeKey,
                                          @NotNull(message = "Match key must be provided!") @QueryParam("matchKey") String matchKey,
                                          @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> assignRefereeToMatch(refereeKey, matchKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));

    }

    private Serializable assignRefereeToMatch(String refereeKey, String matchKey) {
        return matchService.assignRefereeToMatch(refereeKey, matchKey);
    }

    @POST
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/json")
    @Operation(summary = "Search matches",
            responses  = {
                    @ApiResponse(responseCode = "200", description = "Get a list of matches based on given search criteria",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SearchMatchesJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void searchMatches(@Valid MatchSearchCriteria matchSearchCriteria, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> searchMatches(matchSearchCriteria), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable searchMatches(MatchSearchCriteria matchSearchCriteria) {
        return (Serializable) matchService.searchMatches(matchSearchCriteria);
    }
}