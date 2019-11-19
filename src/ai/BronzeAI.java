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
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import rts.UnitActionAssignment;


/*
based: worker,
barracks: light, heavy, ranged
worker: base, barracks
 */
public class BronzeAI extends AbstractionLayerAI {
    public static final int SMALL = 8;
    public static final int MIDDLE = 24;
    public static final int BIG = 32;
    public static final int WORKER_SMALL = 1; //16*16����
    public static final int WORKER_MIDDLE = 2;
    public static final int WORKER_BIG = 4;
    public static final int WORKER_MORE = 8;
    private static boolean flag = false; //����������ź�
    private static int DEFENSEDIS = 1;

    private static int TIME = 0;
    //�����Ķ���


    UnitTypeTable m_utt = null;
    Random r = new Random();
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType rangedType;
    UnitType heavyType;
    UnitType resourceType;


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
        resourceType = m_utt.getUnitType("Resource");
    }


    // Called by microRTS at each game cycle.
    // Returns the action the bot wants to execute.
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        // TODO  �Ľ�������ʱ��ѡ����ԣ� ˼�룺
        //����workRush�Ļ����ǰѶԷ���ɱ��ҡ����,����С��ͼ���ţ�������16*16���µĵ�ͼ�У�������workRushPlus

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
        //һ�㶼��һ������
        for (Unit mybase : myBases) {
            for (Unit enemyBase : enemyBases) {
                minDisBases = Math.min(minDisBases, Math.abs(mybase.getX() - enemyBase.getX()) + Math.abs(mybase.getY() - enemyBase.getY()));
            }
        }


        //������Ҫ˼�룺
        //����һ�����ؾ����������С��ͼ�����ã��ý�workerRush��
        if (pgs.getHeight() <= 16) {
            workerRush(p, gs);
        }
        //���Զ�������ranged ��ս worker���� ֱ�ӹ��������ͱ�
        else if (pgs.getHeight() > 16 && pgs.getHeight() < 32)
            unitsRush(p, gs, 4, 3, 0, 3, 3, true);

            //����һ�Ͳ��Զ��������ԣ���������

            //�������Ͳ�����
                /*˼��:numWRһ�㶼Ϊ0
                    numLR���� = [lightType.hp * Ȩ�� + lightType.attackRange* Ȩ�� +
                    lightType.attackTime*Ȩ�� + (lightType.minDamage +lightType.maxDamage)*Ȩ��] /
                    [lightType.cost*Ȩ�� + lightType.produceTime*Ȩ��]
                    +
                    (pgs.getHeight() * pgs.getWidth() / lightType.produceTime) * Ȩ��;
                 */

            //������������light��ranged��ϵ��㷨

        else if (pgs.getHeight() >= 32 && pgs.getHeight() < 64) {
//            int numLR = (lightType.hp * Ȩ�� + lightType.attackRange* Ȩ�� +
//                    lightType.attackTime*Ȩ�� + (lightType.minDamage +lightType.maxDamage)*Ȩ��) /
//                    (lightType.cost*Ȩ�� + lightType.produceTime*Ȩ��)
//                    + (pgs.getHeight() * pgs.getWidth() / lightType.produceTime) * Ȩ��;
            unitsRush(p, gs, 0, 3, 0, 3, WORKER_BIG, false);
        }
        //�����������û�ϲ��ԣ�������
        else if (pgs.getHeight() >= 64 && pgs.getHeight() < 128) {
            unitsRush(p, gs, 0, 4, 1, 6, 6, false);
        } else if (pgs.getHeight() >= 128) {
            unitsRush(p, gs, 0, 8, 4, 10, WORKER_MORE, false);
        }

        return translateActions(player, gs); //���ز���
    }

    //---------------------------����1�����ڸĽ�workerRush���ԣ� ��������纯����ʾ----------------------------
    public void workerRush(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        //�ҵ�����������������
        baseBehavior(p, gs);
        List<Unit> harvestWorker = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest && u.getPlayer() == p.getID()) {
                if (harvestWorker.size() < WORKER_SMALL) {
                    harvestWorker.add(u);
                } else {
                    meleeBehavior(u, p, gs);  //ֱ��ȥ����
                }
            }
        }
        harvestWorkerGroup(harvestWorker, p, gs);

    }

    //������Ϊ��ֻ����������
    public void baseBehavior(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType && u.getPlayer() == p.getID() && gs.getActionAssignment(u) == null) {
                if (p.getResources() >= workerType.cost && u.getPlayer() == p.getID()) {
                    train(u, workerType); //u�ǻ���
                }
            }
        }
    }

    //�������˹����������������, ÿ����Ӫ������
    public void barracksBehavior(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType && u.getPlayer() == p.getID() && gs.getActionAssignment(u) == null) {
                if (p.getResources() >= type.cost) {
                    train(u, type);
                }
            }
        }
    }

    //���£�����ɿ���Ϊ���ѹ����ɼ�������Դ���ȷ��ڻ���Ȼ���ڲ�ȡ������Ϊ�������������Դ��
    public void harvestWorkerGroup(List<Unit> harvestWorkers, Player p, GameState gs) {
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
            } else if (p.getResources() != 0 && closestBase != null) {
                harvest(worker, worker, closestBase);
            } else {
                meleeBehavior(worker, p, gs);
            }
        }
    }

    ///���£�����ɿ���Ϊ���ѹ����ɼ�������Դ���ȷ��ڻ���Ȼ���ڲ�ȡ������Ϊ�������������Դ��
    public void harvestWorkerBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestBase = null;
        Unit closestResource = null;
        int closestDistance = 0;

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestResource == null || d < closestDistance) {
                    closestResource = u2;
                    closestDistance = d;
                }
            }
        }
        closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestBase == null || d < closestDistance) {
                    closestBase = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestResource != null && closestBase != null) {
            AbstractAction aa = getAbstractAction(u);
            if (aa instanceof Harvest) {
                Harvest h_aa = (Harvest) aa;
                if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                    harvest(u, closestResource, closestBase);
                }
            } else {
                harvest(u, closestResource, closestBase);
            }
        } else if (p.getResources() != 0 && closestBase != null) {
            harvest(u, u, closestBase);
        } else {
            meleeBehavior(u, p, gs);
        }
    }

    //���´��룺���������У����ȹ����������ڹ������أ�С��ͼ����Ч��,
    public void meleeBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        List<Unit> enemyBase = new LinkedList<>();
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                //�����ǻ���Ŀ��
                if ((closestEnemy == null || d < closestDistance) && u2.getType() != baseType) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        for (Unit u3 : pgs.getUnits()) {
            if (u3.getPlayer() >= 0 && u3.getPlayer() != p.getID() && u3.getType() == baseType) {
                enemyBase.add(u3);
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        } else if (!enemyBase.isEmpty()) {
            for (Unit base : enemyBase)
                attack(u, base);
        } else {
            attack(u, null);
        }
    }
    //-----------------------------------����һ����--------------------------------


    //-----------------------------------���Զ�----------------------------------

    /**
     * @param p
     * @param gs
     * @param numWR      ������������
     * @param numLR      ����lights����
     * @param numHR      ����heacys����
     * @param numRR      ����Զ�̱�����
     * @param harvestNum �ɿ�����
     * @param f          �Ƿ���۱��� false�ͻ��۱� true�ͽ���
     * @Description
     */
    public void unitsRush(Player p, GameState gs, int numWR, int numLR, int numHR, int numRR, int harvestNum, boolean f) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        boolean lock = true; //�жϵ�ͼ�Ƿ�����
//        int minCost = Math.min(rangedType.cost,Math.min(heavyType.cost, lightType.cost));

        //������Ϊ
        int workers = countUnits(p, gs, workerType);
        if (workers < numWR + harvestNum)
            baseBehavior(p, gs);
        //������Ϊ
        //��ȡ�з����ؾ���
        int baseDistance = Integer.MAX_VALUE;
        List<Unit> myBases = new ArrayList<>();
        List<Unit> enemyBases = new ArrayList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType) {
                if (u.getPlayer() == p.getID()) {
                    myBases.add(u);
                } else {
                    enemyBases.add(u);
                }
            }
        }
        //һ�㶼��һ������
        for (Unit mybase : myBases) {
            for (Unit enemyBase : enemyBases) {
                baseDistance = Math.min(baseDistance, Math.abs(mybase.getX() - enemyBase.getX()) + Math.abs(mybase.getY() - enemyBase.getY()));
            }
        }


//        for(Unit worker : pgs.getUnits()){
//            if(worker.getType()==workerType && worker.getPlayer() == p.getID() ) {
//                UnitActionAssignment uaa = gs.getActionAssignment(worker);
//                if (uaa != null ) {
//                    System.out.println("--------------����������=================");
//                    System.out.println(uaa.action.getType());
//                }
//
//
//            }
//        }

        //------------------------�����ǹ�����Ϊ-------------------------------------
        //���й�������
        List<Unit> freeWorkers = countUnitsList(p, gs, workerType);
        List<Unit> harvestWorkers = new LinkedList<>();
        int nbases = countUnits(p, gs, baseType);
        int barracks = countUnits(p, gs, barracksType);

        if (freeWorkers.isEmpty()) {
            return;
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();

        //������   ע��:��Ϊ���ܸ��ǣ��������λ�ȡ�б����Ըı���Ϊ�������Ի�ȡ����
        if (nbases == 0) {
            if (p.getResources() >= baseType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
            }
        }

        //����Ӫ  ע��:��Ϊ���ܸ��ǣ��������λ�ȡ�б����Ըı���Ϊ�������Ի�ȡ����
        if (barracks == 0) {
            if (p.getResources() >= barracksType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u, barracksType, u.getX(), u.getY(), reservedPositions, p, pgs);
            }
        }

        //worker�������
        for (Unit u : freeWorkers) {
            if (harvestWorkers.size() >= harvestNum) {
                meleeCloseBehavior(u, p, gs);
            } else
                harvestWorkers.add(u);
        }

        harvestWorkerGroup(harvestWorkers, p, gs);
        //-------------------------�����ǹ�����Ϊ---------------------------------------


        //���������ڵȴ�״̬��������,ֻҪ��һ���������˶������������
        for (Unit worker : pgs.getUnits()) {
            if (worker.getPlayer() == p.getID() && worker.getType() == workerType) {
                UnitActionAssignment uaa = gs.getActionAssignment(worker);
                if (uaa != null && uaa.action.getType() != UnitAction.TYPE_NONE) {
                    lock = false;
//                    System.out.println("Unlock");
                    break;
                }
            }
        }
        if (lock) {
            TIME++;
            if (TIME % 10 == 9) {
                lock = true;
            } else
                lock = false;
        }

        //-------------------------��Ӫ��Ϊ-------------------------------------
        //��ȡ��ǰ���ֵ�������
        int numL = countUnits(p, gs, lightType);
        int numR = countUnits(p, gs, rangedType);
        int numH = countUnits(p, gs, heavyType);
        int total = numL + numR + numH;


        //����˳��
        if (barracks != 0) {
            if (numL < (numLR + 1) / 2 && p.getResources() >= lightType.cost)
                barracksBehavior(p, gs, lightType);
            else if (numR < (numRR + 1) / 2 && p.getResources() >= rangedType.cost)
                barracksBehavior(p, gs, rangedType);
            else if (numH < (numHR + 1) / 2 && p.getResources() >= heavyType.cost)
                barracksBehavior(p, gs, heavyType);
            else if (numL < total * numLR / (numLR + numLR + numHR) && p.getResources() >= lightType.cost) { //ranged̫��ʱ
                barracksBehavior(p, gs, lightType);
            } else if (numR < total * numRR / (numLR + numLR + numHR) && p.getResources() >= rangedType.cost) {
                barracksBehavior(p, gs, rangedType);
            } else if (numH < total * numHR / (numLR + numLR + numHR) && p.getResources() >= heavyType.cost)
                barracksBehavior(p, gs, heavyType);
            else
                barracksBehavior(p, gs, lightType);

        }
        //-------------------------��Ӫ��Ϊ����-----------------------------------


        //------------------------������������������Ϊ------------------------------

        //����ͳ�Ʊ������
        numL = countUnits(p, gs, lightType);
        numR = countUnits(p, gs, rangedType);
        numH = countUnits(p, gs, heavyType);
        total = numL + numR + numH;
        //�ж����޿�
        int resource = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == resourceType) {
                resource++;
            }
        }
        //�����Ȼ��۱���
        //��ͼ��Դ�޿���ȫ������
        if (resource == 0) {
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && u.getPlayer() == p.getID()) {
                    //�ǹ���
                    if (!u.getType().canHarvest) {
                        meleeCloseBehavior(u, p, gs);
                    }
                    //��������Դ�Ž������ڽ���
                    else {
                        harvestWorkerBehavior(u, p, gs);
                    }
                }
            }
        }
        //�������߻��۱�����,���߶�����غܽ��ͽ��������������ⶼ����(flagΪ�����ź�)��ֻҪ�����˾�һֱ����
        else if (total >= (numLR + numLR + numHR) || f || flag || DEFENSEDIS > baseDistance / 2 || (p.getResources() < lightType.cost && pgs.getHeight() >128)) {
            flag = true;
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID())
                    meleeCloseBehavior(u, p, gs);
            }
        } else {
            if (lock)
                DEFENSEDIS++;
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID()) {
                    defenseBehavior(u, p, gs);
                }
            }
        }
        //-----------------------------����������ı�����Ϊ����------------------------------------
    }


    //����Unit���͹���
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

    //ͳ���ض����͵ı�������
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

    //���ݿɹ��������������⣩�趨������Χ
    public void defenseBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int canAttack = 0;
        for (Unit unit : pgs.getUnits()) {
            if (unit.getType().canAttack && !unit.getType().canHarvest && p.getID() == unit.getPlayer()) {
//                System.out.println("____________����__________");
//                UnitActionAssignment uaa = gs.getActionAssignment(unit);
//                if(uaa!=null){
//                    System.out.println(uaa.action.getType());
//                }
//                System.out.println();
//                System.out.println(unit.getUnitActions(gs));
                canAttack++;
            }
        }
        int defenceDistance = rangedType.attackRange + DEFENSEDIS;
        System.out.println("------------------����������---------------------");
        System.out.println(defenceDistance);
        int closestDistance = 0;
        int attackarea = lightType.attackRange + 2;

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


    public void meleeCloseGroup(List<Unit> group, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;

        for (Unit u : group) {
            if (u != null) {
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                        int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                        //�����ǻ���Ŀ��
                        if ((closestEnemy == null || d < closestDistance)) {
                            closestEnemy = u2;
                            closestDistance = d;
                        }
                    }
                }
                if (closestEnemy != null) {
                    attack(u, closestEnemy);
                } else {
                    attack(u, null);
                }
            }
        }
    }

    public void meleeCloseBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                //�����ǻ���Ŀ��
                if ((closestEnemy == null || d < closestDistance)) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
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

