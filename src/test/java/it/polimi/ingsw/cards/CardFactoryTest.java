package it.polimi.ingsw.cards;

import it.polimi.ingsw.cards.exceptions.CardLoadingException;
import it.polimi.ingsw.cards.exceptions.InvalidCardException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardFactoryTest {

    @Test
    void testInitialization(){
        try{
            CardFactory factory = CardFactory.getInstance();
            assert true;
        } catch (CardLoadingException | InvalidCardException e) {
            assert false;
        }
    }

    @Test
    void valuesNotNull(){
        CardFactory factory = null;
        try{
            factory = CardFactory.getInstance();
            assert true;
        } catch (CardLoadingException | InvalidCardException e) {
            assert false;
        }
        CardFile defaultStrategy = factory.getDefaultStrategy();
        List<CardFile> cardFiles = factory.getCards();
        assertNotNull(defaultStrategy);
        assertNotNull(cardFiles);
    }
}