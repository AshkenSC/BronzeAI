package ai;

/**
 * Author: Shicheng Ai, Jianhai Wang
 * An AI based on baseline AIs
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.Move;
import ai.abstraction.pathfinding.AStarPathFinding;
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
import util.Pair;


/*
based: worker,
barracks: light, heavy, ranged
worker: base, barracks
 */
public class BronzeAI extends AbstractionLayerAI {
    public static final int SMALL = 8;
    public static final int MIDDLE = 24;
    public static final int BIG = 32;
    public static final int workerSmall = 1; //16*16以下
    public static final int workerMiddle = 5;
    public static final int workerBig = 4;
    public static final int workerMore = 8;



    private static List<Unit> lightsList = new LinkedList<>();
    private static List<Unit> heaysList = new LinkedList<>();
    private static List<Unit> rangedsList = new LinkedList<>();


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
        super(pf);    // # set "time budget" and "iteration budget"
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

    //基地行为，只能生产工兵
    public List<Unit> baseBehavior(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        List<Unit> works = new LinkedList<>();
        for(Unit u: pgs.getUnits()) {
            if (u.getType()==baseType && u.getPlayer() == p.getID() && gs.getActionAssignment(u)==null) {
                if (p.getResources() >= workerType.cost && u.getPlayer() == p.getID()) {
                    train(u, workerType);
                    works.add(u);
                }
            }
        }
        return works;
    }

    //产生除了工兵意以外的其他兵, 每个兵营都产生
    public List<Unit> barracksBehavior( Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        List<Unit> list = new LinkedList<>();
        for(Unit u:pgs.getUnits()) {
            if (u.getType()==barracksType && u.getPlayer() == p.getID() && gs.getActionAssignment(u)==null) {
                if (p.getResources() >= type.cost) {
                    train(u, type);
                    list.add(u);
                }
            }
        }
        return list;
    }

    // Called by microRTS at each game cycle.
    // Returns the action the bot wants to execute.
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        // TODO  改进：根据时间选择策略， 思想：
        //采用workRush的基础是把对方扼杀在摇篮里,根据小地图最优，所以在16*16以下的地图中，都采用workRushPlus
        //和其他策略的时间差
        //采取策略之前的计算

        int produce = barracksType.produceTime + lightType.produceTime - workerType.produceTime;
        List<Unit> myBases = new ArrayList<>();
        List<Unit> enemyBases = new ArrayList<>();
        int minDisBases = Integer.MAX_VALUE;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType) {
                if (u.getPlayer() == player) {
                    myBases.add(u);
                } else {
                    enemyBases.add(u);
                }
            }
        }
        //一般都是一个基地
        for (Unit mybase : myBases) {
            for (Unit enemyBase : enemyBases) {
                minDisBases = Math.min(minDisBases, Math.abs(mybase.getX() - enemyBase.getX()) + Math.abs(mybase.getY() - enemyBase.getY()));
            }
        }


        //策略主要思想：
        //策略一：基地距离近，或者小地图都采用（该进workerRush）1
        if (pgs.getHeight()<32 && minDisBases < produce) {
            workerRush(p, gs);
        }
        //策略二：采用Light策略 2
        else if(pgs.getHeight() >= 32 && pgs.getHeight() < 128) {
            unitsRush(p, gs, workerMiddle,3, 1, 5);
        }
        //策略三：采用混合策略，调参数
        else if (pgs.getHeight() >= 128) {
            unitsRush(p,gs,workerBig,0,5,3);
        }

        return translateActions(player, gs); //返回操作
    }

    //策略1：基于workerRush策略
    public void workerRush(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //找到己方基地生产工兵
        baseBehavior(p, gs);
        List<Unit> harvestWorker = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest && u.getPlayer() == p.getID()) {
                if (harvestWorker.size() < workerSmall) {
                    harvestWorker.add(u);
                } else {
                    meleeBehavior(u, p, gs);  //直接去攻击
                }
            }
        }
        harvestWorkerBehavior(harvestWorker, p, gs);

    }

    //采矿行为
    public void harvestWorkerBehavior(List<Unit> harvestWorkers, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestBase = null;
        Unit closestResource = null;
        int closestDistance = 0;
        for (Unit worker : harvestWorkers) {
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isResource) {
                    int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                    if (closestResource == null || d < closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                    int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) {
                AbstractAction aa = getAbstractAction(worker);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest) aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                        harvest(worker, closestResource, closestBase);
                    }
                } else {
                    harvest(worker, closestResource, closestBase);
                }
            } else if (p.getResources() != 0 && closestBase!=null) {
                harvest(worker, worker, closestBase);
            } else {
                meleeBehavior(worker,p,gs);
            }
        }
    }

    //距离近，疯狂进攻，生产即进攻（任意兵种都可以进攻）
    public void meleeBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        List<Unit> enemyBase = new LinkedList<>();
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                //攻击非基地目标
                if ((closestEnemy == null || d < closestDistance) && u2.getType()!=baseType) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        for (Unit u3 : pgs.getUnits()) {
            if (u3.getPlayer() >= 0 && u3.getPlayer() != p.getID() && u3.getType()==baseType) {
              enemyBase.add(u3);
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        } else if(!enemyBase.isEmpty()){
            for(Unit base: enemyBase)
                attack(u,base);
        } else{
            attack(u,null);
        }
    }


    //归类
    public List<Unit> countUnitsList(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        List<Unit> list = new LinkedList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == type && u.getPlayer() == p.getID()) {
                list.add(u);
            }
        }
        return list;
    }
//      无任务兵
    public List<Unit> countUnitsListUnAssigment(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        List<Unit> list = new LinkedList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == type && u.getPlayer() == p.getID()&&gs.getActionAssignment(u) == null) {
                list.add(u);
            }
        }
        return list;
    }

    //数兵
    public int countUnits(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int number = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == type && u.getPlayer() == p.getID()) {
                number++;
            }
        }
        return number;
    }

    public int countUnitsUnagginment(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int number = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == type && u.getPlayer() == p.getID() && gs.getActionAssignment(u) ==null) {
                number++;
            }
        }
        return number;
    }

    public int countUnitsAgginment(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int number = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == type && u.getPlayer() == p.getID() && gs.getActionAssignment(u) !=null) {
                number++;
            }
        }
        return number;
    }

    //策略2：轻兵策略
    public void unitsRush(Player p, GameState gs,int numWork, int numLR, int numHR, int numRR) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int maxCost = Math.max(rangedType.cost,Math.max(heavyType.cost, lightType.cost));
        int workers = countUnits(p,gs,workerType);
        if(workers < numWork)
            baseBehavior(p,gs);
        int resourcesUsed = 0;
        List<Unit> freeWorkers = countUnitsList(p,gs,workerType);
        int barracks = countUnits(p,gs,barracksType);

        if (freeWorkers.isEmpty()) {
            return;
        }
        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (barracks == 0) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
            }
        }
        harvestWorkerBehavior(freeWorkers,p,gs);

        if(barracks !=0) {
            if (lightsList.size() < numLR) {
                lightsList.addAll(barracksBehavior(p, gs, lightType));
            }
            else if (rangedsList.size() < numRR)
                barracksBehavior(p, gs, rangedType);
            else if (heaysList.size() < numHR)
                barracksBehavior(p, gs, heavyType);
        }
        if(lightsList.size() >= numLR && rangedsList.size() >= numRR && heaysList.size() >= numHR) {

        }
        System.out.println(lightsList.size());
        if(lightsList.size() >= numLR  || p.getResources() < maxCost){
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID())
                    meleeBehavior(u, p, gs);
            }
        } else{
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID())
                    defenseBehavior(u, p, gs);
            }
        }
    }

    public void defenseBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int defenceDistance = 0;
        int closestDistance = 0;
        int attackarea = 4;
        int mybase = 0;
        //统计敌方最近目标
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            } else if (u2.getPlayer() == p.getID() && u2.getType() == baseType) {
                mybase = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
            }
        }
        if (u.getType() == lightType) {
            defenceDistance = 6;
            attackarea = 6;

        }
        else {
            defenceDistance = 4;
            attackarea = 10;
        }
        if (closestEnemy != null && (closestDistance < attackarea || mybase < defenceDistance)) {
            attack(u, closestEnemy);
        } else {
            attack(u, null);
        }
    }

    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        return parameters;
    }

}

