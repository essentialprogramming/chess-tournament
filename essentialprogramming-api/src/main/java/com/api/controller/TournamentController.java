package com.api.controller;

import com.api.config.Anonymous;
import com.api.model.*;
import com.api.output.*;
import com.api.service.TournamentService;
import com.util.async.ExecutorsProvider;
import com.exception.ExceptionHandler;
import com.util.async.Computation;
import com.util.exceptions.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

@Tag(description = "Tournament API", name = "Tournament")
@Path("/tournament")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TournamentController {

    private final TournamentService tournamentService;

    @POST
    @Path("/create")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create tournament",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Adds a tournament to the DB and returns a JSON of it.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TournamentJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    public void addTournament(@Valid TournamentInput tournamentInput, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> addTournament(tournamentInput), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.status(201).entity(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable addTournament(TournamentInput tournamentInput) throws ApiException, GeneralSecurityException { return tournamentService.addTournament(tournamentInput); }

    @POST
    @Path("invite")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Send an invitation email to a player",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sends an email invitation to a player" +
                            " and returns a custom JSON response if it was successful",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Player invited to the tournament!\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Registrations for this tournament are closed!"),
                    @ApiResponse(responseCode = "422", description = "Player already has an active invitation to this tournament!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void invitePlayerToTournament(@Valid @NotNull(message = "User key must be provided.")
                                         @QueryParam("user_key") String userKey,
                                         @Valid @NotNull(message = "Tournament key must be provided.")
                                         @QueryParam("tournament_key") String tournamentKey,
                                         @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> invitePlayer(tournamentKey, userKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable invitePlayer(String tournamentKey, String userKey) { return tournamentService.invitePlayer(tournamentKey, userKey); }

    @GET
    @Path("confirmation/{user_key}/{tournament_key}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PLAYER")
    @Operation(summary = "Confirm user participation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "If the tournament registration is open " +
                            "and the max participants limit has not been reached, the given player will be registered to the given tournament. " +
                            "Returns a custom JSON response.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"userKey\": \"k2cU2O8bpUXnpfpJ4EUFk\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Tournament participation is disabled!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void registerPlayerToTournament(@Valid @NotNull(message = "Tournament key must be provided.")
                                           @PathParam("user_key") String userKey,
                                           @Valid @NotNull(message = "Tournament key must be provided.")
                                           @PathParam("tournament_key") String tournamentKey,
                                           @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> registerPlayer(userKey, tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable registerPlayer(String userKey, String tournamentKey) { return tournamentService.registerPlayer(userKey, tournamentKey); }

    @POST
    @Path("start")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(description = "The tournament needs to have registered players before it's started, " +
            "it will throw an error otherwise.",
            summary = "Generate tournament rounds and matches",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Starts tournament, generates rounds and matches " +
                            "and returns a list of JSONs containing the rounds.",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation =  RoundJSON.class)))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "This tournament currently has no participants!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void startTournament(@Valid @NotNull(message = "Tournament key must be provided.")
                                @QueryParam("tournament_key") String tournamentKey,
                                @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> startTournament(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable startTournament(String tournamentKey) {
        tournamentService.startTournament(tournamentKey);

        return (Serializable) tournamentService.getGeneratedRounds(tournamentKey);
    }

    @POST
    @Path("round/set")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Set current round of the tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sets the given round's state to ACTIVE," +
                            " makes it the current round of the given tournament," +
                            " starts the round and sends a notification, then a JSON of the round is returned.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RoundJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void setRound(@Valid @NotNull(message = "Tournament key must be provided.")
                         @QueryParam("tournament_key") String tournamentKey,
                         @Valid @NotNull(message = "Round key must be provided.")
                         @QueryParam("round_key") String roundKey,
                         @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> setRound(tournamentKey, roundKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable setRound(String tournamentKey, String setRound) { return tournamentService.setRound(tournamentKey, setRound); }

    @POST
    @Path("round/next")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Switch to the next round of the tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "If the current round has not ended," +
                            " and the tournament is still ongoing then the it will switch to the next round " +
                            "and return a JSON of the round that it switched to.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RoundJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Round is still ongoing!"),
                    @ApiResponse(responseCode = "422", description = "Tournament reached it's final round!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void switchToNextRound(@Valid @NotNull(message = "Tournament key must be provided.")
                                  @QueryParam("tournament_key") String tournamentKey,
                                  @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> nextRound(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable nextRound(String tournamentKey) { return tournamentService.switchToNextRound(tournamentKey); }

    @GET
    @Consumes("application/json")
    @Operation(summary = "Get tournament",
            responses  = {
                    @ApiResponse(responseCode = "200", description = "Gets the tournament with the given key from the DB, " +
                            "send it's status through the WebSocket and returns the JSON of the tournament.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ChessTournamentJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Produces(MediaType.APPLICATION_JSON)
    @Anonymous
    public void getTournament(@Valid @NotNull(message = "Tournament key must be provided.")
                              @QueryParam("tournamentKey") String tournamentKey,
                              @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getTournament(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable getTournament(String tournamentKey) throws GeneralSecurityException { return tournamentService.getTournament(tournamentKey); }

    @GET
    @Path("leaderboard/overall")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get overall leaderboard",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets the players from all tournaments ordered by their score," +
                            " and returns a list of JSONs containing the players.",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation =  PlayerJSON.class)))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void getOverallLeaderboard(@Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getOverallLeaderboard(), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable getOverallLeaderboard() {
        return (Serializable) tournamentService.getOverallLeaderboard();
    }

    @GET
    @Path("leaderboard")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get tournament leaderboard for a specific tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets the players from the given tournament ordered by their score, " +
                            "and returns a list of JSONs containing the players.",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation =  PlayerJSON.class)))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void getTournamentLeaderboard(@Valid @NotNull(message = "Tournament key must be provided.")
                                         @QueryParam("tournamentKey") String tournamentKey, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getTournamentLeaderboard(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable getTournamentLeaderboard(String tournamentKey) { return (Serializable) tournamentService.getTemporaryLeaderboard(tournamentKey); }

    @GET
    @Path("round")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Sets current round of the tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets the round with the given key from the given tournament, " +
                            "and returns a JSON of it.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RoundJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "No round with the given key"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void getRound(@Valid @NotNull(message = "Tournament key must be provided.")
                         @QueryParam("tournament_key") String tournamentKey,
                         @Valid @NotNull(message = "Round key must be provided.")
                         @QueryParam("round_key") String roundKey,
                         @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getRound(tournamentKey, roundKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable getRound(String tournamentKey, String setRound) { return tournamentService.getRound(tournamentKey, setRound); }

    @POST
    @Path("end")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Ends a tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sets the given tournament's state to ENDED and disables participation. " +
                            "Returns a custom JSON response.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Tournament ended.\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Tournament has already ended!"),
                    @ApiResponse(responseCode = "422", description = "Tournament have not begun yet!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void endTournament(@Valid @NotNull(message = "Tournament key must be provided.")
                              @QueryParam("tournament_key") String tournamentKey,
                              @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> endTournament(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable endTournament(String tournamentKey) {
        return tournamentService.endTournament(tournamentKey);
    }

    @GET
    @Path("current-round")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get current tournament round",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Finds the current round and returns a JSON of it.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RoundJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Tournament hasn't started yet!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void getCurrentRound(@Valid @NotNull(message = "Tournament Key must be provided") @QueryParam("tournament_key") String tournamentKey, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getCurrentRound(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable getCurrentRound(String tournamentKey) { return tournamentService.getCurrentRound(tournamentKey); }

    @GET
    @Path("player/results")
    @Operation(summary = "Get a player's results from a tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets all the match results of a player " +
                            "and returns a list of JSONs containing the results.",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation =  MatchResultJSON.class)))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void getResults(@Valid @NotNull(message = "Tournament key can't be null!") @QueryParam("tournament_key") String tournamentKey,
                           @Valid @NotNull(message = "Player key can't be null!") @QueryParam("player_key") String playerKey,
                           @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getResults(tournamentKey, playerKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable getResults(String tournamentKey, String playerKey) { return (Serializable) tournamentService.getPlayerResults(tournamentKey, playerKey); }

    @GET
    @Path("active-round/summary")
    @Operation(summary = "Get summary of the active round from a tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets the currently active round " +
                            "and returns a JSON containing the summary of it.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RoundJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Not found if no tournament is found for the given key."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void getActiveRound(@Valid @NotNull(message = "Tournament key can't be null!") @QueryParam("tournament_key") String tournamentKey,
                               @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getActiveRound(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable getActiveRound(String tournamentKey) {
        return tournamentService.getActiveRound(tournamentKey);
    }

    @POST
    @Path("registration/set")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Enables or disables registrations for a tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Enables or disable registration to the given tournament " +
                            "and returns a custom JSON response.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\"," +
                                            " \"message\": \"Registration status set to true for tournament Chess\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Cannot change registrations for a tournament that's ongoing or finished!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void setRegistrationStatus(@Valid @NotNull(message = "Tournament key must be provided.")
                                      @QueryParam("tournament_key") String tournamentKey,
                                      @Valid @NotNull(message = "TRUE/FALSE value must be provided")
                                      @QueryParam("registration_status") boolean registrationStatus,
                                      @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> setRegistrationStatus(tournamentKey, registrationStatus), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable setRegistrationStatus(String tournamentKey, boolean registrationStatus) { return tournamentService.setRegistrationStatus(tournamentKey, registrationStatus); }

    @POST
    @Path("participants/set")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Sets the maximum number of participants for a tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sets the given tournament's maximum allowed participants to the given number," +
                            " returns a custom JSON response.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\"," +
                                            " \"message\": \"Maximum number of participants set to 30 for tournament Chess\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Cannot change number of participants for a tournament that's ongoing or finished!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void setMaxParticipants(@Valid @NotNull(message = "Tournament key must be provided.")
                                   @QueryParam("tournament_key") String tournamentKey,
                                   @Valid @NotNull(message = "TRUE/FALSE value must be provided")
                                   @QueryParam("max_participants_no") int maxParticipants,
                                   @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> setMaxParticipants(tournamentKey, maxParticipants), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable setMaxParticipants(String tournamentKey, int maxParticipants) { return tournamentService.setMaxParticipants(tournamentKey, maxParticipants); }

    @POST
    @Path("participants")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all tournament participants",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets all the participants of a given tournament " +
                            "and returns a list of JSONs containing them.",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation =  ParticipantStatusJSON.class)))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void listAllParticipants(@Valid @NotNull(message = "Tournament key must be provided.")
                                    @QueryParam("tournamentKey") String tournamentKey,
                                    @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> listAllParticipants(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable listAllParticipants(String tournamentKey) { return (Serializable) tournamentService.listAllParticipants(tournamentKey); }

    @GET
    @Path("/last-played-match")
    @Consumes("application/json")
    @Operation(summary = "Get last played match",
            responses  = {
                    @ApiResponse(responseCode = "200", description = "Gets the match that was played last and returns a JSON of it.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MatchJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Produces(MediaType.APPLICATION_JSON)
    @Anonymous
    public void getLastPlayedMatch (@Valid @NotNull(message = "Tournament key must be provided.")
                                    @QueryParam("tournamentKey") String tournamentKey,
                                    @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getLastPlayedMatch(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }
    public Serializable getLastPlayedMatch(String tournamentKey) throws GeneralSecurityException { return tournamentService.getLastPlayedMatch(tournamentKey); }


    @GET
    @Path("/number-of-ongoing-matches")
    @Consumes("application/json")
    @Operation(summary = "Get total number of ongoing matches",
            responses  = {
                    @ApiResponse(responseCode = "200", description = "Returns the total number of ongoing matches",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "42"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Produces(MediaType.APPLICATION_JSON)
    @Anonymous
    public void getNumberOfOngoingMatches (@Valid @NotNull(message = "Tournament key must be provided.")
                                           @QueryParam("tournamentKey") String tournamentKey,
                                           @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> getNumberOfOngoingMatches(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }
    public Serializable getNumberOfOngoingMatches(String tournamentKey) {
        return tournamentService.getTotalNumberOfOngoingMatches(tournamentKey);
    }

    @POST
    @Path("assign/referee")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Assign referee to tournament",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Adds the referee to the tournament and returns a custom JSON response.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\", \"message\": \"Referee added in tournament!\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Referee is already added to this tournament!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void assignRefereeToTournament(@NotNull(message = "Referee key must be provided!") @QueryParam("refereeKey") String refereeKey,
                                          @NotNull(message = "Tournament key must be provided!") @QueryParam("tournamentKey") String tournamentKey,
                                          @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> assignRefereeToTournament(refereeKey, tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable assignRefereeToTournament(String refereeKey, String tournamentKey) {
        return tournamentService.assignRefereeToTournament(refereeKey, tournamentKey);
    }

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all tournaments",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Finds all tournaments based on the given status " +
                            "and returns a list of them.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\n  \"status\": \"ok\", " +
                                            "\n  \"tournaments\": [" +
                                            "\n {\n    \"tournament_name\": \"Chess Tournament\", " +
                                            "\n   \"tournament_key\": \"HP6c-y8UUVXI3eOD2AVaH\", " +
                                            "\n    \"tournament_status\": \"CREATED\" \n}\n\t]\n}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void loadAllTournaments(@Valid @NotNull @QueryParam("status") TournamentStatus status,
                                   @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> loadAllTournaments(status), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable loadAllTournaments(TournamentStatus status) {
        return (Serializable) tournamentService.loadAllTournaments(status);
    }

    @POST
    @Path("search/participants")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/json")
    @Operation(summary = "Search participants",
            responses  = {
                    @ApiResponse(responseCode = "200", description = "Get a list of participants based on given search criteria",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SearchParticipantsJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "422", description = "Email criteria must have at least 3 characters!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void searchParticipants(@Valid ParticipantSearchCriteria participantSearchCriteria, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> searchParticipants(participantSearchCriteria), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable searchParticipants(ParticipantSearchCriteria participantSearchCriteria) {
        return (Serializable) tournamentService.searchParticipants(participantSearchCriteria);
    }

    @POST
    @Path("search/tournaments")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/json")
    @Operation(summary = "Search tournaments",
            responses  = {
                    @ApiResponse(responseCode = "200", description = "Get a list of tournaments based on given search criteria",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TournamentJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "422", description = "Tournament name criteria must have at least 3 characters!"),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    @Anonymous
    public void searchTournaments(@Valid TournamentSearchCriteria tournamentSearchCriteria,@Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> searchTournaments(tournamentSearchCriteria), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }
    public Serializable searchTournaments(TournamentSearchCriteria tournamentSearchCriteria){
        return (Serializable) tournamentService.searchTournaments(tournamentSearchCriteria);
    }


    @POST
    @Path("search/referees")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/json")
    @Operation (summary = "Search referees",
                responses  = {
                    @ApiResponse(responseCode = "200", description = "Get a list of referees based on given search criteria",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PlayerJSON.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Business error."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @Anonymous
    public void searchReferee(@Valid RefereeSearchCriteria refereeSearchCriteria, @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> searchReferee(refereeSearchCriteria), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    public Serializable searchReferee(RefereeSearchCriteria refereeSearchCriteria) {
        return (Serializable) tournamentService.searchReferee(refereeSearchCriteria);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "Delete a tournament and associated relations",
            responses  = {
                    @ApiResponse(responseCode = "200", description = "Returns a custom JSON with OK status and a message," +
                            "if the tournament, rounds, matches and all other associations have been deleted.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"status\": \"ok\"," +
                                            " \"message\": \"Tournament and associated relations has been deleted.\"}"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized."),
                    @ApiResponse(responseCode = "422", description = "Not found if no tournament is found for the given key."),
                    @ApiResponse(responseCode = "422", description = "Not found if no player is found to be deleted."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            })
    public void deleteTournament(@NotNull(message = "Tournament key must be provided!") @QueryParam("tournament_key") String tournamentKey,
                                 @Suspended AsyncResponse asyncResponse) {

        ExecutorService executorService = ExecutorsProvider.getExecutorService();
        Computation.computeAsync(() -> deleteTournament(tournamentKey), executorService)
                .thenApplyAsync(json -> asyncResponse.resume(Response.ok(json).build()), executorService)
                .exceptionally(error -> asyncResponse.resume(ExceptionHandler.handleException((CompletionException) error)));
    }

    private Serializable deleteTournament(String tournamentKey) {
        return tournamentService.deleteTournament(tournamentKey);
    }
}