package eu.monnetproject.translation.sources.ewn.api;


/**
 * Class that contains the string representations of the different EWN relations.
 *
 * @author Zeno Gantner (changes by Mauricio Espinoza)
 * 
 */
public class Relations {

    
    public final static String[] EWN_RELATION_GROUPS = {
        "synonym relations",
        "is-a relations",
        "part-of relations",
        "antonym relations",
        "causal relations",
        "subevent relations",
        "role relations",
        "manner relations",
        "state relations",
        "fuzzynym relations"
    };
    
    public final static int[] EWN_RELATION_GROUPS_START = {
        0, 2, 6, 17, 20, 22, 24, 56, 58, 60
    };
    
    public final static String[] EWN_RELATION_NAMES = {
	"near_synonym", "xpos_near_synonym",

	"has_hyperonym", "has_hyponym",
	"has_xpos_hyperonym", "has_xpos_hyponym",

        "has_holonym", "has_holo_part", "has_holo_member", "has_holo_portion",
        "has_holo_madeof", "has_holo_location",
	"has_meronym", "has_mero_part", "has_mero_member", "has_mero_madeof",
        "has_mero_location",
		
	"antonym", "near_antonym", "xpos_near_antonym",
		
	"causes", "is_caused_by",
		
	"has_subevent", "is_subevent_of",
		
	"role", "role_agent", "role_instrument", "role_patient", "role_location",
	"role_direction", "role_source_direction", "role_target_direction",
        "role_result", "role_manner",
        "involved", "involved_agent", "involved_patient", "involved_instrument",
        "involved_location", "involved_direction", "involved_source_direction",
        "involved_target_direction", "involved_result",
	"co_role", "co_agent_patient", "co_agent_instrument", "co_agent_result",
	"co_patient_agent", "co_patient_instrument", "co_patient_result",
	"co_instrument_agent", "co_instrument_ patient", "co_instrument_result",
	"co_result_agent", "co_result_patient", "co_result_instrument",
		
	"in_manner", "manner_of",

	"be_in_state", "state_of",
		
	"fuzzynym", "xpos_fuzzynym"
	};

        /*public final static String[] WN_2_0_RELATION_NAMES = {
            
        };*/
        
        /* public final static String[] EN_ADD_RELATION_NAMES = ... */
        /* public final static String[] ES_ADD_RELATION_NAMES = ... */
        /* public final static String[] DE_ADD_RELATION_NAMES = ... */
        
}
