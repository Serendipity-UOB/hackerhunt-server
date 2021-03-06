package com.serendipity.gameController.control;

import com.serendipity.gameController.model.*;
import com.serendipity.gameController.service.beaconService.BeaconServiceImpl;
import com.serendipity.gameController.service.evidenceService.EvidenceServiceImpl;
import com.serendipity.gameController.service.exchangeService.ExchangeServiceImpl;
import com.serendipity.gameController.service.exposeService.ExposeServiceImpl;
import com.serendipity.gameController.service.gameService.GameServiceImpl;
import com.serendipity.gameController.service.interceptService.InterceptServiceImpl;
import com.serendipity.gameController.service.logService.LogServiceImpl;
import com.serendipity.gameController.service.missionService.MissionServiceImpl;
import com.serendipity.gameController.service.playerService.PlayerServiceImpl;
import com.serendipity.gameController.service.zoneService.ZoneServiceImpl;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static java.time.temporal.ChronoUnit.SECONDS;

@Controller
@CrossOrigin
public class MobileController {

    @Autowired
    PlayerServiceImpl playerService;

    @Autowired
    ExchangeServiceImpl exchangeService;

    @Autowired
    BeaconServiceImpl beaconService;

    @Autowired
    GameServiceImpl gameService;

    @Autowired
    EvidenceServiceImpl evidenceService;

    @Autowired
    MissionServiceImpl missionService;

    @Autowired
    ZoneServiceImpl zoneService;

    @Autowired
    InterceptServiceImpl interceptService;

    @Autowired
    LogServiceImpl logService;

    @Autowired
    ExposeServiceImpl exposeService;

    @RequestMapping(value="/registerPlayer", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> registerPlayer(@RequestBody String json) {
        ResponseEntity<String> response;
        JSONObject input = new JSONObject(json);
        System.out.println("/registerPlayer received: " + input);
        String realName = input.getString("real_name");
        String codeName = input.getString("code_name");

        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        Optional<Game> optionalNextGame = gameService.getNextGame();
        if (!optionalNextGame.isPresent()) {
            responseStatus = HttpStatus.NO_CONTENT;
            output.put("error", "No game");
        } else if (!playerService.isValidRealNameAndCodeName(realName, codeName)) {
            output.put("error", "Code name is taken");
        } else {
            Player player = new Player(realName, codeName);
            playerService.savePlayer(player);
            output.put("player_id", player.getId());
            responseStatus = HttpStatus.OK;

            Game nextGame = optionalNextGame.get();
            if (playerService.countAllPlayers() >= nextGame.getMinPlayers()) {

                // Get game length
                int diff = nextGame.getEndTime().toSecondOfDay() - nextGame.getStartTime().toSecondOfDay();
                int minutes = (diff/60) % 60;

                // If min players is reached, set start time to be in 10 seconds
                nextGame.setStartTime(LocalTime.now().plusSeconds(10));

                // Update end time
                nextGame.setEndTime(nextGame.getStartTime().plusMinutes(minutes));

                // Save game
                gameService.saveGame(nextGame);
            }
        }
        logService.printToCSV(new ArrayList<>(Arrays.asList(realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "registerPlayer.csv");
        System.out.println("/registerPlayer returned: " + output);
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/gameInfo", method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getGameInfo(){
        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.OK;

        Optional<Game> optionalNextGame = gameService.getNextGame();
        Optional<Game> optionalCurrentGame = gameService.getCurrentGame();

        if (optionalCurrentGame.isPresent()) {
            // If game has started

            output.put("number_players", playerService.countAllPlayers());
            output.put("game_start", true);
            output.put("countdown", "00:00");

        } else if (optionalNextGame.isPresent()) {
            // If waiting for game to start
            Game nextGame = optionalNextGame.get();

            long numPlayers = playerService.countAllPlayers();
            output.put("number_players", numPlayers);
            output.put("game_start", false);

            // Calculate countdown
            if (numPlayers < nextGame.getMinPlayers()) {
                // Still waiting for more players

                output.put("countdown", "--:--");
            } else {
                // Get time left

                List<Integer> timeRemaining = gameService.getTimeToStart(nextGame);
                Integer minutes = timeRemaining.get(1);
                Integer seconds = timeRemaining.get(2);
                String minutesString;
                if (minutes < 10) minutesString = "0" + minutes.toString();
                else minutesString = minutes.toString();
                String secondsString;
                if (seconds < 10) secondsString = "0" + seconds.toString();
                else secondsString = seconds.toString();
                String countdown = minutesString + ":" + secondsString;
                output.put("countdown", countdown);
            }
        } else {
            // If no games planned/ongoing

            responseStatus =  HttpStatus.NO_CONTENT;
            output.put("error", "No game");
        }

        System.out.println("/gameInfo returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(LocalTime.now().toString(),
                responseStatus.toString(), output.toString())), "gameInfo.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/joinGame", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> joinGame(@RequestBody String json) {
        // Handle JSON
        JSONObject input = new JSONObject(json);
        System.out.println("/joinGame received: " + input);
        Long id = input.getLong("player_id");
        String realName = "";
        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        // Look for player
        Optional<Player> optionalPlayer = playerService.getPlayer(id);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            realName = player.getRealName();
            // Assign zone, if one exists
            Optional<Zone> optionalZone = zoneService.chooseHomeZone(player);
            if (optionalZone.isPresent()) {
                Zone zone = optionalZone.get();
                player.setHomeZone(zone);
                playerService.savePlayer(player);
                // Handle response
                output.put("home_zone_name", zone.getName());
                responseStatus = HttpStatus.OK;
            } else System.out.println("No zones in the database");
        } else System.out.println("No player exists by this id");
        System.out.println("/joinGame returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(id.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "joinGame.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/startInfo", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> getStartInfo(@RequestBody String json) {
        // Read in request body
        JSONObject input = new JSONObject(json);
        System.out.println("/startInfo received: " + input);
        Long playerId = input.getLong("player_id");
        String realName = "";
        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        // Ensure player exists
        Optional<Player> opPlayer = playerService.getPlayer(playerId);
        if (opPlayer.isPresent()){
            Player player = opPlayer.get();
            realName = player.getRealName();
            List<Game> games = gameService.getAllGames();
            // TODO: Deal with multiple game instances
            if(games.size() > 0) {
                // Ensure game exists
                Game game = gameService.getAllGames().get(0);
                // Setup zone dispersion variables
                player.setCurrentZone(player.getHomeZone());
                player.setTimeEnteredZone(LocalTime.now());
                // Assign initial Mission
                Optional<Mission> opMission = missionService.createMission(player);
                if(opMission.isPresent()){
                    // Set mission parameters
                    Mission mission = opMission.get();
                    mission.setStartTime(game.getStartTime());
                    mission.setEndTime(game.getEndTime());
                    // Assign random zone
                    List<Zone> zones = zoneService.getAllZonesExceptUN();
                    Random random = new Random();
                    mission.setZone(zones.get(random.nextInt(100) % zones.size()));
                    mission.setStart(true);
                    missionService.saveMission(mission);
                    player.setMissionAssigned(mission);

                    // Assign new target
                    Optional<Player> newTarget = playerService.newTarget(player);
                    if(newTarget.isPresent()){
                        Player target = newTarget.get();
                        player.setTarget(target);
                        playerService.savePlayer(player);
                        // Return all players
                        responseStatus = HttpStatus.OK;
                        output.put("all_players", playerService.getAllPlayersStartInfo());
                        // Return first target
                        output.put("first_target_id", target.getId());
                        // Return game endTime
                        output.put("end_time", game.getEndTime());
                    } else {
                        System.out.println("Unable to assign target");
                        output.put("BAD_REQUEST", "Unable to assign target");
                    }
                } else {
                    System.out.println("Not enough players");
                    output.put("BAD_REQUEST", "Not enough players");
                }
            } else {
                System.out.println("No game");
                responseStatus = HttpStatus.NO_CONTENT;
                output.put("NO_CONTENT", "No game");
            }
        } else {
            System.out.println("No player exists by this id");
            output.put("BAD_REQUEST", "Couldn't find player given");
        }
        System.out.println("/startInfo returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(playerId.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "startInfo.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/atHomeBeacon", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> atHomeBeacon(@RequestBody String json) {
        // Handle json
        JSONObject input = new JSONObject(json);
        System.out.println("/atHomeBeacon received: " + input);
        Long id = input.getLong("player_id");
        String realName = "";
        JSONArray beacons = input.getJSONArray("beacons");

        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        // Look for player
        Optional<Player> optionalPlayer = playerService.getPlayer(id);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            realName = player.getRealName();
            if (player.hasHomeZone()) {
                Optional<Zone> optionalZone = zoneService.calculateCurrentZone(player, beacons);
                if (optionalZone.isPresent()) {
                    Zone currentZone = optionalZone.get();
                    player.setCurrentZone(currentZone);
                    Zone homeZone = player.getHomeZone();
                    if (currentZone.equals(homeZone)) {
                        output.put("home", true);
                        if(player.isReturnHome()) {
                            player.setReturnHome(false);
                            Optional<Mission> opMission = missionService.createMission(player);
                            if (opMission.isPresent()) {
                                // Set mission parameters
                                Mission mission = opMission.get();
                                mission.setStartTime(LocalTime.now());
                                // 15 seconds to complete mission
                                mission.setEndTime(LocalTime.now().plusSeconds(20));
                                // Assign random zone
                                List<Zone> zones = zoneService.getAllZonesExceptUNandOne(player.getCurrentZone().getId());
                                //                        List<Zone> zones = zoneService.getAllZonesExcept(player.getCurrentZone().getId());
                                Random random = new Random();
                                mission.setZone(zones.get(random.nextInt(100) % zones.size()));
                                //Set type
                                mission.setType(2);
                                missionService.saveMission(mission);
                                player.setMissionAssigned(mission);
                            }
                        }
                        player.setMissionsPaused(false);
                        playerService.savePlayer(player);
                    } else {
                        output.put("home", false);
                    }
                    responseStatus = HttpStatus.OK;
                } else {
                    System.out.println("Couldn't calculate this player's current zone");
                    output.put("home", false);
                    responseStatus = HttpStatus.OK;
                }
            } else System.out.println("This player hasn't been assigned a home zone");
        } else System.out.println("No player exists by this id");
        System.out.println("/atHomeBeacon returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(id.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "atHomeBeacon.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/playerUpdate", method=RequestMethod.POST , consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> playerUpdate(@RequestBody String json) {

        // Handle json
        JSONObject input = new JSONObject(json);
        System.out.println("/playerUpdate received: " + input);
        Long id = input.getLong("player_id");
        String realName = "";
        JSONArray jsonBeacons = input.getJSONArray("beacons");

        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        // Does player exist
        Optional<Player> optionalPlayer = playerService.getPlayer(id);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            realName = player.getRealName();
            // Nearby players
            List<JSONObject> nearbyPlayers = new ArrayList<>();
            // Far away players
            List<JSONObject> farPlayers = new ArrayList<>();
            Optional<Zone> optionalZone = zoneService.calculateCurrentZone(player, jsonBeacons);
            if (optionalZone.isPresent()) {
                Zone zone = optionalZone.get();
                // Get zone assignment from prevZones list
                zone = playerService.averagePrevZone(player, zone);
                // Change to new zone
                if(!zone.equals(player.getCurrentZone())){
                    player.setCurrentZone(zone);
                    player.setTimeEnteredZone(LocalTime.now());
                    playerService.savePlayer(player);
                }
                nearbyPlayers = playerService.getNearbyPlayers(player);
                farPlayers = playerService.getFarPlayers(player);
            } else System.out.println("Couldn't calculate the player's current zone");
            output.put("nearby_players", nearbyPlayers);
            output.put("far_players", farPlayers);

            // Location
            int location = zoneService.locationMapping(player.getCurrentZone());
            output.put("location", location);

            // Reputation
            output.put("reputation", player.getReputation());

            // Position
            output.put("position", playerService.getLeaderboardPosition(player));

            // Exposed
            if (player.getExposedBy() != 0l) {
                output.put("exposed_by", player.getExposedBy());
                player.setExposedBy(0l);
                playerService.savePlayer(player);
            } else {
                output.put("exposed_by", 0l);
            }

            // Game over
            List<Game> games = gameService.getAllGamesByStartTimeAsc();
            if (gameService.isGameOver(games.get(0))) {
                output.put("game_over", true);
            } else {
                output.put("game_over", false);
            }

            // Exchange pending
            Long requesterId = 0l;
            Optional<Exchange> optionalExchange = exchangeService.getNextExchangeToPlayer(player);
            if (optionalExchange.isPresent()) {
                Exchange exchange = optionalExchange.get();
                if (!exchange.isRequestSent()) {
                    requesterId = exchange.getRequestPlayer().getId();
                    exchange.setRequestSent(true);
                    exchangeService.saveExchange(exchange);
                }
            }
            output.put("exchange_pending", requesterId);

            // Dispersion of players
            if(player.getTimeEnteredZone().plusSeconds(20).isBefore(LocalTime.now()) && !player.getCurrentZone().getName().equals("UN")
                && !player.isMissionsPaused()){
                // See if previous mission has been completed
                if(player.getMissionAssigned().isCompleted()) {
                    // Assign mission
                    Optional<Mission> opMission = missionService.createMission(player);
                    if (opMission.isPresent()) {
                        // Set mission parameters
                        Mission mission = opMission.get();
                        mission.setStartTime(LocalTime.now());
                        // 15 seconds to complete mission
                        mission.setEndTime(LocalTime.now().plusSeconds(20));
                        // Assign random zone
                        List<Zone> zones = zoneService.getAllZonesExceptUNandOne(player.getCurrentZone().getId());
//                        List<Zone> zones = zoneService.getAllZonesExcept(player.getCurrentZone().getId());
                        Random random = new Random();
                        mission.setZone(zones.get(random.nextInt(100) % zones.size()));
                        //Set type
                        mission.setType((player.getMissionAssigned().getType() == 1 ? 2 : 1));
                        missionService.saveMission(mission);
                        player.setMissionAssigned(mission);
                        playerService.savePlayer(player);
                    }
                }
            }

            // Mission
            Optional<Mission> opMission = missionService.getMission(player.getMissionAssigned().getId());
            if( opMission.isPresent() ){
                Mission mission = opMission.get();
                // If mission should start
                if (!mission.isSent()) {
                    Player p1 = mission.getPlayer1();
                    Player p2 = mission.getPlayer2();
                    String missionDescription = "";
                    Zone loc = mission.getZone();
                    if(mission.isStart()){
                        missionDescription = "Welcome, Agent.\nGo to " + loc.getName() +
                                " for a hint on your target's location.";
                    } else if (mission.getType() == 1){
                        missionDescription = "Evidence available on " + p1.getRealName()
                                + " and " + p2.getRealName() + ".\nGo to " + loc.getName() + ".";
                    } else if (mission.getType() == 2){
                        Player target = player.getTarget();
                        missionDescription = "Your target, " + target.getCodeName() + ", was last seen in " +
                                    target.getCurrentZone().getName() + ".\nGo find them!";
                        mission.setCompleted(true);
                        player.setTimeEnteredZone(LocalTime.now());
                        playerService.savePlayer(player);
                    }
                    mission.setSent(true);
                    missionService.saveMission(mission);
                    output.put("mission_description", missionDescription);
                    output.put("mission_type", mission.getType());
                } else { output.put("mission_description", ""); }
            } else {
                System.out.println("This player hasn't had a mission assigned to them");
                output.put("mission_description", "");
            }

            responseStatus = HttpStatus.OK;

        } else System.out.println("No player exists by this id");
        System.out.println("/playerUpdate returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(id.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "playerUpdate.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/newTarget", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> getNewTarget(@RequestBody String json) {
        // Create JSON object for response body
        JSONObject output = new JSONObject();
        String realName = "";
        // Set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        // Process json
        JSONObject input = new JSONObject(json);
        System.out.println("/newTarget received: " + input);
        Long playerId = input.getLong("player_id");
        Optional<Player> optionalPlayer = playerService.getPlayer(playerId);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            realName = player.getRealName();

            // Get a new target
            Optional<Player> optionalNewTarget = playerService.newTarget(player);
            if (optionalNewTarget.isPresent()) {
                Player newTarget = optionalNewTarget.get();

                // Set the target
                player.setTarget(newTarget);
                playerService.savePlayer(player);
                output.put("target_player_id", newTarget.getId());

                // Set status code
                responseStatus = HttpStatus.OK;
            } else {
                output.put("BAD_REQUEST", "Couldn't get a new target");
            }
        } else {
            output.put("BAD_REQUEST", "Couldn't find player in database");
        }
        System.out.println("/newTarget returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(playerId.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "newTarget.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/exchangeRequest", method=RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> exchangeRequest(@RequestBody String json) {
        // Handle JSON
        JSONObject input = new JSONObject(json);
        System.out.println("/exchangeRequest received: " + input);
        Long requesterId = input.getLong("requester_id");
        Long responderId = input.getLong("responder_id");
        Long exchangeId = 0L;
        String requesterName = "";
        String responderName = "";

        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        JSONArray jsonContactIds = input.getJSONArray("contact_ids");

        Optional<Player> optionalRequester = playerService.getPlayer(requesterId);
        Optional<Player> optionalResponder = playerService.getPlayer(responderId);

        // Find players
        if (optionalRequester.isPresent() && optionalResponder.isPresent()) {
            Player requester = optionalRequester.get();
            requesterName = requester.getRealName();
            Player responder = optionalResponder.get();
            responderName = responder.getRealName();
            // Find exchange
            Optional<Exchange> optionalExchange = exchangeService.getMostRecentExchangeFromPlayer(requester);
            if (optionalExchange.isPresent()) {
                Exchange exchange = optionalExchange.get();
                exchangeId = exchange.getId();
                // Handle request
                if (exchange.getResponsePlayer() == responder) {
                    if (exchange.isRequesterToldComplete()) {
                        exchangeId = exchangeService.createExchange(requester, responder, jsonContactIds);
                        output.put("CREATED", "Creating exchange");
                        responseStatus = HttpStatus.CREATED;
                    } else if (exchange.getResponse().equals(ExchangeResponse.ACCEPTED)) {
                        List<Evidence> evidenceList = exchangeService.getMyEvidence(exchange, requester);
                        output.put("evidence", evidenceService.evidenceListToJsonArray(evidenceList));
                        exchange.setRequesterToldComplete(true);
                        exchangeService.saveExchange(exchange);
                        // Add exchange to logs
                        logService.saveLog(LogType.EXCHANGE, exchange.getId(), LocalTime.now(), requester.getCurrentZone());
                        output.put("ACCEPTED", "Exchange accepted");
                        responseStatus = HttpStatus.ACCEPTED;
                    } else if (exchangeService.getTimeRemaining(exchange) <= 0l) {
                        exchange.setRequesterToldComplete(true);
                        exchangeService.saveExchange(exchange);
                        output.put("REQUEST_TIMEOUT", "Exchange timeout");
                        responseStatus = HttpStatus.REQUEST_TIMEOUT;
                    } else {
                        if (exchange.getResponse().equals(ExchangeResponse.REJECTED)) {
                            exchange.setRequesterToldComplete(true);
                            exchangeService.saveExchange(exchange);
                            output.put("NO_CONTENT", "Exchange rejected");
                            responseStatus = HttpStatus.NO_CONTENT;
                        } else if (exchange.getResponse().equals(ExchangeResponse.WAITING)) {
                            output.put("PARTIAL_CONTENT", "Waiting for response");
                            responseStatus = HttpStatus.PARTIAL_CONTENT;
                        } else {
                            System.out.println("Something has gone very wrong to get to here");
                            output.put("UNKNOWN", "Something has gone very wrong to get to here");
                        }
                    }
                } else if (exchangeService.getTimeRemaining(exchange) <= 0l || exchange.isRequesterToldComplete()) {
                    System.out.println("Creating exchange");
                    exchangeId = exchangeService.createExchange(requester, responder, jsonContactIds);
                    output.put("CREATED", "Creating exchange");
                    responseStatus = HttpStatus.CREATED;
                } else {
                    System.out.println("Player already requesting exchange with someone else");
                    output.put("NOT_FOUND", "Player already requesting exchange with someone else");
                    responseStatus = HttpStatus.NOT_FOUND;
                }
            } else {
                System.out.println("No exchange exists from this player, creating an exchange");
                output.put("CREATED", "No exchange exists from this player, creating an exchange");
                exchangeId = exchangeService.createExchange(requester, responder, jsonContactIds);
                responseStatus = HttpStatus.CREATED;
            }
        } else {
            System.out.println("Couldn't find either the requester or the responder by these ids");
            output.put("BAD_REQUEST", "Couldn't find either the requester or the responder by these ids");
        }
        System.out.println("/exchangeRequest returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(exchangeId.toString(), requesterName, responderName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "exchangeRequest.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/exchangeResponse", method=RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> exchangeResponse(@RequestBody String json) {
        // Handle JSON
        JSONObject input = new JSONObject(json);
        System.out.println("/exchangeResponse received: " + input);
        Long requesterId = input.getLong("requester_id");
        Long responderId = input.getLong("responder_id");
        Long exchangeId = 0L;
        String requesterName = "";
        String responderName = "";


        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        int exchangeResponseIndex = input.getInt("response");
        ExchangeResponse exchangeResponse = ExchangeResponse.values()[exchangeResponseIndex];
        JSONArray jsonContactIds = input.getJSONArray("contact_ids");
        Optional<Player> optionalRequester = playerService.getPlayer(requesterId);
        Optional<Player> optionalResponder = playerService.getPlayer(responderId);

        // Find players
        if (optionalRequester.isPresent() && optionalResponder.isPresent()) {
            Player requester = optionalRequester.get();
            requesterName = requester.getRealName();
            Player responder = optionalResponder.get();
            responderName = responder.getRealName();

            // Find exchange
            Optional<Exchange> optionalExchange = exchangeService.getExchangeByPlayers(requester, responder);
            if (optionalExchange.isPresent()) {
                Exchange exchange = optionalExchange.get();
                exchangeId = exchange.getId();
                // Handle response
                if (exchangeResponse.equals(ExchangeResponse.WAITING)) {
                    long timeRemainingBuffer = 1l;
                    long timeRemaining = exchangeService.getTimeRemaining(exchange) - timeRemainingBuffer;
                    if (timeRemaining <= 0l) {
                        output.put("REQUEST_TIMEOUT", "Exchange timeout");
                        responseStatus = HttpStatus.REQUEST_TIMEOUT;
                    } else {
                        // 1s buffer for timeout, to avoid race conditions
                        output.put("time_remaining", timeRemaining);
                        responseStatus = HttpStatus.PARTIAL_CONTENT;
                    }
                } else if (exchangeResponse.equals(ExchangeResponse.ACCEPTED)) {
                    List<Long> contactIds = new ArrayList<>();
                    if (jsonContactIds.length() > 0) {
                        for (int i = 0; i < jsonContactIds.length(); i++) {
                            Long id = jsonContactIds.getJSONObject(i).getLong("contact_id");
                            contactIds.add(id);
                        }
                    }
                    List<Evidence> requestEvidenceList = exchangeService.getMyEvidence(exchange, responder);
                    output.put("evidence", evidenceService.evidenceListToJsonArray(requestEvidenceList));
                    List<Evidence> responseEvidenceList = exchangeService.calculateEvidence(exchange, responder, contactIds);
                    exchange.setEvidenceList(responseEvidenceList);
                    exchange.setResponse(exchangeResponse);
                    exchangeService.saveExchange(exchange);
                    playerService.incrementReputation(responder, 1);
                    playerService.incrementReputation(requester, 1);
                    output.put("ACCEPTED", "Exchange accepted");
                    // Set status code
                    responseStatus = HttpStatus.ACCEPTED;
                } else if (exchangeResponse.equals(ExchangeResponse.REJECTED)) {
                    exchange.setResponse(exchangeResponse);
                    exchangeService.saveExchange(exchange);
                    output.put("RESET_CONTENT", "Exchange rejected");
                    responseStatus = HttpStatus.RESET_CONTENT;
                } else {
                    System.out.println("Exchange has no response status, something wrong on the server");
                    output.put("BAD_REQUEST", "Exchange has no response status, something wrong on the server");
                }
            } else {
                System.out.println("Couldn't find an exchange between these players");
                output.put("BAD_REQUEST", "Couldn't find an exchange between these players");
            }
        } else {
            System.out.println("Couldn't find either the requester or the responder by these ids");
            output.put("BAD_REQUEST", "Couldn't find either the requester or the responder by these ids");
        }
        System.out.println("/exchangeResponse returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(exchangeId.toString(), requesterName, responderName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "exchangeResponse.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @PostMapping(value="/decipherCodename")
    @ResponseBody
    public ResponseEntity<String> decipherCodename(@RequestBody String json) {
        JSONObject output = new JSONObject();
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        JSONObject input = new JSONObject(json);
        Long playerId = input.getLong("player_id");
        Optional<Player> optionalPlayer = playerService.getPlayer(playerId);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            playerService.incrementReputation(player, 10);
            responseStatus = HttpStatus.OK;
            output.put("OK", "Updated reputation");
        } else {
            output.put("BAD_REQUEST", "Can't find player");
        }
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/expose", method=RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> expose(@RequestBody String json) {
        // receive JSON object
        JSONObject input = new JSONObject(json);
        System.out.println("/expose received: " + input);
        Long playerId = input.getLong("player_id");
        Long targetId = input.getLong("target_id");
        String realName = "";
        Long exposeId = 0L;
        // create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        // fetch player making request and target given
        Optional<Player> opPlayer = playerService.getPlayer(playerId);
        Optional<Player> opTarget = playerService.getPlayer(targetId);
        // ensure optionals have a value
        if (opPlayer.isPresent() && opTarget.isPresent()) {
            // unpack optional objects
            Player player = opPlayer.get();
            realName = player.getRealName();
            Player target = opTarget.get();
            // ensure given target matches player's assign target and they haven't been exposed
            if(player.getTarget().getId().equals(target.getId())) {
                if((player.getExposedBy() == 0l)) {
                    // increment reputation for player
                    int reputationGain = playerService.calculateReputationGainFromExpose();
                    playerService.incrementReputation(player, reputationGain);
                    output.put("reputation", reputationGain);
                    // Pause players missions
                    player.setMissionsPaused(true);
                    playerService.savePlayer(player);
                    // set targets exposed attribute
                    target.setExposedBy(playerId);
                    target.setMissionsPaused(true);
                    target.setReturnHome(true);
                    playerService.savePlayer(target);
                    // Create expose
                    Expose expose = new Expose(player, target);
                    exposeService.saveExpose(expose);
                    exposeId = expose.getId();
                    // Add expose to logs
                    logService.saveLog(LogType.EXPOSE, expose.getId(), LocalTime.now(), player.getCurrentZone());
                    // set output elements
                    responseStatus = HttpStatus.OK;
                } else {
                    System.out.println("You have been exposed or must return home");
                    output.put("BAD_REQUEST", "Player has been exposed or player must return home");
                }
            } else {
                System.out.println("This player isn't your target");
                output.put("BAD_REQUEST", "Target Id given doesn't match player's assigned Target");
            }
        } else {
            System.out.println("Couldn't find one of the exposer or the target");
            output.put("BAD_REQUEST", "Couldn't find player or target id given");
        }
        System.out.println("/expose returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(exposeId.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "expose.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/intercept", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> intercept(@RequestBody String json) {
        // Read in request body
        JSONObject input = new JSONObject(json);
        System.out.println("/intercept received: " + input);
        Long playerId = input.getLong("player_id");
        Long targetId = input.getLong("target_id");
        String realName = "";
        Long interceptId = 0L;
        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        // Ensure player exists
        Optional<Player> opPlayer = playerService.getPlayer(playerId);
        Optional<Player> opTarget = playerService.getPlayer(targetId);
        if (opPlayer.isPresent() && opTarget.isPresent()) {
            Player player = opPlayer.get();
            realName = player.getRealName();
            Player target = opTarget.get();
            // Find intercept if it exists
            Optional<Intercept> opIntercept = interceptService.getInterceptByPlayer(player);
            if (opIntercept.isPresent()) {
                Intercept intercept = opIntercept.get();
                // Check if intercept has expired
                if (!intercept.isExpired()) {
                    // Check if the correct target has been sent
                    if (target.equals(intercept.getTarget())) {
                        // Check if intercept should expire
                        // Check 9 second after as .isBefore doesn't cover is equal
                        if (LocalTime.now().isBefore(intercept.getStartTime().plusSeconds(10))) {
                            // Find if exchange has been set
                            if (intercept.getExchange() == null) {
                                // Set exchange if exists
                                Optional<Exchange> opExchange = exchangeService.getEarliestActiveExchange(target);
                                if (opExchange.isPresent()) {
                                    Exchange exchange = opExchange.get();
                                    if (!exchange.getResponsePlayer().equals(player) && !exchange.getRequestPlayer().equals(player)){
                                        intercept.setExchange(exchange);
                                        interceptService.saveIntercept(intercept);
                                        System.out.println("Exchange set");
                                        output.put("PARTIAL_CONTENT", "Exchange set");
                                        responseStatus = HttpStatus.PARTIAL_CONTENT;
                                    } else {
                                        System.out.println("No exchange found");
                                        output.put("PARTIAL_CONTENT", "No exchange found");
                                        responseStatus = HttpStatus.PARTIAL_CONTENT;
                                    }
                                } else {
                                    System.out.println("No exchange found");
                                    output.put("PARTIAL_CONTENT", "No exchange found");
                                    responseStatus = HttpStatus.PARTIAL_CONTENT;
                                }
                            } else {
                                System.out.println("Waiting for intercept window");
                                output.put("PARTIAL_CONTENT", "Waiting for intercept window");
                                responseStatus = HttpStatus.PARTIAL_CONTENT;
                            }
                        } else {
                            intercept.setExpired(true);
                            interceptService.saveIntercept(intercept);
                            Exchange exchange = intercept.getExchange();
                            // Determine response
                            if (exchange != null && exchange.getResponse().equals(ExchangeResponse.ACCEPTED)) {
                                JSONArray evidence = new JSONArray();
                                JSONObject p1 = new JSONObject();
                                p1.put("player_id", exchange.getRequestPlayer().getId());
                                p1.put("amount", 25);

                                JSONObject p2 = new JSONObject();
                                p2.put("player_id", exchange.getResponsePlayer().getId());
                                p2.put("amount", 25);

                                evidence.put(p1);
                                evidence.put(p2);
                                output.put("evidence", evidence);

                                playerService.incrementReputation(player, 2);

                                // Add expose to logs
                                logService.saveLog(LogType.INTERCEPT, intercept.getId(), LocalTime.now(), player.getCurrentZone());
                                responseStatus = HttpStatus.OK;
                            } else {
                                System.out.println("Exchange wasn't accepted");
                                output.put("NO_CONTENT", "Exchange wasn't accepted");
                                responseStatus = HttpStatus.NO_CONTENT;
                            }
                        }
                    } else {
                        System.out.println("Active intercept exists for another target");
                        output.put("NOT_FOUND", "Active intercept exists for another target");
                        responseStatus = HttpStatus.NOT_FOUND;
                    }
                } else {
                    System.out.println("Intercept has expired, creating a new one");
                    // Overwrite expired intercept
                    intercept.setExpired(false);
                    intercept.setTarget(target);
                    intercept.setExchange(null);
                    intercept.setStartTime(LocalTime.now());
                    interceptService.saveIntercept(intercept);
                    output.put("CREATED", "Intercept has expired, creating a new one");
                    responseStatus = HttpStatus.CREATED;
                }

            } else {
                System.out.println("No intercept exists, creating one");
                // Create intercept
                Intercept intercept = new Intercept(player, target, LocalTime.now());
                interceptService.saveIntercept(intercept);
                output.put("CREATED", "No intercept exists, creating one");
                responseStatus = HttpStatus.CREATED;
            }
        }
        System.out.println("/intercept returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(interceptId.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "intercept.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/missionUpdate", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> missionUpdate(@RequestBody String json) {
        // receive JSON object
        JSONObject input = new JSONObject(json);
        System.out.println("/missionUpdate received: " + input);
        Long playerId = input.getLong("player_id");
        String realName = "";

        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

        // Check player exists
        Optional<Player> opPlayer = playerService.getPlayer(playerId);
        if (opPlayer.isPresent()) {
            Player player = opPlayer.get();
            realName = player.getRealName();
            Zone playerZone = player.getCurrentZone();
            Mission mission = player.getMissionAssigned();
            if(!player.isMissionsPaused()) {
                // Check if mission hasn't already been completed
                if (!mission.isCompleted()) {
                    // Get mission details
                    Player p1 = mission.getPlayer1();
                    Player p2 = mission.getPlayer2();
                    Zone missionZone = mission.getZone();
                    // Check if the mission hasn't timed out
                    if (LocalTime.now().isBefore(mission.getEndTime().plusSeconds(1)) || mission.isStart()){
                        if (playerZone.getId().intValue() == missionZone.getId().intValue()){
                            // Evidence to return
                            JSONArray evidence = new JSONArray();
                            JSONObject e1 = new JSONObject();
                            e1.put("player_id", p1.getId());
                            e1.put("amount", 30);
                            evidence.put(e1);
                            JSONObject e2 = new JSONObject();
                            e2.put("player_id", p2.getId());
                            e2.put("amount", 30);
                            evidence.put(e2);
                            output.put("evidence", evidence);
                            // Success String
                            String success;
                            if (mission.isStart()) {
                                Player target = player.getTarget();
                                success = "Your target, " + target.getCodeName() + ", was last seen in " +
                                        target.getCurrentZone().getName() + ".\nGo find them!";
                            } else {
                                success = "Reward: Evidence on " + p1.getRealName() + " and " +
                                        p2.getRealName() + ".";
                            }
                            playerService.incrementReputation(player, 5);
                            output.put("success_description", success);
                            // Set mission complete
                            mission.setCompleted(true);
                            missionService.saveMission(mission);
                            // Add mission to logs
                            logService.saveLog(LogType.MISSION, mission.getId(), LocalTime.now(), player.getCurrentZone());
                            responseStatus = HttpStatus.OK;
                        } else {
                            System.out.println("Not at mission location yet");
                            if (mission.isStart()) {
                                responseStatus = HttpStatus.NO_CONTENT;
                            } else {
                                Long timeRemaining = SECONDS.between(LocalTime.now(), mission.getEndTime());
                                output.put("time_remaining", timeRemaining);
                                responseStatus = HttpStatus.PARTIAL_CONTENT;
                            }
                        }
                    } else {
                        System.out.println("Mission has timed out");
                        responseStatus = HttpStatus.NON_AUTHORITATIVE_INFORMATION;
                        String failure = "No evidence gained.";
                        output.put("failure_description", failure);
                        // Set mission complete
                        mission.setCompleted(true);
                        missionService.saveMission(mission);
                        // Reset zone entry time
                        player.setTimeEnteredZone(LocalTime.now());
                        playerService.savePlayer(player);
                    }
                } else {
                    System.out.println("Mission already completed");
                    output.put("BAD_REQUEST", "Mission already completed");
                }
            } else {
                System.out.println("Missions paused");
                mission.setCompleted(true);
                missionService.saveMission(mission);
                responseStatus = HttpStatus.RESET_CONTENT;
            }
        } else {
            System.out.println("No player exists by this id");
            output.put("BAD_REQUEST", "Couldn't find player given");
        }
        System.out.println("/missionUpdate returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(playerId.toString(), realName, LocalTime.now().toString(),
                responseStatus.toString(), input.toString(), output.toString())), "missionUpdate.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }

    @RequestMapping(value="/endInfo", method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> endInfo() {
        // Create JSON object for response body
        JSONObject output = new JSONObject();
        // set default response status
        HttpStatus responseStatus = HttpStatus.OK;

        JSONArray leaderboard = new JSONArray();
        List<Player> players = playerService.getAllPlayersByScore();
        for (Player player : players) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("player_id", player.getId());
            playerInfo.put("position", playerService.getLeaderboardPosition(player));
            playerInfo.put("score", player.getReputation());
            leaderboard.put(playerInfo);
        }
        output.put("leaderboard", leaderboard);
        System.out.println("/endInfo returned: " + output);
        logService.printToCSV(new ArrayList<>(Arrays.asList(LocalTime.now().toString(),
                responseStatus.toString(), output.toString())), "endInfo.csv");
        return new ResponseEntity<>(output.toString(), responseStatus);
    }
}