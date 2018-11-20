package com.serendipity.gameController.control;

import com.serendipity.gameController.model.Information;
import com.serendipity.gameController.model.Player;
import com.serendipity.gameController.service.informationService.InformationService;
import com.serendipity.gameController.service.informationService.InformationServiceImpl;
import com.serendipity.gameController.service.playerService.PlayerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.*;


@Controller
public class MainController {

    @Autowired
    PlayerServiceImpl playerService;

    @Autowired
    InformationServiceImpl informationService;

    @GetMapping(value="/")
    public String home() {
        return "redirect:/selectPlayer";
    }

    @GetMapping(value="/selectPlayer")
    public String home(Model model) {
        List<Player> players = playerService.getAllPlayers();
        model.addAttribute("players", players);
        return "selectPlayer";
    }

    @GetMapping(value="/playerHome/{id}")
    public String playerHome(@PathVariable("id") Long id, Model model) {
        Optional<Player> playerOptional = playerService.getPlayer(id);
        if (playerOptional.isPresent()) {
            Player player = playerOptional.get();
            model.addAttribute("player", player);
            model.addAttribute("informations", informationService.getAllInformationForOwner(player));
        } else {
            //TODO Error
        }
        return "playerHome";
    }

//    @PostMapping(value="/interact")
//    public String interact() {
//        return "redirect:/playerHome/"+id;
//    }

    @GetMapping(value="/initGame")
    public String initGame(){
        init();
        return "redirect:/";
    }

    private void init() {
        playerService.createPlayers();
        playerService.assignTargets();
        initInformation();
    }

    private void initInformation() {
        for (Player player : playerService.getAllPlayers()) {
            for (Player otherPlayer : playerService.getAllPlayersExcept(player)) {
                //Add an information entry
                Information information = new Information(player, otherPlayer, 0);
                informationService.saveInformation(information);
            }
        }
    }


}
