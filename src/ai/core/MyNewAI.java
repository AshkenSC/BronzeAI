package ai.core;


import ai.core.AIWithComputationBudget;

public class MyNewAI extends AIWithComputationBudget {
    UnitTypeTable m_utt = null;

    // This is the default constructor that microRTS will call:
    public MyNewAI(UnitTypeTable utt) {
        super(-1,-1);
        m_utt = utt;
    }

    // This will be called by microRTS when it wants to create new instances of this bot (e.g., to play multiple games).
    public AI clone() {
        return new MyNewAI(m_utt);
    }
    
    // This will be called once at the beginning of each new game:    
    public void reset() {
    }
       
    // Called by microRTS at each game cycle.
    // Returns the action the bot wants to execute.
    public PlayerAction getAction(int player, GameState gs) {
        PlayerAction pa = new PlayerAction();
        pa.fillWithNones(gs, player, 10);
        return pa;
    }    
    
    // This will be called by the microRTS GUI to get the
    // list of parameters that this bot wants exposed
    // in the GUI.
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
}

