package com.serendipity.gameController.service.zoneService;

import com.serendipity.gameController.model.Beacon;
import com.serendipity.gameController.model.Player;
import com.serendipity.gameController.model.Zone;
import org.json.JSONArray;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface ZoneService {

    /*
     * @param id The zone's id.
     */
    Optional<Zone> getZoneById(Long id);

    /*
     * @return A list of all the zones in the database.
     */
    List<Zone> getAllZones();

    /*
     * @param zone The zone to save.
     */
    void saveZone(Zone zone);

    /*
     * @param zone The zone to delete.
     */
    void deleteZone(Zone zone);

    /*
     *
     */
    void deleteAllZones();

    /*
     * @param id The id of the zone not wanted.
     * @return A list of all the zones in the database except the one specified.
     */
    List<Zone> getAllZonesExcept(Long id);

    /*
     * @return A list of all the zones except the UN.
     */
    List<Zone> getAllZonesExceptUN();

    /*
     * @param beacon The beacon to remove from its zone.
     */
    void removeBeaconFromZone(Beacon beacon);

    /*
     * @param zone The zone you're looking at.
     * @return The number of players whose home zone is this zone.
     */
    int getNumPlayersWhoseHomeZone(Zone zone);

    /*
     * @param player The player to get a home beacon for.
     * @return The chosen zone to place the player in, if a Zone exists.
     */
    Optional<Zone> chooseHomeZone(Player player);

    /*
     * @param player The player you are calculating the zone for.
     * @param jsonBeacons A JSONArray of beacon majors, minors and rssi values.
     * @return The zone the player is in, if a Zone exists.
     */
    Optional<Zone> calculateCurrentZone(Player player, JSONArray jsonBeacons);

    /*
     * @param id The id of the zone not wanted.
     * @return A list of all the zones in the database except the one specified and the UN.
     */
    List<Zone> getAllZonesExceptUNandOne(Long id);

    /*
     * @param location The zone the player is at.
     * @return The predefined mapping.
     */
    int locationMapping(Zone location);


}
