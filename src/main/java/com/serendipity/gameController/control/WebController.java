package com.serendipity.gameController.control;

import com.serendipity.gameController.model.Beacon;
import com.serendipity.gameController.model.Game;
import com.serendipity.gameController.model.Zone;
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
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.transaction.Transactional;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Controller
@CrossOrigin
public class WebController {

    @Autowired
    PlayerServiceImpl playerService;

    @Autowired
    BeaconServiceImpl beaconService;

    @Autowired
    GameServiceImpl gameService;

    @Autowired
    ExchangeServiceImpl exchangeService;

    @Autowired
    MissionServiceImpl missionService;

    @Autowired
    InterceptServiceImpl interceptService;

    @Autowired
    ZoneServiceImpl zoneService;

    @Autowired
    EvidenceServiceImpl evidenceService;

    @Autowired
    ExposeServiceImpl exposeService;

    @Autowired
    LogServiceImpl logService;

    @GetMapping(value="/")
    public String home(Model model) {
        model.addAttribute("beacons", beaconService.getAllBeacons());
        model.addAttribute("zones", zoneService.getAllZones());
        model.addAttribute("games", gameService.getAllGames());
        return "admin";
    }

    private void resetTables() {
        logService.deleteAllLogs();
        missionService.unassignAllMissions();
        missionService.deleteAllMissions();
        evidenceService.deleteAllEvidence();
        interceptService.deleteAllIntercepts();
        exchangeService.deleteAllExchanges();
        exposeService.unassignPlayers();
        exposeService.deleteAllExposes();
        playerService.deleteAllPlayers();
        beaconService.deleteAllBeacons();
        zoneService.deleteAllZones();
        gameService.deleteAllGames();
    }

    @PostMapping(value="/resetAll")
    public String resetAll() {
        resetTables();
        return "redirect:/";
    }

    @PostMapping(value="/addTestBeacons")
    public String setupTestGame() {
//        resetTables();
        logService.deleteAllLogs();
        missionService.unassignAllMissions();
        missionService.deleteAllMissions();
        evidenceService.deleteAllEvidence();
        interceptService.deleteAllIntercepts();
        exchangeService.deleteAllExchanges();
        exposeService.unassignPlayers();
        exposeService.deleteAllExposes();
        playerService.deleteAllPlayers();
        beaconService.deleteAllBeacons();
        zoneService.deleteAllZones();

        Zone italy = new Zone("Italy", 0.06f, 0.15f);
        Zone sweden = new Zone("Sweden", 0.25f, 0.55f);
        Zone UN = new Zone("UN", 0.8f, 0.7f);
        Zone switzerland = new Zone("Switzerland", 0.75f, 0.15f);
        Zone czechRepublic = new Zone("Czech Republic", 0.85f, 0.8f);
        zoneService.saveZone(italy);
        zoneService.saveZone(sweden);
        zoneService.saveZone(UN);
        zoneService.saveZone(switzerland);
        zoneService.saveZone(czechRepublic);
        Beacon beacon1 = new Beacon(1, 1, "Beacon 1", italy);
        Beacon beacon2 = new Beacon(1, 2, "Beacon 2", sweden);
        Beacon beacon3 = new Beacon(2, 1, "Beacon 3", UN);
        Beacon beacon4 = new Beacon(2, 2, "Beacon 4", switzerland);
        Beacon beacon5 = new Beacon(3, 1, "Beacon 5", czechRepublic);
        beaconService.saveBeacon(beacon1);
        beaconService.saveBeacon(beacon2);
        beaconService.saveBeacon(beacon3);
        beaconService.saveBeacon(beacon4);
        beaconService.saveBeacon(beacon5);
        return "redirect:/";
    }

    @PostMapping(value="/initZone")
    public String initZone(@ModelAttribute("zone_name") String name,
                           @ModelAttribute("zone_x") float x,
                           @ModelAttribute("zone_y") float y) {
        Zone zone = new Zone(name, x, y);
        zoneService.saveZone(zone);
        return "redirect:/";
    }

    @PostMapping(value="/updateZone")
    public String updateZone(@ModelAttribute("zone_id") Long id,
                             @ModelAttribute("zone_name") String name,
                             @ModelAttribute("zone_x") float x,
                             @ModelAttribute("zone_y") float y) {
        Optional<Zone> optionalZone = zoneService.getZoneById(id);
        if (optionalZone.isPresent()) {
            Zone zone = optionalZone.get();
            zone.setName(name);
            zone.setX(x);
            zone.setY(y);
            zoneService.saveZone(zone);
        }
        return "redirect:/";
    }

    @Transactional
    @PostMapping(value="delZone")
    public String delZone(@ModelAttribute("zone_id") Long id) {
        Optional<Zone> optionalZone = zoneService.getZoneById(id);
        if (optionalZone.isPresent()) {
            Zone zone = optionalZone.get();
            List<Beacon> beacons = zone.getBeacons();
            for (Iterator<Beacon> it = beacons.iterator(); it.hasNext();) {
                Beacon beacon = it.next();
                it.remove();
                beaconService.deleteBeaconById(beacon.getId());
            }
            zoneService.deleteZone(zone);
        }
        return "redirect:/";
    }

    @PostMapping(value="/initBeacon")
    public String initBeacon(@ModelAttribute("beacon_identifier") String identifier,
                             @ModelAttribute("beacon_major") int major,
                             @ModelAttribute("beacon_minor") int minor,
                             @ModelAttribute("beacon_zone") Zone zone) {
        Beacon beacon = new Beacon(major, minor, identifier, zone);
        beaconService.saveBeacon(beacon);
        return "redirect:/";
    }

    @PostMapping(value="/updateBeacon")
    public String updateBeacon(@ModelAttribute("beacon_id") Long id,
                               @ModelAttribute("beacon_identifier") String identifier,
                               @ModelAttribute("beacon_major") int major,
                               @ModelAttribute("beacon_minor") int minor) {
        Optional<Beacon> optionalBeacon = beaconService.getBeaconById(id);
        if (optionalBeacon.isPresent()) {
            Beacon beacon = optionalBeacon.get();
            beacon.setIdentifier(identifier);
            beacon.setMajor(major);
            beacon.setMinor(minor);
            beaconService.saveBeacon(beacon);
        }
        return "redirect:/";
    }

    @Transactional
    @PostMapping(value="/delBeacon")
    public String delBeacon(@ModelAttribute("beacon_id") Long id) {
        Beacon beacon = beaconService.getBeaconById(id).get();
        zoneService.removeBeaconFromZone(beacon);
        beaconService.deleteBeaconById(id);
        return "redirect:/";
    }

    @PostMapping(value="/initGame")
    public String initGame(@ModelAttribute("start_time") int start,
                           @ModelAttribute("minutes") int length,
                           @ModelAttribute("missions") int missions) {
        // Reset all tables except beacons/zones
        logService.deleteAllLogs();
        missionService.unassignAllMissions();
        missionService.deleteAllMissions();
        evidenceService.deleteAllEvidence();
        interceptService.deleteAllIntercepts();
        exchangeService.deleteAllExchanges();
        exposeService.unassignPlayers();
        exposeService.deleteAllExposes();
        playerService.deleteAllPlayers();
        gameService.deleteAllGames();

        // Start game
        LocalTime startTime = LocalTime.now().plusMinutes(start);
        Game game = new Game(startTime, startTime.plusMinutes(length));
         // TODO: Handle number of missions
        gameService.saveGame(game);

        // Initialise CSV files for logging
        logService.initCSVs();
        return "redirect:/";
    }

}
