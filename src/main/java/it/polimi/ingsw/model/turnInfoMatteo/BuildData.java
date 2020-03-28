package it.polimi.ingsw.model.turnInfoMatteo;


import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.enums.BuildingType;

import java.awt.*;
import java.util.*;

/**
 * 
 */
public class BuildData extends TurnData {


    /**
     * 
     */
    private final Map<Point, BuildingType> data;

    /**
     * Default constructor
     */
    public BuildData(Player player, Worker worker, Map<Point,BuildingType> data) {
        super(Player player, Worker worker);
        this.data = data;
    }

    /**
     * @return
     */
    public Map<Point, BuildingType> getData() {
        // TODO implement here
        return null;
    }

}