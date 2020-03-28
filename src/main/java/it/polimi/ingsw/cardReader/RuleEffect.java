package it.polimi.ingsw.cardReader;

import it.polimi.ingsw.cardReader.enums.EffectType;
import it.polimi.ingsw.model.enums.PlayerState;
import org.w3c.dom.Element;

import java.util.Objects;

/**
 * This class represent a rule effect, which will be applied if
 * its rule evaluates true.
 * It MAY also carry additional information used during effect compilation into lambda effect
 */
public class RuleEffect {

    private final EffectType type;
    private final PlayerState playerNextState;
    private final Element data;

    public RuleEffect(EffectType type, PlayerState playerNextState, Element data) {
        this.type = type;
        this.playerNextState = playerNextState;
        this.data = data;
    }

    /**
     * Getter for the effect type of this rule effect
     * @return Enum value corresponding to this effect type
     */
    public EffectType getType(){
        return this.type;
    }

    /**
     * Getter for the player's next state
     * @return Enum value corresponding to the desired player next state
     */
    public PlayerState getNextState(){
        return this.playerNextState;
    }

    /**
     * Getter for the unconverted data tag of the effect.
     * Will be compiled into code during rule compilation.
     * @return XML Element containing effect additional info
     *         null, if data is not present
     */
    public Element getData(){
        return this.data;
    }

    /**
     * Compares two RuleEffect, using the internal state instead of the memory location
     * @param o Object to compare to
     * @return True if the two objects contains the same information
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleEffect that = (RuleEffect) o;
        return type == that.type &&
                playerNextState == that.playerNextState &&
                Objects.equals(data, that.data);
    }

    /**
     * Return an hash code for this class, using the internal information
     * @return The generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, playerNextState, data);
    }
}
