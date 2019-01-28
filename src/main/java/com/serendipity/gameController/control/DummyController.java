package com.serendipity.gameController.control;

import com.google.gson.Gson;
import com.serendipity.gameController.model.Player;
import com.serendipity.gameController.service.playerService.PlayerServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DummyController {

    @Autowired
    PlayerServiceImpl playerService;

    @RequestMapping(value="/registerPlayerTest", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public ResponseEntity registerPlayer(@RequestBody String json) {
        JSONObject input = new JSONObject(json);
        String real = input.getString("real_name");
        String hacker = input.getString("hacker_name");
        Long nfc = input.getLong("nfc_id");
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value="/gameInfoTest", method=RequestMethod.GET)
    @ResponseBody
    public String getGameInfo() {
        JSONObject output = new JSONObject();
        output.put("start_time", LocalTime.now().plus(10, ChronoUnit.SECONDS));
        output.put("number_players", 0);
        return output.toString();
    }

    @RequestMapping(value="/joinGameTest", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public String joinGame(@RequestBody String json) {
        JSONObject input = new JSONObject(json);
        Long id = input.getLong("player_id");
        JSONObject output = new JSONObject();
        output.put("home_beacon_minor", 0);
        output.put("home_beacon_name", "home");
        return output.toString();
    }

    @RequestMapping(value="/startInfoTest", method=RequestMethod.GET)
    @ResponseBody
    public String getStartInfo() {
        List<Player> ret = new ArrayList<>();
        ret.add(new Player("Jack", "Cutiekitten"));
        ret.add(new Player("Tilly", "Puppylover"));
        ret.add(new Player("Tom", "Cookingking"));
        String output = new Gson().toJson(ret);
        return output;
    }

    @RequestMapping(value="/playerUpdateTest", method=RequestMethod.POST)
    @ResponseBody
    public String playerUpdate(@RequestBody String json) {
        JSONObject input = new JSONObject(json);
        Long playerId = input.getLong("player_id");
        JSONArray beacons = input.getJSONArray("beacons");
        List<Long> nearbyPlayerIds = new ArrayList<>();
        nearbyPlayerIds.add(1l);
        nearbyPlayerIds.add(2l);
        JSONObject output = new JSONObject();
        output.put("nearby_players", nearbyPlayerIds);
        int kills = 3;
        output.put("points", kills);
        int position = 1;
        output.put("position", position);
        int takenDown = 0;
        output.put("taken_down", takenDown);
        int reqNewTarget = 0;
        output.put("req_new_target", reqNewTarget);
        return output.toString();
    }

    @RequestMapping(value="/newTargetTest", method=RequestMethod.POST, consumes="application/json")
    @ResponseBody
    public String getNewTarget(@RequestBody String json) {
        JSONObject input = new JSONObject(json);
        Long playerId = input.getLong("player_id");
        Long targetId = 0l;
        JSONObject output = new JSONObject();
        output.put("target_player_id", targetId);
        return output.toString();
    }

    @RequestMapping(value="/exchangeTest", method=RequestMethod.POST)
    @ResponseBody
    public ResponseEntity exchange(@RequestBody String json) {
        JSONObject input = new JSONObject(json);
        Long interacterId = input.getLong("interacter_id");
        Long interacteeId = input.getLong("interactee_id");
        Long secondaryId = 0l;
        JSONObject output = new JSONObject();
        output.put("secondary_id", secondaryId);
        ResponseEntity<String> response = new ResponseEntity<>(output.toString(), HttpStatus.OK);
        return response;
    }

    @RequestMapping(value="/takeDownTest", method=RequestMethod.POST)
    @ResponseBody
    public ResponseEntity takeDown(@RequestBody String json) {
        JSONObject input = new JSONObject(json);
        Long playerId = input.getLong("player_id");
        Long targetId = input.getLong("target_id");
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value="/endInfoTest", method=RequestMethod.GET)
    @ResponseBody
    public String endInfo() {
        List<Player> players = new ArrayList<>();
        players.add(new Player("Tilly","Headshot"));
        players.add(new Player("Tom","Cutiekitten"));
        String output = new Gson().toJson(players);
        return output;
    }

}
