package com.serendipity.gameController.service.logService;

import com.serendipity.gameController.model.*;
import com.serendipity.gameController.repository.LogRepository;
import com.serendipity.gameController.service.exchangeService.ExchangeServiceImpl;
import com.serendipity.gameController.service.exposeService.ExposeServiceImpl;
import com.serendipity.gameController.service.gameService.GameServiceImpl;
import com.serendipity.gameController.service.interceptService.InterceptServiceImpl;
import com.serendipity.gameController.service.missionService.MissionServiceImpl;
import com.serendipity.gameController.service.playerService.PlayerServiceImpl;
import com.serendipity.gameController.service.zoneService.ZoneServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service("logService")
public class LogServiceImpl implements LogService {

    @Autowired
    LogRepository logRepository;

    @Autowired
    MissionServiceImpl missionService;

    @Autowired
    ExposeServiceImpl exposeService;

    @Autowired
    ExchangeServiceImpl exchangeService;

    @Autowired
    InterceptServiceImpl interceptService;

    @Autowired
    PlayerServiceImpl playerService;

    @Autowired
    ZoneServiceImpl zoneService;

    @Autowired
    GameServiceImpl gameService;

    @Override
    public void saveLog(LogType type, Long id, LocalTime time, Zone zone){
        Log log = new Log(type, id, time, zone);
        logRepository.save(log);
    }

    @Override
    public Optional<Log> getLog(Long id){
        return logRepository.findById(id);
    }

    @Override
    public void deleteAllLogs() {
        logRepository.deleteAll();
    }

    @Override
    public JSONArray logOutput() {
        JSONArray output = new JSONArray();

        // Get all non-sent logs
        List<Log> logs = logRepository.findAllBySent(false);

        for (Log l : logs) {
            JSONObject obj = new JSONObject();
            String text = "";
            boolean found = false;
            if (l.getType() == LogType.MISSION) {
                // Compose string for mission log
                // Get mission
                Optional<Mission> opMission = missionService.getMission(l.getInteractionId());
                // See if mission exists
                if (opMission.isPresent()) {
                    Mission mission = opMission.get();
                    Optional<Player> opPlayer = playerService.getPlayerByMission(mission);
                    // See if player exists for the given mission assignment
                    if (opPlayer.isPresent()) {
                        Player player = opPlayer.get();
                        text = player.getRealName() + " completed a mission!";
                        obj.put("interaction", "mission");
                        found = true;
                    }
                }
            } else if (l.getType() == LogType.INTERCEPT) {
                // Compose string for intercept log
                // Get intercept
                Optional<Intercept> opIntercept = interceptService.getIntercept(l.getInteractionId());
                // See if intercept exists
                if (opIntercept.isPresent()) {
                    Intercept intercept = opIntercept.get();
                    // Get player
                    Player player = intercept.getIntercepter();
                    // Get target
                    Exchange exchange = intercept.getExchange();
                    Player target1 = exchange.getRequestPlayer();
                    Player target2 = exchange.getResponsePlayer();
                    text = player.getRealName() + "<span class = \"intercept\"> intercepted</span> an <span class = \"exchange\">exchange</span> between " + target1.getRealName() + " and " + target2.getRealName() + "!";
                    obj.put("interaction", "intercept");
                    found = true;
                }
            } else if (l.getType() == LogType.EXPOSE) {
                // Compose string for expose log
                // Get expose
                Optional<Expose> opExpose = exposeService.getExpose(l.getInteractionId());
                // See if expose exists
                if (opExpose.isPresent()) {
                    Expose expose = opExpose.get();
                    Player player = expose.getPlayer();
                    Player target = expose.getTarget();
                    text = player.getRealName() + " <span class = \"expose\">exposed</span> " + target.getRealName() + "!";
                    obj.put("interaction", "expose");
                    found = true;
                }
            } else if (l.getType() == LogType.EXCHANGE) {
                // Compose string for exchange log
                // Get exchange
                Optional<Exchange> opExchange = exchangeService.getExchange(l.getInteractionId());
                // See if expose exists
                if (opExchange.isPresent()) {
                    Exchange exchange = opExchange.get();
                    Player requester = exchange.getRequestPlayer();
                    Player responder = exchange.getResponsePlayer();
                    text = requester.getRealName() + " <span class = \"exchange\">exchanged</span> with "  + responder.getRealName() + "!";
                    obj.put("interaction", "exchange");
                    found = true;
                }
            }
            if(found) {
                obj.put("time", l.getTime());
                obj.put("message", text);
                obj.put("zone_name", l.getZone().getName());
                output.put(obj);
                l.setSent(true);
                logRepository.save(l);
            }
        }
        return output;
    }

    @Override
    public JSONArray zoneDisplayBlank() {
        JSONArray zones = new JSONArray();
        for(Zone z : zoneService.getAllZones()){
            // Add to output
            JSONObject zoneInfo = new JSONObject();
            zoneInfo.put("zone_id", z.getId());
            zoneInfo.put("zone_name", z.getName());
            zoneInfo.put("x", z.getX());
            zoneInfo.put("y", z.getY());
            zoneInfo.put("size", 0);
            zoneInfo.put("colour", 1);
            zones.put(zoneInfo);
        }
        return zones;
    }

    @Override
    public JSONArray zoneDisplay() {
        JSONArray zones = new JSONArray();

        List<Player> players = playerService.getAllPlayers();

        for(Zone z : zoneService.getAllZones()){
            //Fetch log list for zone
            List<Log> logs = logRepository.findAllByZoneOrderByTimeDesc(z);
            int backLog = (logs.size()>=5) ? 5 : logs.size();
            int count = 0;
            float colour = 0;
            // Select colour
            for(int i = 0; i < backLog; i++){
                Log l = logs.get(i);
                // Add to correct colour
                switch(l.getType()) {
                    case EXCHANGE:
                        colour += 1;
                        count++;
                        break;
                    case INTERCEPT:
                        colour -= 0.5;
                        count++;
                        break;
                    case EXPOSE:
                        colour -= 1;
                        count++;
                        break;
                    default:
                        break;
                }
            }
            if(colour != 0) colour /= count;
            // Default colour of green
            if(colour == 0 && count == 0){
                colour = 1;
            }
            // Get zone size
            List<Player> playersAtZone = playerService.getAllPlayersByCurrentZone(z);
            // Add to output
            JSONObject zoneInfo = new JSONObject();
            zoneInfo.put("zone_id", z.getId());
            zoneInfo.put("zone_name", z.getName());
            zoneInfo.put("x", z.getX());
            zoneInfo.put("y", z.getY());
            if(players.size() > 0 || playersAtZone.size() > 0) {
                float size = ((float)playersAtZone.size() / (float)players.size());
                zoneInfo.put("size", size);
            } else { zoneInfo.put("size", 0); }
            zoneInfo.put("colour", colour);
            zones.put(zoneInfo);
        }

        return zones;
    }

    @Override
    public JSONArray topPlayers(){
        JSONArray output = new JSONArray();
//        List<Player> players = playerService.getAllPlayersByScore();
//        int max = players.size();
//        if(max > players.size()) max = players.size();
//        int count = 0;
//        // Get number of players wanted
//        while(count < max) {
//            JSONObject player = new JSONObject();
//            player.put("position", (count + 1));
//            player.put("real_name", players.get(count).getRealName());
//            player.put("reputation", players.get(count).getReputation());
//            output.put(player);
//            count++;
//        }
        List<Player> players = playerService.getAllPlayersByScore();
        for (Player player : players) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("position", playerService.getLeaderboardPosition(player));
            playerInfo.put("real_name", player.getRealName());
            playerInfo.put("reputation", player.getReputation());
            output.put(playerInfo);
        }
        // TODO: what do we do if there's an overflow due to multiple people with the same position
        return output;
    }

    @Override
    public JSONObject timeRemaining(){
        JSONObject output = new JSONObject();
        List<Game> games = gameService.getAllGames();
        if(games.size() > 0) {
            List<Integer> time = gameService.getTimeRemaining(games.get(0));
            String minutes = "";
            String seconds = "";
            if(time.get(1) < 10){
                minutes = "0" + time.get(1).toString();
            }
            if(time.get(0) < 10){
                seconds = "0" + time.get(0).toString();
            }
            output.put("minutes", time.get(1));
            output.put("seconds", time.get(2));
        } else {
            output.put("minutes", "00");
            output.put("seconds", "00");
        }
        return output;
    }

    @Override
    public void printToCSV(List<String> data, String filename) {
        try {
            FileWriter pw = new FileWriter(filename, true);
            Iterator d = data.iterator();
            if (!d.hasNext()) {
                System.out.println("Empty");
            }
            while (d.hasNext()) {
                String current = replaceCharacters((String) d.next());
                pw.append(current);
                pw.append(",");
            }
            pw.append("\n");
            pw.flush();
            pw.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private String replaceCharacters(String data) {
        if (data.contains(",")) {
            data = data.replace(",", " | ");
        }
        return data;
    }

    @Override
    public void initCSVs() {
        // Setup CSV files and column headers
        List<String> files = new ArrayList<>();
        List<String[]> data = new ArrayList<>();
        files.add("registerPlayer.csv");
        data.add(new String[] {"name", "time", "code", "request", "response"});
        files.add("gameInfo.csv");
        data.add(new String[] {"time", "code", "response"});
        files.add("joinGame.csv");
        data.add(new String[] {"player_id", "name", "time", "code", "request", "response"});
        files.add("startInfo.csv");
        data.add(new String[] {"player_id", "name", "time", "code", "request", "response"});
        files.add("atHomeBeacon.csv");
        data.add(new String[] {"player_id", "name", "time", "code", "request", "response"});
        files.add("playerUpdate.csv");
        data.add(new String[] {"player_id", "name", "time", "code", "request", "response"});
        files.add("newTarget.csv");
        data.add(new String[] {"player_id", "name", "time", "code", "request", "response"});
        files.add("exchangeRequest.csv");
        data.add(new String[] {"id", "requester", "responder", "time", "code", "request", "response"});
        files.add("exchangeResponse.csv");
        data.add(new String[] {"id", "requester", "responder", "time", "code", "request", "response"});
        files.add("expose.csv");
        data.add(new String[] {"id", "name", "time", "code", "request", "response"});
        files.add("intercept.csv");
        data.add(new String[] {"id", "name", "time", "code", "request", "response"});
        files.add("missionUpdate.csv");
        data.add(new String[] {"player_id", "name", "time", "code", "request", "response"});
        files.add("endInfo.csv");
        data.add(new String[] {"time", "code", "response"});

        // Create new CSVs
        for(String f: files){
            try
            {   File file = new File(f);
                FileWriter pw = new FileWriter(file);
                for(String d : data.get(files.indexOf(f))) {
                    pw.append(d);
                    pw.append(",");
                }
                pw.append("\n");
                pw.flush();
                pw.close();
                file.deleteOnExit();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}
