package ai;

/**
 * 
 * Author: Shicheng Ai
 * An AI based on LightRush
 * 
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerDefense;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;


class Const {
	public static final int SMALL = 10;
	public static final int MID = 16;
	public static final int LARGE = 24;
}

public class BronzeAI extends AbstractionLayerAI {
    UnitTypeTable m_utt = null;
    Random r = new Random();
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType rangedType;
    UnitType heavyType;

    // This is the default constructor that microRTS will call:
    public BronzeAI(UnitTypeTable utt) {
        this(utt, new BFSPathFinding());
    }
    
    public BronzeAI(UnitTypeTable utt, PathFinding pf) {
    	super(pf);	// # set "time budget" and "iteration budget"
    	reset(utt);
    }

    // This will be called by microRTS when it wants to create new instances of this bot (e.g., to play multiple games).
    public AI clone() {
        return new BronzeAI(m_utt);
    }
    
    // This will be called once at the beginning of each new game:    
    public void reset(UnitTypeTable a_utt) {
    	m_utt = a_utt;
        workerType = m_utt.getUnitType("Worker");
        baseType = m_utt.getUnitType("Base");
        barracksType = m_utt.getUnitType("Barracks");
        lightType = m_utt.getUnitType("Light");
        rangedType = m_utt.getUnitType("Ranged");
        heavyType = m_utt.getUnitType("Heavy");
    }
       
    // Called by microRTS at each game cycle.
    // Returns the action the bot wants to execute.
    public PlayerAction getAction(int player, GameState gs) {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
        
    	// TODO: Define strategy instances
    	// 新建各个算法的对象，在PlayerAction中根据地图尺寸调用不同算法
    	WorkerRush WR = new WorkerRush(m_utt);
    	LightRush LR = new LightRush(m_utt);
    	HeavyRush HR = new HeavyRush(m_utt);
    	RangedRush RR = new RangedRush(m_utt);
    	WorkerDefense WD = new WorkerDefense(m_utt);
    	
    	Player p = gs.getPlayer(player);
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");

        // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
            	//if (pgs.getHeight() <= Const.SMALL)
            		WR.baseBehavior(u, p, pgs);
            	//else
            		//LR.baseBehavior(u, p, pgs);
            }
        }

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
            	if (pgs.getHeight() <= Const.SMALL) {
            		
            	}
            	else
            		LR.barracksBehavior(u, p, pgs);
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
            	if (pgs.getHeight() <= Const.SMALL)
            		WR.meleeUnitBehavior(u, p, gs);
            	else
            		LR.meleeUnitBehavior(u, p, gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
            }
        }
        if (pgs.getHeight() <= Const.SMALL)
        	WR.workersBehavior(workers, p, gs);
        else
        	LR.workersBehavior(workers, p, pgs);

        // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
        return translateActions(player, gs);
    }    

    public void baseBehavior(Unit u,Player p, PhysicalGameState pgs) {
        if (p.getResources()>=workerType.cost) train(u, workerType);
    }    
    
    // This will be called by the microRTS GUI to get the
    // list of parameters that this bot wants exposed
    // in the GUI.
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}

