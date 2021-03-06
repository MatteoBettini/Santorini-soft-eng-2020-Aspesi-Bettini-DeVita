package it.polimi.ingsw.server.cards.enums;

/**
 * Represents the possible types of a statement verb.
 * Each of them has a specific compiler to get the lambda rule.
 */
public enum StatementVerbType {
    PLAYER_EQUALS,
    STATE_EQUALS,
    HAS_FLAG,
    MOVE_LENGTH,
    EXISTS_DELTA_MORE,
    EXISTS_DELTA_LESS,
    EXISTS_LEVEL_TYPE,
    INTERACTION_NUM,
    POSITION_EQUALS,
    BUILD_NUM,
    BUILD_DOME_EXCEPT,
    BUILD_DOME,
    BUILD_IN_SAME_SPOT,
    IS_NEAR,
    ONLY_COMPLETE_TOWERS_NEAR,
    LAST_BUILD_ON,
    IS_THE_HIGHEST
}
