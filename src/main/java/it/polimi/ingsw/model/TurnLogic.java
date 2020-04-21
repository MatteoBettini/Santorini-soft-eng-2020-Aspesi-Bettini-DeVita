package it.polimi.ingsw.model;

import it.polimi.ingsw.model.cardReader.enums.TriggerType;
import it.polimi.ingsw.model.enums.ActionType;
import it.polimi.ingsw.model.enums.BuildingType;
import it.polimi.ingsw.model.enums.PlayerState;
import it.polimi.ingsw.model.lambdaStrategy.exceptions.PlayerLostSignal;
import it.polimi.ingsw.model.lambdaStrategy.exceptions.PlayerWonSignal;
import it.polimi.ingsw.model.turnInfo.BuildData;
import it.polimi.ingsw.model.turnInfo.MoveData;
import it.polimi.ingsw.observe.Observable;
import it.polimi.ingsw.packets.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TurnLogic extends Observable<PacketContainer> {

    private List<Player>  stillInGamePlayers;
    private final InternalModel model;
    private Player currPlayer;
    private Set<TriggerType> currPossibleActions;
    private Worker currWorker;
    private int activePlayerIndex;


    public TurnLogic(InternalModel model){
        super();
        this.model = model;
        this.currPossibleActions = new HashSet<>();
    }

    public void start(){
        stillInGamePlayers = new ArrayList<>(model.getPlayers());
        currPlayer = stillInGamePlayers.get(0);
        currWorker = null;
        activePlayerIndex = 0;
        askNextPacket();
    }

    private void setNextPlayer(){

        if(stillInGamePlayers.size() == 0)
            return;

        if(stillInGamePlayers.size() == 1){
            PacketUpdateBoard packetUpdateBoard = new PacketUpdateBoard(null, null, null, stillInGamePlayers.get(0).getNickname());
            PacketContainer packetContainer = new PacketContainer(packetUpdateBoard);
            notify(packetContainer);
            model.setWinner(stillInGamePlayers.get(0));
            stillInGamePlayers.clear();
            return;
        }

        incrementActivePlayerIndex();

        currPlayer = stillInGamePlayers.get(activePlayerIndex);
        currWorker = null;
        currPlayer.setPlayerState(PlayerState.TURN_STARTED);
        currPlayer.clearFlags();
        currPossibleActions.clear();

        askNextPacket();
    }

    private void askNextPacket(){

        if(stillInGamePlayers.size() < 2) {
            setNextPlayer();
            return;
        }

        ActionType nextPossibleAction = null;
        currPossibleActions = currPlayer.getPossibleActions();
        if(currPossibleActions.contains(TriggerType.BUILD) && currPossibleActions.contains(TriggerType.MOVE))
            nextPossibleAction = ActionType.MOVE_BUILD;
        else if(currPossibleActions.contains(TriggerType.BUILD))
            nextPossibleAction = ActionType.BUILD;
        else if(currPossibleActions.contains(TriggerType.MOVE))
            nextPossibleAction = ActionType.MOVE;

        assert(currPlayer.getState() == PlayerState.BUILT || nextPossibleAction != null);

        switch (currPlayer.getState()) {
            case FIRST_BUILT:
                assert currWorker != null;
                if (!model.canMove(currPlayer, currWorker)) {
                    makePlayerLoose();
                    return;
                }
                break;
            case TURN_STARTED:
                if (!(model.canMove(currPlayer, currPlayer.getWorkers().get(0)) || model.canMove(currPlayer, currPlayer.getWorkers().get(1)))) {
                    makePlayerLoose();
                    return;
                }
                break;
            case MOVED:
                assert currWorker != null;
                if (!model.canBuild(currPlayer, currWorker)) {
                    makePlayerLoose();
                    return;
                }
                break;
            case BUILT:
                assert currWorker != null;
                setNextPlayer();
                return;
        }
        //System.out.println("Sending do action to " + currPlayer.getNickname());
        PacketDoAction packetDoAction = new PacketDoAction(currPlayer.getNickname(), nextPossibleAction);
        PacketContainer packetContainer = new PacketContainer(packetDoAction);
        notify(packetContainer);

    }

    public void consumePacketMove(String senderID, PacketMove packetMove) throws InvalidPacketException{

        if(!senderID.equals(currPlayer.getNickname()))
            return;

        if(!packetMove.getPlayerNickname().equals(currPlayer.getNickname()))
            throw new InvalidPacketException();

        if(currWorker != null)
            if(!packetMove.getWorkerID().equals(currWorker.getID()))
                throw new InvalidPacketException();

        if(!currPossibleActions.contains(TriggerType.MOVE))
            throw new InvalidPacketException();


        String winner = null;

        Map<String, Point> workersPosition = null;
        MoveData moveData = model.packetMoveToMoveData(packetMove);
        try{

            if(!model.makeMove(moveData))
                throw new InvalidPacketException();

            currWorker = moveData.getWorker();
            workersPosition = new HashMap<>();
            for(Player p : stillInGamePlayers)
                for(Worker w : p.getWorkers())
                    workersPosition.put(w.getID(),w.getPosition());

        } catch (PlayerWonSignal playerWonSignal) {
            assert playerWonSignal.getPlayer().equals(currPlayer);

            model.setWinner(currPlayer);
            winner = currPlayer.getNickname();

            currWorker = moveData.getWorker();
            workersPosition = new HashMap<>();
            for(Player p : stillInGamePlayers)
                for(Worker w : p.getWorkers())
                    workersPosition.put(w.getID(),w.getPosition());

            stillInGamePlayers.clear();

        } catch (PlayerLostSignal playerLostSignal) {
            assert playerLostSignal.getPlayer().equals(currPlayer);
            makePlayerLoose();
            return;
        }

        PacketUpdateBoard packetUpdateBoard = new PacketUpdateBoard(workersPosition,null,null,winner);
        PacketContainer packetContainer = new PacketContainer(packetUpdateBoard);
        notify(packetContainer);

        assert stillInGamePlayers.size() != 1;
        if(stillInGamePlayers.size() > 0)
            askNextPacket();
    }

    public void consumePacketBuild(String senderID, PacketBuild packetBuild) throws InvalidPacketException{

        if(!senderID.equals(currPlayer.getNickname()))
            return;

        if(!packetBuild.getPlayerNickname().equals(currPlayer.getNickname()))
            throw new InvalidPacketException();

        if(currWorker != null)
            if(!packetBuild.getWorkerID().equals(currWorker.getID()))
                throw new InvalidPacketException();


        if(!currPossibleActions.contains(TriggerType.BUILD))
            throw new InvalidPacketException();

        String winner = null;

        Map<Point, List<BuildingType>> newBuildings = null;
        BuildData buildData = model.packetBuildToBuildData(packetBuild);
        try{
            if(!model.makeBuild(buildData))
                throw new InvalidPacketException();

            newBuildings = new HashMap<>(buildData.getData());
            currWorker = buildData.getWorker();

        } catch (PlayerWonSignal playerWonSignal) {
            assert playerWonSignal.getPlayer().equals(currPlayer);

            model.setWinner(currPlayer);
            winner = currPlayer.getNickname();

            newBuildings = new HashMap<>(buildData.getData());
            currWorker = buildData.getWorker();

            stillInGamePlayers.clear();
        } catch (PlayerLostSignal playerLostSignal) {
            assert playerLostSignal.getPlayer().equals(currPlayer);
            makePlayerLoose();
            return;
        }


        PacketUpdateBoard packetUpdateBoard = new PacketUpdateBoard(null,newBuildings,null,winner);
        PacketContainer packetContainer = new PacketContainer(packetUpdateBoard);
        notify(packetContainer);

        assert stillInGamePlayers.size() != 1;
        if(stillInGamePlayers.size() > 0)
            askNextPacket();
    }

    public void getPossibleMoves(String senderID, PacketMove packetMove) {

        if (!senderID.equals(currPlayer.getNickname()) || !packetMove.getPlayerNickname().equals(currPlayer.getNickname()))
            return;

        boolean forBothWorkers = false;

        if (currWorker != null) {
            if (!packetMove.getWorkerID().equals(currWorker.getID()))
                return;
        } else {
            forBothWorkers = true;
            if(!packetMove.getWorkerID().equals(currPlayer.getWorkers().get(0).getID()) && !packetMove.getWorkerID().equals(currPlayer.getWorkers().get(1).getID()))
                return;
        }

        if (!currPossibleActions.contains(TriggerType.MOVE))
            return;

        Worker myWorker = model.getWorkerByID(packetMove.getWorkerID());
        Worker myOtherWorker = currPlayer.getWorkers().stream().filter(x -> !x.getID().equals(packetMove.getWorkerID())).findAny().orElse(null);
        assert myOtherWorker != null;
        Map<String,Set<Point>> possibleMoves = new HashMap<>();
        Set<Point> possiblePointsW1 = new HashSet<>();
        Set<Point> possiblePointsW2 = new HashSet<>();
        possibleMoves.put(myWorker.getID(), possiblePointsW1);
        possibleMoves.put(myOtherWorker.getID(), possiblePointsW2);

        if (!packetMove.getMove().isEmpty()){
            MoveData moveData = null;
            try {
                moveData = model.packetMoveToMoveData(packetMove);
            } catch (InvalidPacketException e) {
                sendPossibleMoves(currPlayer.getNickname(), possibleMoves);
                return;
            }

            possiblePointsW1.addAll(model.getPossibleMoves(moveData));

        } else {
            possiblePointsW1.addAll(model.getPossibleMoves(currPlayer, myWorker));
            if(forBothWorkers){
                possiblePointsW2.addAll(model.getPossibleMoves(currPlayer, myOtherWorker));
            }
        }
        sendPossibleMoves(currPlayer.getNickname(), possibleMoves);
    }

    public void getPossibleBuilds(String senderID, PacketBuild packetBuild) {

        if (!senderID.equals(currPlayer.getNickname()) || !packetBuild.getPlayerNickname().equals(currPlayer.getNickname()))
            return;

        boolean forBothWorkers = false;

        if (currWorker != null) {
            if (!packetBuild.getWorkerID().equals(currWorker.getID()))
                return;
        } else {
            forBothWorkers = true;
            if(!packetBuild.getWorkerID().equals(currPlayer.getWorkers().get(0).getID()) && !packetBuild.getWorkerID().equals(currPlayer.getWorkers().get(1).getID()))
                return;
        }

        if (!currPossibleActions.contains(TriggerType.BUILD))
            return;

        Worker myWorker = model.getWorkerByID(packetBuild.getWorkerID());
        Worker myOtherWorker = currPlayer.getWorkers().stream().filter(x -> !x.getID().equals(packetBuild.getWorkerID())).findAny().orElse(null);
        assert myOtherWorker != null;

        Map<String, Map<Point, List<BuildingType>>> possibleBuilds = new HashMap<>();

        Map<Point, List<BuildingType>> possibleBuildsW1 = new HashMap<>();
        Map<Point, List<BuildingType>> possibleBuildsW2 = new HashMap<>();

        if (!packetBuild.getBuilds().isEmpty()){
            BuildData buildData = null;
            try {
                buildData = model.packetBuildToBuildData(packetBuild);
            } catch (InvalidPacketException e) {
                possibleBuilds.put(myWorker.getID(), possibleBuildsW1);
                possibleBuilds.put(myOtherWorker.getID(), possibleBuildsW2);
                sendPossibleBuilds(currPlayer.getNickname(), possibleBuilds);
                return;
            }

            possibleBuilds.put(myWorker.getID(), model.getPossibleBuildsAdvanced(buildData));
            possibleBuilds.put(myOtherWorker.getID(), possibleBuildsW2);

        } else {
            possibleBuilds.put(myWorker.getID(), model.getPossibleBuildsAdvanced(currPlayer,myWorker));
            if(forBothWorkers){
                possibleBuilds.put(myOtherWorker.getID(),  model.getPossibleBuildsAdvanced(currPlayer, myOtherWorker));
            }else{
                possibleBuilds.put(myOtherWorker.getID(),  possibleBuildsW2);
            }
        }
        sendPossibleBuilds(currPlayer.getNickname(), possibleBuilds);
    }

    private void sendPossibleBuilds(String to, Map<String, Map<Point, List<BuildingType>>> possibleBuilds){
        PacketPossibleBuilds packetPossibleBuilds = new PacketPossibleBuilds(to, possibleBuilds);
        PacketContainer packetContainer = new PacketContainer(packetPossibleBuilds);
        notify(packetContainer);
    }

    private void sendPossibleMoves(String to, Map<String,Set<Point>> possibleMoves){
        PacketPossibleMoves packetPossibleMoves = new PacketPossibleMoves(to, possibleMoves);
        PacketContainer packetContainer = new PacketContainer(packetPossibleMoves);
        notify(packetContainer);
    }

    private void incrementActivePlayerIndex() {
        if (activePlayerIndex >= stillInGamePlayers.size() - 1)
            activePlayerIndex = 0;
        else
            ++activePlayerIndex;
    }

    private void makePlayerLoose(){

        model.addLoser(currPlayer);

        if(activePlayerIndex == 0)
            activePlayerIndex = stillInGamePlayers.size()-1;
        else
            activePlayerIndex--;
        stillInGamePlayers.remove(currPlayer);

        PacketUpdateBoard packetUpdateBoard = new PacketUpdateBoard(null, null, currPlayer.getNickname(), null);
        PacketContainer packetContainer = new PacketContainer(packetUpdateBoard);
        notify(packetContainer);
        setNextPlayer();
    }

}