package ai;

/**
 * BronzeAI v1.4
 *
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
    public static final int workerMiddle = 2;
    public static final int workerBig = 4;
    public static final int workerMore = 8;
    private static boolean flag = false; //发起进攻的信号


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

    // Called by microRTS at each game cycle.
    // Returns the action the bot wants to execute.
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        // TODO  改进：根据时间选择策略， 思想：
        //采用workRush的基础是把对方扼杀在摇篮里,根据小地图最优，所以在16*16以下的地图中，都采用workerRushPlusPlus
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
        if (pgs.getHeight()<= 16 ) {
            workerRush(p, gs);
        }
        //策略二：采用ranged 对战 worker策略 直接攻击，不屯兵
        else if(pgs.getHeight() > 16 && pgs.getHeight() < 32)
            unitsRush(p, gs,4,3,0,3,3, true);

        //策略一和策略二经过测试，表现良好

                //策略三和策略四
                /*思想:numWR一般都为0
                    numLR数量 = [lightType.hp * 权重 + lightType.attackRange* 权重 +
                    lightType.attackTime*权重 + (lightType.minDamage +lightType.maxDamage)*权重] /
                    [lightType.cost*权重 + lightType.produceTime*权重]
                    +
                    (pgs.getHeight() * pgs.getWidth() / lightType.produceTime) * 权重;
                 */

        //策略三：采用light和ranged结合的算法

        else if(pgs.getHeight() >= 32 && pgs.getHeight() < 128) {
//            int numLR = (lightType.hp * 权重 + lightType.attackRange* 权重 +
//                    lightType.attackTime*权重 + (lightType.minDamage +lightType.maxDamage)*权重) /
//                    (lightType.cost*权重 + lightType.produceTime*权重)
//                    + (pgs.getHeight() * pgs.getWidth() / lightType.produceTime) * 权重;
            unitsRush(p, gs,0,5,3,5,workerBig, false);
        }
        //策略三：采用混合策略，调参数
        else if (pgs.getHeight() >= 128) {
            unitsRush(p,gs,0,8,4,12,workerMore,false);
        }

        return translateActions(player, gs); //返回操作
    }
    
    /*
     * 策略
	*/
    
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
    
    //策略2： unitRush，动态调整兵种构成
    /**
     *
     * @param p
     * @param gs
     * @param numWR  		进攻工兵数量
     * @param numLR   		进攻lights数量
     * @param numHR     	进攻heacys数量
     * @param numRR        	进攻远程兵数量
     * @param harvestNum    采矿数量
     * @param f           	是否积累兵力 false
     */
    public void unitsRush(Player p, GameState gs,int numWR, int numLR, int numHR, int numRR, int harvestNum,boolean f) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int maxCost = Math.max(rangedType.cost,Math.max(heavyType.cost, lightType.cost));
        int workers = countUnits(p,gs,workerType);
        if(workers < numWR + harvestNum)
            baseBehavior(p,gs);
        //所有工兵数量
        List<Unit> freeWorkers = countUnitsList(p,gs,workerType);
        List<Unit>  harvestWorkers = new LinkedList<>();
        int nbases = countUnits(p,gs,baseType);
        int barracks = countUnits(p,gs,barracksType);

        if (freeWorkers.isEmpty()) {
            return;
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();

        //建基地   注意:行为不能覆盖，不能两次获取列表（可以改变行为），可以获取个数
        if (nbases == 0) {
            if (p.getResources() >= baseType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
            }
        }

        //建兵营  注意:行为不能覆盖，不能两次获取列表（可以改变行为），可以获取个数
        if (barracks == 0) {
            if (p.getResources() >= barracksType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
            }
        }

        //worker分类
        for(Unit u: freeWorkers) {
            if(harvestWorkers.size() >= harvestNum){
                meleeCloseBehavior(u,p,gs);
            }
            else
                harvestWorkers.add(u);
        }

        harvestWorkerBehavior(harvestWorkers,p,gs);

        //获取当前各种兵的数量
        int currentL = countUnits(p,gs,lightType);
        int currentR = countUnits(p,gs,rangedType);
        int currentH = countUnits(p,gs,heavyType);

        if(barracks !=0) {
            if(currentH < numHR)
                barracksBehavior(p, gs, heavyType);
            else if(currentL < numLR)
                barracksBehavior(p, gs, lightType);
            else
                barracksBehavior(p,gs,rangedType);
        }

        if((currentR >= numRR) || (p.getResources() < maxCost) || f){
            flag = true;
            for(Unit u: pgs.getUnits()){
                if(u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID())
                    meleeCloseBehavior(u,p,gs);
            }
        } else{
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID())
                    defenseBehavior(u, p, gs);
            }
        }
    }
    
    /*
     * 建筑物行为
	*/
    
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

    //兵营行为
    public List<Unit> barracksBehavior(Player p, GameState gs, UnitType type) {
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

    /*
     * 单位行为
	*/
    
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

    //近战单位行为
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

    public void meleeCloseBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        List<Unit> enemyBase = new LinkedList<>();
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                //攻击非基地目标
                if ((closestEnemy == null || d < closestDistance)) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        }
        else{
            attack(u,null);
        }
    }
    
    //防御行为
    public void defenseBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int canAttack = 0;
        for(Unit unit : pgs.getUnits()){
            if(unit.getType().canAttack && !unit.getType().canHarvest && p.getID() == unit.getPlayer())
                canAttack++;
        }
        int defenceDistance = rangedType.attackRange + canAttack;
        int closestDistance = 0;
        int attackarea = rangedType.attackRange;
        int mybase = 0;
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
        if (closestEnemy != null && (closestDistance < attackarea || mybase < defenceDistance)) {
            attack(u, closestEnemy);
        } else {
            attack(u, null);
        }
    }
    
    /*
     * 功能函数
	*/

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

    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        return parameters;
    }

}

